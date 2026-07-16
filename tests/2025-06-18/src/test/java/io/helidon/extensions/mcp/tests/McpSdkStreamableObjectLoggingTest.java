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

import io.helidon.extensions.mcp.tests.common.LoggingNotifications;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ServerTest
class McpSdkStreamableObjectLoggingTest extends AbstractMcpSdkTest {

    private final McpSyncClient client;

    McpSdkStreamableObjectLoggingTest(WebServer server) {
        this.client = McpClient.sync(streamable(server.port()))
                .build();
        client.initialize();
    }

    @Override
    McpSyncClient client() {
        return client;
    }

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        LoggingNotifications.setUpRoute(builder);
    }

    @Test
    void testMcpSdkObjectLoggingData() {
        assertToolResult(client().callTool(new McpSchema.CallToolRequest("object-logging", Map.of())));
    }

    @Test
    void testMcpSdkClassLoggingData() {
        assertToolResult(client().callTool(new McpSchema.CallToolRequest("class-logging", Map.of())));
    }

    private static void assertToolResult(McpSchema.CallToolResult result) {
        assertThat(result.content().size(), is(1));

        McpSchema.Content content = result.content().getFirst();
        assertThat(content.type(), is("text"));

        McpSchema.TextContent text = (McpSchema.TextContent) content;
        assertThat(text.text(), is("Dummy text"));
    }
}
