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
package io.helidon.extensions.mcp.tests.declarative;

import io.helidon.webclient.jsonrpc.JsonRpcClient;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.testing.junit5.ServerTest;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ServerTest
class McpStatelessAnnotationTest {
    private final JsonRpcClient statelessClient;
    private final JsonRpcClient statefulClient;

    McpStatelessAnnotationTest(WebServer server) {
        this.statelessClient = JsonRpcClient.create(client -> client.baseUri("http://localhost:"
                                                                                     + server.port()
                                                                                     + "/mcp-stateless-enabled"));
        this.statefulClient = JsonRpcClient.create(client -> client.baseUri("http://localhost:"
                                                                                    + server.port()
                                                                                    + "/mcp-stateless-disabled"));
    }

    @Test
    void testStateless() {
        try (var response = statelessClient.rpcMethod("tools/list")
                .rpcId(1)
                .submit()) {
            assertThat(response.error().isEmpty(), is(true));
            assertThat(response.result().isEmpty(), is(false));
            assertThat(response.result().orElseThrow().asJsonObject().getJsonArray("tools").size(), is(1));
        }
    }

    @Test
    void testStateful() {
        try (var response = statefulClient.rpcMethod("tools/list")
                .rpcId(2)
                .submit()) {
            assertThat(response.error().isEmpty(), is(false));
            assertThat(response.error().orElseThrow().message(), is("Session not found"));
        }
    }
}
