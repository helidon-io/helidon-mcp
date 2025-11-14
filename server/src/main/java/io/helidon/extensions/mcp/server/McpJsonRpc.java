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

import java.io.ByteArrayOutputStream;
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
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;

import static io.helidon.jsonrpc.core.JsonRpcError.INTERNAL_ERROR;

final class McpJsonRpc {
    static final JsonBuilderFactory JSON_BUILDER_FACTORY = Json.createBuilderFactory(Map.of());
    static final JsonReaderFactory JSON_READER_FACTORY = Json.createReaderFactory(Map.of());
    static final JsonWriterFactory JSON_PP_WRITER_FACTORY = Json.createWriterFactory(
            Map.of(JsonGenerator.PRETTY_PRINTING, true));

    private static final Map<String, JsonObject> CACHE = new ConcurrentHashMap<>();
    private static final JsonObject EMPTY_OBJECT_SCHEMA = JSON_BUILDER_FACTORY.createObjectBuilder()
            .add("type", "object")
            .add("properties", JsonObject.EMPTY_JSON_OBJECT)
            .build();

    /**
     * JSON-RPC {@code initialize} method.
     */
    static final String METHOD_INITIALIZE = "initialize";
    /**
     * JSON-RPC {@code notifications/initialize} method.
     */
    static final String METHOD_NOTIFICATION_INITIALIZED = "notifications/initialized";
    /**
     * JSON-RPC {@code ping} method.
     */
    static final String METHOD_PING = "ping";
    /**
     * JSON-RPC {@code tools/list} method.
     */
    static final String METHOD_TOOLS_LIST = "tools/list";
    /**
     * JSON-RPC {@code tools/call} method.
     */
    static final String METHOD_TOOLS_CALL = "tools/call";
    /**
     * JSON-RPC {@code notifications/tools/list_changed} method.
     */
    static final String METHOD_NOTIFICATION_TOOLS_LIST_CHANGED = "notifications/tools/list_changed";
    /**
     * JSON-RPC {@code resources/list} method.
     */
    static final String METHOD_RESOURCES_LIST = "resources/list";
    /**
     * JSON-RPC {@code resources/read} method.
     */
    static final String METHOD_RESOURCES_READ = "resources/read";
    /**
     * JSON-RPC {@code notifications/resources/list_changed} method.
     */
    static final String METHOD_NOTIFICATION_RESOURCES_LIST_CHANGED = "notifications/resources/list_changed";
    /**
     * JSON-RPC {@code resources/templates/list} method.
     */
    static final String METHOD_RESOURCES_TEMPLATES_LIST = "resources/templates/list";
    /**
     * JSON-RPC {@code resources/subscribe} method.
     */
    static final String METHOD_RESOURCES_SUBSCRIBE = "resources/subscribe";
    /**
     * JSON-RPC {@code resources/unsubscribe} method.
     */
    static final String METHOD_RESOURCES_UNSUBSCRIBE = "resources/unsubscribe";
    /**
     * JSON-RPC {@code prompts/list} method.
     */
    static final String METHOD_PROMPT_LIST = "prompts/list";
    /**
     * JSON-RPC {@code prompts/get} method.
     */
    static final String METHOD_PROMPT_GET = "prompts/get";
    /**
     * JSON-RPC {@code notifications/prompts/list_changed} method.
     */
    static final String METHOD_NOTIFICATION_PROMPTS_LIST_CHANGED = "notifications/prompts/list_changed";
    /**
     * JSON-RPC {@code logging/setLevel} method.
     */
    static final String METHOD_LOGGING_SET_LEVEL = "logging/setLevel";
    /**
     * JSON-RPC {@code notifications/message} method.
     */
    static final String METHOD_NOTIFICATION_MESSAGE = "notifications/message";
    /**
     * JSON-RPC {@code notifications/cancelled} method.
     */
    static final String METHOD_NOTIFICATION_CANCELED = "notifications/cancelled";
    /**
     * JSON-RPC {@code notifications/resources/updated} method.
     */
    static final String METHOD_NOTIFICATION_UPDATE = "notifications/resources/updated";
    /**
     * JSON-RPC {@code completion/complete} method.
     */
    static final String METHOD_COMPLETION_COMPLETE = "completion/complete";
    /**
     * JSON-RPC {@code roots/list} method.
     */
    static final String METHOD_ROOTS_LIST = "roots/list";
    /**
     * JSON-RPC {@code notification/roots/list_changed} method.
     */
    static final String METHOD_NOTIFICATION_ROOTS_LIST_CHANGED = "notifications/roots/list_changed";
    /**
     * JSON-RPC {@code sampling/createMessage} method.
     */
    static final String METHOD_SAMPLING_CREATE_MESSAGE = "sampling/createMessage";
    /**
     * JSON-RPC {@code notifications/progress} method.
     */
    static final String METHOD_NOTIFICATION_PROGRESS = "notifications/progress";
    /**
     * JSON-RPC {@code session/disconnect} method.
     */
    static final String METHOD_SESSION_DISCONNECT = "session/disconnect";

    private McpJsonRpc() {
    }

    static boolean isResponse(JsonObject payload) {
        return !payload.containsKey("method") && payload.containsKey("id");
    }

    static JsonObject toJson(String protocolVersion, Set<McpCapability> capabilities, McpServerConfig config) {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("protocolVersion", protocolVersion)
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
                .add("instructions", "")
                .build();
    }

    static JsonObjectBuilder toJson(McpTool tool, String protocolVersion) {
        JsonObject jsonSchema = CACHE.computeIfAbsent(tool.schema(), schema -> {
            if (schema.isEmpty()) {
                return EMPTY_OBJECT_SCHEMA;
            }
            try (var r = JSON_READER_FACTORY.createReader(new StringReader(schema))) {
                return r.readObject();      // in-memory parsing
            }
        });

        // serialize tool
        JsonObjectBuilder builder = JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("name", tool.name())
                .add("description", tool.description())
                .add("inputSchema", jsonSchema);

        // serialize tool annotations starting 2025
        if (!protocolVersion.startsWith("2024")) {
            McpToolAnnotations annotations = tool.annotations();
            if (annotations != null) {
                JsonObjectBuilder annotBuilder = JSON_BUILDER_FACTORY.createObjectBuilder();
                annotBuilder.add("title", annotations.title());
                annotBuilder.add("destructiveHint", annotations.destructiveHint());
                annotBuilder.add("idempotentHint", annotations.idempotentHint());
                annotBuilder.add("openWorldHint", annotations.openWorldHint());
                annotBuilder.add("readOnlyHint", annotations.readOnlyHint());
                builder.add("annotations", annotBuilder.build());
            }
        }
        return builder;
    }

    static JsonObject toolCall(boolean error, List<McpToolContent> contents) {
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

    static JsonObject listResources(McpPage<McpResource> page) {
        JsonArrayBuilder builder = JSON_BUILDER_FACTORY.createArrayBuilder();
        page.components().stream()
                .map(McpJsonRpc::toJson)
                .forEach(builder::add);
        JsonObjectBuilder resources = JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("resources", builder);
        if (!page.cursor().isBlank()) {
            resources.add("nextCursor", page.cursor());
        }
        return resources.build();
    }

    static JsonObject listTools(McpPage<McpTool> page, String protocolVersion) {
        JsonArrayBuilder builder = JSON_BUILDER_FACTORY.createArrayBuilder();
        page.components().stream()
                .map(t -> toJson(t, protocolVersion))
                .forEach(builder::add);
        JsonObjectBuilder resources = JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("tools", builder);
        if (!page.cursor().isBlank()) {
            resources.add("nextCursor", page.cursor());
        }
        return resources.build();
    }

    static JsonObject listResourceTemplates(McpPage<McpResourceTemplate> page) {
        JsonArrayBuilder builder = JSON_BUILDER_FACTORY.createArrayBuilder();
        page.components().stream()
                .map(McpJsonRpc::resourceTemplates)
                .forEach(builder::add);
        JsonObjectBuilder resources = JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("resourceTemplates", builder);
        if (!page.cursor().isBlank()) {
            resources.add("nextCursor", page.cursor());
        }
        return resources.build();
    }

    static JsonObject listPrompts(McpPage<McpPrompt> page) {
        JsonArrayBuilder builder = JSON_BUILDER_FACTORY.createArrayBuilder();
        page.components().stream()
                .map(McpJsonRpc::toJson)
                .forEach(builder::add);
        JsonObjectBuilder resources = JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("prompts", builder);
        if (!page.cursor().isBlank()) {
            resources.add("nextCursor", page.cursor());
        }
        return resources.build();
    }

    static JsonObjectBuilder toJson(McpToolResourceContent content) {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("type", content.type().text())
                .add("resource", toJson(content.content())
                        .add("uri", content.uri().toASCIIString()));
    }

    static JsonObjectBuilder toJson(McpPrompt prompt) {
        JsonArrayBuilder array = JSON_BUILDER_FACTORY.createArrayBuilder();
        prompt.arguments().stream()
                .map(McpJsonRpc::toJson)
                .forEach(array::add);
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("name", prompt.name())
                .add("description", prompt.description())
                .add("arguments", array);
    }

    static JsonObjectBuilder toJson(McpPromptArgument argument) {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("name", argument.name())
                .add("description", argument.description())
                .add("required", argument.required());
    }

    static JsonObjectBuilder toJson(McpResource resource) {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("uri", resource.uri())
                .add("name", resource.name())
                .add("description", resource.description())
                .add("mimeType", resource.mediaType().text());
    }

    static JsonObjectBuilder resourceTemplates(McpResource resource) {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("uriTemplate", resource.uri())
                .add("name", resource.name())
                .add("description", resource.description())
                .add("mimeType", resource.mediaType().text());
    }

    static JsonObject readResource(String uri, List<McpResourceContent> contents) {
        JsonArrayBuilder array = JSON_BUILDER_FACTORY.createArrayBuilder();
        for (McpResourceContent content : contents) {
            JsonObjectBuilder builder = toJson(content);
            builder.add("uri", uri);
            array.add(builder);
        }
        return JSON_BUILDER_FACTORY.createObjectBuilder().add("contents", array).build();
    }

    static JsonObject toJson(List<McpPromptContent> contents, String description) {
        JsonArrayBuilder array = JSON_BUILDER_FACTORY.createArrayBuilder();
        for (McpPromptContent prompt : contents) {
            array.add(toJson(prompt));
        }
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("description", description)
                .add("messages", array)
                .build();
    }

    static JsonObjectBuilder toJson(McpPromptContent content) {
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

    static JsonObjectBuilder toJson(McpContent content) {
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

    static JsonObjectBuilder toJson(McpSamplingMessage message) {
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

    static JsonObjectBuilder toJson(McpResourceContent content) {
        if (content instanceof McpResourceTextContent text) {
            return toJson(text);
        }
        if (content instanceof McpResourceBinaryContent binary) {
            return toJson(binary);
        }
        throw new IllegalArgumentException("Unsupported content type: " + content.getClass().getName());
    }

    static JsonObjectBuilder toJson(McpPromptResourceContent resource) {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("role", resource.role().text())
                .add("content", JSON_BUILDER_FACTORY.createObjectBuilder()
                        .add("type", resource.type().text())
                        .add("resource", toJson(resource.content())
                                .add("uri", resource.uri().toASCIIString())));
    }

    static JsonObjectBuilder toJson(McpPromptImageContent image) {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("role", image.role().text())
                .add("content", toJson(image.content()));
    }

    static JsonObjectBuilder toJson(McpPromptTextContent content) {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("role", content.role().text())
                .add("content", toJson(content.content()));
    }

    static JsonObjectBuilder toJson(McpPromptAudioContent audio) {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("role", audio.role().text())
                .add("content", toJson(audio.content()));
    }

    static JsonObjectBuilder toJson(McpSamplingImageMessage image) {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("role", image.role().text())
                .add("content", JSON_BUILDER_FACTORY.createObjectBuilder()
                        .add("type", image.type().text())
                        .add("data", image.encodeBase64Data())
                        .add("mimeType", image.mediaType().text()));
    }

    static JsonObjectBuilder toJson(McpSamplingTextMessage text) {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("role", text.role().text())
                .add("content", JSON_BUILDER_FACTORY.createObjectBuilder()
                        .add("type", text.type().text())
                        .add("text", text.text()));
    }

    static JsonObjectBuilder toJson(McpSamplingAudioMessage audio) {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("role", audio.role().text())
                .add("content", JSON_BUILDER_FACTORY.createObjectBuilder()
                        .add("type", audio.type().text())
                        .add("data", audio.encodeBase64Data())
                        .add("mimeType", audio.mediaType().text()));
    }

    static JsonObjectBuilder toJson(McpTextContent content) {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("type", content.type().text())
                .add("text", content.text());
    }

    static JsonObjectBuilder toJson(McpImageContent content) {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("type", content.type().text())
                .add("data", content.base64Data())
                .add("mimeType", content.mediaType().text());
    }

    static JsonObjectBuilder toJson(McpAudioContent content) {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("type", content.type().text())
                .add("data", content.base64Data())
                .add("mimeType", content.mediaType().text());
    }

    static JsonObjectBuilder toJson(McpResourceBinaryContent content) {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("mimeType", content.mimeType().text())
                .add("blob", content.base64Data());
    }

    static JsonObjectBuilder toJson(McpResourceTextContent content) {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("mimeType", content.mimeType().text())
                .add("text", content.text());
    }

    static JsonObject toJson(McpProgress progress, int newProgress, String message) {
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

    static JsonObject createLoggingNotification(McpLogger.Level level, String name, String message) {
        var params = JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("level", level.text())
                .add("logger", name)
                .add("data", message);
        return createJsonRpcNotification(METHOD_NOTIFICATION_MESSAGE, params);
    }

    static JsonObject createUpdateNotification(String uri) {
        var params = JSON_BUILDER_FACTORY.createObjectBuilder().add("uri", uri);
        return createJsonRpcNotification(METHOD_NOTIFICATION_UPDATE, params);
    }

    static JsonObject toJson(McpCompletionContent content) {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("completion", JSON_BUILDER_FACTORY.createObjectBuilder()
                        .add("values", JSON_BUILDER_FACTORY.createArrayBuilder(content.values()))
                        .add("total", content.total())
                        .add("hasMore", content.hasMore()))
                .build();
    }

    static JsonObjectBuilder toJson(McpSamplingRequest request) {
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
                .map(McpJsonRpc::toJson)
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

    static JsonObject createSamplingRequest(long id, McpSamplingRequest request) {
        var params = toJson(request);
        return createJsonRpcRequest(id, METHOD_SAMPLING_CREATE_MESSAGE, params);
    }

    static JsonObject disconnectSession() {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("disconnect", true)
                .build();
    }

    static McpSamplingResponse createSamplingResponse(JsonObject object) throws McpSamplingException {
        find(object, "error")
                .filter(McpJsonRpc::isJsonObject)
                .map(JsonValue::asJsonObject)
                .map(JsonRpcError::create)
                .ifPresent(error -> {
                    throw new McpSamplingException(error.message());
                });
        try {
            var result = find(object, "result")
                    .filter(McpJsonRpc::isJsonObject)
                    .map(JsonValue::asJsonObject)
                    .orElseThrow(() -> new McpSamplingException(String.format("Sampling result not found: %s", object)));

            String model = result.getString("model");
            McpRole role = McpRole.valueOf(result.getString("role").toUpperCase());
            McpSamplingMessage message = parseMessage(role, result.getJsonObject("content"));
            McpStopReason stopReason = find(result, "stopReason")
                    .filter(McpJsonRpc::isJsonString)
                    .map(JsonString.class::cast)
                    .map(JsonString::getString)
                    .map(McpStopReason::map)
                    .orElse(null);
            return new McpSamplingResponseImpl(message, model, stopReason);
        } catch (Exception e) {
            throw new McpSamplingException("Wrong sampling response format", e);
        }
    }

    static String prettyPrint(JsonStructure json) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (JsonWriter writer = JSON_PP_WRITER_FACTORY.createWriter(baos)) {
            writer.write(json);
        }
        return baos.toString(StandardCharsets.UTF_8);
    }

    static JsonObject createJsonRpcNotification(String method, JsonObjectBuilder params) {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("jsonrpc", "2.0")
                .add("method", method)
                .add("params", params)
                .build();
    }

    static JsonObject createJsonRpcRequest(long id, String method, JsonObjectBuilder params) {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("jsonrpc", "2.0")
                .add("id", id)
                .add("method", method)
                .add("params", params)
                .build();
    }

    static JsonObject createJsonRpcRequest(long id, String method) {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("jsonrpc", "2.0")
                .add("id", id)
                .add("method", method)
                .build();
    }

    static JsonObject createJsonRpcErrorResponse(long id, JsonObjectBuilder params) {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("jsonrpc", "2.0")
                .add("id", id)
                .add("error", params)
                .build();
    }

    static JsonObject createJsonRpcResultResponse(long id, JsonValue params) {
        return JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("jsonrpc", "2.0")
                .add("id", id)
                .add("result", params)
                .build();
    }

    static JsonObject timeoutResponse(long requestId) {
        var error = JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("code", INTERNAL_ERROR)
                .add("message", "response timeout");
        return createJsonRpcErrorResponse(requestId, error);
    }

    static List<McpRoot> parseRoots(JsonObject response) {
        find(response, "error")
                .filter(McpJsonRpc::isJsonObject)
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

    private static McpSamplingMessage parseMessage(McpRole role, JsonObject object) {
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

    private static Optional<JsonValue> find(JsonObject object, String key) {
        if (object.containsKey(key)) {
            return Optional.of(object.get(key));
        }
        return Optional.empty();
    }

    private static boolean isJsonObject(JsonValue value) {
        return JsonValue.ValueType.OBJECT.equals(value.getValueType());
    }

    private static boolean isJsonString(JsonValue value) {
        return JsonValue.ValueType.STRING.equals(value.getValueType());
    }
}
