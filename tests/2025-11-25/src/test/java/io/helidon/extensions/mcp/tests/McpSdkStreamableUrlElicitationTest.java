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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.helidon.extensions.mcp.server.McpElicitation;
import io.helidon.extensions.mcp.server.McpElicitationResponse;
import io.helidon.extensions.mcp.server.McpElicitationUrlRequest;
import io.helidon.extensions.mcp.server.McpException;
import io.helidon.extensions.mcp.server.McpServerFeature;
import io.helidon.extensions.mcp.server.McpToolResult;
import io.helidon.extensions.mcp.server.McpUrlElicitationRequiredException;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ServerTest
class McpSdkStreamableUrlElicitationTest {
    private static final String PROTOCOL_VERSION = "2025-11-25";
    private static final String LEGACY_PROTOCOL_VERSION = "2025-06-18";
    private static final String URL_TOOL_NAME = "url-elicitation";
    private static final String REQUIRED_URL_TOOL_NAME = "required-url-elicitation";
    private static final String REQUIRED_URL_TOOL_SWITCH_TRANSPORT_NAME = "required-url-elicitation-switch-transport";
    private static final String RESERVED_ERROR_CODE_TOOL_NAME = "reserved-url-elicitation-error";
    private static final String URL_MESSAGE = "Complete authorization in your browser";
    private static final String ID = "authorization-123";
    private static final URI URL = URI.create("https://example.com/authorize");
    private static final McpElicitationUrlRequest REQUIRED_URL_ELICITATION = McpElicitationUrlRequest.builder()
            .message(URL_MESSAGE)
            .elicitationId(ID)
            .url(URL)
            .build();
    private static final String FORM_TOOL_NAME = "form-elicitation";
    private static final String FORM_MESSAGE = "Provide your name";
    private static final String FORM_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "name": {"type": "string"}
              },
              "required": ["name"]
            }
            """;

    private final int port;

    McpSdkStreamableUrlElicitationTest(WebServer server) {
        port = server.port();
    }

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        builder.addFeature(McpServerFeature.builder()
                                   .path("/")
                                   .addTool(tool -> tool.name(URL_TOOL_NAME)
                                           .description("Request URL elicitation")
                                           .schema("")
                                           .tool(request -> {
                                               McpElicitation elicitation = request.features().elicitation();
                                               if (!elicitation.enabledUrl()) {
                                                   throw new McpException("URL elicitation is not enabled");
                                               }
                                               McpElicitationResponse response = elicitation.requestUrl(urlRequest -> urlRequest
                                                       .message(URL_MESSAGE)
                                                       .elicitationId(ID)
                                                       .url(URL));
                                               return McpToolResult.create("Elicitation action: " + response.action().name());
                                           }))
                                   .addTool(tool -> tool.name(FORM_TOOL_NAME)
                                           .description("Request form elicitation")
                                           .schema("")
                                           .tool(request -> {
                                               McpElicitation elicitation = request.features().elicitation();
                                               if (!elicitation.enabled()) {
                                                   throw new McpException("Form elicitation is not enabled");
                                               }
                                               McpElicitationResponse response = elicitation.request(formRequest -> formRequest
                                                       .message(FORM_MESSAGE)
                                                       .schema(FORM_SCHEMA));
                                               return McpToolResult.create("Elicitation action: " + response.action().name());
                                           }))
                                   .addTool(tool -> tool.name(REQUIRED_URL_TOOL_NAME)
                                           .description("Require URL elicitation")
                                           .schema("")
                                           .tool(request -> {
                                               throw new McpUrlElicitationRequiredException(REQUIRED_URL_ELICITATION);
                                           }))
                                   .addTool(tool -> tool.name(REQUIRED_URL_TOOL_SWITCH_TRANSPORT_NAME)
                                           .description("Require URL elicitation after switching transport")
                                           .schema("")
                                           .tool(request -> {
                                               request.features().logger().info("Switching to the SSE channel");
                                               throw new McpUrlElicitationRequiredException(REQUIRED_URL_ELICITATION);
                                           }))
                                   .addTool(tool -> tool.name(RESERVED_ERROR_CODE_TOOL_NAME)
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
    void testUrlElicitation() {
        for (McpSchema.ElicitResult.Action action : McpSchema.ElicitResult.Action.values()) {
            AtomicReference<McpSchema.ElicitUrlRequest> receivedRequest = new AtomicReference<>();
            try (McpSyncClient client = McpClient.sync(HttpClientStreamableHttpTransport
                                                               .builder("http://localhost:" + port)
                                                               .endpoint("/")
                                                               .build())
                    .capabilities(McpSchema.ClientCapabilities.builder()
                                          .elicitation(McpSchema.ClientCapabilities.Elicitation.builder()
                                                               .url(new McpSchema.ClientCapabilities.Elicitation.Url())
                                                               .build())
                                          .build())
                    .urlElicitation(request -> {
                        receivedRequest.set(request);
                        return McpSchema.ElicitResult.builder(action).build();
                    })
                    .build()) {
                client.initialize();

                McpSchema.CallToolResult result = client.callTool(McpSchema.CallToolRequest.builder(URL_TOOL_NAME)
                                                                         .build());
                assertThat(result.content().size(), is(1));
                McpSchema.Content content = result.content().getFirst();
                assertThat(content, instanceOf(McpSchema.TextContent.class));
                assertThat(((McpSchema.TextContent) content).text(), is("Elicitation action: " + action.name()));
            }

            McpSchema.ElicitUrlRequest request = receivedRequest.get();
            assertThat(request, notNullValue());
            assertThat(request.mode(), is("url"));
            assertThat(request.message(), is(URL_MESSAGE));
            assertThat(request.elicitationId(), is(ID));
            assertThat(request.url(), is(URL.toASCIIString()));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {REQUIRED_URL_TOOL_NAME, REQUIRED_URL_TOOL_SWITCH_TRANSPORT_NAME})
    void testUrlElicitationRequiredError(String toolName) {
        AtomicInteger directElicitations = new AtomicInteger();
        try (McpSyncClient client = McpClient.sync(HttpClientStreamableHttpTransport
                                                           .builder("http://localhost:" + port)
                                                           .endpoint("/")
                                                           .supportedProtocolVersions(List.of(PROTOCOL_VERSION))
                                                           .build())
                .capabilities(McpSchema.ClientCapabilities.builder()
                                      .elicitation(McpSchema.ClientCapabilities.Elicitation.builder()
                                                           .url(new McpSchema.ClientCapabilities.Elicitation.Url())
                                                           .build())
                                      .build())
                .urlElicitation(request -> {
                    directElicitations.incrementAndGet();
                    return McpSchema.ElicitResult.builder(McpSchema.ElicitResult.Action.CANCEL).build();
                })
                .build()) {
            client.initialize();

            McpError error = assertThrows(McpError.class,
                                          () -> client.callTool(McpSchema.CallToolRequest
                                                                        .builder(toolName)
                                                                        .build()));

            assertThat(error.getJsonRpcError().code(), is(McpSchema.ErrorCodes.URL_ELICITATION_REQUIRED));
            assertThat(error.getJsonRpcError().message(), is("URL elicitation required"));
            Map<?, ?> data = asMap(error.getJsonRpcError().data());
            assertThat(data.size(), is(1));
            List<?> elicitations = asList(data.get("elicitations"));
            assertThat(elicitations.size(), is(1));
            Map<?, ?> elicitation = asMap(elicitations.getFirst());
            assertThat(elicitation.size(), is(4));
            assertThat(elicitation.get("mode"), is("url"));
            assertThat(elicitation.get("message"), is(URL_MESSAGE));
            assertThat(elicitation.get("elicitationId"), is(ID));
            assertThat(elicitation.get("url"), is(URL.toASCIIString()));
            assertThat(directElicitations.get(), is(0));
        }
    }

    @Test
    void rejectsReservedUrlElicitationErrorCodeFromMcpException() {
        try (McpSyncClient client = McpClient.sync(HttpClientStreamableHttpTransport
                                                           .builder("http://localhost:" + port)
                                                           .endpoint("/")
                                                           .supportedProtocolVersions(List.of(PROTOCOL_VERSION))
                                                           .build())
                .build()) {
            client.initialize();

            McpError error = assertThrows(McpError.class,
                                          () -> client.callTool(McpSchema.CallToolRequest
                                                                        .builder(RESERVED_ERROR_CODE_TOOL_NAME)
                                                                        .build()));

            assertThat(error.getJsonRpcError().code(), is(McpSchema.ErrorCodes.INTERNAL_ERROR));
            assertThat(error.getJsonRpcError().message(),
                       is("McpUrlElicitationRequiredException must be used for error code "
                                  + McpUrlElicitationRequiredException.ERROR_CODE));
            assertThat(error.getJsonRpcError().data(), nullValue());
        }
    }

    @Test
    void preservesCustomUrlElicitationErrorCodeForLegacyProtocol() {
        try (McpSyncClient client = McpClient.sync(HttpClientStreamableHttpTransport
                                                           .builder("http://localhost:" + port)
                                                           .endpoint("/")
                                                           .supportedProtocolVersions(List.of(LEGACY_PROTOCOL_VERSION))
                                                           .build())
                .build()) {
            client.initialize();

            McpError error = assertThrows(McpError.class,
                                          () -> client.callTool(McpSchema.CallToolRequest
                                                                        .builder(RESERVED_ERROR_CODE_TOOL_NAME)
                                                                        .build()));

            assertThat(error.getJsonRpcError().code(), is(McpUrlElicitationRequiredException.ERROR_CODE));
            assertThat(error.getJsonRpcError().message(),
                       is("Invalid reserved error code for protocol " + LEGACY_PROTOCOL_VERSION));
            assertThat(error.getJsonRpcError().data(), nullValue());
        }
    }

    @Test
    void rejectsUrlElicitationRequiredErrorForLegacyProtocol() {
        try (McpSyncClient client = McpClient.sync(HttpClientStreamableHttpTransport
                                                           .builder("http://localhost:" + port)
                                                           .endpoint("/")
                                                           .supportedProtocolVersions(List.of(LEGACY_PROTOCOL_VERSION))
                                                           .build())
                .build()) {
            client.initialize();

            McpError error = assertThrows(McpError.class,
                                          () -> client.callTool(McpSchema.CallToolRequest
                                                                        .builder(REQUIRED_URL_TOOL_NAME)
                                                                        .build()));

            assertThat(error.getJsonRpcError().code(), is(McpSchema.ErrorCodes.INTERNAL_ERROR));
        }
    }

    @Test
    void testFormElicitation() {
        for (McpSchema.ElicitResult.Action action : McpSchema.ElicitResult.Action.values()) {
            AtomicReference<McpSchema.ElicitFormRequest> receivedRequest = new AtomicReference<>();
            try (McpSyncClient client = McpClient.sync(HttpClientStreamableHttpTransport
                                                               .builder("http://localhost:" + port)
                                                               .endpoint("/")
                                                               .build())
                    .capabilities(McpSchema.ClientCapabilities.builder()
                                          .elicitation(McpSchema.ClientCapabilities.Elicitation.builder()
                                                               .form(new McpSchema.ClientCapabilities.Elicitation.Form())
                                                               .build())
                                          .build())
                    .elicitation(request -> {
                        receivedRequest.set(request);
                        var result = McpSchema.ElicitResult.builder(action);
                        if (action == McpSchema.ElicitResult.Action.ACCEPT) {
                            result.content(Map.of("name", "Ada"));
                        }
                        return result.build();
                    })
                    .build()) {
                client.initialize();

                McpSchema.CallToolResult result = client.callTool(McpSchema.CallToolRequest.builder(FORM_TOOL_NAME)
                                                                         .build());
                assertThat(result.content().size(), is(1));
                McpSchema.Content content = result.content().getFirst();
                assertThat(content, instanceOf(McpSchema.TextContent.class));
                assertThat(((McpSchema.TextContent) content).text(), is("Elicitation action: " + action.name()));
            }

            McpSchema.ElicitFormRequest request = receivedRequest.get();
            assertThat(request, notNullValue());
            assertThat(request.mode(), is("form"));
            assertThat(request.message(), is(FORM_MESSAGE));
            assertThat(request.requestedSchema().get("type"), is("object"));
            assertThat(request.requestedSchema().containsKey("properties"), is(true));
        }
    }

    private static Map<?, ?> asMap(Object value) {
        assertThat(value, instanceOf(Map.class));
        return (Map<?, ?>) value;
    }

    private static List<?> asList(Object value) {
        assertThat(value, instanceOf(List.class));
        return (List<?>) value;
    }
}
