/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.helidon.extensions.mcp.server;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.helidon.json.JsonObject;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

class McpPendingResponsesTest {

    @Test
    void rejectsOverflowWithoutEvictionAndReusesDiscardedSlot() {
        McpPendingResponses responses = new McpPendingResponses(2);
        responses.prepare(1);
        responses.prepare(2);

        McpInternalException exception = assertThrows(McpInternalException.class, () -> responses.prepare(3));

        assertThat(exception.getMessage(), is("Maximum pending response count reached"));
        JsonObject firstResponse = JsonObject.builder().set("id", 1).build();
        JsonObject secondResponse = JsonObject.builder().set("id", 2).build();
        JsonObject thirdResponse = JsonObject.builder().set("id", 3).build();
        responses.accept(2, secondResponse);
        responses.accept(1, firstResponse);
        assertThat(pollAndDiscard(responses, 1, Duration.ofSeconds(1)).orElseThrow(), sameInstance(firstResponse));

        responses.prepare(3);
        responses.accept(3, thirdResponse);

        assertThat(pollAndDiscard(responses, 2, Duration.ofSeconds(1)).orElseThrow(), sameInstance(secondResponse));
        assertThat(pollAndDiscard(responses, 3, Duration.ofSeconds(1)).orElseThrow(), sameInstance(thirdResponse));
    }

    @Test
    void timeoutReclaimsCapacity() {
        McpPendingResponses responses = new McpPendingResponses(1);
        responses.prepare(1);

        assertThat(pollAndDiscard(responses, 1, Duration.ZERO).isEmpty(), is(true));

        JsonObject response = JsonObject.builder().set("id", 2).build();
        responses.prepare(2);
        responses.accept(2, response);
        assertThat(pollAndDiscard(responses, 2, Duration.ofSeconds(1)).orElseThrow(), sameInstance(response));
    }

    @Test
    void disconnectCompletesAllPendingAndRejectsNewRegistrations() throws InterruptedException {
        McpPendingResponses responses = new McpPendingResponses(2);
        responses.prepare(1);
        responses.prepare(2);
        AtomicReference<Throwable> firstFailure = new AtomicReference<>();
        AtomicReference<Throwable> secondFailure = new AtomicReference<>();
        Thread firstPoller = poll(responses, 1, firstFailure);
        Thread secondPoller = poll(responses, 2, secondFailure);
        awaitWaiting(firstPoller, secondPoller);

        responses.disconnect();
        firstPoller.join(1000);
        secondPoller.join(1000);

        assertThat(firstPoller.isAlive(), is(false));
        assertThat(secondPoller.isAlive(), is(false));
        assertDisconnected(firstFailure.get());
        assertDisconnected(secondFailure.get());
        McpInternalException exception = assertThrows(McpInternalException.class, () -> responses.prepare(3));
        assertThat(exception.getMessage(), is("Session disconnected"));
        responses.accept(1, JsonObject.builder().set("id", 1).build());
    }

    @Test
    void concurrentPrepareNeverExceedsCapacity() throws Exception {
        int capacity = 2;
        int callers = 8;
        McpPendingResponses responses = new McpPendingResponses(capacity);
        CountDownLatch ready = new CountDownLatch(callers);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger rejected = new AtomicInteger();
        Queue<Long> prepared = new ConcurrentLinkedQueue<>();
        List<Future<?>> tasks = new ArrayList<>();
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        try {
            for (long requestId = 0; requestId < callers; requestId++) {
                long id = requestId;
                tasks.add(executor.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        responses.prepare(id);
                        prepared.add(id);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(e);
                    } catch (McpInternalException e) {
                        if (!"Maximum pending response count reached".equals(e.getMessage())) {
                            throw e;
                        }
                        rejected.incrementAndGet();
                    }
                }));
            }
            assertThat(ready.await(1, TimeUnit.SECONDS), is(true));
            start.countDown();
            for (Future<?> task : tasks) {
                task.get(1, TimeUnit.SECONDS);
            }
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(1, TimeUnit.SECONDS), is(true));
        }

        assertThat(prepared.size(), is(capacity));
        assertThat(rejected.get(), is(callers - capacity));
        for (long requestId : prepared) {
            JsonObject response = JsonObject.builder().set("id", requestId).build();
            responses.accept(requestId, response);
            assertThat(pollAndDiscard(responses, requestId, Duration.ofSeconds(1)).orElseThrow(), sameInstance(response));
        }
    }

    private static Thread poll(McpPendingResponses responses,
                               long requestId,
                               AtomicReference<Throwable> failure) {
        return Thread.ofVirtual().start(() -> {
            try {
                pollAndDiscard(responses, requestId, Duration.ofSeconds(5));
            } catch (Throwable e) {
                failure.set(e);
            }
        });
    }

    private static Optional<JsonObject> pollAndDiscard(McpPendingResponses responses,
                                                       long requestId,
                                                       Duration timeout) {
        try {
            return responses.poll(requestId, timeout);
        } finally {
            responses.discard(requestId);
        }
    }

    private static void awaitWaiting(Thread... threads) {
        assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
            while (!allWaiting(threads)) {
                Thread.onSpinWait();
            }
        });
    }

    private static boolean allWaiting(Thread... threads) {
        for (Thread thread : threads) {
            if (thread.getState() != Thread.State.WAITING
                    && thread.getState() != Thread.State.TIMED_WAITING) {
                return false;
            }
        }
        return true;
    }

    private static void assertDisconnected(Throwable failure) {
        assertThat(failure, instanceOf(McpInternalException.class));
        assertThat(failure.getMessage(), is("Session disconnected"));
    }
}
