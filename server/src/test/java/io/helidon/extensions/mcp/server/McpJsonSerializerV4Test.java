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

import java.util.Set;

import io.helidon.json.JsonObject;
import io.helidon.json.JsonParser;
import io.helidon.json.JsonValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;

class McpJsonSerializerV4Test {
    private static final McpJsonSerializer SERIALIZER =
            McpJsonSerializer.create(McpProtocolVersion.VERSION_2025_11_25);
    private static final McpJsonSerializer LEGACY_SERIALIZER =
            McpJsonSerializer.create(McpProtocolVersion.VERSION_2025_06_18);

    @Test
    void serializesServerWebsiteUrl() {
        McpServerConfig config = McpServerConfig.builder()
                .websiteUrl("https://example.com/mcp")
                .buildPrototype();
        JsonObject response = SERIALIZER.createJsonInitializeResponse(Set.of(), config).build();
        JsonObject expected = JsonObject.builder()
                .set("name", "mcp-server")
                .set("version", "0.0.1")
                .set("websiteUrl", "https://example.com/mcp")
                .build();

        assertThat(response.objectValue("serverInfo").orElseThrow(), is(expected));
    }

    @Test
    void omitsUnconfiguredServerWebsiteUrl() {
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
    void omitsServerWebsiteUrlFromLegacyProtocolVersions(McpProtocolVersion version) {
        McpServerConfig config = McpServerConfig.builder()
                .websiteUrl("https://example.com/mcp")
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
    void parsesSingleSamplingContentBlock() {
        McpSamplingResponse response = SERIALIZER.createSamplingResponse(response("""
                {
                  "type": "text",
                  "text": "first"
                }
                """, "endTurn"));

        assertThat(response.messages().size(), is(1));
        assertThat(response.asTextMessage().text(), is("first"));
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

        assertThat(response.messages().size(), is(2));
        assertThat(((McpSamplingTextMessage) response.messages().get(0)).text(), is("first"));
        assertThat(((McpSamplingTextMessage) response.messages().get(1)).text(), is("second"));
        assertThat(response.messages().getFirst().role(), is(McpRole.ASSISTANT));
        assertThat(response.messages().get(1).role(), is(McpRole.ASSISTANT));
        assertThat(response.model(), is("test-model"));
        assertThat(response.rawStopReason().orElseThrow(), is("endTurn"));
        assertThat(response.stopReason().orElseThrow(), is(McpStopReason.END_TURN));
        assertThrows(UnsupportedOperationException.class, () -> response.messages().clear());
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

        assertThat(response.message(), sameInstance(response.messages().getFirst()));
        assertThat(response.asTextMessage().text(), is("first"));
    }

    @Test
    void supportsEmptySamplingContentBlockArray() {
        McpSamplingResponse response = SERIALIZER.createSamplingResponse(response("[]", "endTurn"));

        assertThat(response.messages().isEmpty(), is(true));
        assertThrows(McpSamplingException.class, response::message);
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

        assertThat(response.asTextMessage().text(), is("first"));
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

    private static JsonObject response(String content, String stopReason) {
        JsonValue parsedContent = JsonParser.create(content).readJsonValue();
        JsonObject result = JsonObject.builder()
                .set("model", "test-model")
                .set("role", "assistant")
                .set("content", parsedContent)
                .set("stopReason", stopReason)
                .build();
        return JsonObject.builder()
                .set("jsonrpc", "2.0")
                .set("id", 1)
                .set("result", result)
                .build();
    }
}
