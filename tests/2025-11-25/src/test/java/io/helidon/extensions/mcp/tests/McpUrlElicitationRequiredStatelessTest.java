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

import java.net.URI;

import io.helidon.extensions.mcp.server.McpElicitationUrlRequest;
import io.helidon.extensions.mcp.server.McpException;
import io.helidon.extensions.mcp.server.McpServerFeature;
import io.helidon.extensions.mcp.server.McpUrlElicitationRequiredException;
import io.helidon.http.Status;
import io.helidon.json.JsonObject;
import io.helidon.json.JsonValue;
import io.helidon.jsonrpc.core.JsonRpcError;
import io.helidon.webclient.jsonrpc.JsonRpcClient;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

@ServerTest
class McpUrlElicitationRequiredStatelessTest {
    private static final int REQUEST_ID = 42;
    private static final String TOOL_NAME = "required-url-elicitation";
    private static final String RESERVED_ERROR_TOOL_NAME = "reserved-url-elicitation-error";
    private static final String MESSAGE = "Complete authorization in your browser";
    private static final String ELICITATION_ID = "authorization-123";
    private static final URI URL = URI.create("https://example.com/authorize");

    private final JsonRpcClient client;

    McpUrlElicitationRequiredStatelessTest(JsonRpcClient client) {
        this.client = client;
    }

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        builder.addFeature(McpServerFeature.builder()
                                   .path("/")
                                   .stateless(true)
                                   .addTool(tool -> tool.name(TOOL_NAME)
                                           .description("Require URL elicitation")
                                           .schema("")
                                           .tool(request -> {
                                               throw new McpUrlElicitationRequiredException(
                                                       McpElicitationUrlRequest.builder()
                                                               .message(MESSAGE)
                                                               .elicitationId(ELICITATION_ID)
                                                               .url(URL)
                                                               .build());
                                           }))
                                   .addTool(tool -> tool.name(RESERVED_ERROR_TOOL_NAME)
                                           .description("Throw reserved URL elicitation error code")
                                           .schema("")
                                           .tool(request -> {
                                               throw new McpException(
                                                       McpUrlElicitationRequiredException.ERROR_CODE,
                                                       "Invalid reserved error code for protocol "
                                                               + request.protocolVersion());
                                           })));
    }

    @Test
    void sendsStructuredErrorWithoutSession() {
        try (var response = client.rpcMethod("tools/call")
                .rpcId(REQUEST_ID)
                .param("name", TOOL_NAME)
                .param("arguments", JsonObject.empty())
                .submit()) {
            assertThat(response.status(), is(Status.OK_200));
            assertThat(response.asJsonObject().longValue("id").orElseThrow(), is((long) REQUEST_ID));
            assertThat(response.result().isEmpty(), is(true));

            JsonRpcError error = response.error().orElseThrow();
            assertThat(error.code(), is(McpUrlElicitationRequiredException.ERROR_CODE));
            assertThat(error.message(), is("URL elicitation required"));
            JsonObject data = error.data().orElseThrow().asObject();
            assertThat(data.keysAsStrings(), contains("elicitations"));
            JsonObject elicitation = data.arrayValue("elicitations")
                    .flatMap(elicitations -> elicitations.get(0))
                    .map(JsonValue::asObject)
                    .orElseThrow();
            assertThat(elicitation.keysAsStrings(),
                       containsInAnyOrder("mode", "message", "elicitationId", "url"));
            assertThat(elicitation.stringValue("mode").orElseThrow(), is("url"));
            assertThat(elicitation.stringValue("message").orElseThrow(), is(MESSAGE));
            assertThat(elicitation.stringValue("elicitationId").orElseThrow(), is(ELICITATION_ID));
            assertThat(elicitation.stringValue("url").orElseThrow(), is(URL.toASCIIString()));
        }
    }

    @Test
    void rejectsGenericReservedErrorCodeWithoutSession() {
        try (var response = client.rpcMethod("tools/call")
                .rpcId(REQUEST_ID)
                .param("name", RESERVED_ERROR_TOOL_NAME)
                .param("arguments", JsonObject.empty())
                .submit()) {
            assertThat(response.status(), is(Status.OK_200));
            assertThat(response.asJsonObject().longValue("id").orElseThrow(), is((long) REQUEST_ID));
            assertThat(response.result().isEmpty(), is(true));

            JsonRpcError error = response.error().orElseThrow();
            assertThat(error.code(), is(JsonRpcError.INTERNAL_ERROR));
            assertThat(error.message(),
                       is("McpUrlElicitationRequiredException must be used for error code "
                                  + McpUrlElicitationRequiredException.ERROR_CODE));
            assertThat(error.data().isEmpty(), is(true));
        }
    }
}
