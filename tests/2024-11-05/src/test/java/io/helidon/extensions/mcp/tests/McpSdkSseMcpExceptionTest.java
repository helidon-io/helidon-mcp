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

import java.util.Map;

import io.helidon.extensions.mcp.tests.common.McpExceptionServer;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@ServerTest
class McpSdkSseMcpExceptionTest extends AbstractMcpSdkTest {
    private final McpSyncClient client;

    McpSdkSseMcpExceptionTest(WebServer server) {
        this.client = McpClient.sync(sse(server.port())).build();
        client.initialize();
    }

    @Override
    McpSyncClient client() {
        return client;
    }

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        McpExceptionServer.setUpRoute(builder);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "error-tool",
            "error-tool-switch-transport"
    })
    void testErrorTool(String name) {
        try {
            var request = new McpSchema.CallToolRequest(name, Map.of());
            var result = client().callTool(request);
            assertThat("Tool execution must throw an exception", true, is(false));
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Tool error message"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "error-prompt",
            "error-prompt-switch-transport"
    })
    void testErrorPrompt(String name) {
        try {
            var request = new McpSchema.GetPromptRequest(name, Map.of());
            var result = client().getPrompt(request);
            assertThat("Prompt execution must throw an exception", true, is(false));
        } catch (McpError e) {
            assertThat(e.getMessage(), containsString("Prompt error message"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "error-resource",
            "error-resource-switch-transport"
    })
    void testErrorResource(String uri) {
        try {
            var request = new McpSchema.ReadResourceRequest(uri);
            client().readResource(request);
            assertThat("Resource execution must throw an exception", true, is(false));
        } catch (McpError e) {
            assertThat(e.getMessage(), containsString("Resource error message"));
        }
    }
}
