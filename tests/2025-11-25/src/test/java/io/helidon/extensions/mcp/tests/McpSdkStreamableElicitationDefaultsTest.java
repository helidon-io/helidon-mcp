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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import io.helidon.extensions.mcp.server.McpElicitation;
import io.helidon.extensions.mcp.server.McpElicitationAction;
import io.helidon.extensions.mcp.server.McpElicitationResponse;
import io.helidon.extensions.mcp.server.McpException;
import io.helidon.extensions.mcp.server.McpParameters;
import io.helidon.extensions.mcp.server.McpServerFeature;
import io.helidon.extensions.mcp.server.McpToolResult;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@ServerTest
class McpSdkStreamableElicitationDefaultsTest {
    private static final String PROTOCOL_VERSION = "2025-11-25";
    private static final String TOOL_NAME = "test_elicitation_sep1034_defaults";
    private static final String MESSAGE = "Provide profile details";
    private static final String NAME = "name";
    private static final String AGE = "age";
    private static final String SCORE = "score";
    private static final String STATUS = "status";
    private static final String VERIFIED = "verified";
    private static final String FORM_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "name": {
                  "type": "string",
                  "default": "John Doe"
                },
                "age": {
                  "type": "integer",
                  "default": 30
                },
                "score": {
                  "type": "number",
                  "default": 95.5
                },
                "status": {
                  "type": "string",
                  "enum": ["active", "inactive", "pending"],
                  "default": "active"
                },
                "verified": {
                  "type": "boolean",
                  "default": true
                }
              },
              "required": []
            }
            """;

    private final int port;

    McpSdkStreamableElicitationDefaultsTest(WebServer server) {
        port = server.port();
    }

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        builder.addFeature(McpServerFeature.builder()
                                   .path("/")
                                   .addTool(tool -> tool.name(TOOL_NAME)
                                           .description("Request form elicitation with default values")
                                           .schema("")
                                           .tool(request -> {
                                               McpElicitation elicitation = request.features().elicitation();
                                               if (!elicitation.enabled()) {
                                                   throw new McpException("Form elicitation is not enabled");
                                               }
                                               McpElicitationResponse response = elicitation.request(formRequest -> formRequest
                                                       .message(MESSAGE)
                                                       .schema(FORM_SCHEMA));
                                               if (response.action() != McpElicitationAction.ACCEPT) {
                                                   return McpToolResult.create("Elicitation action: "
                                                                                       + response.action().name());
                                               }
                                               McpParameters content = response.content().orElseThrow();
                                               String name = content.get(NAME).asString().get();
                                               int age = content.get(AGE).asInteger().get();
                                               double score = content.get(SCORE).asDouble().get();
                                               String status = content.get(STATUS).asString().get();
                                               boolean verified = content.get(VERIFIED).asBoolean().get();
                                               return McpToolResult.create(String.join("|",
                                                                                      name,
                                                                                      Integer.toString(age),
                                                                                      Double.toString(score),
                                                                                      status,
                                                                                      Boolean.toString(verified)));
                                           })));
    }

    @Test
    void preservesDefaultsForAllPrimitiveTypes() {
        AtomicReference<McpSchema.ElicitFormRequest> receivedRequest = new AtomicReference<>();
        try (McpSyncClient client = McpClient.sync(HttpClientStreamableHttpTransport
                                                           .builder("http://localhost:" + port)
                                                           .endpoint("/")
                                                           .supportedProtocolVersions(List.of(PROTOCOL_VERSION))
                                                           .build())
                .capabilities(McpSchema.ClientCapabilities.builder()
                                      .elicitation(McpSchema.ClientCapabilities.Elicitation.builder()
                                                           .form(new McpSchema.ClientCapabilities.Elicitation.Form())
                                                           .build())
                                      .build())
                .elicitation(request -> {
                    receivedRequest.set(request);
                    return McpSchema.ElicitResult.builder(McpSchema.ElicitResult.Action.ACCEPT)
                            .content(Map.of(NAME, "Jane Smith",
                                            AGE, 25,
                                            SCORE, 88.0,
                                            STATUS, "inactive",
                                            VERIFIED, false))
                            .build();
                })
                .build()) {
            client.initialize();

            McpSchema.CallToolResult result = client.callTool(McpSchema.CallToolRequest.builder(TOOL_NAME).build());

            assertThat(result.content().size(), is(1));
            McpSchema.Content content = result.content().getFirst();
            assertThat(content, instanceOf(McpSchema.TextContent.class));
            assertThat(((McpSchema.TextContent) content).text(), is("Jane Smith|25|88.0|inactive|false"));
        }

        McpSchema.ElicitFormRequest request = receivedRequest.get();
        assertThat(request, notNullValue());
        assertThat(request.mode(), is("form"));
        assertThat(request.message(), is(MESSAGE));
        assertThat(request.requestedSchema().get("type"), is("object"));

        Object propertiesValue = request.requestedSchema().get("properties");
        assertThat(propertiesValue, instanceOf(Map.class));
        Map<?, ?> properties = (Map<?, ?>) propertiesValue;
        Map<?, ?> nameSchema = assertSchemaType(properties, NAME, "string");
        Map<?, ?> ageSchema = assertSchemaType(properties, AGE, "integer");
        Map<?, ?> scoreSchema = assertSchemaType(properties, SCORE, "number");
        Map<?, ?> statusSchema = assertSchemaType(properties, STATUS, "string");
        Map<?, ?> verifiedSchema = assertSchemaType(properties, VERIFIED, "boolean");

        assertThat(nameSchema.containsKey("default"), is(true));
        assertThat(nameSchema.get("default"), is("John Doe"));
        assertThat(ageSchema.containsKey("default"), is(true));
        Object ageDefault = ageSchema.get("default");
        assertThat(ageDefault, instanceOf(Number.class));
        assertThat(new BigDecimal(ageDefault.toString()), comparesEqualTo(BigDecimal.valueOf(30)));
        assertThat(scoreSchema.containsKey("default"), is(true));
        Object scoreDefault = scoreSchema.get("default");
        assertThat(scoreDefault, instanceOf(Number.class));
        assertThat(((Number) scoreDefault).doubleValue(), is(95.5));
        assertThat(statusSchema.get("enum"), is(List.of("active", "inactive", "pending")));
        assertThat(statusSchema.containsKey("default"), is(true));
        assertThat(statusSchema.get("default"), is("active"));
        assertThat(verifiedSchema.containsKey("default"), is(true));
        assertThat(verifiedSchema.get("default"), is(true));
    }

    private static Map<?, ?> assertSchemaType(Map<?, ?> properties, String name, String type) {
        Object schemaValue = properties.get(name);
        assertThat(schemaValue, instanceOf(Map.class));
        Map<?, ?> schema = (Map<?, ?>) schemaValue;
        assertThat(schema.get("type"), is(type));
        return schema;
    }
}
