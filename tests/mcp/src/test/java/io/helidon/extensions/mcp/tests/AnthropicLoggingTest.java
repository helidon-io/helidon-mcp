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

import java.util.Map;

import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ServerTest
class AnthropicLoggingTest {
    private static McpSyncClient client;

    AnthropicLoggingTest(WebServer server) {
        client = McpClient.sync(HttpClientSseClientTransport.builder("http://localhost:" + server.port())
                                        .sseEndpoint("/")
                                        .build())
                .loggingConsumer(notification -> {
                    assertThat(notification.level(), is(McpSchema.LoggingLevel.INFO));
                    assertThat(notification.logger(), is("helidon-logger"));
                    assertThat(notification.data(), is("Logging data"));
                })
                .build();
        client.initialize();
    }

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        LoggingNotifications.setUpRoute(builder);
    }

    @AfterAll
    static void closeClient() {
        client.close();
    }

    @Test
    void testAnthropicProgress() {
        client.callTool(new McpSchema.CallToolRequest("logging", Map.of("question", "")));
    }
}
