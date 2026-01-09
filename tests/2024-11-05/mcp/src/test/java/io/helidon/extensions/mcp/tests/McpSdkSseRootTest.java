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

import java.util.List;
import java.util.Map;

import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

@ServerTest
class McpSdkSseRootTest extends AbstractMcpSdkTest {
    private final McpSyncClient client;

    McpSdkSseRootTest(WebServer server) {
        client = McpClient.sync(sse(server.port()))
                .capabilities(new McpSchema.ClientCapabilities(null,
                                                               new McpSchema.ClientCapabilities.RootCapabilities(true),
                                                               null,
                                                               null))
                .roots(roots())
                .build();
        client.initialize();
    }

    @Override
    McpSyncClient client() {
        return client;
    }

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        RootsServer.setUpRoute(builder);
    }

    static List<McpSchema.Root> roots() {
        return List.of(new McpSchema.Root("file://foo.txt", "foo"),
                       new McpSchema.Root("file://bar.txt", "bar"));
    }

    @Test
    void testRootNameTool() {
        var request = new McpSchema.CallToolRequest("roots-name-tool", Map.of());
        McpSchema.CallToolResult result = client().callTool(request);
        assertThat(result.isError(), is(false));

        List<McpSchema.Content> contents = result.content();
        assertThat(contents.size(), is(2));

        List<String> names = contents.stream()
                .filter(content -> content instanceof McpSchema.TextContent)
                .map(McpSchema.TextContent.class::cast)
                .map(McpSchema.TextContent::text)
                .toList();
        assertThat(names.size(), is(2));
        assertThat(names, containsInAnyOrder("foo", "bar"));
    }

    @Test
    void testRootUriTool() {
        var request = new McpSchema.CallToolRequest("roots-uri-tool", Map.of());
        McpSchema.CallToolResult result = client().callTool(request);
        assertThat(result.isError(), is(false));

        List<McpSchema.Content> contents = result.content();
        assertThat(contents.size(), is(2));

        List<String> names = contents.stream()
                .filter(content -> content instanceof McpSchema.TextContent)
                .map(McpSchema.TextContent.class::cast)
                .map(McpSchema.TextContent::text)
                .toList();
        assertThat(names.size(), is(2));
        assertThat(names, containsInAnyOrder("file://foo.txt", "file://bar.txt"));
    }

    @Test
    void testRootUpdate() {
        var request = new McpSchema.CallToolRequest("roots-name-tool", Map.of());
        McpSchema.CallToolResult result = client().callTool(request);
        assertThat(result.isError(), is(false));

        client().addRoot(new McpSchema.Root("file://file.txt", "file"));
        client().rootsListChangedNotification();

        request = new McpSchema.CallToolRequest("roots-name-tool", Map.of());
        result = client().callTool(request);
        List<McpSchema.Content> contents = result.content();
        assertThat(contents.size(), is(3));

        List<String> names = contents.stream()
                .filter(content -> content instanceof McpSchema.TextContent)
                .map(McpSchema.TextContent.class::cast)
                .map(McpSchema.TextContent::text)
                .toList();
        assertThat(names.size(), is(3));
        assertThat(names, containsInAnyOrder("foo", "bar", "file"));
    }
}
