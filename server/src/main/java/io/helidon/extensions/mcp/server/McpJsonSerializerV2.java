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

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObjectBuilder;

/**
 * JSON serializer for {@code 2025-03-26} MCP specification.
 */
class McpJsonSerializerV2 extends McpJsonSerializerV1 {
    private static final JsonBuilderFactory JSON_BUILDER_FACTORY = Json.createBuilderFactory(Map.of());

    @Override
    public JsonObjectBuilder toJson(Set<McpCapability> capabilities, McpServerConfig config) {
        return super.toJson(capabilities, config)
                .add("protocolVersion", McpProtocolVersion.VERSION_2025_03_26.text());
    }

    @Override
    public Optional<JsonObjectBuilder> toJson(McpContent content) {
        if (content instanceof McpAudioContent audio) {
            return toJson(audio);
        }
        return super.toJson(content);
    }

    @Override
    public Optional<JsonObjectBuilder> toJson(McpPromptContent content) {
        if (content instanceof McpPromptAudioContent resource) {
            return toJson(resource);
        }
        return super.toJson(content);
    }

    @Override
    public Optional<JsonObjectBuilder> toJson(McpAudioContent content) {
        return Optional.of(JSON_BUILDER_FACTORY.createObjectBuilder()
                                   .add("type", content.type().text())
                                   .add("data", content.base64Data())
                                   .add("mimeType", content.mediaType().text()));
    }

    @Override
    public JsonObjectBuilder toJson(McpTool tool) {
        var builder = super.toJson(tool);
        McpToolAnnotations annotations = tool.annotations();
        JsonObjectBuilder annotBuilder = JSON_BUILDER_FACTORY.createObjectBuilder();
        annotBuilder.add("title", annotations.title());
        annotBuilder.add("destructiveHint", annotations.destructiveHint());
        annotBuilder.add("idempotentHint", annotations.idempotentHint());
        annotBuilder.add("openWorldHint", annotations.openWorldHint());
        annotBuilder.add("readOnlyHint", annotations.readOnlyHint());
        builder.add("annotations", annotBuilder.build());
        return builder;
    }
}
