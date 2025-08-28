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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

import io.helidon.builder.api.RuntimeType;
import io.helidon.common.LruCache;
import io.helidon.http.Status;
import io.helidon.http.sse.SseEvent;
import io.helidon.jsonrpc.core.JsonRpcError;
import io.helidon.webserver.http.HttpFeature;
import io.helidon.webserver.http.HttpRequest;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import io.helidon.webserver.jsonrpc.JsonRpcHandlers;
import io.helidon.webserver.jsonrpc.JsonRpcRequest;
import io.helidon.webserver.jsonrpc.JsonRpcResponse;
import io.helidon.webserver.jsonrpc.JsonRpcRouting;
import io.helidon.webserver.sse.SseSink;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import static io.helidon.extensions.mcp.server.McpSession.State.INITIALIZING;
import static io.helidon.extensions.mcp.server.McpSession.State.UNINITIALIZED;

/**
 * Mcp http feature is the actual MCP server as a Helidon {@link HttpFeature}.
 */
@RuntimeType.PrototypedBy(McpServerConfig.class)
public final class McpServerFeature implements HttpFeature, RuntimeType.Api<McpServerConfig> {
    private static final int SESSION_CACHE_SIZE = 1000;
    private static final String PROTOCOL_VERSION = "2024-11-05";

    private final String endpoint;
    private final McpServerConfig config;
    private final JsonRpcHandlers jsonRpcHandlers;
    private final LruCache<String, McpSession> sessions = LruCache.create(SESSION_CACHE_SIZE);
    private final Set<McpCapability> capabilities = new HashSet<>();
    private final Map<String, McpTool> tools = new ConcurrentHashMap<>();
    private final Map<String, McpPrompt> prompts = new ConcurrentHashMap<>();
    private final Map<String, McpResource> resources = new ConcurrentHashMap<>();
    private final Map<String, McpCompletion> completions = new ConcurrentHashMap<>();
    private final Map<String, McpResource> resourceTemplates = new ConcurrentHashMap<>();

    private McpServerFeature(McpServerConfig config) {
        String path = config.path();
        JsonRpcHandlers.Builder builder = JsonRpcHandlers.builder();

        this.config = config;
        this.endpoint = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        for (McpResource resource : config.resources()) {
            resources.put(resource.uri(), resource);
            if (isTemplate(resource)) {
                resourceTemplates.put(resource.uri(), resource);
            }
        }
        for (McpTool tool : config.tools()) {
            tools.put(tool.name(), tool);
        }
        for (McpPrompt prompt : config.prompts()) {
            prompts.put(prompt.name(), prompt);
        }
        for (McpCompletion completion : config.completions()) {
            completions.put(completion.reference(), completion);
        }

        builder.putMethod(McpJsonRpc.METHOD_PING, this::pingRpc);
        builder.putMethod(McpJsonRpc.METHOD_INITIALIZE, this::initializeRpc);

        if (!config.tools().isEmpty()) {
            capabilities.add(McpCapability.TOOL_LIST_CHANGED);
            builder.putMethod(McpJsonRpc.METHOD_TOOLS_LIST, this::toolsListRpc);
            builder.putMethod(McpJsonRpc.METHOD_TOOLS_CALL, this::toolsCallRpc);
        }

        if (!config.resources().isEmpty()) {
            capabilities.add(McpCapability.RESOURCE_LIST_CHANGED);
            capabilities.add(McpCapability.RESOURCE_SUBSCRIBE);
            builder.putMethod(McpJsonRpc.METHOD_RESOURCES_LIST, this::resourcesListRpc);
            builder.putMethod(McpJsonRpc.METHOD_RESOURCES_READ, this::resourcesReadRpc);
            builder.putMethod(McpJsonRpc.METHOD_RESOURCES_TEMPLATES_LIST, this::resourceTemplateListRpc);
            builder.putMethod(McpJsonRpc.METHOD_RESOURCES_SUBSCRIBE, this::resourceSubscribeRpc);
            builder.putMethod(McpJsonRpc.METHOD_RESOURCES_UNSUBSCRIBE, this::resourceUnsubscribeRpc);
        }

        if (!config.prompts().isEmpty()) {
            capabilities.add(McpCapability.PROMPT_LIST_CHANGED);
            builder.putMethod(McpJsonRpc.METHOD_PROMPT_LIST, this::promptsListRpc);
            builder.putMethod(McpJsonRpc.METHOD_PROMPT_GET, this::promptsGetRpc);
        }

        if (config.logging()) {
            capabilities.add(McpCapability.LOGGING);
            builder.putMethod(McpJsonRpc.METHOD_LOGGING_SET_LEVEL, this::loggingLogLevelRpc);
        }

        capabilities.add(McpCapability.COMPLETION);
        completions.put(NoopCompletion.REFERENCE, new NoopCompletion());
        builder.putMethod(McpJsonRpc.METHOD_COMPLETION_COMPLETE, this::completionRpc);

        builder.putMethod(McpJsonRpc.METHOD_NOTIFICATION_INITIALIZED, this::notificationInitRpc);
        builder.putMethod(McpJsonRpc.METHOD_NOTIFICATION_CANCELED, this::notificationCancelRpc);
        builder.putMethod(McpJsonRpc.METHOD_SESSION_DISCONNECT, this::disconnect);

        builder.errorHandler(this::handleErrorRequest);

        jsonRpcHandlers = builder.build();
    }

    static McpServerFeature create(McpServerConfig config) {
        return new McpServerFeature(config);
    }

    static McpServerFeature create(Consumer<McpServerConfig.Builder> consumer) {
        McpServerConfig.Builder builder = McpServerConfig.builder();
        consumer.accept(builder);
        return builder.build();
    }

    /**
     * McpServer builder.
     *
     * @return McpServer builder
     */
    public static McpServerConfig.Builder builder() {
        return McpServerConfig.builder();
    }

    @Override
    public void setup(HttpRouting.Builder routing) {
        // add all the JSON-RPC routes first
        JsonRpcRouting jsonRpcRouting = JsonRpcRouting.builder()
                .register(endpoint + "/message", jsonRpcHandlers)
                .build();
        jsonRpcRouting.routing(routing);

        // additional HTTP routes for SSE and session disconnect
        routing.get(endpoint, this::sse)
                .delete(endpoint, this::disconnect);
    }

    @Override
    public McpServerConfig prototype() {
        return config;
    }

    private void disconnect(ServerRequest request, ServerResponse response) {
        disconnectSession(request, response);
    }

    private void disconnect(JsonRpcRequest request, JsonRpcResponse response) {
        disconnectSession(request, response);
    }

    private void disconnectSession(HttpRequest request, ServerResponse response) {
        Optional<McpSession> foundSession = findSession(request);
        if (foundSession.isEmpty()) {
            response.status(Status.NOT_FOUND_404).send();
            return;
        }
        foundSession.get().disconnect();
    }

    private void sse(ServerRequest request, ServerResponse response) {
        String sessionId = UUID.randomUUID().toString();
        McpSession session = new McpSession(capabilities);
        sessions.put(sessionId, session);

        try (SseSink sink = response.sink(SseSink.TYPE)) {
            sink.emit(SseEvent.builder()
                              .name("endpoint")
                              .data(endpoint + "/message?sessionId=" + sessionId)
                              .build());
            session.poll(message -> sink.emit(SseEvent.builder()
                                                      .name("message")
                                                      .data(message)
                                                      .build()));
        } catch (McpException e) {
            session.disconnect();
            sessions.remove(sessionId);
        }
    }

    private Optional<McpSession> findSession(HttpRequest req) {
        try {
            String sessionId = req.query().get("sessionId");
            return sessions.get(sessionId);
        } catch (NoSuchElementException e) {
            return Optional.empty();
        }
    }

    /**
     * If we receive what looks like a response on the error handler,
     * pass it to the session.
     *
     * @param req    the HTTP request
     * @param object the invalid JSON-RPC request
     * @return whether error was handled or not
     */
    private Optional<JsonRpcError> handleErrorRequest(ServerRequest req, JsonObject object) {
        if (McpJsonRpc.isResponse(object)) {
            Optional<McpSession> session = findSession(req);
            if (session.isPresent()) {
                session.get().send(object);
                return Optional.empty();
            }
        }
        return Optional.of(JsonRpcError.create(JsonRpcError.INVALID_REQUEST, "Invalid request"));
    }

    private void notificationInitRpc(JsonRpcRequest req, JsonRpcResponse res) {
        Optional<McpSession> session = findSession(req);
        if (session.isEmpty()) {
            res.status(Status.NOT_FOUND_404).send();
            return;
        }
        session.get().state(McpSession.State.INITIALIZED);
    }

    private void notificationCancelRpc(JsonRpcRequest req, JsonRpcResponse res) {
    }

    private void initializeRpc(JsonRpcRequest req, JsonRpcResponse res) {
        Optional<McpSession> foundSession = findSession(req);
        if (foundSession.isEmpty()) {
            res.status(Status.NOT_FOUND_404).send();
            return;
        }
        McpSession session = foundSession.get();

        if (session.state() == UNINITIALIZED) {
            session.state(INITIALIZING);
            McpParameters params = new McpParameters(req.params(), req.params().asJsonObject());
            parseClientCapabilities(session, params);
        }
        session.send(res.result(McpJsonRpc.toJson(PROTOCOL_VERSION, capabilities, config)));
    }

    private void parseClientCapabilities(McpSession session, McpParameters parameters) {
        var capabilities = parameters.get("capabilities");
        if (capabilities.get(McpCapability.SAMPLING.text()).isPresent()) {
            session.capabilities(McpCapability.SAMPLING);
        }
        capabilities.get("roots")
                .get("listChanged")
                .asBoolean()
                .ifPresent(listChanged -> {
                    if (listChanged) {
                        session.capabilities(McpCapability.ROOT);
                    }
                });
    }

    private void pingRpc(JsonRpcRequest req, JsonRpcResponse res) {
        Optional<McpSession> foundSession = findSession(req);
        if (foundSession.isEmpty()) {
            res.status(Status.NOT_FOUND_404).send();
            return;
        }
        foundSession.get().send(res.result(JsonValue.EMPTY_JSON_OBJECT));
    }

    private void toolsListRpc(JsonRpcRequest req, JsonRpcResponse res) {
        Optional<McpSession> foundSession = findSession(req);
        if (foundSession.isEmpty()) {
            res.status(Status.NOT_FOUND_404).send();
            return;
        }
        foundSession.get().send(res.result(McpJsonRpc.listTools(tools.values())));
    }

    private void toolsCallRpc(JsonRpcRequest req, JsonRpcResponse res) {
        Optional<McpSession> foundSession = findSession(req);
        if (foundSession.isEmpty()) {
            res.status(Status.NOT_FOUND_404).send();
            return;
        }
        McpSession session = foundSession.get();
        McpParameters parameters = new McpParameters(req.params(), req.params().asJsonObject());
        enableProgress(session, parameters);

        String name = parameters.get("name").asString().orElse("");
        Optional<McpTool> tool = tools.values()
                .stream()
                .filter(t -> name.equals(t.name()))
                .findAny();

        if (tool.isEmpty()) {
            session.send(res.error(JsonRpcError.INVALID_PARAMS, "Tool with name %s is not available".formatted(name)));
            return;
        }

        List<McpToolContent> contents = tool.get()
                .tool()
                .apply(McpRequest.builder()
                               .parameters(parameters.get("arguments"))
                               .features(session.features())
                               .build());
        session.features().progress().stopSending();
        session.send(res.result(McpJsonRpc.toolCall(contents).build()));
    }

    private void resourcesListRpc(JsonRpcRequest req, JsonRpcResponse res) {
        Optional<McpSession> foundSession = findSession(req);
        if (foundSession.isEmpty()) {
            res.status(Status.NOT_FOUND_404).send();
            return;
        }
        McpSession session = foundSession.get();
        List<McpResource> resourceList = resources.values().stream()
                .filter(this::isNotTemplate)
                .toList();
        session.send(res.result(McpJsonRpc.listResources(resourceList)));
    }

    private void resourcesReadRpc(JsonRpcRequest req, JsonRpcResponse res) {
        Optional<McpSession> foundSession = findSession(req);
        if (foundSession.isEmpty()) {
            res.status(Status.NOT_FOUND_404).send();
            return;
        }
        McpSession session = foundSession.get();

        McpParameters parameters = new McpParameters(req.params(), req.params().asJsonObject());
        String resourceUri = parameters.get("uri").asString().orElse("");
        Optional<McpResource> resource = resources.values()
                .stream()
                .filter(it -> Objects.equals(it.uri(), resourceUri))
                .findFirst();

        if (resource.isEmpty()) {
            session.send(res.error(JsonRpcError.INVALID_REQUEST, "Resource does not exist"));
            return;
        }
        if (isTemplate(resource.get())) {
            session.send(res.error(JsonRpcError.INVALID_REQUEST, "Resource Template cannot be read."));
            return;
        }

        enableProgress(session, parameters);
        List<McpResourceContent> contents = resource.get().resource().apply(McpRequest.builder()
                                                                                    .parameters(parameters)
                                                                                    .features(session.features())
                                                                                    .build());
        session.features().progress().stopSending();
        session.send(res.result(McpJsonRpc.readResource(resourceUri, contents)));
    }

    private void resourceSubscribeRpc(JsonRpcRequest req, JsonRpcResponse res) {
    }

    private void resourceUnsubscribeRpc(JsonRpcRequest req, JsonRpcResponse res) {
    }

    private void resourceTemplateListRpc(JsonRpcRequest req, JsonRpcResponse res) {
        Optional<McpSession> foundSession = findSession(req);
        if (foundSession.isEmpty()) {
            res.status(Status.NOT_FOUND_404).send();
            return;
        }
        McpSession session = foundSession.get();
        session.send(res.result(McpJsonRpc.listResourceTemplates(resourceTemplates.values())));
    }

    private void promptsListRpc(JsonRpcRequest req, JsonRpcResponse res) {
        Optional<McpSession> foundSession = findSession(req);
        if (foundSession.isEmpty()) {
            res.status(Status.NOT_FOUND_404).send();
            return;
        }
        McpSession session = foundSession.get();
        session.send(res.result(McpJsonRpc.listPrompts(prompts.values())));
    }

    private void promptsGetRpc(JsonRpcRequest req, JsonRpcResponse res) {
        Optional<McpSession> foundSession = findSession(req);
        if (foundSession.isEmpty()) {
            res.status(Status.NOT_FOUND_404).send();
            return;
        }
        McpSession session = foundSession.get();
        McpParameters parameters = new McpParameters(req.params(), req.params().asJsonObject());
        enableProgress(session, parameters);

        String name = parameters.get("name").asString().orElse(null);
        if (name == null) {
            JsonRpcResponse error = res.error(JsonRpcError.INVALID_REQUEST, "Prompt name is missing from request " + req.id());
            session.send(error);
            return;
        }

        var prompt = prompts.values()
                .stream()
                .filter(p -> Objects.equals(p.name(), name))
                .findFirst();
        if (prompt.isEmpty()) {
            session.features().progress().stopSending();
            session.send(res.error(JsonRpcError.INVALID_PARAMS, "Wrong prompt name: " + name));
            return;
        }

        List<McpPromptContent> contents = prompt.get()
                .prompt()
                .apply(McpRequest.builder()
                               .parameters(parameters.get("arguments"))
                               .features(session.features())
                               .build());
        session.features().progress().stopSending();
        session.send(res.result(McpJsonRpc.toJson(contents, prompt.get().description())));
    }

    private void loggingLogLevelRpc(JsonRpcRequest req, JsonRpcResponse res) {
        Optional<McpSession> foundSession = findSession(req);
        if (foundSession.isEmpty()) {
            res.status(Status.NOT_FOUND_404).send();
            return;
        }
        McpSession session = foundSession.get();

        McpParameters parameters = new McpParameters(req.params(), req.params().asJsonObject());
        parameters.get("level").asString().ifPresent(level -> {
            McpLogger.Level logLevel = McpLogger.Level.valueOf(level.toUpperCase());
            session.features().logger().setLevel(logLevel);
        });
        session.send(res.result(McpJsonRpc.empty()));
    }

    private void completionRpc(JsonRpcRequest req, JsonRpcResponse res) {
        Optional<McpSession> foundSession = findSession(req);
        if (foundSession.isEmpty()) {
            res.status(Status.NOT_FOUND_404).send();
            return;
        }
        McpSession session = foundSession.get();
        McpParameters parameters = new McpParameters(req.params(), req.params().asJsonObject());
        String reference = parseCompletionName(parameters.get("ref"));
        if (reference == null) {
            session.send(res.error(JsonRpcError.INVALID_REQUEST, "Completion name is missing from request"));
            return;
        }

        McpCompletion completion = completions.get(reference);
        if (completion == null) {
            completion = completions.get(NoopCompletion.REFERENCE);
        }
        McpCompletionContent result = completion.completion()
                .apply(McpRequest.builder()
                               .parameters(parameters.get("argument"))
                               .features(session.features())
                               .build());
        session.send(res.result(McpJsonRpc.toJson(result)));
    }

    private String parseCompletionName(McpParameters completion) {
        var name = completion.get("name").asString();
        if (name.isPresent()) {
            return name.get();
        }
        var uri = completion.get("uri").asString();
        if (uri.isPresent()) {
            return uri.get();
        }
        return null;
    }

    private void enableProgress(McpSession session, McpParameters parameters) {
        var progressToken = parameters.get("_meta").get("progressToken");
        if (progressToken.isEmpty()) {
            return;
        }
        if (progressToken.isNumber()) {
            session.features()
                    .progress()
                    .token(progressToken.asInteger().get());
        }
        if (progressToken.isString()) {
            session.features()
                    .progress()
                    .token(progressToken.asString().get());
        }
    }

    private boolean isTemplate(McpResource resource) {
        String uri = resource.uri();
        return uri.contains("{") || uri.contains("}");
    }

    private boolean isNotTemplate(McpResource resource) {
        return !isTemplate(resource);
    }

    private static final class NoopCompletion implements McpCompletion {
        private static final String REFERENCE = "noop-completion";

        @Override
        public String reference() {
            return REFERENCE;
        }

        @Override
        public Function<McpRequest, McpCompletionContent> completion() {
            return request -> McpCompletionContents.completion("");
        }
    }
}
