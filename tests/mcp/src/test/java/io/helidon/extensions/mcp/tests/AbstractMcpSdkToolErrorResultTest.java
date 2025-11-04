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

import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

abstract class AbstractMcpSdkToolErrorResultTest extends AbstractMcpSdkTest {

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        ToolErrorResultServer.setUpRoute(builder);
    }

    @ParameterizedTest
    @ValueSource(strings = {"failing-tool", "failing-tool-1"})
    void testFailingToolResult(String name) {
        var result = client().callTool(McpSchema.CallToolRequest.builder()
                                               .name(name)
                                               .build());
        assertThat(result.isError(), is(true));

        var content = result.content().getFirst();
        assertThat(content, instanceOf(McpSchema.TextContent.class));

        McpSchema.TextContent text = (McpSchema.TextContent) content;
        assertThat(text.text(), is("Tool error message"));
    }

    @Test
    void testMultipleMessageError() {
        var result = client().callTool(McpSchema.CallToolRequest.builder()
                                               .name("failing-tool-2")
                                               .build());
        assertThat(result.isError(), is(true));
        assertThat(result.content().size(), is(2));

        var content = result.content().getFirst();
        assertThat(content, instanceOf(McpSchema.TextContent.class));

        McpSchema.TextContent text = (McpSchema.TextContent) content;
        assertThat(text.text(), is("Tool error message"));

        content = result.content().get(1);
        assertThat(content, instanceOf(McpSchema.TextContent.class));

        text = (McpSchema.TextContent) content;
        assertThat(text.text(), is("Second error message"));
    }
}
