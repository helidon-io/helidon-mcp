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

import io.helidon.json.JsonArray;
import io.helidon.json.JsonObject;
import io.helidon.json.JsonValue;
import io.helidon.jsonrpc.core.JsonRpcError;

/**
 * JSON serializer for {@code 2025-06-18} MCP specification.
 */
class McpJsonSerializerV3 extends McpJsonSerializerV2 {
    private static final McpSchemaHashMap OUTPUT_SCHEMA = new McpSchemaHashMap();
    private static final McpSchemaHashMap ELICITATION_SCHEMA = new McpSchemaHashMap();
    private static final System.Logger LOGGER = System.getLogger(McpJsonSerializerV3.class.getName());

    @Override
    public JsonObject.Builder createJsonInitializeResponse(Set<McpCapability> capabilities, McpServerConfig config) {
        return super.createJsonInitializeResponse(capabilities, config)
                .set("protocolVersion", McpProtocolVersion.VERSION_2025_06_18.text());
    }

    @Override
    public JsonObject toolCall(McpTool tool, McpToolResult result) {
        if (tool.outputSchema().isEmpty() && result.structuredContent().isPresent()) {
            if (LOGGER.isLoggable(System.Logger.Level.WARNING)) {
                LOGGER.log(System.Logger.Level.WARNING, "Output schema must be specified for tool '"
                        + tool.name() + "' when returning a structured output.");
            }
        }

        JsonObject.Builder builder = JsonObject.builder().from(super.toolCall(tool, result));
        result.structuredContent().ifPresent((content) -> {
            JsonObject sc = McpJsonBinding.serializeObject(content);
            String json = sc.toString();
            builder.set("structuredContent", sc);
            if (result.textContents().isEmpty()) {
                McpToolContent text = McpToolTextContent.builder().text(json).build();
                toJson(text).ifPresent(it -> builder.set("content", JsonArray.create(it.build())));
            }
        });
        return builder.build();
    }

    @Override
    public Optional<JsonObject.Builder> toJson(McpContent content) {
        if (content instanceof McpResourceLinkContent link) {
            return Optional.of(toJson(link));
        }
        return super.toJson(content);
    }

    @Override
    public JsonObject.Builder toJson(McpTool tool) {
        var builder = super.toJson(tool);
        tool.title().ifPresent(title -> builder.set("title", title));
        tool.outputSchema()
                .map(OUTPUT_SCHEMA::get)
                .ifPresent(outputSchema -> builder.set("outputSchema", outputSchema));
        return builder;
    }

    @Override
    public JsonObject.Builder toJson(McpResource resource) {
        var builder = super.toJson(resource);
        resource.title().ifPresent(title -> builder.set("title", title));
        return builder;
    }

    @Override
    public JsonObject.Builder toJson(McpPrompt prompt) {
        var builder = super.toJson(prompt);
        prompt.title().ifPresent(title -> builder.set("title", title));
        return builder;
    }

    @Override
    public JsonObject.Builder toJson(McpPromptArgument argument) {
        var builder = super.toJson(argument);
        argument.title().ifPresent(title -> builder.set("title", title));
        return builder;
    }

    private JsonObject.Builder toJson(McpResourceLinkContent content) {
        var builder = JsonObject.builder()
                .set("type", content.type().text())
                .set("uri", content.uri())
                .set("name", content.name());
        content.size().ifPresent(size -> builder.set("size", size));
        content.title().ifPresent(title -> builder.set("title", title));
        content.mediaType().ifPresent(mediaType -> builder.set("mimeType", mediaType.text()));
        content.description().ifPresent(description -> builder.set("description", description));
        return builder;
    }

    @Override
    public McpElicitationResponse createElicitationResponse(JsonObject object) throws McpElicitationException {
        find(object, "error")
                .filter(this::isJsonObject)
                .map(JsonValue::asObject)
                .map(JsonRpcError::create)
                .ifPresent(error -> {
                    throw new McpElicitationException(error.message());
                });
        try {
            var result = find(object, "result")
                    .filter(this::isJsonObject)
                    .map(JsonValue::asObject)
                    .orElseThrow(() -> new McpElicitationException(String.format("Elicitation result not found: %s", object)));

            McpElicitationAction action = result.stringValue("action")
                    .map(String::toUpperCase)
                    .map(McpElicitationAction::valueOf)
                    .orElseThrow();
            JsonObject content = find(result, "content")
                    .filter(this::isJsonObject)
                    .map(JsonValue::asObject)
                    .orElse(null);
            return new McpElicitationResponseImpl(action, content);
        } catch (Exception e) {
            throw new McpElicitationException("Wrong elicitation response format", e);
        }
    }

    @Override
    public JsonObject createElicitationRequest(long id, McpElicitationRequest request) {
        var builder = createJsonRpcRequest(id, METHOD_ELICITATION_CREATE);
        var params = JsonObject.builder();
        JsonObject jsonSchema = ELICITATION_SCHEMA.get(request.schema());
        params.set("message", request.message());
        params.set("requestedSchema", jsonSchema);
        builder.set("params", params.build());
        return builder.build();
    }
}
