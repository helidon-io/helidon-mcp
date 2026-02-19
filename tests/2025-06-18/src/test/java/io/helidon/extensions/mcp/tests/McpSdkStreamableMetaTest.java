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

import java.util.Map;

import io.helidon.extensions.mcp.tests.common.MetaServer;
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

@ServerTest
class McpSdkStreamableMetaTest extends AbstractMcpSdkTest {
    private final McpSyncClient client;

    McpSdkStreamableMetaTest(WebServer server) {
        client = McpClient.sync(streamable(server.port())).build();
        client.initialize();
    }

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        MetaServer.setUpRoute(builder);
    }

    @Override
    McpSyncClient client() {
        return client;
    }

    @Test
    void testMetaTool() {
        McpSchema.CallToolResult result = client.callTool(McpSchema.CallToolRequest.builder()
                                .name("meta-tool")
                                .meta(Map.of("foo", "bar"))
                                .build());
        assertThat(result.content().size(), is(1));

        McpSchema.Content content = result.content().getFirst();
        assertThat(content, instanceOf(McpSchema.TextContent.class));

        McpSchema.TextContent text = (McpSchema.TextContent) content;
        assertThat(text.text(), is("bar"));
    }

    @Test
    void testMetaPrompt() {
        McpSchema.GetPromptRequest request = new McpSchema.GetPromptRequest("meta-prompt", Map.of(), Map.of("foo", "bar"));
        McpSchema.GetPromptResult result = client.getPrompt(request);
        assertThat(result.messages().size(), is(1));

        McpSchema.PromptMessage message = result.messages().getFirst();
        assertThat(message.content(), instanceOf(McpSchema.TextContent.class));

        McpSchema.TextContent text = (McpSchema.TextContent) message.content();
        assertThat(text.text(), is("bar"));
    }

    @Test
    void testMetaResource() {
        McpSchema.ReadResourceRequest request = new McpSchema.ReadResourceRequest("https://foo", Map.of("foo", "bar"));
        McpSchema.ReadResourceResult result = client.readResource(request);
        assertThat(result.contents().size(), is(1));

        McpSchema.ResourceContents content = result.contents().getFirst();
        assertThat(content, instanceOf(McpSchema.TextResourceContents.class));

        McpSchema.TextResourceContents text = (McpSchema.TextResourceContents) content;
        assertThat(text.text(), is("bar"));
    }

    @Test
    void testMetaCompletion() {
        McpSchema.CompleteRequest request = new McpSchema.CompleteRequest(new McpSchema.PromptReference("meta-completion"),
                                                                          new McpSchema.CompleteRequest.CompleteArgument("", ""),
                                                                          Map.of("foo", "bar"));
        McpSchema.CompleteResult result = client.completeCompletion(request);
        assertThat(result.completion().values().size(), is(1));
        assertThat(result.completion().values().getFirst(), is("bar"));
    }
}
