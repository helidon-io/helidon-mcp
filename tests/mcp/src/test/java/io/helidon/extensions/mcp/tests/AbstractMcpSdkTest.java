/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
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

package io.helidon.extensions.mcp.tests;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.helidon.faulttolerance.Async;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import org.junit.jupiter.api.AfterEach;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

abstract class AbstractMcpSdkTest {

    abstract McpSyncClient client();

    McpClientTransport streamable(int port) {
        return streamable(port, "/");
    }

    McpClientTransport streamable(int port, String endpoint) {
        return HttpClientStreamableHttpTransport.builder("http://localhost:" + port)
                .endpoint(endpoint)
                .build();
    }

    McpClientTransport sse(int port) {
        return sse(port, "/");
    }

    McpClientTransport sse(int port, String endpoint) {
        return HttpClientSseClientTransport.builder("http://localhost:" + port)
                .sseEndpoint(endpoint)
                .build();
    }

    CountDownLatch latch() {
        return new CountDownLatch(0);
    }

    @AfterEach
    void afterEach() throws InterruptedException {
        assertThat(latch().await(20, TimeUnit.SECONDS), is(true));
    }

    void async(Runnable runnable) {
        Async.create().invoke(() -> {
            runnable.run();
            return null;
        });
    }
}
