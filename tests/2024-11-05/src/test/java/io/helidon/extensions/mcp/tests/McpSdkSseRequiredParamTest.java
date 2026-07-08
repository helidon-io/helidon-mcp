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
class McpSdkSseRequiredParamTest extends AbstractMcpSdkTest {
    private final McpSyncClient client;

    McpSdkSseRequiredParamTest(WebServer server) {
        client = McpClient.sync(sse(server.port())).build();
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
        var request = new McpSchema.CallToolRequest("single-required-param-tool", Map.of());
        var result = client().callTool(request);
        assertThat(result.isError(), is(true));

        var content = result.content().getFirst();
        assertThat(content, instanceOf(McpSchema.TextContent.class));

        McpSchema.TextContent text = (McpSchema.TextContent) content;
        assertThat(text.text(), is("Missing required parameter: mandatory"));
    }

    @Test
    void testMissingMultipleRequiredParams() {
        var request = new McpSchema.CallToolRequest("multiple-required-params-tool", Map.of());
        var result = client().callTool(request);
        assertThat(result.isError(), is(true));

        var content = result.content().getFirst();
        assertThat(content, instanceOf(McpSchema.TextContent.class));

        McpSchema.TextContent text = (McpSchema.TextContent) content;
        assertThat(text.text(), is("Missing required parameters: a, b"));
    }

    @Test
    void testRequiredParamsProvided() {
        var request = new McpSchema.CallToolRequest("multiple-required-params-tool", Map.<String, Object>of("a", "x", "b", 42));
        var result = client().callTool(request);
        assertThat(result.isError(), is(false));

        var content = result.content().getFirst();
        assertThat(content, instanceOf(McpSchema.TextContent.class));

        McpSchema.TextContent text = (McpSchema.TextContent) content;
        assertThat(text.text(), is("a=x|b=42"));
    }
}
