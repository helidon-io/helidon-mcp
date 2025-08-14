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

package io.helidon.mcp.tests;

import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.logging.McpLogLevel;
import dev.langchain4j.mcp.client.logging.McpLogMessage;
import dev.langchain4j.mcp.client.logging.McpLogMessageHandler;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ServerTest
class Langchain4jLoggingTest {
    private static McpClient client;

    Langchain4jLoggingTest(WebServer server) {
        McpTransport transport = new HttpMcpTransport.Builder()
                .sseUrl("http://localhost:" + server.port())
                .logRequests(true)
                .logResponses(true)
                .build();

        client = new DefaultMcpClient.Builder()
                .transport(transport)
                .logHandler(new MyLogMessageHandler())
                .build();
    }

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        LoggingNotifications.setUpRoute(builder);
    }

    @AfterAll
    static void closeClient() throws Exception {
        client.close();
    }

    @Test
    void testLogging() {
        client.executeTool(ToolExecutionRequest.builder().name("logging").build());
    }

    private static class MyLogMessageHandler implements McpLogMessageHandler {
        @Override
        public void handleLogMessage(McpLogMessage message) {
            assertThat(message.level(), is(McpLogLevel.INFO));
            assertThat(message.logger(), is("helidon-logger"));
            assertThat(message.data().asText(), is("Logging data"));
        }
    }
}
