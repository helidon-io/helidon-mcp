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

import java.util.List;

import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ServerTest
class Langchain4jMultipleToolTest {
    private static McpClient client;

    Langchain4jMultipleToolTest(WebServer server) {
        McpTransport transport = new HttpMcpTransport.Builder()
                .sseUrl("http://localhost:" + server.port())
                .logRequests(true)
                .logResponses(true)
                .build();
        client = new DefaultMcpClient.Builder()
                .transport(transport)
                .build();
    }

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        MultipleTool.setUpRoute(builder);
    }

    @AfterAll
    static void closeClient() throws Exception {
        client.close();
    }

    @Test
    void testListTools() {
        List<ToolSpecification> tools = client.listTools();
        assertThat(tools.size(), is(4));

        ToolSpecification tool1 = tools.getLast();
        assertThat(tool1.name(), is("tool1"));
        assertThat(tool1.description(), is("Tool 1"));
        assertThat(tool1.parameters().properties().isEmpty(), is(false));
    }

    @Test
    void testTool4() {
        String result = client.executeTool(ToolExecutionRequest.builder()
                                                   .name("tool4")
                                                   .arguments("{\"name\":\"Praha\", \"population\":10}")
                                                   .build());
        assertThat(result, is("Praha has a population of 10 inhabitants"));
    }
}
