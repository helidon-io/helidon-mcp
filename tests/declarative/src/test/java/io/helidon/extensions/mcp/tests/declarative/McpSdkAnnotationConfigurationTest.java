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

package io.helidon.extensions.mcp.tests.declarative;

import java.time.Duration;

import io.helidon.json.JsonObject;
import io.helidon.jsonrpc.core.JsonRpcResult;
import io.helidon.webclient.jsonrpc.JsonRpcClient;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.testing.junit5.ServerTest;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ServerTest
class McpSdkAnnotationConfigurationTest {
    private final int port;
    private final JsonRpcClient jsonRpcClient;

    McpSdkAnnotationConfigurationTest(WebServer server) {
        port = server.port();
        jsonRpcClient = JsonRpcClient.create(config -> config.baseUri("http://localhost:"
                                                                              + port
                                                                              + "/mcp-custom"));
    }

    @Test
    void toolAnnotationConfig() {
        try (McpSyncClient client = McpClient.sync(HttpClientSseClientTransport.builder("http://localhost:" + port)
                                                          .sseEndpoint("/mcp-custom")
                                                          .build())
                .capabilities(McpSchema.ClientCapabilities.builder().build())
                .requestTimeout(Duration.ofSeconds(1))
                .build()) {
            var result = client.initialize();
            var infos = result.serverInfo();

            assertThat(infos.version(), is("0.0.1-SNAPSHOT"));
            assertThat(infos.name(), is("mcp-server-custom-path"));
        }
    }

    @Test
    void websiteUrlAnnotationConfig() {
        JsonObject clientInfo = JsonObject.builder()
                .set("name", "test-client")
                .set("version", "1.0.0")
                .build();

        try (var response = jsonRpcClient.rpcMethod("initialize")
                .rpcId(2)
                .param("protocolVersion", "2025-11-25")
                .param("capabilities", JsonObject.empty())
                .param("clientInfo", clientInfo)
                .submit()) {
            String websiteUrl = response.result()
                    .map(JsonRpcResult::asJsonObject)
                    .flatMap(result -> result.objectValue("serverInfo"))
                    .flatMap(serverInfo -> serverInfo.stringValue("websiteUrl"))
                    .orElseThrow();

            assertThat(websiteUrl, is("https://example.com/mcp"));
        }
    }
}
