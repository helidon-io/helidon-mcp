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
package io.helidon.extensions.mcp.server;

import java.util.Optional;
import java.util.Set;

import io.helidon.json.JsonObject;

/**
 * JSON serializer for {@code 2025-03-26} MCP specification.
 */
class McpJsonSerializerV2 extends McpJsonSerializerV1 {
    @Override
    public JsonObject.Builder createJsonInitializeResponse(Set<McpCapability> capabilities, McpServerConfig config) {
        return super.createJsonInitializeResponse(capabilities, config)
                .set("protocolVersion", McpProtocolVersion.VERSION_2025_03_26.text());
    }

    @Override
    public Optional<JsonObject.Builder> toJson(McpContent content) {
        if (content instanceof McpAudioContent audio) {
            return toJson(audio);
        }
        return super.toJson(content);
    }

    @Override
    public Optional<JsonObject.Builder> toJson(McpPromptContent content) {
        if (content instanceof McpPromptAudioContent resource) {
            return toJson(resource);
        }
        return super.toJson(content);
    }

    @Override
    public Optional<JsonObject.Builder> toJson(McpAudioContent content) {
        return Optional.of(JsonObject.builder()
                                   .set("type", content.type().text())
                                   .set("data", content.base64Data())
                                   .set("mimeType", content.mediaType().text()));
    }

    @Override
    public Optional<JsonObject.Builder> toJson(McpPromptAudioContent audio) {
        return toJson((McpAudioContent) audio)
                .map(content -> JsonObject.builder()
                        .set("role", audio.role().text())
                        .set("content", content.build()));
    }

    @Override
    public JsonObject.Builder toJson(McpTool tool) {
        var builder = super.toJson(tool);
        tool.annotations().ifPresent(annotations -> {
            JsonObject.Builder annotBuilder = JsonObject.builder();
            annotBuilder.set("title", annotations.title());
            annotBuilder.set("destructiveHint", annotations.destructiveHint());
            annotBuilder.set("idempotentHint", annotations.idempotentHint());
            annotBuilder.set("openWorldHint", annotations.openWorldHint());
            annotBuilder.set("readOnlyHint", annotations.readOnlyHint());
            builder.set("annotations", annotBuilder.build());
        });
        return builder;
    }
}
