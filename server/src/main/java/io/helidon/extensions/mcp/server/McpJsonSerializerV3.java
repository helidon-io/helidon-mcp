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
import java.util.concurrent.locks.ReentrantLock;

import io.helidon.jsonrpc.core.JsonRpcError;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReaderFactory;
import jakarta.json.JsonValue;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

/**
 * JSON serializer for {@code 2025-06-18} MCP specification.
 */
class McpJsonSerializerV3 extends McpJsonSerializerV2 {
    private static final Jsonb JSON_B = JsonbBuilder.create();
    private static final Map<String, JsonObject> OUTPUT_SCHEMA = new McpSchemaHashMap();
    private static final Map<String, JsonObject> ELICITATION_SCHEMA = new McpSchemaHashMap();
    private static final JsonReaderFactory JSON_READER_FACTORY = Json.createReaderFactory(Map.of());
    private static final System.Logger LOGGER = System.getLogger(McpJsonSerializerV3.class.getName());
    private static final JsonBuilderFactory JSON_BUILDER_FACTORY = Json.createBuilderFactory(Map.of());

    private final ReentrantLock lock = new ReentrantLock();

    @Override
    public JsonObjectBuilder createJsonInitializeResponse(Set<McpCapability> capabilities, McpServerConfig config) {
        return super.createJsonInitializeResponse(capabilities, config)
                .add("protocolVersion", McpProtocolVersion.VERSION_2025_06_18.text());
    }

    @Override
    public JsonObject toolCall(McpTool tool, McpToolResult result) {
        if (tool.outputSchema().isEmpty() && result.structuredContent().isPresent()) {
            if (LOGGER.isLoggable(System.Logger.Level.WARNING)) {
                LOGGER.log(System.Logger.Level.WARNING, "Output schema must be specified for tool '"
                        + tool.name() + "' when returning a structured output.");
            }
        }

        JsonObjectBuilder builder = JSON_BUILDER_FACTORY.createObjectBuilder(super.toolCall(tool, result));
        result.structuredContent().ifPresent((content) -> {
            String json = JSON_B.toJson(content);
            JsonObject sc = JSON_B.fromJson(json, JsonObject.class);
            builder.add("structuredContent", sc);
            if (result.textContents().isEmpty()) {
                McpToolContent text = McpToolTextContent.builder().text(json).build();
                toJson(text).ifPresent(it -> builder.add("content", JSON_BUILDER_FACTORY.createArrayBuilder().add(it)));
            }
        });
        return builder.build();
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
        tool.outputSchema()
                .map(OUTPUT_SCHEMA::get)
                .ifPresent(outputSchema -> builder.add("outputSchema", outputSchema));
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

    @Override
    public McpElicitationResponse createElicitationResponse(JsonObject object) throws McpElicitationException {
        find(object, "error")
                .filter(this::isJsonObject)
                .map(JsonValue::asJsonObject)
                .map(JsonRpcError::create)
                .ifPresent(error -> {
                    throw new McpElicitationException(error.message());
                });
        try {
            var result = find(object, "result")
                    .filter(this::isJsonObject)
                    .map(JsonValue::asJsonObject)
                    .orElseThrow(() -> new McpElicitationException(String.format("Elicitation result not found: %s", object)));

            McpElicitationAction action = McpElicitationAction.valueOf(result.getString("action").toUpperCase());
            JsonObject content = find(result, "content")
                    .filter(this::isJsonObject)
                    .map(JsonObject.class::cast)
                    .orElse(null);
            return new McpElicitationResponseImpl(action, content);
        } catch (Exception e) {
            throw new McpElicitationException("Wrong elicitation response format", e);
        }
    }

    @Override
    public JsonObject createElicitationRequest(long id, McpElicitationRequest request) {
        var builder = createJsonRpcRequest(id, METHOD_ELICITATION_CREATE);
        var params = JSON_BUILDER_FACTORY.createObjectBuilder();
        JsonObject jsonSchema = ELICITATION_SCHEMA.get(request.schema());
        params.add("message", request.message());
        params.add("requestedSchema", jsonSchema);
        builder.add("params", params);
        return builder.build();
    }
}
