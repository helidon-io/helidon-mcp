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
package io.helidon.extensions.mcp.tests;

import java.util.List;
import java.util.Map;

import io.helidon.extensions.mcp.tests.common.OutputSchemaTools;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@ServerTest
class McpSdkStreamableOutputSchemaToolsTest extends AbstractMcpSdkTest {
    private final McpSyncClient client;

    McpSdkStreamableOutputSchemaToolsTest(WebServer server) {
        client = McpClient.sync(streamable(server.port())).build();
        client.initialize();
    }

    @Override
    McpSyncClient client() {
        return client;
    }

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        OutputSchemaTools.setUpRoute(builder);
    }

    @Test
    void listTools() {
        McpSchema.ListToolsResult list = client.listTools();
        List<McpSchema.Tool> tools = list.tools();
        assertThat(tools.size(), is(4));

        McpSchema.Tool tool = tools.getFirst();
        assertThat(tool.name(), is("empty-output-schema"));
        assertThat(tool.description(), is("Tool with an empty output schema"));
        assertThat(tool.outputSchema().size(), is(2));
        assertThat(tool.inputSchema().properties().size(), is(0));

        McpSchema.Tool tool1 = tools.get(1);
        assertThat(tool1.name(), is("valid-output-schema"));
        assertThat(tool1.description(), is("Tool with a valid output schema"));
        assertThat(tool1.outputSchema().size(), is(3));
        assertThat(tool1.inputSchema().properties().size(), is(1));

        McpSchema.Tool tool2 = tools.get(2);
        assertThat(tool2.name(), is("invalid-output-schema"));
        assertThat(tool2.description(), is("""
                    Tool should return a structured content. This scenario work
                    until schema validation is implemented.
                    """));
        assertThat(tool2.outputSchema().size(), is(3));
        assertThat(tool2.inputSchema().properties().size(), is(0));

        McpSchema.Tool tool3 = tools.get(3);
        assertThat(tool3.name(), is("output-schema-and-content"));
        assertThat(tool3.description(), is("Tool returns text content and structured content"));
        assertThat(tool3.outputSchema().size(), is(3));
        assertThat(tool3.inputSchema().properties().size(), is(0));
    }

    @Test
    void testEmptyOutputSchema() {
        McpSchema.CallToolRequest request = McpSchema.CallToolRequest.builder().name("empty-output-schema").build();
        McpSchema.CallToolResult result = client.callTool(request);
        assertThat(result.isError(), is(false));
        assertThat(result.content().size(), is(0));
        assertThat(result.structuredContent(), is(nullValue()));
    }

    @Test
    void testInvalidOutputSchema() {
        McpSchema.CallToolRequest request = McpSchema.CallToolRequest.builder().name("invalid-output-schema").build();
        McpSchema.CallToolResult result = client.callTool(request);
        assertThat(result.isError(), is(false));
        assertThat(result.content().size(), is(0));
        assertThat(result.structuredContent(), is(nullValue()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testValidOutputSchema() {
        McpSchema.CallToolRequest request = McpSchema.CallToolRequest.builder()
                .name("valid-output-schema")
                .arguments(Map.of("foo", "bar"))
                .build();
        McpSchema.CallToolResult result = client.callTool(request);
        assertThat(result.isError(), is(false));
        assertThat(result.content().size(), is(1));

        McpSchema.Content content = result.content().getFirst();
        assertThat(content, instanceOf(McpSchema.TextContent.class));

        McpSchema.TextContent textContent = (McpSchema.TextContent) content;
        assertThat(textContent.text(), is("{\"foo\":\"bar\"}"));

        Object structuredContent = result.structuredContent();
        assertThat(structuredContent, is(notNullValue()));
        assertThat(structuredContent, instanceOf(Map.class));
        Map<String, String> map = (Map<String, String>) structuredContent;
        assertThat(map.get("foo"), is("bar"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testOutputSchemaAndContent() {
        McpSchema.CallToolRequest request = McpSchema.CallToolRequest.builder().name("output-schema-and-content").build();
        McpSchema.CallToolResult result = client.callTool(request);
        assertThat(result.isError(), is(false));
        assertThat(result.content().size(), is(1));

        McpSchema.Content content = result.content().getFirst();
        assertThat(content, instanceOf(McpSchema.TextContent.class));

        McpSchema.TextContent textContent = (McpSchema.TextContent) content;
        assertThat(textContent.text(), is("content"));

        Object structuredContent = result.structuredContent();
        assertThat(structuredContent, is(notNullValue()));
        assertThat(structuredContent, instanceOf(Map.class));
        Map<String, String> map = (Map<String, String>) structuredContent;
        assertThat(map.get("foo"), is("bar"));
    }
}
