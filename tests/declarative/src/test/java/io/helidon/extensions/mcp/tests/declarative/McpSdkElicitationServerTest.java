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
package io.helidon.extensions.mcp.tests.declarative;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import io.helidon.webserver.WebServer;
import io.helidon.webserver.testing.junit5.ServerTest;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

@ServerTest
class McpSdkElicitationServerTest {
    private static McpSyncClient client;
    private CountDownLatch latch;

    McpSdkElicitationServerTest(WebServer server) {
        client = McpClient.sync(HttpClientStreamableHttpTransport.builder("http://localhost:" + server.port())
                                        .endpoint("/elicitation")
                                        .build())
                .requestTimeout(Duration.ofSeconds(10))
                .build();
        client.initialize();
    }

    @AfterAll
    static void afterAll() {
        if (client != null) {
            client.close();
        }
    }

    @Test
    void testElicitationTool() {
        McpSchema.CallToolResult result = client.callTool(McpSchema.CallToolRequest.builder()
                                                                   .name("elicitationTool")
                                                                   .build());
        assertThat(result.content().size(), is(1));

        var tool = result.content().getFirst();
        assertThat(tool.type(), is("text"));
        assertThat(((McpSchema.TextContent) tool).text(), is("foo"));
    }

    @Test
    void testElicitationPrompt() {
        McpSchema.GetPromptResult result = client.getPrompt(new McpSchema.GetPromptRequest("elicitationPrompt", Map.of()));

        assertThat(result.messages().size(), is(1));

        var prompt = result.messages().getFirst();
        assertThat(prompt.content().type(), is("text"));
        assertThat(((McpSchema.TextContent) prompt.content()).text(), is("foo"));
    }

    @Test
    void testElicitationResource() {
        McpSchema.ReadResourceResult result = client.readResource(new McpSchema.ReadResourceRequest("https://foo", Map.of()));
        assertThat(result.contents().size(), is(1));

        McpSchema.ResourceContents resource = result.contents().getFirst();
        assertThat(resource, instanceOf(McpSchema.TextResourceContents.class));
        assertThat(((McpSchema.TextResourceContents) resource).text(), is("foo"));
    }
}
