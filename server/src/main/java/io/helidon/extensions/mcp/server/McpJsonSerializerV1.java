/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
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
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import io.helidon.common.media.type.MediaType;
import io.helidon.common.media.type.MediaTypes;
import io.helidon.jsonrpc.core.JsonRpcError;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReaderFactory;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

import static io.helidon.jsonrpc.core.JsonRpcError.INTERNAL_ERROR;

class McpJsonSerializerV1 implements McpJsonSerializer {
    private static final Map<String, JsonObject> CACHE = new ConcurrentHashMap<>();
    private static final JsonReaderFactory JSON_READER_FACTORY = Json.createReaderFactory(Map.of());
    private static final JsonBuilderFactory JSON_BUILDER_FACTORY = Json.createBuilderFactory(Map.of());
    private static final JsonObject EMPTY_OBJECT_SCHEMA = JSON_BUILDER_FACTORY.createObjectBuilder()
            .add("type", "object")
            .add("properties", JsonObject.EMPTY_JSON_OBJECT)
            .build();

    @Override
    public JsonObjectBuilder toJson(Set<McpCapability> capabilities, McpServerConfig config) {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("protocolVersion", McpProtocolVersion.VERSION_2024_11_05.text())
                .add("capabilities", JSON_BUILDER_FACTORY.createObjectBuilder()
                        .add("logging", JsonValue.EMPTY_JSON_OBJECT)
                        .add("prompts", JSON_BUILDER_FACTORY.createObjectBuilder()
                                .add("listChanged", capabilities.contains(McpCapability.PROMPT_LIST_CHANGED)))
                        .add("tools", JSON_BUILDER_FACTORY.createObjectBuilder()
                                .add("listChanged", capabilities.contains(McpCapability.TOOL_LIST_CHANGED)))
                        .add("resources", JSON_BUILDER_FACTORY.createObjectBuilder()
                                .add("listChanged", capabilities.contains(McpCapability.RESOURCE_LIST_CHANGED))
                                .add("subscribe", capabilities.contains(McpCapability.RESOURCE_SUBSCRIBE)))
                        .add("completions", JsonValue.EMPTY_JSON_OBJECT))
                .add("serverInfo", JSON_BUILDER_FACTORY.createObjectBuilder()
                        .add("name", config.name())
                        .add("version", config.version()))
                .add("instructions", "");
    }

    @Override
    public JsonObjectBuilder toJson(McpTool tool) {
        JsonObject jsonSchema = CACHE.computeIfAbsent(tool.schema(), schema -> {
            if (schema.isEmpty()) {
                return EMPTY_OBJECT_SCHEMA;
            }
            try (var r = JSON_READER_FACTORY.createReader(new StringReader(schema))) {
                return r.readObject();      // in-memory parsing
            }
        });
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("name", tool.name())
                .add("description", tool.description())
                .add("inputSchema", jsonSchema);
    }

    @Override
    public JsonObject toolCall(boolean error, List<McpToolContent> contents) {
        JsonArrayBuilder array = JSON_BUILDER_FACTORY.createArrayBuilder();
        for (McpToolContent content : contents) {
            if (content instanceof McpToolResourceContent trc) {
                array.add(toJson(trc));
                continue;
            }
            array.add(toJson(content.content()));
        }
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("content", array)
                .add("isError", error)
                .build();
    }

    @Override
    public JsonObject listResources(McpPage<McpResource> page) {
        JsonArrayBuilder builder = JSON_BUILDER_FACTORY.createArrayBuilder();
        page.components().stream()
                .map(this::toJson)
                .forEach(builder::add);
        JsonObjectBuilder resources = JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("resources", builder);
        if (!page.cursor().isBlank()) {
            resources.add("nextCursor", page.cursor());
        }
        return resources.build();
    }

    @Override
    public JsonObject listTools(McpPage<McpTool> page) {
        JsonArrayBuilder builder = JSON_BUILDER_FACTORY.createArrayBuilder();
        page.components().stream()
                .map(this::toJson)
                .forEach(builder::add);
        JsonObjectBuilder tools = JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("tools", builder);
        if (!page.cursor().isBlank()) {
            tools.add("nextCursor", page.cursor());
        }
        return tools.build();
    }

    @Override
    public JsonObject listResourceTemplates(McpPage<McpResourceTemplate> page) {
        JsonArrayBuilder builder = JSON_BUILDER_FACTORY.createArrayBuilder();
        page.components().stream()
                .map(this::resourceTemplates)
                .forEach(builder::add);
        JsonObjectBuilder templates = JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("resourceTemplates", builder);
        if (!page.cursor().isBlank()) {
            templates.add("nextCursor", page.cursor());
        }
        return templates.build();
    }

    @Override
    public JsonObject listPrompts(McpPage<McpPrompt> page) {
        JsonArrayBuilder builder = JSON_BUILDER_FACTORY.createArrayBuilder();
        page.components().stream()
                .map(this::toJson)
                .forEach(builder::add);
        JsonObjectBuilder prompts = JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("prompts", builder);
        if (!page.cursor().isBlank()) {
            prompts.add("nextCursor", page.cursor());
        }
        return prompts.build();
    }

    @Override
    public JsonObjectBuilder toJson(McpToolResourceContent content) {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("type", content.type().text())
                .add("resource", toJson(content.content())
                        .add("uri", content.uri().toASCIIString()));
    }

    @Override
    public JsonObjectBuilder toJson(McpPrompt prompt) {
        JsonArrayBuilder array = JSON_BUILDER_FACTORY.createArrayBuilder();
        prompt.arguments().stream()
                .map(this::toJson)
                .forEach(array::add);
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("name", prompt.name())
                .add("description", prompt.description())
                .add("arguments", array);
    }

    @Override
    public JsonObjectBuilder toJson(McpPromptArgument argument) {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("name", argument.name())
                .add("description", argument.description())
                .add("required", argument.required());
    }

    @Override
    public JsonObjectBuilder toJson(McpResource resource) {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("uri", resource.uri())
                .add("name", resource.name())
                .add("description", resource.description())
                .add("mimeType", resource.mediaType().text());
    }

    @Override
    public JsonObjectBuilder resourceTemplates(McpResource resource) {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("uriTemplate", resource.uri())
                .add("name", resource.name())
                .add("description", resource.description())
                .add("mimeType", resource.mediaType().text());
    }

    @Override
    public JsonObject readResource(String uri, List<McpResourceContent> contents) {
        JsonArrayBuilder array = JSON_BUILDER_FACTORY.createArrayBuilder();
        for (McpResourceContent content : contents) {
            JsonObjectBuilder builder = toJson(content);
            builder.add("uri", uri);
            array.add(builder);
        }
        return JSON_BUILDER_FACTORY.createObjectBuilder().add("contents", array).build();
    }

    @Override
    public JsonObject toJson(List<McpPromptContent> contents, String description) {
        JsonArrayBuilder array = JSON_BUILDER_FACTORY.createArrayBuilder();
        for (McpPromptContent prompt : contents) {
            array.add(toJson(prompt));
        }
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("description", description)
                .add("messages", array)
                .build();
    }

    @Override
    public JsonObjectBuilder toJson(McpPromptContent content) {
        if (content instanceof McpPromptImageContent image) {
            return toJson(image);
        }
        if (content instanceof McpPromptTextContent text) {
            return toJson(text);
        }
        if (content instanceof McpPromptResourceContent resource) {
            return toJson(resource);
        }
        if (content instanceof McpPromptAudioContent resource) {
            return toJson(resource);
        }
        throw new IllegalArgumentException("Unsupported content type: " + content.getClass().getName());
    }

    @Override
    public JsonObjectBuilder toJson(McpContent content) {
        if (content instanceof McpTextContent text) {
            return toJson(text);
        }
        if (content instanceof McpImageContent image) {
            return toJson(image);
        }
        if (content instanceof McpResourceContent resource) {
            return toJson(resource);
        }
        if (content instanceof McpAudioContent audio) {
            return toJson(audio);
        }
        throw new IllegalArgumentException("Unsupported content type: " + content.getClass().getName());
    }

    @Override
    public JsonObjectBuilder toJson(McpSamplingMessage message) {
        if (message instanceof McpSamplingTextMessageImpl text) {
            return toJson(text);
        }
        if (message instanceof McpSamplingImageMessageImpl image) {
            return toJson(image);
        }
        if (message instanceof McpSamplingAudioMessageImpl resource) {
            return toJson(resource);
        }
        throw new IllegalArgumentException("Unsupported content type: " + message.getClass().getName());
    }

    @Override
    public JsonObjectBuilder toJson(McpResourceContent content) {
        if (content instanceof McpResourceTextContent text) {
            return toJson(text);
        }
        if (content instanceof McpResourceBinaryContent binary) {
            return toJson(binary);
        }
        throw new IllegalArgumentException("Unsupported content type: " + content.getClass().getName());
    }

    @Override
    public JsonObjectBuilder toJson(McpPromptResourceContent resource) {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("role", resource.role().text())
                .add("content", JSON_BUILDER_FACTORY.createObjectBuilder()
                        .add("type", resource.type().text())
                        .add("resource", toJson(resource.content())
                                .add("uri", resource.uri().toASCIIString())));
    }

    @Override
    public JsonObjectBuilder toJson(McpPromptImageContent image) {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("role", image.role().text())
                .add("content", toJson(image.content()));
    }

    @Override
    public JsonObjectBuilder toJson(McpPromptTextContent content) {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("role", content.role().text())
                .add("content", toJson(content.content()));
    }

    @Override
    public JsonObjectBuilder toJson(McpPromptAudioContent audio) {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("role", audio.role().text())
                .add("content", toJson(audio.content()));
    }

    @Override
    public JsonObjectBuilder toJson(McpSamplingImageMessage image) {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("role", image.role().text())
                .add("content", JSON_BUILDER_FACTORY.createObjectBuilder()
                        .add("type", image.type().text())
                        .add("data", image.encodeBase64Data())
                        .add("mimeType", image.mediaType().text()));
    }

    @Override
    public JsonObjectBuilder toJson(McpSamplingTextMessage text) {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("role", text.role().text())
                .add("content", JSON_BUILDER_FACTORY.createObjectBuilder()
                        .add("type", text.type().text())
                        .add("text", text.text()));
    }

    @Override
    public JsonObjectBuilder toJson(McpSamplingAudioMessage audio) {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("role", audio.role().text())
                .add("content", JSON_BUILDER_FACTORY.createObjectBuilder()
                        .add("type", audio.type().text())
                        .add("data", audio.encodeBase64Data())
                        .add("mimeType", audio.mediaType().text()));
    }

    @Override
    public JsonObjectBuilder toJson(McpTextContent content) {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("type", content.type().text())
                .add("text", content.text());
    }

    @Override
    public JsonObjectBuilder toJson(McpImageContent content) {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("type", content.type().text())
                .add("data", content.base64Data())
                .add("mimeType", content.mediaType().text());
    }

    @Override
    public JsonObjectBuilder toJson(McpAudioContent content) {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("type", content.type().text())
                .add("data", content.base64Data())
                .add("mimeType", content.mediaType().text());
    }

    @Override
    public JsonObjectBuilder toJson(McpResourceBinaryContent content) {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("mimeType", content.mimeType().text())
                .add("blob", content.base64Data());
    }

    @Override
    public JsonObjectBuilder toJson(McpResourceTextContent content) {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("mimeType", content.mimeType().text())
                .add("text", content.text());
    }

    @Override
    public JsonObject toJson(McpProgress progress, int newProgress, String message) {
        JsonObjectBuilder params = JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("progress", newProgress)
                .add("total", progress.total());
        if (progress.token().isBlank()) {
            params.add("progressToken", progress.tokenInt());
        } else {
            params.add("progressToken", progress.token());
        }
        if (message != null) {
            params.add("message", message);
        }
        return createJsonRpcNotification(METHOD_NOTIFICATION_PROGRESS, params);
    }

    @Override
    public JsonObject createLoggingNotification(McpLogger.Level level, String name, String message) {
        var params = JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("level", level.text())
                .add("logger", name)
                .add("data", message);
        return createJsonRpcNotification(METHOD_NOTIFICATION_MESSAGE, params);
    }

    @Override
    public JsonObject createUpdateNotification(String uri) {
        var params = JSON_BUILDER_FACTORY.createObjectBuilder().add("uri", uri);
        return createJsonRpcNotification(METHOD_NOTIFICATION_UPDATE, params);
    }

    @Override
    public JsonObject toJson(McpCompletionContent content) {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("completion", JSON_BUILDER_FACTORY.createObjectBuilder()
                        .add("values", JSON_BUILDER_FACTORY.createArrayBuilder(content.values()))
                        .add("total", content.total())
                        .add("hasMore", content.hasMore()))
                .build();
    }

    @Override
    public JsonObjectBuilder toJson(McpSamplingRequest request) {
        var hints = JSON_BUILDER_FACTORY.createArrayBuilder();
        var params = JSON_BUILDER_FACTORY.createObjectBuilder();
        var messages = JSON_BUILDER_FACTORY.createArrayBuilder();
        var sequences = JSON_BUILDER_FACTORY.createArrayBuilder();
        var modelPreference = JSON_BUILDER_FACTORY.createObjectBuilder();

        request.hints()
                .stream()
                .flatMap(List::stream)
                .map(hint -> JSON_BUILDER_FACTORY.createObjectBuilder().add("name", hint))
                .forEach(hints::add);
        request.hints().map(it -> modelPreference.add("hints", hints));
        request.speedPriority().map(speed -> modelPreference.add("speedPriority", speed));
        request.costPriority().map(priority -> modelPreference.add("costPriority", priority));
        request.intelligencePriority().map(intelligence -> modelPreference.add("intelligencePriority", intelligence));
        params.add("modelPreference", modelPreference);

        request.messages().stream()
                .map(this::toJson)
                .forEach(messages::add);
        params.add("messages", messages);
        params.add("maxTokens", request.maxTokens());
        request.systemPrompt().map(prompt -> params.add("systemPrompt", prompt));
        request.temperature().map(temperature -> params.add("temperature", temperature));
        request.includeContext().map(context -> params.add("includeContext", context.text()));
        request.stopSequences()
                .stream()
                .flatMap(List::stream)
                .forEach(sequences::add);
        request.stopSequences().map(it -> params.add("stopSequences", sequences));
        request.metadata().map(metadata -> params.add("metadata", metadata));
        return params;
    }

    @Override
    public JsonObject createJsonRpcNotification(String method, JsonObjectBuilder params) {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("jsonrpc", "2.0")
                .add("method", method)
                .add("params", params)
                .build();
    }

    @Override
    public JsonObject createJsonRpcRequest(long id, String method, JsonObjectBuilder params) {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("jsonrpc", "2.0")
                .add("id", id)
                .add("method", method)
                .add("params", params)
                .build();
    }

    @Override
    public JsonObject createJsonRpcRequest(long id, String method) {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("jsonrpc", "2.0")
                .add("id", id)
                .add("method", method)
                .build();
    }

    @Override
    public JsonObject createJsonRpcErrorResponse(long id, JsonObjectBuilder params) {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("jsonrpc", "2.0")
                .add("id", id)
                .add("error", params)
                .build();
    }

    @Override
    public JsonObject createJsonRpcResultResponse(long id, JsonValue params) {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("jsonrpc", "2.0")
                .add("id", id)
                .add("result", params)
                .build();
    }

    @Override
    public JsonObject timeoutResponse(long requestId) {
        var error = JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("code", INTERNAL_ERROR)
                .add("message", "response timeout");
        return createJsonRpcErrorResponse(requestId, error);
    }

    @Override
    public List<McpRoot> parseRoots(JsonObject response) {
        find(response, "error")
                .filter(this::isJsonObject)
                .map(JsonValue::asJsonObject)
                .map(JsonRpcError::create)
                .ifPresent(error -> {
                    throw new McpRootException(error.message());
                });
        JsonArray roots = find(response, "result")
                .map(JsonValue::asJsonObject)
                .flatMap(result -> find(result, "roots"))
                .map(JsonValue::asJsonArray)
                .orElseThrow(() -> new McpRootException("Wrong response format: %s".formatted(response)));

        return IntStream.range(0, roots.size())
                .mapToObj(roots::getJsonObject)
                .map(root -> McpRoot.builder()
                        .uri(URI.create(root.getString("uri")))
                        .name(Optional.ofNullable(root.getString("name", null)))
                        .build())
                .toList();
    }

    @Override
    public JsonObject createSamplingRequest(long id, McpSamplingRequest request) {
        var params = toJson(request);
        return createJsonRpcRequest(id, METHOD_SAMPLING_CREATE_MESSAGE, params);
    }

    @Override
    public McpSamplingResponse createSamplingResponse(JsonObject object) throws McpSamplingException {
        find(object, "error")
                .filter(this::isJsonObject)
                .map(JsonValue::asJsonObject)
                .map(JsonRpcError::create)
                .ifPresent(error -> {
                    throw new McpSamplingException(error.message());
                });
        try {
            var result = find(object, "result")
                    .filter(this::isJsonObject)
                    .map(JsonValue::asJsonObject)
                    .orElseThrow(() -> new McpSamplingException(String.format("Sampling result not found: %s", object)));

            String model = result.getString("model");
            McpRole role = McpRole.valueOf(result.getString("role").toUpperCase());
            McpSamplingMessage message = parseMessage(role, result.getJsonObject("content"));
            McpStopReason stopReason = find(result, "stopReason")
                    .filter(this::isJsonString)
                    .map(JsonString.class::cast)
                    .map(JsonString::getString)
                    .map(McpStopReason::map)
                    .orElse(null);
            return new McpSamplingResponseImpl(message, model, stopReason);
        } catch (Exception e) {
            throw new McpSamplingException("Wrong sampling response format", e);
        }
    }

    McpSamplingMessage parseMessage(McpRole role, JsonObject object) {
        String type = object.getString("type").toUpperCase();
        McpSamplingMessageType messageType = McpSamplingMessageType.valueOf(type);
        return switch (messageType) {
            case TEXT -> new McpSamplingTextMessageImpl(object.getString("text"), role);
            case IMAGE -> {
                byte[] data = object.getString("data").getBytes(StandardCharsets.UTF_8);
                MediaType mediaType = MediaTypes.create(object.getString("mimeType"));
                yield new McpSamplingImageMessageImpl(data, mediaType, role);
            }
            case AUDIO -> {
                byte[] data = object.getString("data").getBytes(StandardCharsets.UTF_8);
                MediaType mediaType = MediaTypes.create(object.getString("mimeType"));
                yield new McpSamplingAudioMessageImpl(data, mediaType, role);
            }
        };
    }

    Optional<JsonValue> find(JsonObject object, String key) {
        if (object.containsKey(key)) {
            return Optional.of(object.get(key));
        }
        return Optional.empty();
    }

    boolean isJsonObject(JsonValue value) {
        return JsonValue.ValueType.OBJECT.equals(value.getValueType());
    }

    boolean isJsonString(JsonValue value) {
        return JsonValue.ValueType.STRING.equals(value.getValueType());
    }
}
