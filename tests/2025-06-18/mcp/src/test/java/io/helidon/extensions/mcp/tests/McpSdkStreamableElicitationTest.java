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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

@ServerTest
class McpSdkStreamableElicitationTest extends AbstractMcpSdkTest {
    private final McpSyncClient client;
    /**
     * Verifies that the elicitation handler is called.
     */
    private final CountDownLatch latch = new CountDownLatch(1);

    McpSdkStreamableElicitationTest(WebServer server) {
        client = McpClient.sync(streamable(server.port()))
                .capabilities(McpSchema.ClientCapabilities.builder()
                                      .roots(true)
                                      .elicitation()
                                      .build())
                .elicitation(this::elicitationHandler)
                .build();
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

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        ElicitationServer.setUpRoute(builder);
    }

    @Test
    void testElicitation() {
        McpSchema.CallToolResult result = client.callTool(McpSchema.CallToolRequest.builder()
                                                                  .name("elicitation-tool")
                                                                  .build());
        List<McpSchema.Content> contents = result.content();
        assertThat(contents.size(), is(2));

        McpSchema.Content text = contents.getFirst();
        assertThat(text, instanceOf(McpSchema.TextContent.class));

        McpSchema.TextContent textContent = (McpSchema.TextContent) text;
        assertThat(textContent.text(), containsString("ACCEPT"));

        McpSchema.Content text1 =  contents.getLast();
        assertThat(text1, instanceOf(McpSchema.TextContent.class));

        McpSchema.TextContent textContent2 =  (McpSchema.TextContent) text1;
        assertThat(textContent2.text(), containsString("bar"));
    }

    @Test
    void testElicitationException() {
        try {
            McpSchema.CallToolResult result = client.callTool(McpSchema.CallToolRequest.builder()
                                                                      .name("elicitation-exception")
                                                                      .build());
            assertThat("Test must throw an exception", true, is(false));
        } catch (McpError e) {
            assertThat(e.getMessage(), is("Elicitation exception"));
        } finally {
            // unblock the test
            latch.countDown();
        }
    }

    @Test
    void testElicitationTimeout() {
        McpSchema.CallToolResult result = client.callTool(McpSchema.CallToolRequest.builder()
                                               .name("elicitation-timeout")
                                               .build());
        assertThat(result.isError(), is(true));

        List<McpSchema.Content> contents = result.content();
        assertThat(result.content().size(), is(1));

        McpSchema.Content content = contents.getFirst();
        assertThat(content, instanceOf(McpSchema.TextContent.class));

        McpSchema.TextContent textContent = (McpSchema.TextContent) content;
        assertThat(textContent.text(), is("response timeout"));
    }

    private McpSchema.ElicitResult elicitationHandler(McpSchema.ElicitRequest request) {
        latch.countDown();
        if ("timeout".equals(request.message())) {
            try {
                TimeUnit.SECONDS.sleep(3);
                return McpSchema.ElicitResult.builder()
                        .message(McpSchema.ElicitResult.Action.ACCEPT)
                        .content(Map.of("foo", "timeout"))
                        .build();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        assertThat(request.message() , is("foo"));
        assertThat(request.requestedSchema().toString(), containsString("foo"));
        return McpSchema.ElicitResult.builder()
                .message(McpSchema.ElicitResult.Action.ACCEPT)
                .content(Map.of("foo", "bar"))
                .build();
    }

}
