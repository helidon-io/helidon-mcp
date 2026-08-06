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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.helidon.common.LruCache;
import io.helidon.json.JsonObject;

/**
 * Pending JSON-RPC responses for an MCP session.
 */
final class McpPendingResponses {
    private final Lock lock = new ReentrantLock();
    private final LruCache<Long, CompletableFuture<JsonObject>> responses;
    private final List<CompletableFuture<JsonObject>> responseFutures = new ArrayList<>();

    private boolean active = true;

    McpPendingResponses(int capacity) {
        responses = LruCache.create(capacity);
    }

    void prepare(long requestId) {
        lock.lock();
        try {
            if (!active) {
                throw new McpInternalException("Session disconnected");
            }
            if (responses.get(requestId).isPresent()) {
                return;
            }
            if (responses.size() >= responses.capacity()) {
                throw new McpInternalException("Maximum pending response count reached");
            }
            CompletableFuture<JsonObject> response = new CompletableFuture<>();
            responses.put(requestId, response);
            responseFutures.add(response);
        } finally {
            lock.unlock();
        }
    }

    void accept(long requestId, JsonObject response) {
        Optional<CompletableFuture<JsonObject>> pendingResponse;
        lock.lock();
        try {
            if (!active) {
                return;
            }
            pendingResponse = responses.get(requestId);
        } finally {
            lock.unlock();
        }
        pendingResponse.ifPresent(future -> future.complete(response));
    }

    Optional<JsonObject> poll(long requestId, Duration timeout) {
        CompletableFuture<JsonObject> pendingResponse;
        lock.lock();
        try {
            if (!active) {
                throw new McpInternalException("Session disconnected");
            }
            pendingResponse = responses.get(requestId)
                    .orElseThrow(() -> new McpInternalException("No pending response for request id " + requestId));
        } finally {
            lock.unlock();
        }
        try {
            return Optional.of(pendingResponse.get(timeout.toMillis(), TimeUnit.MILLISECONDS));
        } catch (TimeoutException e) {
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new McpInternalException("Session interrupted.", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new McpInternalException("Unable to receive session response", cause);
        } finally {
            discard(requestId);
        }
    }

    void discard(long requestId) {
        lock.lock();
        try {
            responses.remove(requestId).ifPresent(responseFutures::remove);
        } finally {
            lock.unlock();
        }
    }

    void disconnect() {
        List<CompletableFuture<JsonObject>> pending;
        lock.lock();
        try {
            if (!active) {
                return;
            }
            active = false;
            pending = List.copyOf(responseFutures);
            responseFutures.clear();
            responses.clear();
        } finally {
            lock.unlock();
        }
        var exception = new McpInternalException("Session disconnected");
        pending.forEach(response -> response.completeExceptionally(exception));
    }
}
