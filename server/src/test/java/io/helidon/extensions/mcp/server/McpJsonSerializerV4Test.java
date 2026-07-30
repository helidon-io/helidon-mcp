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
package io.helidon.extensions.mcp.server;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.json.JsonArray;
import io.helidon.json.JsonObject;
import io.helidon.json.JsonParser;
import io.helidon.json.JsonString;
import io.helidon.json.JsonValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static io.helidon.extensions.mcp.server.McpMetadata.META;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;

class McpJsonSerializerV4Test {
    private static final McpJsonSerializer SERIALIZER =
            McpJsonSerializer.create(McpProtocolVersion.VERSION_2025_11_25);
    private static final McpJsonSerializer LEGACY_SERIALIZER =
            McpJsonSerializer.create(McpProtocolVersion.VERSION_2025_06_18);
    private static final McpIcon FULL_ICON = McpIcon.builder()
            .source("https://example.com/icon.svg")
            .mediaType(MediaTypes.create("image/svg+xml"))
            .addSize("48x48")
            .addSize("96x96")
            .theme(McpIconTheme.DARK)
            .build();
    private static final McpIcon MINIMAL_ICON = McpIcon.builder()
            .source("data:image/png;base64,iVBORw0KGgo=")
            .build();
    private static final JsonArray EXPECTED_ICONS = JsonParser.create("""
            [
              {
                "src": "https://example.com/icon.svg",
                "mimeType": "image/svg+xml",
                "sizes": ["48x48", "96x96"],
                "theme": "dark"
              },
              {
                "src": "data:image/png;base64,iVBORw0KGgo="
              }
            ]
            """).readJsonArray();
    private static final JsonObject EXPECTED_TOOL = JsonObject.builder()
            .set("name", "tool")
            .set("description", "description")
            .set("inputSchema", McpJsonSerializerV1.EMPTY_OBJECT_SCHEMA)
            .set("annotations", JsonObject.builder()
                    .set("title", "annotation title")
                    .set("destructiveHint", false)
                    .set("idempotentHint", true)
                    .set("openWorldHint", false)
                    .set("readOnlyHint", true)
                    .build())
            .set("title", "tool title")
            .set("outputSchema", McpJsonSerializerV1.EMPTY_OBJECT_SCHEMA)
            .set("icons", EXPECTED_ICONS)
            .build();
    private static final JsonObject EXPECTED_PROMPT = JsonObject.builder()
            .set("name", "prompt")
            .set("description", "description")
            .set("arguments", JsonArray.empty())
            .set("title", "prompt title")
            .set("icons", EXPECTED_ICONS)
            .build();
    private static final JsonObject EXPECTED_RESOURCE = JsonObject.builder()
            .set("uri", "https://example.com/resource")
            .set("name", "resource")
            .set("description", "description")
            .set("mimeType", MediaTypes.TEXT_PLAIN.text())
            .set("title", "resource title")
            .set("icons", EXPECTED_ICONS)
            .build();
    private static final JsonObject EXPECTED_RESOURCE_TEMPLATE = JsonObject.builder()
            .set("uriTemplate", "https://example.com/resource/{id}")
            .set("name", "resource")
            .set("description", "description")
            .set("mimeType", MediaTypes.TEXT_PLAIN.text())
            .set("icons", EXPECTED_ICONS)
            .build();

    @Test
    void serializesToolIcons() {
        JsonObject response = SERIALIZER.listTools(new McpPage<>(List.of(tool())));

        assertThat(component(response, "tools"), is(EXPECTED_TOOL));
    }

    @Test
    void serializesPromptIcons() {
        JsonObject response = SERIALIZER.listPrompts(new McpPage<>(List.of(prompt())));

        assertThat(component(response, "prompts"), is(EXPECTED_PROMPT));
    }

    @Test
    void serializesResourceIcons() {
        JsonObject response = SERIALIZER.listResources(new McpPage<>(List.of(resource("https://example.com/resource"))));

        assertThat(component(response, "resources"), is(EXPECTED_RESOURCE));
    }

    @Test
    void serializesResourceTemplateIcons() {
        McpResourceTemplate template = new McpResourceTemplate(resource("https://example.com/resource/{id}"));
        JsonObject response = SERIALIZER.listResourceTemplates(new McpPage<>(List.of(template)));

        assertThat(component(response, "resourceTemplates"), is(EXPECTED_RESOURCE_TEMPLATE));
    }

    @Test
    void omitsEmptyIconMetadata() {
        McpToolConfig config = McpToolConfig.builder()
                .name("tool")
                .description("description")
                .schema("")
                .icons(List.of())
                .tool(request -> McpToolResult.create())
                .build();
        JsonObject response = SERIALIZER.listTools(new McpPage<>(List.of(new McpToolImpl(config))));

        assertThat(component(response, "tools").containsKey("icons"), is(false));
    }

    @Test
    void serializesThemeIndependentlyOfDefaultLocale() {
        McpPromptConfig config = McpPromptConfig.builder()
                .name("prompt")
                .description("description")
                .addIcon(icon -> icon.source("https://example.com/light.svg").theme(McpIconTheme.LIGHT))
                .prompt(request -> McpPromptResult.create())
                .build();
        Locale defaultLocale = Locale.getDefault();
        String theme;
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            JsonObject response = SERIALIZER.listPrompts(new McpPage<>(List.of(new McpPromptImpl(config))));
            theme = component(response, "prompts")
                    .arrayValue("icons")
                    .flatMap(icons -> icons.get(0))
                    .map(JsonValue::asObject)
                    .flatMap(icon -> icon.stringValue("theme"))
                    .orElseThrow();
        } finally {
            Locale.setDefault(defaultLocale);
        }

        assertThat(theme, is("light"));
    }

    @Test
    void omitsUnspecifiedIconTheme() {
        McpPromptConfig config = McpPromptConfig.builder()
                .name("prompt")
                .description("description")
                .addIcon(icon -> icon.source("https://example.com/icon.svg").theme(McpIconTheme.UNSPECIFIED))
                .prompt(request -> McpPromptResult.create())
                .build();
        JsonObject response = SERIALIZER.listPrompts(new McpPage<>(List.of(new McpPromptImpl(config))));
        JsonObject icon = component(response, "prompts")
                .arrayValue("icons")
                .flatMap(icons -> icons.get(0))
                .map(JsonValue::asObject)
                .orElseThrow();

        assertThat(icon.containsKey("theme"), is(false));
    }

    @ParameterizedTest
    @EnumSource(value = McpProtocolVersion.class,
                names = {"VERSION_2024_11_05", "VERSION_2025_03_26", "VERSION_2025_06_18"})
    void omitsIconsFromLegacyProtocolVersions(McpProtocolVersion version) {
        McpJsonSerializer serializer = McpJsonSerializer.create(version);
        McpResource resource = resource("https://example.com/resource");
        McpResourceTemplate template = new McpResourceTemplate(resource("https://example.com/resource/{id}"));

        List<Boolean> iconProperties = List.of(
                component(serializer.listTools(new McpPage<>(List.of(tool()))), "tools").containsKey("icons"),
                component(serializer.listPrompts(new McpPage<>(List.of(prompt()))), "prompts").containsKey("icons"),
                component(serializer.listResources(new McpPage<>(List.of(resource))), "resources").containsKey("icons"),
                component(serializer.listResourceTemplates(new McpPage<>(List.of(template))),
                          "resourceTemplates").containsKey("icons"));

        assertThat(iconProperties, everyItem(is(false)));
    }

    @Test
    void serializesServerMetadata() {
        McpServerConfig config = McpServerConfig.builder()
                .description("Helidon MCP server")
                .websiteUrl("https://example.com/mcp")
                .addIcon(FULL_ICON)
                .addIcon(MINIMAL_ICON)
                .buildPrototype();
        JsonObject response = SERIALIZER.createJsonInitializeResponse(Set.of(), config).build();
        JsonObject expected = JsonObject.builder()
                .set("name", "mcp-server")
                .set("version", "0.0.1")
                .set("description", "Helidon MCP server")
                .set("websiteUrl", "https://example.com/mcp")
                .set("icons", EXPECTED_ICONS)
                .build();

        assertThat(response.objectValue("serverInfo").orElseThrow(), is(expected));
    }

    @Test
    void omitsUnconfiguredServerMetadata() {
        McpServerConfig config = McpServerConfig.builder().buildPrototype();
        JsonObject response = SERIALIZER.createJsonInitializeResponse(Set.of(), config).build();
        JsonObject expected = JsonObject.builder()
                .set("name", "mcp-server")
                .set("version", "0.0.1")
                .build();

        assertThat(response.objectValue("serverInfo").orElseThrow(), is(expected));
    }

    @ParameterizedTest
    @EnumSource(value = McpProtocolVersion.class,
                names = {"VERSION_2024_11_05", "VERSION_2025_03_26", "VERSION_2025_06_18"})
    void omitsServerMetadataFromLegacyProtocolVersions(McpProtocolVersion version) {
        McpServerConfig config = McpServerConfig.builder()
                .description("Helidon MCP server")
                .websiteUrl("https://example.com/mcp")
                .addIcon(FULL_ICON)
                .buildPrototype();
        JsonObject response = McpJsonSerializer.create(version)
                .createJsonInitializeResponse(Set.of(), config)
                .build();
        JsonObject expected = JsonObject.builder()
                .set("name", "mcp-server")
                .set("version", "0.0.1")
                .build();

        assertThat(response.objectValue("serverInfo").orElseThrow(), is(expected));
    }

    @Test
    void serializesFormElicitationRequest() {
        String schema = """
                {
                  "type": "object",
                  "properties": {
                    "name": {"type": "string"}
                  }
                }
                """;
        McpElicitationRequest elicitationRequest = McpElicitationRequest.builder()
                .message("Provide your name")
                .schema(schema)
                .build();
        JsonObject expected = JsonObject.builder()
                .set("jsonrpc", "2.0")
                .set("id", 1)
                .set("method", "elicitation/create")
                .set("params", JsonObject.builder()
                        .set("mode", "form")
                        .set("message", "Provide your name")
                        .set("requestedSchema", JsonParser.create(schema).readJsonObject())
                        .build())
                .build();

        JsonObject request = SERIALIZER.createElicitationRequest(1, elicitationRequest);

        assertThat(request.toString(), is(expected.toString()));
    }

    @Test
    void serializesUrlElicitationRequest() {
        McpElicitationUrlRequest elicitationRequest = McpElicitationUrlRequest.builder()
                .message("Authorize access")
                .elicitationId("elicitation-id")
                .url(URI.create("https://example.com/authorize"))
                .build();
        JsonObject expected = JsonObject.builder()
                .set("jsonrpc", "2.0")
                .set("id", 1)
                .set("method", "elicitation/create")
                .set("params", JsonObject.builder()
                        .set("mode", "url")
                        .set("message", "Authorize access")
                        .set("elicitationId", "elicitation-id")
                        .set("url", "https://example.com/authorize")
                        .build())
                .build();

        JsonObject request = SERIALIZER.createElicitationRequest(1, elicitationRequest);

        assertThat(request.toString(), is(expected.toString()));
    }

    @ParameterizedTest
    @EnumSource(McpElicitationAction.class)
    void createsUrlElicitationResponseWithoutFormContent(McpElicitationAction action) {
        JsonObject result = JsonObject.builder()
                .set("action", action.text())
                .set("content", JsonObject.builder()
                        .set("marker", "ignored-content")
                        .build())
                .build();
        JsonObject clientResponse = SERIALIZER.createJsonRpcResultResponse(1, result);

        McpElicitationResponse response = SERIALIZER.createElicitationUrlResponse(clientResponse);

        assertThat(response.action(), is(action));
        assertThat(response.content().isEmpty(), is(true));
    }

    @Test
    void createsFormElicitationResponseWithMultiSelectContent() {
        JsonObject result = JsonObject.builder()
                .set("action", "accept")
                .set("content", JsonObject.builder()
                        .setStrings("colors", List.of("red", "blue"))
                        .build())
                .build();
        JsonObject clientResponse = SERIALIZER.createJsonRpcResultResponse(1, result);

        McpElicitationResponse response = SERIALIZER.createElicitationResponse(clientResponse);

        assertThat(response.action(), is(McpElicitationAction.ACCEPT));
        List<String> colors = response.content()
                .orElseThrow()
                .get("colors")
                .asList(String.class)
                .get();
        assertThat(colors, is(List.of("red", "blue")));
    }

    @Test
    void parsesUrlElicitationActionIndependentlyOfDefaultLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr"));
            JsonObject result = JsonObject.builder()
                    .set("action", "decline")
                    .build();
            JsonObject clientResponse = SERIALIZER.createJsonRpcResultResponse(1, result);

            McpElicitationResponse response = SERIALIZER.createElicitationUrlResponse(clientResponse);

            assertThat(response.action(), is(McpElicitationAction.DECLINE));
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void rejectsUrlElicitationResponseForLegacyProtocolVersions() {
        JsonObject result = JsonObject.builder()
                .set("action", "accept")
                .build();
        JsonObject clientResponse = LEGACY_SERIALIZER.createJsonRpcResultResponse(1, result);

        McpElicitationException exception = assertThrows(McpElicitationException.class,
                                                         () -> LEGACY_SERIALIZER.createElicitationUrlResponse(
                                                                 clientResponse));

        assertThat(exception.getMessage(), is("URL elicitation not supported"));
    }

    @Test
    void rejectsUrlElicitationForLegacyProtocolVersions() {
        McpElicitationUrlRequest elicitationRequest = McpElicitationUrlRequest.builder()
                .message("Authorize access")
                .elicitationId("elicitation-id")
                .url(URI.create("https://example.com/authorize"))
                .build();

        McpElicitationException exception = assertThrows(McpElicitationException.class,
                                                         () -> LEGACY_SERIALIZER.createElicitationRequest(
                                                                 1,
                                                                 elicitationRequest));

        assertThat(exception.getMessage(), is("URL elicitation not supported"));
    }

    @Test
    void serializesSamplingToolsAndToolChoiceExactly() {
        McpToolConfig toolConfig = McpToolConfig.builder()
                .name("weather")
                .title("Weather")
                .description("Looks up the weather for a city")
                .schema("""
                                {
                                  "type": "object",
                                  "properties": {
                                    "city": {"type": "string"}
                                  },
                                  "required": ["city"]
                                }
                                """)
                .outputSchema("""
                                      {
                                        "type": "object",
                                        "properties": {
                                          "temperature": {"type": "number"}
                                        },
                                        "required": ["temperature"]
                                      }
                                      """)
                .tool(request -> McpToolResult.create())
                .build();
        McpTool tool = new McpToolImpl(toolConfig);
        McpSamplingRequest request = McpSamplingRequest.builder()
                .addTool(tool.name())
                .toolChoice(McpToolChoice.REQUIRED)
                .build();

        JsonObject params = SERIALIZER.toJson(request, List.of(tool)).build();
        JsonObject expected = JsonParser.create("""
                {
                  "tools": [
                    {
                      "name": "weather",
                      "title": "Weather",
                      "description": "Looks up the weather for a city",
                      "inputSchema": {
                        "type": "object",
                        "properties": {
                          "city": {"type": "string"}
                        },
                        "required": ["city"]
                      },
                      "outputSchema": {
                        "type": "object",
                        "properties": {
                          "temperature": {"type": "number"}
                        },
                        "required": ["temperature"]
                      }
                    }
                  ],
                  "toolChoice": {"mode": "required"}
                }
                """).readJsonObject();

        assertThat(params.value("tools").orElseThrow(), is(expected.value("tools").orElseThrow()));
        assertThat(params.value("toolChoice").orElseThrow(), is(expected.value("toolChoice").orElseThrow()));
    }

    @ParameterizedTest
    @EnumSource(McpToolChoice.class)
    void serializesEverySamplingToolChoiceMode(McpToolChoice choice) {
        String expected = switch (choice) {
            case AUTO -> "auto";
            case REQUIRED -> "required";
            case NONE -> "none";
        };
        McpSamplingRequest request = McpSamplingRequest.builder()
                .toolChoice(choice)
                .build();

        JsonObject toolChoice = SERIALIZER.toJson(request, List.of())
                .build()
                .objectValue("toolChoice")
                .orElseThrow();

        assertThat(toolChoice.stringValue("mode").orElseThrow(), is(expected));
    }

    @Test
    void serializesSamplingToolChoiceIndependentlyOfDefaultLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr"));

            assertThat(McpToolChoice.REQUIRED.text(), is("required"));
        } finally {
            Locale.setDefault(previous);
        }
    }

    @ParameterizedTest
    @EnumSource(value = McpProtocolVersion.class,
                names = {"VERSION_2024_11_05", "VERSION_2025_03_26", "VERSION_2025_06_18"})
    void omitsSamplingToolsAndToolChoiceFromLegacyProtocolVersions(McpProtocolVersion version) {
        McpToolConfig toolConfig = McpToolConfig.builder()
                .name("weather")
                .schema("{\"type\":\"object\"}")
                .tool(request -> McpToolResult.create())
                .build();
        McpTool tool = new McpToolImpl(toolConfig);
        McpSamplingRequest request = McpSamplingRequest.builder()
                .addTool(tool.name())
                .toolChoice(McpToolChoice.NONE)
                .build();

        JsonObject params = McpJsonSerializer.create(version).toJson(request, List.of(tool)).build();

        assertThat(params.value("tools").isEmpty(), is(true));
        assertThat(params.value("toolChoice").isEmpty(), is(true));
    }

    @Test
    void parsesSingleSamplingContentBlock() {
        McpSamplingResponse response = SERIALIZER.createSamplingResponse(response("""
                {
                  "type": "text",
                  "text": "first"
                }
                """, "endTurn"));

        assertThat(response.message().role(), is(McpRole.ASSISTANT));
        assertThat(response.message().contents().size(), is(1));
        assertThat(response.asTextContent().text(), is("first"));
        assertThat(response.message().contents().getFirst(), sameInstance(response.asTextContent()));
    }

    @Test
    void parsesSingleSamplingToolUseContentBlock() {
        McpSamplingResponse response = SERIALIZER.createSamplingResponse(response("""
                {
                  "type": "tool_use",
                  "id": "call-1",
                  "name": "weather",
                  "input": {"city": "Prague"},
                  "_meta": {"provider": "test"}
                }
                """, "toolUse"));

        McpSamplingToolUseContent content = response.asToolUseContent();
        assertThat(response.message().role(), is(McpRole.ASSISTANT));
        assertThat(response.message().contents().size(), is(1));
        assertThat(response.message().contents().getFirst(), sameInstance(content));
        assertThat(content.type(), is(McpSamplingContentType.TOOL_USE));
        assertThat(content.id(), is("call-1"));
        assertThat(content.name(), is("weather"));
        assertThat(content.input().get("city").asString().orElse(""), is("Prague"));
        assertThat(content.metadata().orElseThrow().get("provider").asString().orElse(""), is("test"));
        assertThrows(McpSamplingException.class, response::asTextContent);
    }

    @Test
    void parsesSamplingToolResultContentBlock() {
        JsonObject wireResponse = response("""
                {
                  "type": "tool_result",
                  "toolUseId": "call-1",
                  "content": [
                    {
                      "type": "image",
                      "data": "ZGF0YQ==",
                      "mimeType": "image/png",
                      "annotations": {"audience": ["assistant"], "priority": 0.6},
                      "_meta": {"image": true}
                    },
                    {
                      "type": "text",
                      "text": "18 C",
                      "annotations": {"lastModified": "2026-08-06T10:15:30Z"},
                      "_meta": {"text": true}
                    },
                    {
                      "type": "resource_link",
                      "uri": "https://example.com/weather",
                      "name": "weather",
                      "annotations": {"audience": ["user"]},
                      "_meta": {"link": true},
                      "icons": [
                        {
                          "src": "https://example.com/weather.png",
                          "mimeType": "image/png",
                          "sizes": ["48x48", "any"],
                          "theme": "dark"
                        }
                      ]
                    },
                    {
                      "type": "resource",
                      "resource": {
                        "uri": "memory://forecast",
                        "mimeType": "text/plain",
                        "text": "sunny",
                        "_meta": {"resourceText": true}
                      },
                      "annotations": {"priority": 0.7},
                      "_meta": {"embeddedText": true}
                    },
                    {
                      "type": "audio",
                      "data": "ZGF0YQ==",
                      "mimeType": "audio/wav",
                      "annotations": {"audience": ["assistant"]},
                      "_meta": {"audio": true}
                    },
                    {
                      "type": "resource",
                      "resource": {
                        "uri": "memory://raw",
                        "mimeType": "application/octet-stream",
                        "blob": "ZGF0YQ==",
                        "_meta": {"resourceBinary": true}
                      },
                      "annotations": {"priority": 0.2},
                      "_meta": {"embeddedBinary": true}
                    }
                  ],
                  "structuredContent": {"temperature": 18},
                  "isError": false,
                  "_meta": {"cached": true}
                }
                """, "endTurn", "user");
        JsonArray wireContent = wireResponse.objectValue("result").orElseThrow()
                .objectValue("content").orElseThrow()
                .arrayValue("content").orElseThrow();
        McpJsonSerializerV4 serializer = (McpJsonSerializerV4) SERIALIZER;
        McpSamplingToolResultContent content = (McpSamplingToolResultContent) serializer.parseContent(
                wireResponse.objectValue("result").orElseThrow().objectValue("content").orElseThrow());
        McpSamplingMessage resultMessage = McpSamplingMessage.builder()
                .role(McpRole.USER)
                .addContent(content)
                .build();

        McpToolResult result = content.result();
        assertThat(content.toolUseId(), is("call-1"));
        assertThat(result.textContents().getFirst().text(), is("18 C"));
        assertThat(result.imageContents().getFirst().data(), is("data".getBytes(StandardCharsets.UTF_8)));
        assertThat(result.audioContents().getFirst().data(), is("data".getBytes(StandardCharsets.UTF_8)));
        McpToolTextResourceContent textResource = result.textResourceContents().getFirst();
        assertThat(textResource.text(), is("sunny"));
        assertThat(textResource.metadata().orElseThrow().get("embeddedText").asBoolean().orElseThrow(), is(true));
        assertThat(textResource.metadata().orElseThrow().get("resourceText").isEmpty(), is(true));
        McpToolBinaryResourceContent binaryResource = result.binaryResourceContents().getFirst();
        assertThat(binaryResource.data(), is("data".getBytes(StandardCharsets.UTF_8)));
        assertThat(binaryResource.metadata().orElseThrow().get("embeddedBinary").asBoolean().orElseThrow(), is(true));
        assertThat(binaryResource.metadata().orElseThrow().get("resourceBinary").isEmpty(), is(true));
        McpToolResourceLinkContent link = result.resourceLinkContents().getFirst();
        assertThat(link.name(), is("weather"));
        assertThat(link.icons().getFirst().theme().orElseThrow(), is(McpIconTheme.DARK));
        assertThat(result.structuredContent().isPresent(), is(true));
        assertThat(result.error(), is(false));
        assertThat(content.metadata().orElseThrow().get("cached").asBoolean().orElseThrow(), is(true));

        McpSamplingRequest replay = McpSamplingRequest.builder()
                .addMessage(McpSamplingMessage.builder()
                                    .role(McpRole.ASSISTANT)
                                    .addContent(McpSamplingToolUseContent.builder()
                                                        .id("call-1")
                                                        .name("weather")
                                                        .input(new McpParameters(JsonObject.empty()))
                                                        .build())
                                    .build())
                .addMessage(resultMessage)
                .build();
        JsonArray replayedContent = SERIALIZER.toJson(replay, List.of()).build()
                .arrayValue("messages").orElseThrow()
                .values().get(1).asObject()
                .objectValue("content").orElseThrow()
                .arrayValue("content").orElseThrow();
        assertThat(replayedContent.values().stream()
                           .map(JsonValue::asObject)
                           .map(block -> block.stringValue("type").orElseThrow())
                           .toList(),
                   contains("text", "image", "audio", "resource", "resource", "resource_link"));
        assertThat(replayedContent.values().get(0).toString(), is(wireContent.values().get(1).toString()));
        assertThat(replayedContent.values().get(1).toString(), is(wireContent.values().get(0).toString()));
        assertThat(replayedContent.values().get(2).toString(), is(wireContent.values().get(4).toString()));
        assertThat(replayedContent.values().get(5).toString(), is(wireContent.values().get(2).toString()));
        JsonObject replayedTextResource = replayedContent.values().get(3).asObject();
        assertThat(replayedTextResource.objectValue(META).orElseThrow()
                           .booleanValue("embeddedText").orElseThrow(),
                   is(true));
        assertThat(replayedTextResource.objectValue("resource").orElseThrow().objectValue(META).isEmpty(), is(true));
        JsonObject replayedBinaryResource = replayedContent.values().get(4).asObject();
        assertThat(replayedBinaryResource.objectValue(META).orElseThrow()
                           .booleanValue("embeddedBinary").orElseThrow(),
                   is(true));
        assertThat(replayedBinaryResource.objectValue("resource").orElseThrow()
                           .objectValue(META).isEmpty(),
                   is(true));
    }

    @Test
    void rejectsUnspecifiedWireIconTheme() {
        JsonObject wireResponse = response("""
                {
                  "type": "tool_result",
                  "toolUseId": "call-1",
                  "content": [
                    {
                      "type": "resource_link",
                      "uri": "https://example.com/resource",
                      "name": "resource",
                      "icons": [
                        {
                          "src": "https://example.com/icon.svg",
                          "theme": "unspecified"
                        }
                      ]
                    }
                  ]
                }
                """, "endTurn", "user");
        McpJsonSerializerV4 serializer = (McpJsonSerializerV4) SERIALIZER;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                                                           () -> serializer.parseContent(wireResponse
                                                                   .objectValue("result").orElseThrow()
                                                                   .objectValue("content").orElseThrow()));

        assertThat(exception.getMessage(), is("Unsupported icon theme: unspecified"));
    }

    @Test
    void preservesSamplingMessageAndContentMetadata() {
        McpSamplingResponse response = SERIALIZER.createSamplingResponse(JsonParser.create("""
                {
                  "jsonrpc": "2.0",
                  "id": 1,
                  "result": {
                    "model": "test-model",
                    "role": "assistant",
                    "content": {
                      "type": "text",
                      "text": "hello",
                      "annotations": {
                        "audience": ["user", "assistant"],
                        "priority": 0.8,
                        "lastModified": "2026-08-06T10:15:30Z"
                      },
                      "_meta": {"contentKey": "contentValue"}
                    },
                    "_meta": {"messageKey": "messageValue"},
                    "stopReason": "endTurn"
                  }
                }
                """).readJsonObject());

        assertThat(response.message().metadata().orElseThrow()
                           .get("messageKey").asString().orElseThrow(),
                   is("messageValue"));
        assertThat(response.asTextContent().metadata().orElseThrow()
                           .get("contentKey").asString().orElseThrow(),
                   is("contentValue"));
        McpAnnotations annotations = response.asTextContent().annotations().orElseThrow();
        assertThat(annotations.audience(), contains(McpRole.USER, McpRole.ASSISTANT));
        assertThat(annotations.priority().orElseThrow(), is(0.8));
        assertThat(annotations.lastModified().orElseThrow(), is("2026-08-06T10:15:30Z"));

        McpSamplingRequest request = McpSamplingRequest.builder()
                .addMessage(response.message())
                .build();
        JsonObject serialized = SERIALIZER.toJson(request, List.of()).build()
                .arrayValue("messages").orElseThrow()
                .values().getFirst().asObject();

        assertThat(serialized.objectValue(META).orElseThrow()
                           .stringValue("messageKey").orElseThrow(),
                   is("messageValue"));
        assertThat(serialized.objectValue("content").orElseThrow()
                           .objectValue(META).orElseThrow()
                           .stringValue("contentKey").orElseThrow(),
                   is("contentValue"));
        JsonObject serializedAnnotations = serialized.objectValue("content").orElseThrow()
                .objectValue("annotations").orElseThrow();
        assertThat(serializedAnnotations.arrayValue("audience").orElseThrow().values().stream()
                           .map(JsonValue::asString)
                           .map(JsonString::value)
                           .toList(),
                   contains("user", "assistant"));
        assertThat(serializedAnnotations.doubleValue("priority").orElseThrow(), is(0.8));
        assertThat(serializedAnnotations.stringValue("lastModified").orElseThrow(),
                   is("2026-08-06T10:15:30Z"));
    }

    @Test
    void preservesSamplingContentAnnotationsAndMetadataIn2025JuneProtocol() {
        McpSamplingTextContent content = McpSamplingTextContent.builder()
                .text("hello")
                .annotations(McpAnnotations.builder()
                                     .addAudience(McpRole.USER)
                                     .priority(0.8)
                                     .lastModified("2026-08-06T10:15:30Z")
                                     .build())
                .metadata(McpParameters.create(Map.of("provider", "test")))
                .build();
        McpSamplingRequest request = McpSamplingRequest.builder()
                .addMessage(McpSamplingMessage.builder()
                                    .role(McpRole.USER)
                                    .addContent(content)
                                    .build())
                .build();

        JsonObject serialized = LEGACY_SERIALIZER.toJson(request, List.of()).build()
                .arrayValue("messages").orElseThrow()
                .values().getFirst().asObject()
                .objectValue("content").orElseThrow();

        JsonObject annotations = serialized.objectValue("annotations").orElseThrow();
        assertThat(annotations.arrayValue("audience").orElseThrow().values().getFirst().asString().value(),
                   is("user"));
        assertThat(annotations.doubleValue("priority").orElseThrow(), is(0.8));
        assertThat(annotations.stringValue("lastModified").orElseThrow(), is("2026-08-06T10:15:30Z"));
        assertThat(serialized.objectValue(META).orElseThrow().stringValue("provider").orElseThrow(),
                   is("test"));
    }

    @Test
    void parsesSamplingAnnotationsIndependentlyOfDefaultLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr"));
            McpSamplingResponse response = SERIALIZER.createSamplingResponse(response("""
                    {
                      "type": "text",
                      "text": "hello",
                      "annotations": {"audience": ["assistant"]}
                    }
                    """, "endTurn"));

            assertThat(response.asTextContent().annotations().orElseThrow().audience(),
                       contains(McpRole.ASSISTANT));
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void validatesAnnotationPriorityRange() {
        assertThat(McpAnnotations.builder().priority(0.0).build().priority().orElseThrow(), is(0.0));
        assertThat(McpAnnotations.builder().priority(1.0).build().priority().orElseThrow(), is(1.0));
        assertThat(McpAnnotations.builder().priority(0.5).clearPriority().build().priority().isEmpty(), is(true));

        for (double priority : List.of(-0.1, 1.1, Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)) {
            assertThrows(IllegalArgumentException.class, () -> McpAnnotations.builder().priority(priority));
        }
    }

    @Test
    void parsesParallelSamplingToolUseContentBlocks() {
        McpSamplingResponse response = SERIALIZER.createSamplingResponse(response("""
                [
                  {
                    "type": "tool_use",
                    "id": "call-1",
                    "name": "weather",
                    "input": {"city": "Prague"}
                  },
                  {
                    "type": "tool_use",
                    "id": "call-2",
                    "name": "weather",
                    "input": {"city": "London"}
                  }
                ]
                """, "toolUse"));

        List<McpSamplingContent> contents = response.message().contents();
        assertThat(response.message().role(), is(McpRole.ASSISTANT));
        assertThat(contents.size(), is(2));
        assertThat(contents.get(0), instanceOf(McpSamplingToolUseContent.class));
        assertThat(contents.get(1), instanceOf(McpSamplingToolUseContent.class));
        McpSamplingToolUseContent first = (McpSamplingToolUseContent) contents.get(0);
        McpSamplingToolUseContent second = (McpSamplingToolUseContent) contents.get(1);
        assertThat(first.id(), is("call-1"));
        assertThat(first.input().get("city").asString().orElse(""), is("Prague"));
        assertThat(second.id(), is("call-2"));
        assertThat(second.input().get("city").asString().orElse(""), is("London"));
        assertThat(response.asToolUseContent(), sameInstance(first));
    }

    @Test
    void rejectsDuplicateSamplingToolUseIdentifiersInResponse() {
        McpSamplingException exception = assertThrows(McpSamplingException.class,
                                                      () -> SERIALIZER.createSamplingResponse(response("""
                                                              [
                                                                {
                                                                  "type": "tool_use",
                                                                  "id": "call-1",
                                                                  "name": "weather",
                                                                  "input": {}
                                                                },
                                                                {
                                                                  "type": "tool_use",
                                                                  "id": "call-1",
                                                                  "name": "weather",
                                                                  "input": {}
                                                                }
                                                              ]
                                                              """, "toolUse")));

        assertThat(exception.getMessage(), is("Sampling tool use identifiers must be unique within a message"));
    }

    @Test
    void groupsSamplingToolResultsWithStructuredContentAndError() {
        McpSamplingToolUseContent firstUse = McpSamplingToolUseContent.builder()
                .id("call-1")
                .name("weather")
                .input(new McpParameters(JsonParser.create("{\"city\":\"Prague\"}").readJsonObject()))
                .build();
        McpSamplingToolUseContent secondUse = McpSamplingToolUseContent.builder()
                .id("call-2")
                .name("weather")
                .input(new McpParameters(JsonParser.create("{\"city\":\"London\"}").readJsonObject()))
                .build();
        McpSamplingToolResultContent firstResult = McpSamplingToolResultContent.builder()
                .toolUseId("call-1")
                .result(McpToolResult.builder()
                                .structuredContent(Map.of("temperature", 18))
                                .build())
                .metadata(McpParameters.create(Map.of("cached", true)))
                .build();
        McpSamplingToolResultContent secondResult = McpSamplingToolResultContent.builder()
                .toolUseId("call-2")
                .result(McpToolResult.builder()
                                .addTextContent("Weather provider failed")
                                .error(true)
                                .build())
                .build();
        McpSamplingRequest request = McpSamplingRequest.builder()
                .addMessage(McpSamplingMessage.builder()
                                    .role(McpRole.USER)
                                    .addContent(McpSamplingTextContent.create("Compare the weather"))
                                    .build())
                .addMessage(McpSamplingMessage.builder()
                                    .role(McpRole.ASSISTANT)
                                    .addContent(firstUse)
                                    .addContent(secondUse)
                                    .build())
                .addMessage(McpSamplingMessage.builder()
                                    .role(McpRole.USER)
                                    .addContent(firstResult)
                                    .addContent(secondResult)
                                    .build())
                .build();

        JsonArray messages = SERIALIZER.toJson(request, List.of()).build().arrayValue("messages").orElseThrow();
        assertThat(messages.size(), is(3));
        JsonObject resultGroup = messages.values().get(2).asObject();
        JsonArray results = resultGroup.arrayValue("content").orElseThrow();
        JsonObject first = results.values().get(0).asObject();
        JsonObject second = results.values().get(1).asObject();
        assertThat(resultGroup.stringValue("role").orElseThrow(), is("user"));
        assertThat(results.size(), is(2));
        assertThat(first.stringValue("type").orElseThrow(), is("tool_result"));
        assertThat(first.stringValue("toolUseId").orElseThrow(), is("call-1"));
        JsonArray firstContent = first.arrayValue("content").orElseThrow();
        assertThat(firstContent.size(), is(1));
        assertThat(firstContent.values().getFirst().asObject()
                           .stringValue("text").orElseThrow(),
                   is("{\"temperature\":18}"));
        assertThat(first.booleanValue("isError").orElseThrow(), is(false));
        assertThat(first.objectValue("structuredContent").orElseThrow()
                           .intValue("temperature").orElseThrow(),
                   is(18));
        assertThat(first.objectValue(META).orElseThrow()
                           .booleanValue("cached").orElseThrow(),
                   is(true));
        assertThat(second.stringValue("type").orElseThrow(), is("tool_result"));
        assertThat(second.stringValue("toolUseId").orElseThrow(), is("call-2"));
        assertThat(second.arrayValue("content").orElseThrow().values().getFirst().asObject()
                           .stringValue("text").orElseThrow(),
                   is("Weather provider failed"));
        assertThat(second.booleanValue("isError").orElseThrow(), is(true));
        assertThat(second.value("structuredContent").isEmpty(), is(true));
    }

    @Test
    void preservesOrderedMultiRoundSamplingMessages() {
        McpSamplingRequest request = McpSamplingRequest.builder()
                .addMessage(McpSamplingMessage.builder()
                                    .role(McpRole.USER)
                                    .addContent(McpSamplingTextContent.create("Weather in Prague?"))
                                    .build())
                .addMessage(McpSamplingMessage.builder()
                                    .role(McpRole.ASSISTANT)
                                    .addContent(McpSamplingToolUseContent.builder()
                                                        .id("call-1")
                                                        .name("weather")
                                                        .input(new McpParameters(JsonParser.create("""
                                                                {"city":"Prague"}
                                                                """).readJsonObject()))
                                                        .build())
                                    .build())
                .addMessage(McpSamplingMessage.builder()
                                    .role(McpRole.USER)
                                    .addContent(McpSamplingToolResultContent.builder()
                                                        .toolUseId("call-1")
                                                        .result(McpToolResult.create("18 C"))
                                                        .build())
                                    .build())
                .addMessage(McpSamplingMessage.builder()
                                    .role(McpRole.ASSISTANT)
                                    .addContent(McpSamplingTextContent.create("It is 18 C in Prague"))
                                    .build())
                .addMessage(McpSamplingMessage.builder()
                                    .role(McpRole.USER)
                                    .addContent(McpSamplingTextContent.create("And London?"))
                                    .build())
                .addMessage(McpSamplingMessage.builder()
                                    .role(McpRole.ASSISTANT)
                                    .addContent(McpSamplingToolUseContent.builder()
                                                        .id("call-2")
                                                        .name("weather")
                                                        .input(new McpParameters(JsonParser.create("""
                                                                {"city":"London"}
                                                                """).readJsonObject()))
                                                        .build())
                                    .build())
                .addMessage(McpSamplingMessage.builder()
                                    .role(McpRole.USER)
                                    .addContent(McpSamplingToolResultContent.builder()
                                                        .toolUseId("call-2")
                                                        .result(McpToolResult.create("15 C"))
                                                        .build())
                                    .build())
                .build();

        JsonArray messages = SERIALIZER.toJson(request, List.of()).build().arrayValue("messages").orElseThrow();

        assertThat(messages.values().stream()
                           .map(JsonValue::asObject)
                           .map(message -> message.stringValue("role").orElseThrow())
                           .toList(),
                   contains("user", "assistant", "user", "assistant", "user", "assistant", "user"));
        assertThat(messages.values().stream()
                           .map(JsonValue::asObject)
                           .map(message -> message.objectValue("content").orElseThrow())
                           .map(content -> content.stringValue("type").orElseThrow())
                           .toList(),
                   contains("text", "tool_use", "tool_result", "text", "text", "tool_use", "tool_result"));
        assertThat(messages.values().get(1).asObject()
                           .objectValue("content").orElseThrow()
                           .stringValue("id").orElseThrow(),
                   is("call-1"));
        assertThat(messages.values().get(5).asObject()
                           .objectValue("content").orElseThrow()
                           .stringValue("id").orElseThrow(),
                   is("call-2"));
    }

    @Test
    void preservesAdjacentSameRoleSamplingMessageBoundaries() {
        McpSamplingRequest request = McpSamplingRequest.builder()
                .addMessage(McpSamplingMessage.builder()
                                    .role(McpRole.USER)
                                    .addContent(McpSamplingTextContent.create("first"))
                                    .build())
                .addMessage(McpSamplingMessage.builder()
                                    .role(McpRole.USER)
                                    .addContent(McpSamplingTextContent.create("second"))
                                    .build())
                .build();

        JsonArray messages = SERIALIZER.toJson(request, List.of()).build().arrayValue("messages").orElseThrow();

        assertThat(messages.size(), is(2));
        assertThat(messages.values().get(0).asObject()
                           .objectValue("content").orElseThrow()
                           .stringValue("text").orElseThrow(),
                   is("first"));
        assertThat(messages.values().get(1).asObject()
                           .objectValue("content").orElseThrow()
                           .stringValue("text").orElseThrow(),
                   is("second"));
    }

    @Test
    void keepsContentAfterToolUseInTheSameSamplingMessage() {
        McpSamplingRequest request = McpSamplingRequest.builder()
                .addMessage(McpSamplingMessage.builder()
                                    .role(McpRole.ASSISTANT)
                                    .addContent(McpSamplingToolUseContent.builder()
                                                        .id("call-1")
                                                        .name("weather")
                                                        .input(new McpParameters(JsonObject.empty()))
                                                        .build())
                                    .addContent(McpSamplingTextContent.create("Calling the weather tool"))
                                    .build())
                .addMessage(McpSamplingMessage.builder()
                                    .role(McpRole.USER)
                                    .addContent(McpSamplingToolResultContent.builder()
                                                        .toolUseId("call-1")
                                                        .result(McpToolResult.create("18 C"))
                                                        .build())
                                    .build())
                .build();

        JsonArray messages = SERIALIZER.toJson(request, List.of()).build().arrayValue("messages").orElseThrow();

        assertThat(messages.size(), is(2));
        JsonArray assistantContent = messages.values().getFirst().asObject()
                .arrayValue("content")
                .orElseThrow();
        assertThat(assistantContent.values().stream()
                           .map(JsonValue::asObject)
                           .map(content -> content.stringValue("type").orElseThrow())
                           .toList(),
                   contains("tool_use", "text"));
        assertThat(messages.values().get(1).asObject()
                           .objectValue("content").orElseThrow()
                           .stringValue("type").orElseThrow(),
                   is("tool_result"));
    }

    @Test
    void validatesSamplingToolUseAndResultBalance() {
        McpSamplingToolUseContent toolUse = McpSamplingToolUseContent.builder()
                .id("call-1")
                .name("weather")
                .input(new McpParameters(JsonObject.empty()))
                .build();
        McpSamplingMessage toolUseMessage = McpSamplingMessage.builder()
                .role(McpRole.ASSISTANT)
                .addContent(toolUse)
                .build();
        McpSamplingRequest missingResult = McpSamplingRequest.builder()
                .addMessage(toolUseMessage)
                .build();
        McpSamplingRequest mismatchedResult = McpSamplingRequest.builder()
                .addMessage(toolUseMessage)
                .addMessage(McpSamplingMessage.builder()
                                    .role(McpRole.USER)
                                    .addContent(McpSamplingToolResultContent.builder()
                                                        .toolUseId("call-2")
                                                        .result(McpToolResult.create("result"))
                                                        .build())
                                    .build())
                .build();
        McpSamplingRequest orphanResult = McpSamplingRequest.builder()
                .addMessage(McpSamplingMessage.builder()
                                    .role(McpRole.USER)
                                    .addContent(McpSamplingToolResultContent.builder()
                                                        .toolUseId("call-1")
                                                        .result(McpToolResult.create("result"))
                                                        .build())
                                    .build())
                .build();

        McpSamplingException missing = assertThrows(McpSamplingException.class,
                                                    () -> SERIALIZER.toJson(missingResult, List.of()));
        McpSamplingException mismatched = assertThrows(McpSamplingException.class,
                                                       () -> SERIALIZER.toJson(mismatchedResult, List.of()));
        McpSamplingException orphan = assertThrows(McpSamplingException.class,
                                                   () -> SERIALIZER.toJson(orphanResult, List.of()));

        assertThat(missing.getMessage(), is("Sampling tool uses must be followed by matching tool results"));
        assertThat(mismatched.getMessage(), is("Sampling tool uses must be followed by matching tool results"));
        assertThat(orphan.getMessage(), is("Sampling tool result does not match a preceding tool use"));
    }

    @Test
    void validatesSamplingToolContentRoles() {
        McpSamplingToolUseContent toolUse = McpSamplingToolUseContent.builder()
                .id("call-1")
                .name("weather")
                .input(new McpParameters(JsonObject.empty()))
                .build();
        McpSamplingToolResultContent toolResult = McpSamplingToolResultContent.builder()
                .toolUseId("call-1")
                .result(McpToolResult.create("result"))
                .build();
        McpSamplingRequest userToolUse = McpSamplingRequest.builder()
                .addMessage(McpSamplingMessage.builder()
                                    .role(McpRole.USER)
                                    .addContent(toolUse)
                                    .build())
                .build();
        McpSamplingRequest assistantToolResult = McpSamplingRequest.builder()
                .addMessage(McpSamplingMessage.builder()
                                    .role(McpRole.ASSISTANT)
                                    .addContent(toolUse)
                                    .build())
                .addMessage(McpSamplingMessage.builder()
                                    .role(McpRole.ASSISTANT)
                                    .addContent(toolResult)
                                    .build())
                .build();

        McpSamplingException useRole = assertThrows(McpSamplingException.class,
                                                    () -> SERIALIZER.toJson(userToolUse, List.of()));
        McpSamplingException resultRole = assertThrows(McpSamplingException.class,
                                                       () -> SERIALIZER.toJson(assistantToolResult, List.of()));

        assertThat(useRole.getMessage(), is("Sampling tool use content must have an assistant message role"));
        assertThat(resultRole.getMessage(), is("Sampling tool result content must have a user message role"));
    }

    @Test
    void rejectsDuplicateAndInterruptedSamplingToolResults() {
        McpSamplingToolUseContent firstUse = McpSamplingToolUseContent.builder()
                .id("call-1")
                .name("weather")
                .input(new McpParameters(JsonObject.empty()))
                .build();
        McpSamplingToolUseContent secondUse = McpSamplingToolUseContent.builder()
                .id("call-2")
                .name("weather")
                .input(new McpParameters(JsonObject.empty()))
                .build();
        McpSamplingToolResultContent firstResult = McpSamplingToolResultContent.builder()
                .toolUseId("call-1")
                .result(McpToolResult.create("result"))
                .build();
        McpSamplingRequest duplicateUses = McpSamplingRequest.builder()
                .addMessage(McpSamplingMessage.builder()
                                    .role(McpRole.ASSISTANT)
                                    .addContent(firstUse)
                                    .addContent(McpSamplingToolUseContent.builder(firstUse).build())
                                    .build())
                .build();
        McpSamplingRequest duplicateResults = McpSamplingRequest.builder()
                .addMessage(McpSamplingMessage.builder()
                                    .role(McpRole.ASSISTANT)
                                    .addContent(firstUse)
                                    .addContent(secondUse)
                                    .build())
                .addMessage(McpSamplingMessage.builder()
                                    .role(McpRole.USER)
                                    .addContent(firstResult)
                                    .addContent(McpSamplingToolResultContent.builder(firstResult).build())
                                    .build())
                .build();
        McpSamplingRequest interruptedResults = McpSamplingRequest.builder()
                .addMessage(McpSamplingMessage.builder()
                                    .role(McpRole.ASSISTANT)
                                    .addContent(firstUse)
                                    .build())
                .addMessage(McpSamplingMessage.builder()
                                    .role(McpRole.USER)
                                    .addContent(McpSamplingTextContent.create("intervening message"))
                                    .build())
                .addMessage(McpSamplingMessage.builder()
                                    .role(McpRole.USER)
                                    .addContent(firstResult)
                                    .build())
                .build();

        McpSamplingException duplicateUse = assertThrows(McpSamplingException.class,
                                                         () -> SERIALIZER.toJson(duplicateUses, List.of()));
        McpSamplingException duplicateResult = assertThrows(McpSamplingException.class,
                                                            () -> SERIALIZER.toJson(duplicateResults, List.of()));
        McpSamplingException interrupted = assertThrows(McpSamplingException.class,
                                                        () -> SERIALIZER.toJson(interruptedResults, List.of()));

        assertThat(duplicateUse.getMessage(), is("Sampling tool use identifiers must be unique within a message"));
        assertThat(duplicateResult.getMessage(), is("Sampling tool uses must be followed by matching tool results"));
        assertThat(interrupted.getMessage(), is("Sampling tool uses must be followed by matching tool results"));
    }

    @Test
    void rejectsNonObjectSamplingToolParameters() {
        McpParameters scalarParameter = new McpParameters(JsonObject.builder().set("value", 1).build()).get("value");
        McpSamplingToolUseContent validUse = McpSamplingToolUseContent.builder()
                .id("call-1")
                .name("weather")
                .input(new McpParameters(JsonObject.empty()))
                .build();
        McpSamplingToolResultContent validResult = McpSamplingToolResultContent.builder()
                .toolUseId("call-1")
                .result(McpToolResult.create("result"))
                .build();
        McpSamplingRequest scalarInput = McpSamplingRequest.builder()
                .addMessage(McpSamplingMessage.builder()
                                    .role(McpRole.ASSISTANT)
                                    .addContent(McpSamplingToolUseContent.builder()
                                                        .id("call-1")
                                                        .name("weather")
                                                        .input(scalarParameter)
                                                        .build())
                                    .build())
                .addMessage(McpSamplingMessage.builder()
                                    .role(McpRole.USER)
                                    .addContent(validResult)
                                    .build())
                .build();
        McpSamplingRequest scalarUseMeta = McpSamplingRequest.builder()
                .addMessage(McpSamplingMessage.builder()
                                    .role(McpRole.ASSISTANT)
                                    .addContent(McpSamplingToolUseContent.builder(validUse)
                                                        .metadata(scalarParameter)
                                                        .build())
                                    .build())
                .addMessage(McpSamplingMessage.builder()
                                    .role(McpRole.USER)
                                    .addContent(validResult)
                                    .build())
                .build();
        McpSamplingRequest scalarResultMeta = McpSamplingRequest.builder()
                .addMessage(McpSamplingMessage.builder()
                                    .role(McpRole.ASSISTANT)
                                    .addContent(validUse)
                                    .build())
                .addMessage(McpSamplingMessage.builder()
                                    .role(McpRole.USER)
                                    .addContent(McpSamplingToolResultContent.builder(validResult)
                                                        .metadata(scalarParameter)
                                                        .build())
                                    .build())
                .build();
        McpSamplingRequest scalarMessageMeta = McpSamplingRequest.builder()
                .addMessage(McpSamplingMessage.builder()
                                    .role(McpRole.ASSISTANT)
                                    .metadata(scalarParameter)
                                    .addContent(validUse)
                                    .build())
                .addMessage(McpSamplingMessage.builder()
                                    .role(McpRole.USER)
                                    .addContent(validResult)
                                    .build())
                .build();

        McpSamplingException input = assertThrows(McpSamplingException.class,
                                                  () -> SERIALIZER.toJson(scalarInput, List.of()));
        McpSamplingException useMeta = assertThrows(McpSamplingException.class,
                                                    () -> SERIALIZER.toJson(scalarUseMeta, List.of()));
        McpSamplingException resultMeta = assertThrows(McpSamplingException.class,
                                                       () -> SERIALIZER.toJson(scalarResultMeta, List.of()));
        McpSamplingException messageMeta = assertThrows(McpSamplingException.class,
                                                        () -> SERIALIZER.toJson(scalarMessageMeta, List.of()));

        assertThat(input.getMessage(), is("Sampling tool input must be a JSON object"));
        assertThat(useMeta.getMessage(), is("Sampling content metadata must be a JSON object"));
        assertThat(resultMeta.getMessage(), is("Sampling content metadata must be a JSON object"));
        assertThat(messageMeta.getMessage(), is("Sampling message metadata must be a JSON object"));
    }

    @Test
    void parsesSamplingContentBlockArray() {
        McpSamplingResponse response = SERIALIZER.createSamplingResponse(response("""
                [
                  {
                    "type": "text",
                    "text": "first"
                  },
                  {
                    "type": "text",
                    "text": "second"
                  }
                ]
                """, "endTurn"));

        List<McpSamplingContent> contents = response.message().contents();
        assertThat(response.message().role(), is(McpRole.ASSISTANT));
        assertThat(contents.size(), is(2));
        assertThat(((McpSamplingTextContent) contents.get(0)).text(), is("first"));
        assertThat(((McpSamplingTextContent) contents.get(1)).text(), is("second"));
        assertThat(response.model(), is("test-model"));
        assertThat(response.rawStopReason().orElseThrow(), is("endTurn"));
        assertThat(response.stopReason().orElseThrow(), is(McpStopReason.END_TURN));
        assertThrows(UnsupportedOperationException.class, contents::clear);
    }

    @Test
    void preservesSamplingMediaThroughToolLoopReplay() {
        McpSamplingResponse response = SERIALIZER.createSamplingResponse(response("""
                [
                  {
                    "type": "image",
                    "data": "ZGF0YQ==",
                    "mimeType": "image/png"
                  },
                  {
                    "type": "audio",
                    "data": "ZGF0YQ==",
                    "mimeType": "audio/wav"
                  }
                ]
                """, "endTurn"));
        McpSamplingRequest request = McpSamplingRequest.builder()
                .addMessage(response.message())
                .build();

        assertThat(new String(response.asImageContent().data(), StandardCharsets.UTF_8), is("data"));
        assertThat(response.asImageContent().encodeBase64Data(), is("ZGF0YQ=="));
        McpSamplingAudioContent audio = (McpSamplingAudioContent) response.message().contents().get(1);
        assertThat(new String(audio.data(), StandardCharsets.UTF_8), is("data"));
        assertThat(audio.encodeBase64Data(), is("ZGF0YQ=="));

        JsonArray replayedMessages = SERIALIZER.toJson(request, List.of())
                .build()
                .arrayValue("messages")
                .orElseThrow();
        assertThat(replayedMessages.size(), is(1));
        JsonArray replayedContent = replayedMessages.values().getFirst().asObject()
                .arrayValue("content")
                .orElseThrow();
        assertThat(replayedContent.values().get(0).asObject()
                           .stringValue("data").orElseThrow(),
                   is("ZGF0YQ=="));
        assertThat(replayedContent.values().get(1).asObject()
                           .stringValue("data").orElseThrow(),
                   is("ZGF0YQ=="));
    }

    @Test
    void preservesMixedSamplingResponseBoundaryDuringReplay() {
        McpSamplingResponse response = SERIALIZER.createSamplingResponse(response("""
                [
                  {
                    "type": "text",
                    "text": "I will call the weather tool"
                  },
                  {
                    "type": "tool_use",
                    "id": "call-1",
                    "name": "weather",
                    "input": {"city": "Prague"}
                  }
                ]
                """, "toolUse"));
        McpSamplingRequest request = McpSamplingRequest.builder()
                .addMessage(response.message())
                .addMessage(McpSamplingMessage.builder()
                                    .role(McpRole.USER)
                                    .addContent(McpSamplingToolResultContent.builder()
                                                        .toolUseId("call-1")
                                                        .result(McpToolResult.create("18 C"))
                                                        .build())
                                    .build())
                .build();

        JsonArray messages = SERIALIZER.toJson(request, List.of()).build().arrayValue("messages").orElseThrow();

        assertThat(messages.size(), is(2));
        JsonArray assistantContent = messages.values().getFirst().asObject()
                .arrayValue("content")
                .orElseThrow();
        assertThat(assistantContent.values().stream()
                           .map(JsonValue::asObject)
                           .map(content -> content.stringValue("type").orElseThrow())
                           .toList(),
                   contains("text", "tool_use"));
        assertThat(messages.values().get(1).asObject()
                           .objectValue("content").orElseThrow()
                           .stringValue("type").orElseThrow(),
                   is("tool_result"));
    }

    @Test
    void preservesSingularAccessForSamplingContentBlockArray() {
        McpSamplingResponse response = SERIALIZER.createSamplingResponse(response("""
                [
                  {
                    "type": "text",
                    "text": "first"
                  },
                  {
                    "type": "text",
                    "text": "second"
                  }
                ]
                """, "endTurn"));

        assertThat(response.asTextContent(), sameInstance(response.message().contents().getFirst()));
        assertThat(response.asTextContent().text(), is("first"));
    }

    @Test
    void supportsEmptySamplingContentBlockArray() {
        McpSamplingResponse response = SERIALIZER.createSamplingResponse(response("[]", "endTurn"));

        assertThat(response.message().role(), is(McpRole.ASSISTANT));
        assertThat(response.message().contents().isEmpty(), is(true));
        assertThrows(UnsupportedOperationException.class,
                     () -> response.message().contents().add(McpSamplingTextContent.create("unexpected")));
        assertThrows(McpSamplingException.class, response::asTextContent);

        McpSamplingRequest request = McpSamplingRequest.builder()
                .addMessage(response.message())
                .build();
        JsonArray serializedMessages = SERIALIZER.toJson(request, List.of()).build().arrayValue("messages").orElseThrow();
        assertThat(serializedMessages.size(), is(1));
        assertThat(serializedMessages.values().getFirst().asObject()
                           .arrayValue("content").orElseThrow().size(),
                   is(0));
    }

    @Test
    void parsesToolUseStopReason() {
        McpSamplingResponse response = SERIALIZER.createSamplingResponse(response("""
                {
                  "type": "text",
                  "text": "first"
                }
                """, "toolUse"));

        assertThat(response.rawStopReason().orElseThrow(), is("toolUse"));
        assertThat(response.stopReason().orElseThrow(), is(McpStopReason.TOOL_USE));
    }

    @Test
    void preservesCaseOfKnownSamplingStopReason() {
        McpSamplingResponse response = SERIALIZER.createSamplingResponse(response("""
                {
                  "type": "text",
                  "text": "first"
                }
                """, "EndTurn"));

        assertThat(response.rawStopReason().orElseThrow(), is("EndTurn"));
        assertThat(response.stopReason().orElseThrow(), is(McpStopReason.END_TURN));
    }

    @ParameterizedTest
    @EnumSource(McpProtocolVersion.class)
    void acceptsUnknownSamplingStopReason(McpProtocolVersion version) {
        McpSamplingResponse response = McpJsonSerializer.create(version).createSamplingResponse(response("""
                {
                  "type": "text",
                  "text": "first"
                }
                """, "providerSpecificReason"));

        assertThat(response.asTextContent().text(), is("first"));
        assertThat(response.model(), is("test-model"));
        assertThat(response.rawStopReason().orElseThrow(), is("providerSpecificReason"));
        assertThat(response.stopReason().isPresent(), is(false));
    }

    @Test
    void preservesAbsentSamplingStopReason() {
        JsonObject result = JsonObject.builder()
                .set("model", "test-model")
                .set("role", "assistant")
                .set("content", JsonObject.builder()
                        .set("type", "text")
                        .set("text", "first")
                        .build())
                .build();
        JsonObject json = JsonObject.builder()
                .set("jsonrpc", "2.0")
                .set("id", 1)
                .set("result", result)
                .build();

        McpSamplingResponse response = SERIALIZER.createSamplingResponse(json);

        assertThat(response.rawStopReason().isEmpty(), is(true));
        assertThat(response.stopReason().isEmpty(), is(true));
    }

    @Test
    void rejectsNonObjectSamplingContentBlock() {
        McpSamplingException exception = assertThrows(McpSamplingException.class,
                                                      () -> SERIALIZER.createSamplingResponse(
                                                              response("[42]", "endTurn")));

        assertThat(exception.getMessage(), is("Wrong sampling response format"));
    }

    @Test
    void acceptsUserRoleOrdinarySamplingResponse() {
        McpSamplingResponse response = SERIALIZER.createSamplingResponse(response("""
                {
                  "type": "text",
                  "text": "user response"
                }
                """, "endTurn", "user"));

        assertThat(response.message().role(), is(McpRole.USER));
        assertThat(response.asTextContent().text(), is("user response"));
    }

    @Test
    void rejectsUserRoleSamplingToolUseResponse() {
        McpSamplingException exception = assertThrows(McpSamplingException.class,
                                                      () -> SERIALIZER.createSamplingResponse(response("""
                                                              {
                                                                "type": "tool_use",
                                                                "id": "call-1",
                                                                "name": "weather",
                                                                "input": {}
                                                              }
                                                              """, "toolUse", "user")));

        assertThat(exception.getMessage(), is("Sampling response must have an assistant message role"));
    }

    @Test
    void rejectsSamplingToolResultResponse() {
        McpSamplingException exception = assertThrows(McpSamplingException.class,
                                                      () -> SERIALIZER.createSamplingResponse(response("""
                                                              {
                                                                "type": "tool_result",
                                                                "toolUseId": "call-1",
                                                                "content": [],
                                                                "isError": false
                                                              }
                                                              """, "endTurn", "user")));

        assertThat(exception.getMessage(), is("Sampling response must not contain tool result content"));
    }

    @Test
    void keepsLegacySamplingContentObjectOnly() {
        McpSamplingException exception = assertThrows(McpSamplingException.class,
                                                      () -> LEGACY_SERIALIZER.createSamplingResponse(response("""
                                                              [
                                                                {
                                                                  "type": "text",
                                                                  "text": "first"
                                                                }
                                                              ]
                                                              """, "endTurn")));

        assertThat(exception.getMessage(), is("Wrong sampling response format"));
    }

    @ParameterizedTest
    @EnumSource(value = McpProtocolVersion.class,
                names = {"VERSION_2024_11_05", "VERSION_2025_03_26", "VERSION_2025_06_18"})
    void rejectsSamplingToolUseResponsesForLegacyProtocolVersions(McpProtocolVersion version) {
        McpSamplingException exception = assertThrows(McpSamplingException.class,
                                                      () -> McpJsonSerializer.create(version)
                                                              .createSamplingResponse(response("""
                                                                      {
                                                                        "type": "tool_use",
                                                                        "id": "call-1",
                                                                        "name": "weather",
                                                                        "input": {"city": "Prague"}
                                                                      }
                                                                      """, "toolUse")));

        assertThat(exception.getMessage(), is("Wrong sampling response format"));
    }

    private static JsonObject response(String content, String stopReason) {
        return response(content, stopReason, "assistant");
    }

    private static JsonObject response(String content, String stopReason, String role) {
        JsonValue parsedContent = JsonParser.create(content).readJsonValue();
        JsonObject result = JsonObject.builder()
                .set("model", "test-model")
                .set("role", role)
                .set("content", parsedContent)
                .set("stopReason", stopReason)
                .build();
        return JsonObject.builder()
                .set("jsonrpc", "2.0")
                .set("id", 1)
                .set("result", result)
                .build();
    }

    private static JsonObject component(JsonObject response, String property) {
        JsonArray components = response.arrayValue(property).orElseThrow();
        return components.get(0).map(JsonValue::asObject).orElseThrow();
    }

    private static McpTool tool() {
        McpToolConfig config = McpToolConfig.builder()
                .name("tool")
                .description("description")
                .schema("")
                .title("tool title")
                .annotations(McpToolAnnotations.builder()
                                     .title("annotation title")
                                     .destructiveHint(false)
                                     .idempotentHint(true)
                                     .openWorldHint(false)
                                     .readOnlyHint(true)
                                     .build())
                .outputSchema("")
                .addIcon(FULL_ICON)
                .addIcon(MINIMAL_ICON)
                .tool(request -> McpToolResult.create())
                .build();
        return new McpToolImpl(config);
    }

    private static McpPrompt prompt() {
        McpPromptConfig config = McpPromptConfig.builder()
                .name("prompt")
                .description("description")
                .title("prompt title")
                .addIcon(FULL_ICON)
                .addIcon(MINIMAL_ICON)
                .prompt(request -> McpPromptResult.create())
                .build();
        return new McpPromptImpl(config);
    }

    private static McpResource resource(String uri) {
        McpResourceConfig config = McpResourceConfig.builder()
                .uri(uri)
                .name("resource")
                .description("description")
                .title("resource title")
                .mediaType(MediaTypes.TEXT_PLAIN)
                .addIcon(FULL_ICON)
                .addIcon(MINIMAL_ICON)
                .resource(request -> McpResourceResult.create())
                .build();
        return new McpResourceImpl(config);
    }
}
