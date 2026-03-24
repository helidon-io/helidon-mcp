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

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.helidon.common.media.type.MediaType;
import io.helidon.common.media.type.MediaTypes;
import io.helidon.json.JsonArray;
import io.helidon.json.JsonObject;
import io.helidon.json.JsonString;
import io.helidon.json.JsonValue;
import io.helidon.jsonrpc.core.JsonRpcError;

import static io.helidon.jsonrpc.core.JsonRpcError.INTERNAL_ERROR;

/**
 * JSON serializer for {@code 2024-11-05} MCP specification.
 */
class McpJsonSerializerV1 implements McpJsonSerializer {
    private static final Map<String, JsonObject> INPUT_SCHEMA = new McpSchemaHashMap();
    static final JsonObject EMPTY_OBJECT_SCHEMA = JsonObject.builder()
            .set("type", "object")
            .set("properties", JsonObject.empty())
            .build();

    @Override
    public JsonObject.Builder createJsonInitializeResponse(Set<McpCapability> capabilities, McpServerConfig config) {
        return JsonObject.builder()
                .set("protocolVersion", McpProtocolVersion.VERSION_2024_11_05.text())
                .set("capabilities", JsonObject.builder()
                        .set("logging", JsonObject.empty())
                        .set("prompts", JsonObject.builder()
                                .set("listChanged", capabilities.contains(McpCapability.PROMPT_LIST_CHANGED)).build())
                        .set("tools", JsonObject.builder()
                                .set("listChanged", capabilities.contains(McpCapability.TOOL_LIST_CHANGED)).build())
                        .set("resources", JsonObject.builder()
                                .set("listChanged", capabilities.contains(McpCapability.RESOURCE_LIST_CHANGED))
                                .set("subscribe", capabilities.contains(McpCapability.RESOURCE_SUBSCRIBE)).build())
                        .set("completions", JsonObject.empty())
                        .set("elicitation", JsonObject.empty()).build())
                .set("serverInfo", JsonObject.builder()
                        .set("name", config.name())
                        .set("version", config.version()).build())
                .set("instructions", config.instructions().orElse(""));
    }

    @Override
    public JsonObject.Builder toJson(McpTool tool) {
        String schema = tool.schema();
        return JsonObject.builder()
                .set("name", tool.name())
                .set("description", tool.description())
                .set("inputSchema", INPUT_SCHEMA.get(schema));
    }

    @Override
    public JsonObject toolCall(McpTool tool, McpToolResult result) {
        List<JsonValue> contentValues = new ArrayList<>();
        for (McpToolContent content : McpToolSupport.aggregateContent(result)) {
            toJson(content).map(JsonObject.Builder::build).ifPresent(contentValues::add);
        }
        return JsonObject.builder()
                .setValues("content", contentValues)
                .set("isError", result.error())
                .build();
    }

    @Override
    public JsonObject listResources(McpPage<McpResource> page) {
        List<JsonValue> values = page.components().stream()
                .map(this::toJson)
                .map(JsonObject.Builder::build)
                .map(JsonValue.class::cast)
                .toList();
        JsonObject.Builder resources = JsonObject.builder()
                .setValues("resources", values);
        if (!page.cursor().isBlank()) {
            resources.set("nextCursor", page.cursor());
        }
        return resources.build();
    }

    @Override
    public JsonObject listTools(McpPage<McpTool> page) {
        List<JsonValue> values = page.components().stream()
                .map(this::toJson)
                .map(JsonObject.Builder::build)
                .map(JsonValue.class::cast)
                .toList();
        JsonObject.Builder tools = JsonObject.builder()
                .setValues("tools", values);
        if (!page.cursor().isBlank()) {
            tools.set("nextCursor", page.cursor());
        }
        return tools.build();
    }

    @Override
    public JsonObject listResourceTemplates(McpPage<McpResourceTemplate> page) {
        List<JsonValue> values = page.components().stream()
                .map(this::resourceTemplates)
                .map(JsonObject.Builder::build)
                .map(JsonValue.class::cast)
                .toList();
        JsonObject.Builder templates = JsonObject.builder()
                .setValues("resourceTemplates", values);
        if (!page.cursor().isBlank()) {
            templates.set("nextCursor", page.cursor());
        }
        return templates.build();
    }

    @Override
    public JsonObject listPrompts(McpPage<McpPrompt> page) {
        List<JsonValue> values = page.components().stream()
                .map(this::toJson)
                .map(JsonObject.Builder::build)
                .map(JsonValue.class::cast)
                .toList();
        JsonObject.Builder prompts = JsonObject.builder()
                .setValues("prompts", values);
        if (!page.cursor().isBlank()) {
            prompts.set("nextCursor", page.cursor());
        }
        return prompts.build();
    }

    @Override
    public JsonObject.Builder toJson(McpEmbeddedTextResourceContent content) {
        var resource = JsonObject.builder()
                .set("uri", content.uri().toASCIIString())
                .set("mimeType", content.mediaType().text())
                .set("text", content.text());
        return JsonObject.builder()
                .set("type", content.type().text())
                .set("resource", resource.build());
    }

    @Override
    public JsonObject.Builder toJson(McpEmbeddedBinaryResourceContent content) {
        var resource = JsonObject.builder()
                .set("uri", content.uri().toASCIIString())
                .set("mimeType", content.mediaType().text())
                .set("blob", content.base64Data());
        return JsonObject.builder()
                .set("type", content.type().text())
                .set("resource", resource.build());
    }

    @Override
    public JsonObject.Builder toJson(McpPrompt prompt) {
        List<JsonValue> arguments = prompt.arguments().stream()
                .map(this::toJson)
                .map(JsonObject.Builder::build)
                .map(JsonValue.class::cast)
                .toList();
        return JsonObject.builder()
                .set("name", prompt.name())
                .set("description", prompt.description())
                .setValues("arguments", arguments);
    }

    @Override
    public JsonObject.Builder toJson(McpPromptArgument argument) {
        return JsonObject.builder()
                .set("name", argument.name())
                .set("description", argument.description())
                .set("required", argument.required());
    }

    @Override
    public JsonObject.Builder toJson(McpResource resource) {
        return JsonObject.builder()
                .set("uri", resource.uri())
                .set("name", resource.name())
                .set("description", resource.description())
                .set("mimeType", resource.mediaType().text());
    }

    @Override
    public JsonObject.Builder resourceTemplates(McpResource resource) {
        return JsonObject.builder()
                .set("uriTemplate", resource.uri())
                .set("name", resource.name())
                .set("description", resource.description())
                .set("mimeType", resource.mediaType().text());
    }

    @Override
    public JsonObject resourceRead(String uri, McpResourceResult result) {
        List<JsonValue> contents = new ArrayList<>();
        for (McpResourceContent content : McpResourceSupport.aggregateContent(result)) {
            toJson(content)
                    .map(builder -> builder.set("uri", uri).build())
                    .ifPresent(contents::add);
        }
        return JsonObject.builder().setValues("contents", contents).build();
    }

    @Override
    public JsonObject promptGet(McpPromptResult result) {
        List<JsonValue> messages = new ArrayList<>();
        JsonObject.Builder object = JsonObject.builder();
        for (McpPromptContent prompt : McpPromptSupport.aggregateContent(result)) {
            toJson(prompt).map(JsonObject.Builder::build).ifPresent(messages::add);
        }
        result.description().ifPresent(description -> object.set("description", description));
        return object.setValues("messages", messages).build();
    }

    @Override
    public Optional<JsonObject.Builder> toJson(McpPromptContent content) {
        if (content instanceof McpPromptTextContent text) {
            return Optional.of(toJson(text));
        }
        if (content instanceof McpPromptImageContent image) {
            return Optional.of(toJson(image));
        }
        if (content instanceof McpPromptTextResourceContent text) {
            return Optional.of(toJson(text));
        }
        if (content instanceof McpPromptBinaryResourceContent binary) {
            return Optional.of(toJson(binary));
        }
        return Optional.empty();
    }

    @Override
    public Optional<JsonObject.Builder> toJson(McpContent content) {
        if (content instanceof McpTextContent text) {
            return Optional.of(toJson(text));
        }
        if (content instanceof McpImageContent image) {
            return Optional.of(toJson(image));
        }
        if (content instanceof McpEmbeddedTextResourceContent text) {
            return Optional.of(toJson(text));
        }
        if (content instanceof McpEmbeddedBinaryResourceContent binary) {
            return Optional.of(toJson(binary));
        }
        return Optional.empty();
    }

    @Override
    public JsonObject.Builder toJson(McpSamplingMessage message) {
        if (message instanceof McpSamplingTextMessage text) {
            return toJson(text);
        }
        if (message instanceof McpSamplingImageMessage image) {
            return toJson(image);
        }
        if (message instanceof McpSamplingAudioMessage resource) {
            return toJson(resource);
        }
        throw new IllegalArgumentException("Unsupported content type: " + message.getClass().getName());
    }

    @Override
    public Optional<JsonObject.Builder> toJson(McpResourceContent content) {
        if (content instanceof McpResourceTextContent text) {
            return Optional.of(toJson(text));
        }
        if (content instanceof McpResourceBinaryContent binary) {
            return Optional.of(toJson(binary));
        }
        return Optional.empty();
    }

    @Override
    public JsonObject.Builder toJson(McpPromptImageContent image) {
        return JsonObject.builder()
                .set("role", image.role().text())
                .set("content", toJson((McpImageContent) image).build());
    }

    @Override
    public JsonObject.Builder toJson(McpPromptTextResourceContent text) {
        var builder = JsonObject.builder();
        var content = toJson((McpEmbeddedTextResourceContent) text);
        return builder.set("role", text.role().text()).set("content", content.build());
    }

    @Override
    public JsonObject.Builder toJson(McpPromptBinaryResourceContent binary) {
        var builder = JsonObject.builder();
        var content = toJson((McpEmbeddedBinaryResourceContent) binary);
        return builder.set("role", binary.role().text()).set("content", content.build());
    }

    @Override
    public JsonObject.Builder toJson(McpPromptTextContent content) {
        return JsonObject.builder()
                .set("role", content.role().text())
                .set("content", toJson((McpTextContent) content).build());
    }

    @Override
    public Optional<JsonObject.Builder> toJson(McpPromptAudioContent audio) {
        return Optional.empty();
    }

    @Override
    public JsonObject.Builder toJson(McpSamplingImageMessage image) {
        return JsonObject.builder()
                .set("role", image.role().text())
                .set("content", JsonObject.builder()
                        .set("type", image.type().text())
                        .set("data", image.encodeBase64Data())
                        .set("mimeType", image.mediaType().text()).build());
    }

    @Override
    public JsonObject.Builder toJson(McpSamplingTextMessage text) {
        return JsonObject.builder()
                .set("role", text.role().text())
                .set("content", JsonObject.builder()
                        .set("type", text.type().text())
                        .set("text", text.text()).build());
    }

    @Override
    public JsonObject.Builder toJson(McpSamplingAudioMessage audio) {
        return JsonObject.builder()
                .set("role", audio.role().text())
                .set("content", JsonObject.builder()
                        .set("type", audio.type().text())
                        .set("data", audio.encodeBase64Data())
                        .set("mimeType", audio.mediaType().text()).build());
    }

    @Override
    public JsonObject.Builder toJson(McpTextContent content) {
        return JsonObject.builder()
                .set("type", content.type().text())
                .set("text", content.text());
    }

    @Override
    public JsonObject.Builder toJson(McpImageContent content) {
        return JsonObject.builder()
                .set("type", content.type().text())
                .set("data", content.base64Data())
                .set("mimeType", content.mediaType().text());
    }

    @Override
    public Optional<JsonObject.Builder> toJson(McpAudioContent content) {
        return Optional.empty();
    }

    @Override
    public JsonObject.Builder toJson(McpResourceBinaryContent content) {
        return JsonObject.builder()
                .set("mimeType", content.mediaType().text())
                .set("blob", content.base64Data());
    }

    @Override
    public JsonObject.Builder toJson(McpResourceTextContent content) {
        return JsonObject.builder()
                .set("mimeType", content.mediaType().text())
                .set("text", content.text());
    }

    @Override
    public JsonObject progressNotification(McpProgress progress, int newProgress, String message) {
        JsonObject.Builder params = JsonObject.builder()
                .set("progress", newProgress)
                .set("total", progress.total());
        if (progress.token().isBlank()) {
            params.set("progressToken", progress.tokenInt());
        } else {
            params.set("progressToken", progress.token());
        }
        if (message != null) {
            params.set("message", message);
        }
        return createJsonRpcNotification(METHOD_NOTIFICATION_PROGRESS, params);
    }

    @Override
    public JsonObject createLoggingNotification(McpLogger.Level level, String name, String message) {
        var params = JsonObject.builder()
                .set("level", level.text())
                .set("logger", name)
                .set("data", message);
        return createJsonRpcNotification(METHOD_NOTIFICATION_MESSAGE, params);
    }

    @Override
    public JsonObject createUpdateNotification(String uri) {
        var params = JsonObject.builder().set("uri", uri);
        return createJsonRpcNotification(METHOD_NOTIFICATION_UPDATE, params);
    }

    @Override
    public JsonObject completionComplete(McpCompletionResult result) {
        var builder = JsonObject.builder()
                .setStrings("values", result.values());
        result.hasMore().ifPresent(hasMore -> builder.set("hasMore", hasMore));
        result.total().ifPresent(total -> builder.set("total", total));
        return JsonObject.builder()
                .set("completion", builder.build())
                .build();
    }

    @Override
    public JsonObject.Builder toJson(McpSamplingRequest request) {
        List<JsonValue> hints = new ArrayList<>();
        var params = JsonObject.builder();
        List<JsonValue> messages = new ArrayList<>();
        var modelPreference = JsonObject.builder();

        request.hints()
                .stream()
                .flatMap(List::stream)
                .map(hint -> JsonObject.builder().set("name", hint).build())
                .forEach(hints::add);
        request.hints().ifPresent(it -> modelPreference.setValues("hints", hints));
        request.speedPriority().ifPresent(speed -> modelPreference.set("speedPriority", speed));
        request.costPriority().ifPresent(priority -> modelPreference.set("costPriority", priority));
        request.intelligencePriority().ifPresent(intelligence -> modelPreference.set("intelligencePriority", intelligence));
        params.set("modelPreference", modelPreference.build());

        McpSamplingSupport.aggregate(request).stream()
                .map(this::toJson)
                .map(JsonObject.Builder::build)
                .forEach(messages::add);
        params.setValues("messages", messages);
        params.set("maxTokens", request.maxTokens());
        request.systemPrompt().ifPresent(prompt -> params.set("systemPrompt", prompt));
        request.temperature().ifPresent(temperature -> params.set("temperature", temperature));
        request.includeContext().ifPresent(context -> params.set("includeContext", context.text()));
        request.stopSequences().ifPresent(sequences -> params.setStrings("stopSequences", sequences));
        request.metadata().ifPresent(metadata -> params.set("metadata", McpJsonBinding.serialize(metadata)));
        return params;
    }

    @Override
    public JsonObject createJsonRpcNotification(String method, JsonObject.Builder params) {
        return JsonObject.builder()
                .set("jsonrpc", "2.0")
                .set("method", method)
                .set("params", params.build())
                .build();
    }

    @Override
    public JsonObject createJsonRpcRequest(long id, String method, JsonObject.Builder params) {
        return JsonObject.builder()
                .set("jsonrpc", "2.0")
                .set("id", id)
                .set("method", method)
                .set("params", params.build())
                .build();
    }

    @Override
    public JsonObject.Builder createJsonRpcRequest(long id, String method) {
        return JsonObject.builder()
                .set("jsonrpc", "2.0")
                .set("id", id)
                .set("method", method);
    }

    @Override
    public JsonObject createJsonRpcErrorResponse(long id, JsonObject.Builder params) {
        return JsonObject.builder()
                .set("jsonrpc", "2.0")
                .set("id", id)
                .set("error", params.build())
                .build();
    }

    @Override
    public JsonObject createJsonRpcResultResponse(long id, JsonValue params) {
        return JsonObject.builder()
                .set("jsonrpc", "2.0")
                .set("id", id)
                .set("result", params)
                .build();
    }

    @Override
    public JsonObject jsonrpcErrorTimeoutResponse(long requestId) {
        var error = JsonObject.builder()
                .set("code", INTERNAL_ERROR)
                .set("message", "response timeout");
        return createJsonRpcErrorResponse(requestId, error);
    }

    @Override
    public List<McpRoot> parseRoots(JsonObject response) {
        find(response, "error")
                .filter(this::isJsonObject)
                .map(JsonValue::asObject)
                .map(JsonRpcError::create)
                .ifPresent(error -> {
                    throw new McpRootException(error.message());
                });
        JsonArray roots = find(response, "result")
                .map(JsonValue::asObject)
                .flatMap(result -> find(result, "roots"))
                .map(JsonValue::asArray)
                .orElseThrow(() -> new McpRootException("Wrong response format: %s".formatted(response)));

        return roots.values().stream()
                .map(JsonValue::asObject)
                .map(root -> McpRoot.builder()
                        .uri(URI.create(root.stringValue("uri").orElseThrow()))
                        .name(root.stringValue("name"))
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
                .map(JsonValue::asObject)
                .map(JsonRpcError::create)
                .ifPresent(error -> {
                    throw new McpSamplingException(error.message());
                });
        try {
            var result = find(object, "result")
                    .filter(this::isJsonObject)
                    .map(JsonValue::asObject)
                    .orElseThrow(() -> new McpSamplingException(String.format("Sampling result not found: %s", object)));

            String model = result.stringValue("model").orElseThrow();
            McpRole role = result.stringValue("role")
                    .map(String::toUpperCase)
                    .map(McpRole::valueOf)
                    .orElseThrow();
            McpSamplingMessage message = parseMessage(role, result.objectValue("content").orElseThrow());
            McpStopReason stopReason = find(result, "stopReason")
                    .filter(this::isJsonString)
                    .map(JsonString.class::cast)
                    .map(JsonString::value)
                    .map(McpStopReason::map)
                    .orElse(null);
            return new McpSamplingResponseImpl(message, model, stopReason);
        } catch (Exception e) {
            throw new McpSamplingException("Wrong sampling response format", e);
        }
    }

    @Override
    public McpElicitationResponse createElicitationResponse(JsonObject object) throws McpElicitationException {
        throw new McpElicitationException("Elicitation not supported");
    }

    @Override
    public JsonObject createElicitationRequest(long id, McpElicitationRequest request) {
        throw new McpElicitationException("Elicitation not supported");
    }

    McpSamplingMessage parseMessage(McpRole role, JsonObject object) {
        String type = object.stringValue("type")
                .map(String::toUpperCase)
                .orElseThrow();
        McpSamplingMessageType messageType = McpSamplingMessageType.valueOf(type);
        return switch (messageType) {
            case TEXT -> McpSamplingTextMessage.builder().text(object.stringValue("text").orElseThrow()).role(role).build();
            case IMAGE -> {
                byte[] data = object.stringValue("data")
                        .map(value -> value.getBytes(StandardCharsets.UTF_8))
                        .orElseThrow();
                MediaType mediaType = MediaTypes.create(object.stringValue("mimeType").orElseThrow());
                yield McpSamplingImageMessage.builder().data(data).mediaType(mediaType).role(role).build();
            }
            case AUDIO -> {
                byte[] data = object.stringValue("data")
                        .map(value -> value.getBytes(StandardCharsets.UTF_8))
                        .orElseThrow();
                MediaType mediaType = MediaTypes.create(object.stringValue("mimeType").orElseThrow());
                yield McpSamplingAudioMessage.builder().data(data).mediaType(mediaType).role(role).build();
            }
        };
    }

    Optional<JsonValue> find(JsonObject object, String key) {
        return object.value(key);
    }

    boolean isJsonObject(JsonValue value) {
        return value instanceof JsonObject;
    }

    boolean isJsonString(JsonValue value) {
        return value instanceof JsonString;
    }
}
