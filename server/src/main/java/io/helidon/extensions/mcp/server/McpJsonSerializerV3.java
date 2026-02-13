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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReaderFactory;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

/**
 * JSON serializer for {@code 2025-06-18} MCP specification.
 */
class McpJsonSerializerV3 extends McpJsonSerializerV2 {
    private static final Jsonb JSON_B = JsonbBuilder.create();
    private static final Map<String, JsonObject> CACHE = new HashMap<>();
    private static final JsonReaderFactory JSON_READER_FACTORY = Json.createReaderFactory(Map.of());
    private static final System.Logger LOGGER = System.getLogger(McpJsonSerializerV3.class.getName());
    private static final JsonBuilderFactory JSON_BUILDER_FACTORY = Json.createBuilderFactory(Map.of());

    private final ReentrantLock lock = new ReentrantLock();

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
                toJson(text.content())
                        .ifPresent(it -> builder.add("content", JSON_BUILDER_FACTORY.createArrayBuilder().add(it)));
            }
        });
        return builder;
    }

    @Override
    public Optional<JsonObjectBuilder> toJson(McpContent content) {
        if (content instanceof McpResourceLinkContent link) {
            return Optional.of(toJson(link));
        }
        return super.toJson(content);
    }

    @Override
    public JsonObjectBuilder toJson(McpTool tool) {
        var builder = super.toJson(tool);
        tool.title().ifPresent(title -> builder.add("title", title));
        tool.outputSchema().ifPresent(outputSchema -> {
            JsonObject jsonSchema = CACHE.get(outputSchema);
            if (jsonSchema == null) {
                // lock to write the newly parsed output schema
                lock.lock();
                try {
                    jsonSchema = CACHE.get(outputSchema);
                    // double check that another thread did not write the schema outside of this lock
                    if (jsonSchema == null) {
                        JsonObject parsed;
                        if (outputSchema.isEmpty()) {
                            parsed = EMPTY_OBJECT_SCHEMA;
                        } else {
                            try (var r = JSON_READER_FACTORY.createReader(new StringReader(outputSchema))) {
                                parsed = r.readObject();
                            }
                        }
                        CACHE.put(outputSchema, parsed);
                        jsonSchema = parsed;
                    }
                } finally {
                    lock.unlock();
                }
            }
            builder.add("outputSchema", jsonSchema);
        });
        return builder;
    }

    @Override
    public JsonObjectBuilder toJson(McpResource resource) {
        var builder = super.toJson(resource);
        resource.title().ifPresent(title -> builder.add("title", title));
        return builder;
    }

    @Override
    public JsonObjectBuilder toJson(McpPrompt prompt) {
        var builder = super.toJson(prompt);
        prompt.title().ifPresent(title -> builder.add("title", title));
        return builder;
    }

    @Override
    public JsonObjectBuilder toJson(McpPromptArgument argument) {
        var builder = super.toJson(argument);
        argument.title().ifPresent(title -> builder.add("title", title));
        return builder;
    }

    private JsonObjectBuilder toJson(McpResourceLinkContent content) {
        var builder = JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("type", content.type().text())
                .add("uri", content.uri())
                .add("name", content.name());
        content.size().ifPresent(size -> builder.add("size", size));
        content.title().ifPresent(title -> builder.add("title", title));
        content.mediaType().ifPresent(mediaType -> builder.add("mimeType", mediaType.text()));
        content.description().ifPresent(description -> builder.add("description", description));
        return builder;
    }
}
