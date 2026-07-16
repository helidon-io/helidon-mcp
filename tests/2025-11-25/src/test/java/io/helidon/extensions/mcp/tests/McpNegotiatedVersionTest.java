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
import io.helidon.http.HeaderValues;
import io.helidon.http.Status;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@ServerTest
class McpNegotiatedVersionTest {
    private static final HeaderName SESSION_ID_HEADER = HeaderNames.create("Mcp-Session-Id");
    private static final HeaderName MCP_PROTOCOL_VERSION = HeaderNames.create("Mcp-Protocol-Version");

    private final int port;
    private final Http1Client client;

    McpNegotiatedVersionTest(WebServer server) {
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
    void testMcpVersion() {
        String initRequest = """
                {
                  "jsonrpc": "2.0",
                  "id": 1,
                  "method": "initialize",
                  "params": {
                    "protocolVersion": "2025-11-25",
                    "capabilities": {"roots": {"listChanged": true}},
                    "clientInfo": {"name": "Example Client Display Name", "version": "1.0.0"}
                  }
                }
                """;
        String sessionId;
        try (var response = client.post()
                .header(HeaderValues.CONTENT_TYPE_JSON)
                .submit(initRequest)) {
            sessionId = response.headers().get(SESSION_ID_HEADER).get();
            String result = response.entity().as(String.class);
            assertThat(result, containsString("\"protocolVersion\":\"2025-11-25\""));
        }

        String listTools = """
                {
                  "jsonrpc": "2.0",
                  "id": 2,
                  "method": "tools/list"
                }
                """;
        try (var response = client.post()
                .header(SESSION_ID_HEADER, sessionId)
                .header(MCP_PROTOCOL_VERSION, "2025-11-25")
                .header(HeaderValues.CONTENT_TYPE_JSON)
                .submit(listTools)) {
            assertThat(response.status(), is(Status.OK_200));
        }
    }

    @Test
    void testMcpSdkVersion() {
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
    void testLangchain4jVersion() throws Exception {
        McpTransport transport = new StreamableHttpMcpTransport.Builder()
                .url("http://localhost:" + port)
                .build();

        try (McpClient mcpClient = new DefaultMcpClient.Builder()
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
