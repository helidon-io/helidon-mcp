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

import io.helidon.extensions.mcp.tests.common.ProtocolVersion;
import io.helidon.http.HeaderName;
import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.json.JsonObject;
import jakarta.json.spi.JsonProvider;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ServerTest
class Mcp20251125NegotiatedVersionTest {
    private static final JsonProvider JSON_PROVIDER = JsonProvider.provider();
    private static final HeaderName SESSION_ID_HEADER = HeaderNames.create("Mcp-Session-Id");
    private static final HeaderName MCP_PROTOCOL_VERSION = HeaderNames.create("Mcp-Protocol-Version");

    private final Http1Client client;
    private final int port;

    Mcp20251125NegotiatedVersionTest(WebServer server) {
        this.port = server.port();
        this.client = Http1Client.builder()
                .baseUri("http://localhost:" + port)
                .build();
    }

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        ProtocolVersion.setUpRoute(builder);
    }

    @Test
    void testMcp20251125Version() {
        JsonObject initRequest = JSON_PROVIDER.createObjectBuilder()
                .add("jsonrpc", "2.0")
                .add("id", 1)
                .add("method", "initialize")
                .add("params", JSON_PROVIDER.createObjectBuilder()
                        .add("protocolVersion", "2025-11-25")
                        .add("capabilities", JSON_PROVIDER.createObjectBuilder()
                                .add("roots", JSON_PROVIDER.createObjectBuilder()
                                        .add("listChanged", true)))
                        .add("clientInfo", JSON_PROVIDER.createObjectBuilder()
                                .add("name", "Example Client Display Name")
                                .add("version", "1.0.0")))
                .build();
        String sessionId;
        try (var response = client.post().submit(initRequest)) {
            sessionId = response.headers().get(SESSION_ID_HEADER).get();
            JsonObject result = response.entity().as(JsonObject.class).getJsonObject("result");
            assertThat(result.getString("protocolVersion"), is("2025-11-25"));
        }

        JsonObject listTools = JSON_PROVIDER.createObjectBuilder()
                .add("jsonrpc", "2.0")
                .add("id", 2)
                .add("method", "tools/list")
                .build();
        try (var response = client.post()
                .header(SESSION_ID_HEADER, sessionId)
                .header(MCP_PROTOCOL_VERSION, "2025-11-25")
                .submit(listTools)) {
            assertThat(response.status(), is(Status.OK_200));
        }
    }

    @Test
    void testMcpSdk20251125Version() {
        try (McpSyncClient mcpClient = io.modelcontextprotocol.client.McpClient
                .sync(HttpClientStreamableHttpTransport.builder("http://localhost:" + port)
                              .endpoint("/")
                              .build())
                .build()) {
            McpSchema.InitializeResult init = mcpClient.initialize();
            assertThat(init.protocolVersion(), is("2025-11-25"));

            McpSchema.CallToolResult result = mcpClient.callTool(
                    new McpSchema.CallToolRequest("protocolVersion", Map.of()));
            String protocolVersion = ((McpSchema.TextContent) result.content().getFirst()).text();
            assertThat(protocolVersion, is("2025-11-25"));
        }
    }

    @Test
    void testLangchain4j20251125Version() throws Exception {
        McpTransport transport = new StreamableHttpMcpTransport.Builder()
                .url("http://localhost:" + port)
                .build();

        try (dev.langchain4j.mcp.client.McpClient mcpClient = new DefaultMcpClient.Builder()
                .autoHealthCheck(false)
                .transport(transport)
                .build()) {
            var result = mcpClient.executeTool(ToolExecutionRequest.builder()
                                                       .name("protocolVersion")
                                                       .arguments("{}")
                                                       .build());
            assertThat(result.resultText(), is("2025-11-25"));
        }
    }
}
