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

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;

/**
 * Serialize MCP classes to JSON.
 */
interface McpJsonSerializer {
    JsonBuilderFactory JSON_BUILDER_FACTORY = Json.createBuilderFactory(Map.of());
    JsonWriterFactory JSON_PP_WRITER_FACTORY = Json.createWriterFactory(Map.of(JsonGenerator.PRETTY_PRINTING, true));

    /**
     * JSON-RPC {@code initialize} method.
     */
    String METHOD_INITIALIZE = "initialize";
    /**
     * JSON-RPC {@code notifications/initialize} method.
     */
    String METHOD_NOTIFICATION_INITIALIZED = "notifications/initialized";
    /**
     * JSON-RPC {@code ping} method.
     */
    String METHOD_PING = "ping";
    /**
     * JSON-RPC {@code tools/list} method.
     */
    String METHOD_TOOLS_LIST = "tools/list";
    /**
     * JSON-RPC {@code tools/call} method.
     */
    String METHOD_TOOLS_CALL = "tools/call";
    /**
     * JSON-RPC {@code notifications/tools/list_changed} method.
     */
    String METHOD_NOTIFICATION_TOOLS_LIST_CHANGED = "notifications/tools/list_changed";
    /**
     * JSON-RPC {@code resources/list} method.
     */
    String METHOD_RESOURCES_LIST = "resources/list";
    /**
     * JSON-RPC {@code resources/read} method.
     */
    String METHOD_RESOURCES_READ = "resources/read";
    /**
     * JSON-RPC {@code notifications/resources/list_changed} method.
     */
    String METHOD_NOTIFICATION_RESOURCES_LIST_CHANGED = "notifications/resources/list_changed";
    /**
     * JSON-RPC {@code resources/templates/list} method.
     */
    String METHOD_RESOURCES_TEMPLATES_LIST = "resources/templates/list";
    /**
     * JSON-RPC {@code resources/subscribe} method.
     */
    String METHOD_RESOURCES_SUBSCRIBE = "resources/subscribe";
    /**
     * JSON-RPC {@code resources/unsubscribe} method.
     */
    String METHOD_RESOURCES_UNSUBSCRIBE = "resources/unsubscribe";
    /**
     * JSON-RPC {@code prompts/list} method.
     */
    String METHOD_PROMPT_LIST = "prompts/list";
    /**
     * JSON-RPC {@code prompts/get} method.
     */
    String METHOD_PROMPT_GET = "prompts/get";
    /**
     * JSON-RPC {@code notifications/prompts/list_changed} method.
     */
    String METHOD_NOTIFICATION_PROMPTS_LIST_CHANGED = "notifications/prompts/list_changed";
    /**
     * JSON-RPC {@code logging/setLevel} method.
     */
    String METHOD_LOGGING_SET_LEVEL = "logging/setLevel";
    /**
     * JSON-RPC {@code notifications/message} method.
     */
    String METHOD_NOTIFICATION_MESSAGE = "notifications/message";
    /**
     * JSON-RPC {@code notifications/cancelled} method.
     */
    String METHOD_NOTIFICATION_CANCELED = "notifications/cancelled";
    /**
     * JSON-RPC {@code notifications/resources/updated} method.
     */
    String METHOD_NOTIFICATION_UPDATE = "notifications/resources/updated";
    /**
     * JSON-RPC {@code completion/complete} method.
     */
    String METHOD_COMPLETION_COMPLETE = "completion/complete";
    /**
     * JSON-RPC {@code roots/list} method.
     */
    String METHOD_ROOTS_LIST = "roots/list";
    /**
     * JSON-RPC {@code notification/roots/list_changed} method.
     */
    String METHOD_NOTIFICATION_ROOTS_LIST_CHANGED = "notifications/roots/list_changed";
    /**
     * JSON-RPC {@code sampling/createMessage} method.
     */
    String METHOD_SAMPLING_CREATE_MESSAGE = "sampling/createMessage";
    /**
     * JSON-RPC {@code notifications/progress} method.
     */
    String METHOD_NOTIFICATION_PROGRESS = "notifications/progress";
    /**
     * JSON-RPC {@code session/disconnect} method.
     */
    String METHOD_SESSION_DISCONNECT = "session/disconnect";

    static McpJsonSerializer create(McpProtocolVersion version) {
        return switch (version) {
            case VERSION_2025_06_18 -> new McpJsonSerializerV3();
            case VERSION_2025_03_26 -> new McpJsonSerializerV2();
            case VERSION_2024_11_05 -> new McpJsonSerializerV1();
        };
    }

    static String prettyPrint(JsonStructure json) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (JsonWriter writer = JSON_PP_WRITER_FACTORY.createWriter(baos)) {
            writer.write(json);
        }
        return baos.toString(StandardCharsets.UTF_8);
    }

    static boolean isResponse(JsonObject payload) {
        return !payload.containsKey("method") && payload.containsKey("id");
    }

    JsonObjectBuilder toJson(Set<McpCapability> capabilities, McpServerConfig config);

    JsonObjectBuilder toJson(McpTool tool);

    JsonObjectBuilder toolCall(McpTool tool, McpToolResult result);

    JsonObject listResources(McpPage<McpResource> page);

    JsonObject listTools(McpPage<McpTool> page);

    JsonObject listResourceTemplates(McpPage<McpResourceTemplate> page);

    JsonObject listPrompts(McpPage<McpPrompt> page);

    Optional<JsonObjectBuilder> toJson(McpToolResourceContent content);

    JsonObjectBuilder toJson(McpPrompt prompt);

    JsonObjectBuilder toJson(McpPromptArgument argument);

    JsonObjectBuilder toJson(McpResource resource);

    JsonObjectBuilder resourceTemplates(McpResource resource);

    JsonObject readResource(String uri, List<McpResourceContent> contents);

    JsonObject toJson(List<McpPromptContent> contents, String description);

    Optional<JsonObjectBuilder> toJson(McpPromptContent content);

    Optional<JsonObjectBuilder> toJson(McpContent content);

    JsonObjectBuilder toJson(McpSamplingMessage message);

    JsonObjectBuilder toJson(McpResourceContent content);

    Optional<JsonObjectBuilder> toJson(McpPromptResourceContent resource);

    Optional<JsonObjectBuilder> toJson(McpPromptImageContent image);

    Optional<JsonObjectBuilder> toJson(McpPromptTextContent content);

    Optional<JsonObjectBuilder> toJson(McpPromptAudioContent audio);

    JsonObjectBuilder toJson(McpSamplingImageMessage image);

    JsonObjectBuilder toJson(McpSamplingTextMessage text);

    JsonObjectBuilder toJson(McpSamplingAudioMessage audio);

    JsonObjectBuilder toJson(McpTextContent content);

    JsonObjectBuilder toJson(McpImageContent content);

    Optional<JsonObjectBuilder> toJson(McpAudioContent content);

    JsonObjectBuilder toJson(McpResourceBinaryContent content);

    JsonObjectBuilder toJson(McpResourceTextContent content);

    JsonObject toJson(McpProgress progress, int newProgress, String message);

    JsonObject createLoggingNotification(McpLogger.Level level, String name, String message);

    JsonObject createUpdateNotification(String uri);

    JsonObject toJson(McpCompletionContent content);

    JsonObjectBuilder toJson(McpSamplingRequest request);

    JsonObject createJsonRpcNotification(String method, JsonObjectBuilder params);

    JsonObject createJsonRpcRequest(long id, String method, JsonObjectBuilder params);

    JsonObject createJsonRpcRequest(long id, String method);

    JsonObject createJsonRpcErrorResponse(long id, JsonObjectBuilder params);

    JsonObject createJsonRpcResultResponse(long id, JsonValue params);

    JsonObject timeoutResponse(long requestId);

    List<McpRoot> parseRoots(JsonObject response);

    JsonObject createSamplingRequest(long id, McpSamplingRequest request);

    McpSamplingResponse createSamplingResponse(JsonObject object) throws McpSamplingException;
}
