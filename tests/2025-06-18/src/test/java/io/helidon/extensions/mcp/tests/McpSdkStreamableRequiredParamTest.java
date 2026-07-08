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

import io.helidon.extensions.mcp.tests.common.RequiredParamServer;
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
class McpSdkStreamableRequiredParamTest extends AbstractMcpSdkTest {
    private final McpSyncClient client;

    McpSdkStreamableRequiredParamTest(WebServer server) {
        client = McpClient.sync(streamable(server.port())).build();
        client.initialize();
    }

    @Override
    McpSyncClient client() {
        return client;
    }

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        RequiredParamServer.setUpRoute(builder);
    }

    @Test
    void testMissingSingleRequiredParam() {
        var result = client().callTool(McpSchema.CallToolRequest.builder()
                                               .name("single-required-param-tool")
                                               .build());
        assertThat(result.isError(), is(true));

        var content = result.content().getFirst();
        assertThat(content, instanceOf(McpSchema.TextContent.class));

        McpSchema.TextContent text = (McpSchema.TextContent) content;
        assertThat(text.text(), is("Missing required parameter: mandatory"));
    }

    @Test
    void testMissingMultipleRequiredParams() {
        var result = client().callTool(McpSchema.CallToolRequest.builder()
                                               .name("multiple-required-params-tool")
                                               .build());
        assertThat(result.isError(), is(true));

        var content = result.content().getFirst();
        assertThat(content, instanceOf(McpSchema.TextContent.class));

        McpSchema.TextContent text = (McpSchema.TextContent) content;
        assertThat(text.text(), is("Missing required parameters: a, b"));
    }

    @Test
    void testRequiredParamsProvided() {
        var result = client().callTool(McpSchema.CallToolRequest.builder()
                                               .name("multiple-required-params-tool")
                                               .arguments(Map.<String, Object>of("a", "x", "b", 42))
                                               .build());
        assertThat(result.isError(), is(false));

        var content = result.content().getFirst();
        assertThat(content, instanceOf(McpSchema.TextContent.class));

        McpSchema.TextContent text = (McpSchema.TextContent) content;
        assertThat(text.text(), is("a=x|b=42"));
    }
}
