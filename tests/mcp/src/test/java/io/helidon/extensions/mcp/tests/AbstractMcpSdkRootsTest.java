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

import java.util.List;

import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

abstract class AbstractMcpSdkRootsTest extends AbstractMcpSdkTest {
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
        McpSchema.CallToolResult result = client().callTool(McpSchema.CallToolRequest.builder()
                                                                    .name("roots-name-tool")
                                                                    .build());
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
        McpSchema.CallToolResult result = client().callTool(McpSchema.CallToolRequest.builder()
                                                                    .name("roots-uri-tool")
                                                                    .build());
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
        McpSchema.CallToolResult result = client().callTool(McpSchema.CallToolRequest.builder()
                                                                    .name("roots-name-tool")
                                                                    .build());
        assertThat(result.isError(), is(false));

        client().addRoot(new McpSchema.Root("file://file.txt", "file"));
        client().rootsListChangedNotification();

        result = client().callTool(McpSchema.CallToolRequest.builder()
                                           .name("roots-name-tool")
                                           .build());
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
