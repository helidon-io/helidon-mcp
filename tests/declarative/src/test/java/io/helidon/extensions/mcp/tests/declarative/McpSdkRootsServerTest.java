/*
 * Copyright (c) 2024, 2025 Oracle and/or its affiliates.
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
package io.helidon.extensions.mcp.tests.declarative;

import java.util.List;
import java.util.Map;

import io.helidon.webserver.WebServer;
import io.helidon.webserver.testing.junit5.ServerTest;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

@ServerTest
class McpSdkRootsServerTest {
    private final McpSyncClient client;

    McpSdkRootsServerTest(WebServer server) {
        client = McpClient.sync(HttpClientSseClientTransport.builder("http://localhost:" + server.port())
                                        .sseEndpoint("/roots")
                                        .build())
                .build();
        client.initialize();
    }

    @ParameterizedTest
    @ValueSource(strings = {"tool", "tool1", "tool2", "tool3"})
    void testRootsTool(String name) {
        McpSchema.CallToolResult result = client.callTool(McpSchema.CallToolRequest.builder()
                                                                  .name(name)
                                                                  .build());
        assertThat(result.isError(), is(false));

        var contents = result.content();
        assertThat(contents.size(), is(1));
        assertThat(contents.getFirst(), instanceOf(McpSchema.TextContent.class));

        McpSchema.TextContent text = (McpSchema.TextContent) contents.getFirst();
        assertThat(text.text(), is(""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"prompt", "prompt1", "prompt2", "prompt3"})
    void testRootsPrompt(String name) {
        McpSchema.GetPromptResult result = client.getPrompt(new McpSchema.GetPromptRequest(name, Map.of()));
        List<McpSchema.PromptMessage> messages = result.messages();
        assertThat(messages.size(), is(1));

        var content = messages.getFirst().content();
        assertThat(content, instanceOf(McpSchema.TextContent.class));

        McpSchema.TextContent text = (McpSchema.TextContent) content;
        assertThat(text.text(), is(""));
    }

    @ParameterizedTest
    @ValueSource(strings = {"resource", "resource1", "resource2", "resource3"})
    void testRootsResource(String name) {
        McpSchema.ReadResourceResult result = client.readResource(new McpSchema.ReadResourceRequest("https://" + name));
        List<McpSchema.ResourceContents> contents = result.contents();
        assertThat(contents.size(), is(1));

        McpSchema.ResourceContents content = contents.getFirst();
        assertThat(content, instanceOf(McpSchema.TextResourceContents.class));

        McpSchema.TextResourceContents text = (McpSchema.TextResourceContents) content;
        assertThat(text.text(), is(""));
    }
}
