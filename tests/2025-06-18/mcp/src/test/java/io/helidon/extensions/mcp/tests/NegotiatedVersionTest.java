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

import io.helidon.http.HeaderName;
import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.webclient.http1.Http1Client;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import jakarta.json.JsonObject;
import jakarta.json.spi.JsonProvider;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@ServerTest
class NegotiatedVersionTest {
    private static final JsonProvider JSON_PROVIDER = JsonProvider.provider();
    private static final HeaderName SESSION_ID_HEADER = HeaderNames.create("Mcp-Session-Id");
    private static final HeaderName MCP_PROTOCOL_VERSION = HeaderNames.create("Mcp-Protocol-Version");

    private final Http1Client client;

    NegotiatedVersionTest(WebServer server) {
        this.client = Http1Client.builder()
                .baseUri("http://localhost:" + server.port())
                .build();
    }

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        MultipleTool.setUpRoute(builder);
    }

    @Test
    void testInvalidMcpVersion() {
        JsonObject initRequest = JSON_PROVIDER.createObjectBuilder()
                .add("jsonrpc", "2.0")
                .add("id", 1)
                .add("method", "initialize")
                .add("params", JSON_PROVIDER.createObjectBuilder()
                        .add("protocolVersion", "2025-06-15")
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
        }

        JsonObject listTools = JSON_PROVIDER.createObjectBuilder()
                .add("jsonrpc", "2.0")
                .add("id", 2)
                .add("method", "tools/list")
                .build();
        try (var response = client.post()
                .header(SESSION_ID_HEADER, sessionId)
                .header(MCP_PROTOCOL_VERSION, "2025-12-34")
                .submit(listTools)) {
            String res = response.entity().as(String.class);
            assertThat(response.status(), is(Status.BAD_REQUEST_400));
            assertThat(res, containsString("Wrong MCP protocol version"));
        }
    }
}
