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

import io.helidon.webserver.WebServer;
import io.helidon.webserver.testing.junit5.ServerTest;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;

@ServerTest
class McpSdkSseLoggingTest extends AbstractMcpSdkLoggingTest {

    private final McpSyncClient client;
    private final CountDownLatch latch;

    McpSdkSseLoggingTest(WebServer server) {
        this.client = McpClient.sync(sse(server.port()))
                .loggingConsumer(new LoggingConsumer(McpSchema.LoggingLevel.INFO))
                .build();
        this.latch = new CountDownLatch(1);
        client.initialize();
    }

    @Override
    McpSyncClient client() {
        return client;
    }

    @Override
    CountDownLatch latch() {
        return latch;
    }
}
