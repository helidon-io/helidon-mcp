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
import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@ServerTest
class McpSdkStreamableElicitationEnumTest {
    private static final String TOOL_NAME = "elicitation-enums";
    private static final String MESSAGE = "Choose colors";
    private static final String LEGACY = "legacy";
    private static final String UNTITLED_SINGLE = "untitledSingle";
    private static final String TITLED_SINGLE = "titledSingle";
    private static final String UNTITLED_MULTI = "untitledMulti";
    private static final String TITLED_MULTI = "titledMulti";
    private static final String FORM_SCHEMA = """
            {
              "type": "object",
              "properties": {
                "legacy": {
                  "type": "string",
                  "title": "Legacy color",
                  "description": "Choose one legacy color",
                  "enum": ["red", "green", "blue"],
                  "enumNames": ["Red", "Green", "Blue"],
                  "default": "green"
                },
                "untitledSingle": {
                  "type": "string",
                  "title": "Single color",
                  "description": "Choose one color",
                  "enum": ["red", "green", "blue"],
                  "default": "blue"
                },
                "titledSingle": {
                  "type": "string",
                  "title": "Titled single color",
                  "description": "Choose one titled color",
                  "oneOf": [
                    {"const": "red", "title": "Red"},
                    {"const": "green", "title": "Green"},
                    {"const": "blue", "title": "Blue"}
                  ],
                  "default": "red"
                },
                "untitledMulti": {
                  "type": "array",
                  "title": "Multiple colors",
                  "description": "Choose multiple colors",
                  "minItems": 1,
                  "maxItems": 2,
                  "items": {
                    "type": "string",
                    "enum": ["red", "green", "blue"]
                  },
                  "default": ["red", "blue"]
                },
                "titledMulti": {
                  "type": "array",
                  "title": "Titled multiple colors",
                  "description": "Choose multiple titled colors",
                  "minItems": 1,
                  "maxItems": 2,
                  "items": {
                    "anyOf": [
                      {"const": "red", "title": "Red"},
                      {"const": "green", "title": "Green"},
                      {"const": "blue", "title": "Blue"}
                    ]
                  },
                  "default": ["green", "blue"]
                }
              },
              "required": ["legacy", "untitledSingle", "titledSingle", "untitledMulti", "titledMulti"]
            }
            """;

    private final int port;

    McpSdkStreamableElicitationEnumTest(WebServer server) {
        port = server.port();
    }

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        builder.addFeature(McpServerFeature.builder()
                                   .path("/")
                                   .addTool(tool -> tool.name(TOOL_NAME)
                                           .description("Request enum form elicitation")
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
                                                   return McpToolResult.create("Elicitation action: " + response.action().name());
                                               }
                                               McpParameters content = response.content().orElseThrow();
                                               String legacy = content.get(LEGACY).asString().get();
                                               String untitledSingle = content.get(UNTITLED_SINGLE).asString().get();
                                               String titledSingle = content.get(TITLED_SINGLE).asString().get();
                                               List<String> untitledMulti = content.get(UNTITLED_MULTI)
                                                       .asList(String.class)
                                                       .get();
                                               List<String> titledMulti = content.get(TITLED_MULTI)
                                                       .asList(String.class)
                                                       .get();
                                               return McpToolResult.create(String.join("|",
                                                                                      legacy,
                                                                                      untitledSingle,
                                                                                      titledSingle,
                                                                                      String.join(",", untitledMulti),
                                                                                      String.join(",", titledMulti)));
                                           })));
    }

    @SuppressWarnings("deprecation")
    @Test
    void supportsElicitationEnumSchemasAndMultiSelectResults() {
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
                    return McpSchema.ElicitResult.builder(McpSchema.ElicitResult.Action.ACCEPT)
                            .content(Map.of(LEGACY, "green",
                                            UNTITLED_SINGLE, "blue",
                                            TITLED_SINGLE, "red",
                                            UNTITLED_MULTI, List.of("red", "blue"),
                                            TITLED_MULTI, List.of("green", "blue")))
                            .build();
                })
                .build()) {
            client.initialize();

            McpSchema.CallToolResult result = client.callTool(McpSchema.CallToolRequest.builder(TOOL_NAME).build());

            assertThat(result.content().size(), is(1));
            McpSchema.Content content = result.content().getFirst();
            assertThat(content, instanceOf(McpSchema.TextContent.class));
            assertThat(((McpSchema.TextContent) content).text(),
                       is("green|blue|red|red,blue|green,blue"));
        }

        McpSchema.ElicitFormRequest request = receivedRequest.get();
        assertThat(request, notNullValue());
        assertThat(request.mode(), is("form"));
        assertThat(request.message(), is(MESSAGE));
        assertThat(request.requestedSchema().get("type"), is("object"));
        assertThat(request.requestedSchema().get("required"),
                   is(List.of(LEGACY, UNTITLED_SINGLE, TITLED_SINGLE, UNTITLED_MULTI, TITLED_MULTI)));
        Object propertiesValue = request.requestedSchema().get("properties");
        assertThat(propertiesValue, instanceOf(Map.class));
        Map<?, ?> properties = (Map<?, ?>) propertiesValue;
        Map<?, ?> legacySchema = assertSchemaType(properties, LEGACY, "string");
        Map<?, ?> untitledSingleSchema = assertSchemaType(properties, UNTITLED_SINGLE, "string");
        Map<?, ?> titledSingleSchema = assertSchemaType(properties, TITLED_SINGLE, "string");
        Map<?, ?> untitledMultiSchema = assertSchemaType(properties, UNTITLED_MULTI, "array");
        Map<?, ?> titledMultiSchema = assertSchemaType(properties, TITLED_MULTI, "array");

        assertSchemaMetadata(legacySchema, "Legacy color", "Choose one legacy color");
        assertSchemaMetadata(untitledSingleSchema, "Single color", "Choose one color");
        assertSchemaMetadata(titledSingleSchema, "Titled single color", "Choose one titled color");
        assertSchemaMetadata(untitledMultiSchema, "Multiple colors", "Choose multiple colors");
        assertSchemaMetadata(titledMultiSchema, "Titled multiple colors", "Choose multiple titled colors");
        Object untitledMultiItemsValue = untitledMultiSchema.get("items");
        assertThat(untitledMultiItemsValue, instanceOf(Map.class));
        assertThat(((Map<?, ?>) untitledMultiItemsValue).get("type"), is("string"));

        McpSchema.LegacyTitledEnumSchema legacy = McpJsonDefaults.getMapper()
                .convertValue(properties.get(LEGACY), McpSchema.LegacyTitledEnumSchema.class);
        assertThat(legacy.type(), is("string"));
        assertThat(legacy.enumValues(), is(List.of("red", "green", "blue")));
        assertThat(legacy.enumNames(), is(List.of("Red", "Green", "Blue")));
        assertThat(legacy.defaultValue(), is("green"));

        McpSchema.UntitledSingleSelectEnumSchema untitledSingle = McpJsonDefaults.getMapper()
                .convertValue(properties.get(UNTITLED_SINGLE), McpSchema.UntitledSingleSelectEnumSchema.class);
        assertThat(untitledSingle.type(), is("string"));
        assertThat(untitledSingle.enumValues(), is(List.of("red", "green", "blue")));
        assertThat(untitledSingle.defaultValue(), is("blue"));

        List<McpSchema.EnumSchemaOption> options = List.of(new McpSchema.EnumSchemaOption("red", "Red"),
                                                           new McpSchema.EnumSchemaOption("green", "Green"),
                                                           new McpSchema.EnumSchemaOption("blue", "Blue"));
        McpSchema.TitledSingleSelectEnumSchema titledSingle = McpJsonDefaults.getMapper()
                .convertValue(properties.get(TITLED_SINGLE), McpSchema.TitledSingleSelectEnumSchema.class);
        assertThat(titledSingle.type(), is("string"));
        assertThat(titledSingle.oneOf(), is(options));
        assertThat(titledSingle.defaultValue(), is("red"));

        McpSchema.UntitledMultiSelectEnumSchema untitledMulti = McpJsonDefaults.getMapper()
                .convertValue(properties.get(UNTITLED_MULTI), McpSchema.UntitledMultiSelectEnumSchema.class);
        assertThat(untitledMulti.type(), is("array"));
        assertThat(untitledMulti.items().enumValues(), is(List.of("red", "green", "blue")));
        assertThat(untitledMulti.minItems(), is(1));
        assertThat(untitledMulti.maxItems(), is(2));
        assertThat(untitledMulti.defaultValue(), is(List.of("red", "blue")));

        McpSchema.TitledMultiSelectEnumSchema titledMulti = McpJsonDefaults.getMapper()
                .convertValue(properties.get(TITLED_MULTI), McpSchema.TitledMultiSelectEnumSchema.class);
        assertThat(titledMulti.type(), is("array"));
        assertThat(titledMulti.items().anyOf(), is(options));
        assertThat(titledMulti.minItems(), is(1));
        assertThat(titledMulti.maxItems(), is(2));
        assertThat(titledMulti.defaultValue(), is(List.of("green", "blue")));
    }

    private static Map<?, ?> assertSchemaType(Map<?, ?> properties, String name, String type) {
        Object schemaValue = properties.get(name);
        assertThat(schemaValue, instanceOf(Map.class));
        Map<?, ?> schema = (Map<?, ?>) schemaValue;
        assertThat(schema.get("type"), is(type));
        return schema;
    }

    private static void assertSchemaMetadata(Map<?, ?> schema, String title, String description) {
        assertThat(schema.get("title"), is(title));
        assertThat(schema.get("description"), is(description));
    }
}
