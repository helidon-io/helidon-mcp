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

import io.helidon.common.media.type.MediaTypes;
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
class AnthropicMultipleToolTest {
    private static McpSyncClient client;

    AnthropicMultipleToolTest(WebServer server) {
        client = McpClient.sync(HttpClientSseClientTransport.builder("http://localhost:" + server.port())
                                        .sseEndpoint("/")
                                        .build())
                .build();
        client.initialize();
    }

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        MultipleTool.setUpRoute(builder);
    }

    @AfterAll
    static void closeClient() {
        client.close();
    }

    @Test
    void testListTools() {
        McpSchema.ListToolsResult result = client.listTools();
        assertThat(result.tools().size(), is(4));
    }

    @Test
    void testTool1() {
        McpSchema.CallToolResult tool1 = client.callTool(new McpSchema.CallToolRequest("tool1", Map.of()));
        assertThat(tool1.content().size(), is(1));

        McpSchema.Content content = tool1.content().getFirst();
        assertThat(content.type(), is("image"));

        McpSchema.ImageContent image = (McpSchema.ImageContent) content;
        assertThat(image.data(), is("binary"));
        assertThat(image.mimeType(), is(MediaTypes.APPLICATION_OCTET_STREAM_VALUE));
    }

    @Test
    void testTool2() {
        McpSchema.CallToolResult tool2 = client.callTool(new McpSchema.CallToolRequest("tool2", Map.of()));
        assertThat(tool2.content().size(), is(1));

        McpSchema.Content first = tool2.content().getFirst();
        assertThat(first.type(), is("resource"));

        var resource = (McpSchema.EmbeddedResource) first;
        var text = (McpSchema.TextResourceContents) resource.resource();
        assertThat(text.text(), is("resource"));
        assertThat(text.uri(), is("http://resource"));
        assertThat(text.mimeType(), is(MediaTypes.TEXT_PLAIN_VALUE));
    }

    @Test
    void testTool3() {
        McpSchema.CallToolResult tool3 = client.callTool(new McpSchema.CallToolRequest("tool3", Map.of()));
        assertThat(tool3.content().size(), is(3));

        McpSchema.Content first = tool3.content().getFirst();
        McpSchema.Content second = tool3.content().get(1);
        McpSchema.Content third = tool3.content().get(2);
        assertThat(first.type(), is("image"));
        assertThat(second.type(), is("resource"));
        assertThat(third.type(), is("text"));

        McpSchema.ImageContent image = (McpSchema.ImageContent) first;
        assertThat(image.mimeType(), is(MediaTypes.APPLICATION_OCTET_STREAM_VALUE));
        assertThat(image.data(), is("binary"));

        McpSchema.EmbeddedResource resource = (McpSchema.EmbeddedResource) second;
        assertThat(resource.resource().uri(), is("http://resource"));
        assertThat(resource.resource().mimeType(), is(MediaTypes.TEXT_PLAIN_VALUE));

        McpSchema.TextContent text = (McpSchema.TextContent) third;
        assertThat(text.text(), is("text"));
    }

    @Test
    void testTool4() {
        McpSchema.CallToolResult tool4 = client.callTool(
                new McpSchema.CallToolRequest("tool4", Map.of("name", "Paris", "population", 10)));
        assertThat(tool4.content().size(), is(1));

        McpSchema.TextContent text = (McpSchema.TextContent) tool4.content().getFirst();
        assertThat(text.text(), is("Paris has a population of 10 inhabitants"));
    }
}
