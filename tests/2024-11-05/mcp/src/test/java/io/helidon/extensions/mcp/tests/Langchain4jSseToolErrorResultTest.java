/*
 * Copyright (c) 2025, 2026 Oracle and/or its affiliates.
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

import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.exception.ToolExecutionException;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@ServerTest
class Langchain4jSseToolErrorResultTest {
    private static McpClient client;

    Langchain4jSseToolErrorResultTest(WebServer server) {
        McpTransport transport = new HttpMcpTransport.Builder()
                .sseUrl("http://localhost:" + server.port())
                .logRequests(true)
                .logResponses(true)
                .build();
        client = new DefaultMcpClient.Builder()
                .protocolVersion("2024-11-05")
                .transport(transport)
                .build();
    }

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        ToolErrorResultServer.setUpRoute(builder);
    }

    @AfterAll
    static void closeClient() throws Exception {
        client.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"failing-tool", "failing-tool-1"})
    void testFailingToolResult(String name) {
        try {
            var result = client.executeTool(ToolExecutionRequest.builder()
                                                    .name(name)
                                                    .build());
        } catch (ToolExecutionException e) {
            assertThat(e.getMessage(), is("Tool error message"));
        }
    }

    @Test
    void testMultipleErrorMessages() {
        try {
            var result = client.executeTool(ToolExecutionRequest.builder()
                                                    .name("failing-tool-2")
                                                    .build());
        } catch (ToolExecutionException e) {
            assertThat(e.getMessage(), is("Tool error message\nSecond error message"));
        }
    }
}
