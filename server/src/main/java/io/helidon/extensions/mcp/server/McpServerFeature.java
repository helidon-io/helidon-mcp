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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.helidon.builder.api.RuntimeType;
import io.helidon.common.LruCache;
import io.helidon.http.HeaderName;
import io.helidon.http.HeaderNames;
import io.helidon.http.ServerRequestHeaders;
import io.helidon.http.Status;
import io.helidon.http.sse.SseEvent;
import io.helidon.jsonrpc.core.JsonRpcError;
import io.helidon.jsonrpc.core.JsonRpcParams;
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
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

import static io.helidon.extensions.mcp.server.McpJsonRpc.METHOD_COMPLETION_COMPLETE;
import static io.helidon.extensions.mcp.server.McpJsonRpc.METHOD_INITIALIZE;
import static io.helidon.extensions.mcp.server.McpJsonRpc.METHOD_LOGGING_SET_LEVEL;
import static io.helidon.extensions.mcp.server.McpJsonRpc.METHOD_NOTIFICATION_CANCELED;
import static io.helidon.extensions.mcp.server.McpJsonRpc.METHOD_NOTIFICATION_INITIALIZED;
import static io.helidon.extensions.mcp.server.McpJsonRpc.METHOD_PING;
import static io.helidon.extensions.mcp.server.McpJsonRpc.METHOD_PROMPT_GET;
import static io.helidon.extensions.mcp.server.McpJsonRpc.METHOD_PROMPT_LIST;
import static io.helidon.extensions.mcp.server.McpJsonRpc.METHOD_RESOURCES_LIST;
import static io.helidon.extensions.mcp.server.McpJsonRpc.METHOD_RESOURCES_READ;
import static io.helidon.extensions.mcp.server.McpJsonRpc.METHOD_RESOURCES_SUBSCRIBE;
import static io.helidon.extensions.mcp.server.McpJsonRpc.METHOD_RESOURCES_TEMPLATES_LIST;
import static io.helidon.extensions.mcp.server.McpJsonRpc.METHOD_RESOURCES_UNSUBSCRIBE;
import static io.helidon.extensions.mcp.server.McpJsonRpc.METHOD_SESSION_DISCONNECT;
import static io.helidon.extensions.mcp.server.McpJsonRpc.METHOD_TOOLS_CALL;
import static io.helidon.extensions.mcp.server.McpJsonRpc.METHOD_TOOLS_LIST;
import static io.helidon.extensions.mcp.server.McpJsonRpc.empty;
import static io.helidon.extensions.mcp.server.McpJsonRpc.isResponse;
import static io.helidon.extensions.mcp.server.McpJsonRpc.listPrompts;
import static io.helidon.extensions.mcp.server.McpJsonRpc.listResourceTemplates;
import static io.helidon.extensions.mcp.server.McpJsonRpc.listResources;
import static io.helidon.extensions.mcp.server.McpJsonRpc.listTools;
import static io.helidon.extensions.mcp.server.McpJsonRpc.readResource;
import static io.helidon.extensions.mcp.server.McpJsonRpc.toJson;
import static io.helidon.extensions.mcp.server.McpJsonRpc.toolCall;
import static io.helidon.extensions.mcp.server.McpSession.State.INITIALIZING;
import static io.helidon.extensions.mcp.server.McpSession.State.UNINITIALIZED;
import static io.helidon.jsonrpc.core.JsonRpcError.INVALID_PARAMS;
import static io.helidon.jsonrpc.core.JsonRpcError.INVALID_REQUEST;

/**
 * Mcp http feature is the actual MCP server as a Helidon {@link HttpFeature}.
 */
@RuntimeType.PrototypedBy(McpServerConfig.class)
public final class McpServerFeature implements HttpFeature, RuntimeType.Api<McpServerConfig> {
    private static final int SESSION_CACHE_SIZE = 1000;
    private static final List<String> PROTOCOL_VERSION = List.of("2024-11-05", "2025-03-26");
    private static final HeaderName SESSION_ID_HEADER = HeaderNames.create("Mcp-Session-Id");
    private static final Logger LOGGER = Logger.getLogger(McpServerFeature.class.getName());

    private final String endpoint;
    private final McpServerConfig config;
    private final JsonRpcHandlers jsonRpcHandlers;
    private final McpPagination<McpTool> tools;
    private final McpPagination<McpPrompt> prompts;
    private final McpPagination<McpResource> resources;
    private final McpPagination<McpResourceTemplate> resourceTemplates;
    private final Set<McpCapability> capabilities = new HashSet<>();
    private final Map<String, McpCompletion> completions = new ConcurrentHashMap<>();
    private final LruCache<String, McpSession> sessions = LruCache.create(SESSION_CACHE_SIZE);

    private McpServerFeature(McpServerConfig config) {
        String path = config.path();
        List<McpTool> tools = new CopyOnWriteArrayList<>(config.tools());
        List<McpPrompt> prompts = new CopyOnWriteArrayList<>(config.prompts());
        List<McpResource> resources = new CopyOnWriteArrayList<>();
        List<McpResourceTemplate> templates = new CopyOnWriteArrayList<>();
        JsonRpcHandlers.Builder builder = JsonRpcHandlers.builder();

        this.config = config;
        this.endpoint = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        for (McpResource resource : config.resources()) {
            if (isTemplate(resource)) {
                templates.add(new McpResourceTemplate(resource));
            } else {
                resources.add(resource);
            }
        }
        for (McpCompletion completion : config.completions()) {
            completions.put(completion.reference(), completion);
        }

        this.tools = new McpPagination<>(tools, config.toolsPageSize());
        this.prompts = new McpPagination<>(prompts, config.promptsPageSize());
        this.resources = new McpPagination<>(resources, config.resourcesPageSize());
        this.resourceTemplates = new McpPagination<>(templates, config.resourceTemplatesPageSize());

        builder.method(METHOD_PING, this::pingRpc);
        builder.method(METHOD_INITIALIZE, this::initializeRpc);

        if (!config.tools().isEmpty()) {
            capabilities.add(McpCapability.TOOL_LIST_CHANGED);
            builder.method(METHOD_TOOLS_LIST, this::toolsListRpc);
            builder.method(METHOD_TOOLS_CALL, this::toolsCallRpc);
        }

        if (!config.resources().isEmpty()) {
            capabilities.add(McpCapability.RESOURCE_LIST_CHANGED);
            capabilities.add(McpCapability.RESOURCE_SUBSCRIBE);
            builder.method(METHOD_RESOURCES_LIST, this::resourcesListRpc);
            builder.method(METHOD_RESOURCES_READ, this::resourcesReadRpc);
            builder.method(METHOD_RESOURCES_SUBSCRIBE, this::resourceSubscribeRpc);
            builder.method(METHOD_RESOURCES_UNSUBSCRIBE, this::resourceUnsubscribeRpc);
            builder.method(METHOD_RESOURCES_TEMPLATES_LIST, this::resourceTemplateListRpc);
        }

        if (!config.prompts().isEmpty()) {
            capabilities.add(McpCapability.PROMPT_LIST_CHANGED);
            builder.method(METHOD_PROMPT_LIST, this::promptsListRpc);
            builder.method(METHOD_PROMPT_GET, this::promptsGetRpc);
        }

        capabilities.add(McpCapability.LOGGING);
        builder.method(METHOD_LOGGING_SET_LEVEL, this::loggingLogLevelRpc);

        capabilities.add(McpCapability.COMPLETION);
        completions.put(NoopCompletion.REFERENCE, new NoopCompletion());
        builder.method(METHOD_COMPLETION_COMPLETE, this::completionRpc);

        builder.method(METHOD_SESSION_DISCONNECT, this::disconnect);
        builder.method(METHOD_NOTIFICATION_CANCELED, this::notificationCancelRpc);
        builder.method(METHOD_NOTIFICATION_INITIALIZED, this::notificationInitRpc);

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
     * Create a server configuration builder instance.
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
                .register(endpoint, jsonRpcHandlers)        // streamable HTTP
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

    /**
     * Checks if using SSE or streamable HTTP.
     *
     * @param headers the request headers
     * @return outcome of test
     */
    private boolean isStreamableHttp(ServerRequestHeaders headers) {
        return headers.contains(SESSION_ID_HEADER);
    }

    private void disconnect(ServerRequest request, ServerResponse response) {
        disconnectSession(request, response);
        if (isStreamableHttp(request.headers())) {
            response.status(Status.ACCEPTED_202).send();
        }
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
        if (isStreamableHttp(request.headers())) {
            Optional<McpSession> session = findSession(request);
            if (session.isEmpty()) {
                response.status(Status.NOT_FOUND_404).send();
                return;
            }
            // streamable HTTP and active session
            response.status(Status.METHOD_NOT_ALLOWED_405).send();
        } else {
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
    }

    /**
     * Finds session by either looking for header (streamable HTTP) or query
     * param (SSE).
     *
     * @param req the request
     * @return the optional session
     */
    private Optional<McpSession> findSession(HttpRequest req) {
        try {
            String sessionId;
            if (req.headers().contains(SESSION_ID_HEADER)) {
                sessionId = req.headers().get(SESSION_ID_HEADER).values();
            } else {
                sessionId = req.query().get("sessionId");
            }
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
        if (isResponse(object)) {
            Optional<McpSession> session = findSession(req);
            if (session.isPresent()) {
                session.get().send(object);
                return Optional.empty();
            }
        }
        return Optional.of(JsonRpcError.create(INVALID_REQUEST, "Invalid request"));
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

        // is this streamable HTTP?
        if (foundSession.isEmpty()) {
            // create a new session
            String sessionId = UUID.randomUUID().toString();
            McpSession session = new McpSession();
            sessions.put(sessionId, session);

            // parse capabilities and update response
            McpParameters params = new McpParameters(req.params(), req.params().asJsonObject());
            parseClientCapabilities(session, params);
            String version = parseClientVersion(params);
            res.header(SESSION_ID_HEADER, sessionId);
            res.result(toJson(version, capabilities, config));
            LOGGER.log(Level.FINEST, () -> String.format("Streamable HTTP: %s", res.asJsonObject()));
            res.send();
        } else {
            McpSession session = foundSession.get();
            McpParameters params = new McpParameters(req.params(), req.params().asJsonObject());
            String version = parseClientVersion(params);
            parseClientCapabilities(session, params);
            if (session.state() == UNINITIALIZED) {
                session.state(INITIALIZING);
            }
            res.result(toJson(version, capabilities, config));
            LOGGER.log(Level.FINEST, () -> String.format("SSE: %s", res.asJsonObject()));
            session.send(res);
        }
    }

    private String parseClientVersion(McpParameters parameters) {
        McpParameters protocolVersion = parameters.get("protocolVersion");
        if (protocolVersion.isPresent()) {
            String version = protocolVersion.asString().get();
            if (PROTOCOL_VERSION.contains(version)) {
                return version;
            }
        }
        return PROTOCOL_VERSION.getLast();
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
        processSimpleCall(req, res, session -> res.result(JsonValue.EMPTY_JSON_OBJECT));
    }

    private void toolsListRpc(JsonRpcRequest req, JsonRpcResponse res) {
        Optional<McpSession> foundSession = findSession(req);
        if (foundSession.isEmpty()) {
            res.status(Status.NOT_FOUND_404).send();
            return;
        }
        McpSession session = foundSession.get();
        McpPage<McpTool> page = page(tools, req.params());
        if (page == null) {
            res.error(INVALID_PARAMS, "Wrong cursor provided.");
            sendResponse(req, res, session);
            return;
        }
        res.result(listTools(page));
        sendResponse(req, res, session);
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
        Optional<McpTool> tool = tools.content().stream()
                .filter(t -> name.equals(t.name()))
                .findFirst();

        if (tool.isEmpty()) {
            res.error(INVALID_PARAMS, "Tool with name %s is not available".formatted(name));
            sendResponse(req, res, session);
            return;
        }

        List<McpToolContent> contents = tool.get()
                .tool()
                .apply(McpRequest.builder()
                               .parameters(parameters.get("arguments"))
                               .features(session.features())
                               .build());
        session.features().progress().stopSending();
        res.result(toolCall(contents));
        sendResponse(req, res, session);
    }

    private void resourcesListRpc(JsonRpcRequest req, JsonRpcResponse res) {
        Optional<McpSession> foundSession = findSession(req);
        if (foundSession.isEmpty()) {
            res.status(Status.NOT_FOUND_404).send();
            return;
        }
        McpSession session = foundSession.get();
        McpPage<McpResource> page = page(resources, req.params());
        if (page == null) {
            res.error(INVALID_PARAMS, "Wrong cursor provided.");
            sendResponse(req, res, session);
            return;
        }
        var resourceList = listResources(page);
        res.result(resourceList);
        sendResponse(req, res, session);
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
        Optional<McpResource> resource = resources.content().stream()
                .filter(r -> resourceUri.equals(r.uri()))
                .findFirst();

        // Fall back on resource template processing if resource is not found
        if (resource.isEmpty()) {
            var templates = resourceTemplates.content().stream()
                    .filter(template -> template.matches(resourceUri))
                    .findFirst();

            if (templates.isEmpty()) {
                res.error(JsonRpcError.INVALID_REQUEST, "Resource does not exist");
                sendResponse(req, res, session);
                return;
            }

            McpResourceTemplate template = templates.get();
            parameters = template.parameters(req.params(), resourceUri);
            resource = templates.map(Function.identity());
        }

        enableProgress(session, parameters);
        List<McpResourceContent> contents = resource.get()
                .resource()
                .apply(McpRequest.builder()
                               .parameters(parameters)
                               .features(session.features())
                               .build());
        session.features().progress().stopSending();
        res.result(readResource(resourceUri, contents));
        sendResponse(req, res, session);
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
        McpPage<McpResourceTemplate> page = page(resourceTemplates, req.params());
        if (page == null) {
            res.error(INVALID_PARAMS, "Wrong cursor provided.");
            sendResponse(req, res, session);
            return;
        }
        res.result(listResourceTemplates(page));
        sendResponse(req, res, session);
    }

    private void promptsListRpc(JsonRpcRequest req, JsonRpcResponse res) {
        Optional<McpSession> foundSession = findSession(req);
        if (foundSession.isEmpty()) {
            res.status(Status.NOT_FOUND_404).send();
            return;
        }
        McpSession session = foundSession.get();
        McpPage<McpPrompt> page = page(prompts, req.params());
        if (page == null) {
            res.error(INVALID_PARAMS, "Wrong cursor provided.");
            sendResponse(req, res, session);
            return;
        }
        res.result(listPrompts(page));
        sendResponse(req, res, session);
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
            res.error(INVALID_REQUEST, "Prompt name is missing from request " + req.id());
            sendResponse(req, res, session);
            return;
        }

        Optional<McpPrompt> prompt = prompts.content().stream()
                .filter(p -> name.equals(p.name()))
                .findFirst();
        if (prompt.isEmpty()) {
            session.features().progress().stopSending();
            res.error(INVALID_PARAMS, "Wrong prompt name: " + name);
            sendResponse(req, res, session);
            return;
        }

        List<McpPromptContent> contents = prompt.get()
                .prompt()
                .apply(McpRequest.builder()
                               .parameters(parameters.get("arguments"))
                               .features(session.features())
                               .build());
        session.features().progress().stopSending();
        res.result(toJson(contents, prompt.get().description()));
        sendResponse(req, res, session);
    }

    private void loggingLogLevelRpc(JsonRpcRequest req, JsonRpcResponse res) {
        processSimpleCall(req, res, session -> {
            McpParameters parameters = new McpParameters(req.params(), req.params().asJsonObject());
            parameters.get("level").asString().ifPresent(level -> {
                McpLogger.Level logLevel = McpLogger.Level.valueOf(level.toUpperCase());
                session.features().logger().setLevel(logLevel);
            });
            res.result(empty());
        });
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
            res.error(INVALID_REQUEST, "Completion name is missing from request");
            sendResponse(req, res, session);
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
        res.result(toJson(result));
        sendResponse(req, res, session);
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

    private <T> McpPage<T> page(McpPagination<T> pagination, JsonRpcParams params) {
        Optional<JsonValue> cursor = params.find("cursor");
        if (cursor.isPresent()) {
            String cursorString = cursor.map(JsonString.class::cast)
                    .get()
                    .getString();
            return pagination.page(cursorString);
        }
        return pagination.firstPage();
    }

    /**
     * Process call by checking that session exists, executing logic by calling consumer
     * and returning a result according to the protocol in use.
     *
     * @param req the request
     * @param res the response
     * @param consumer a consumer implementing logic
     */
    private void processSimpleCall(JsonRpcRequest req, JsonRpcResponse res, Consumer<McpSession> consumer) {
        Optional<McpSession> foundSession = findSession(req);
        if (foundSession.isEmpty()) {
            res.status(Status.NOT_FOUND_404).send();
            return;
        }
        McpSession session = foundSession.get();
        consumer.accept(session);
        sendResponse(req, res, session);
    }

    /**
     * Sends response according to the protocol in use.
     *
     * @param req the request
     * @param res the response
     * @param session the active session
     */
    private void sendResponse(JsonRpcRequest req, JsonRpcResponse res, McpSession session) {
        if (isStreamableHttp(req.headers())) {
            LOGGER.log(Level.FINEST,
                       () -> String.format("Streamable HTTP: %s", res.asJsonObject()));
            res.send();
        } else {
            LOGGER.log(Level.FINEST,
                       () -> String.format("SSE: %s", res.asJsonObject()));
            session.send(res);
        }
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
