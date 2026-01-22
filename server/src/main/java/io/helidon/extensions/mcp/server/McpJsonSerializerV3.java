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

import java.io.StringReader;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReaderFactory;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

class McpJsonSerializerV3 extends McpJsonSerializerV2 {
    private static final Jsonb JSON_B = JsonbBuilder.create();
    private static final Map<String, JsonObject> CACHE = new ConcurrentHashMap<>();
    private static final JsonReaderFactory JSON_READER_FACTORY = Json.createReaderFactory(Map.of());
    private static final System.Logger LOGGER = System.getLogger(McpJsonSerializerV3.class.getName());
    private static final JsonBuilderFactory JSON_BUILDER_FACTORY = Json.createBuilderFactory(Map.of());

    @Override
    public JsonObjectBuilder toJson(Set<McpCapability> capabilities, McpServerConfig config) {
        return super.toJson(capabilities, config)
                .add("protocolVersion", McpProtocolVersion.VERSION_2025_06_18.text());
    }

    @Override
    public JsonObjectBuilder toolCall(McpTool tool, McpToolResult result) {
        if (tool.outputSchema().isEmpty() && result.structuredContent().isPresent()) {
            if (LOGGER.isLoggable(System.Logger.Level.WARNING)) {
                LOGGER.log(System.Logger.Level.WARNING, "Output schema must be specified for tool '"
                        + tool.name() + "' when returning a structured output.");
            }
        }

        JsonObjectBuilder builder = super.toolCall(tool, result);
        result.structuredContent().ifPresent((content) -> {
            String json = JSON_B.toJson(content);
            JsonObject sc = JSON_B.fromJson(json, JsonObject.class);
            builder.add("structuredContent", sc);
            if (result.contents().isEmpty()) {
                var text = McpToolContents.textContent(json);
                builder.add("content", JSON_BUILDER_FACTORY.createArrayBuilder()
                        .add(toJson(text.content())));
            }
        });
        return builder;
    }

    @Override
    public JsonObjectBuilder toJson(McpTool tool) {
        var builder = super.toJson(tool);

        if (!tool.title().isBlank()) {
            builder.add("title", tool.title());
        }
        tool.outputSchema().ifPresent(outputSchema -> {
            JsonObject jsonSchema = CACHE.computeIfAbsent(outputSchema, schema -> {
                if (schema.isEmpty()) {
                    return EMPTY_OBJECT_SCHEMA;
                }
                try (var r = JSON_READER_FACTORY.createReader(new StringReader(schema))) {
                    return r.readObject();
                }
            });
            builder.add("outputSchema", jsonSchema);
        });
        return builder;
    }

    @Override
    public JsonObjectBuilder toJson(McpResource resource) {
        var builder = super.toJson(resource);
        if (!resource.title().isBlank()) {
            builder.add("title", resource.title());
        }
        return builder;
    }

    @Override
    public JsonObjectBuilder toJson(McpPrompt prompt) {
        var builder = super.toJson(prompt);
        if (!prompt.title().isBlank()) {
            builder.add("title", prompt.title());
        }
        return builder;
    }

    @Override
    public JsonObjectBuilder toJson(McpPromptArgument argument) {
        var builder = super.toJson(argument);
        if (!argument.title().isBlank()) {
            builder.add("title", argument.title());
        }
        return builder;
    }
}
