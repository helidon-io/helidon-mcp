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

import java.lang.System.Logger.Level;
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

import io.helidon.builder.api.RuntimeType;
import io.helidon.common.mapper.OptionalValue;
import io.helidon.config.Config;
import io.helidon.cors.CrossOriginConfig;
import io.helidon.http.HeaderNames;
import io.helidon.http.HeaderValues;
import io.helidon.http.ServerRequestHeaders;
import io.helidon.http.Status;
import io.helidon.jsonrpc.core.JsonRpcError;
import io.helidon.service.registry.Services;
import io.helidon.webserver.cors.CorsSupport;
import io.helidon.webserver.http.HttpFeature;
import io.helidon.webserver.http.HttpRequest;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import io.helidon.webserver.jsonrpc.JsonRpcHandlers;
import io.helidon.webserver.jsonrpc.JsonRpcRequest;
import io.helidon.webserver.jsonrpc.JsonRpcResponse;
import io.helidon.webserver.jsonrpc.JsonRpcRouting;

import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

import static io.helidon.extensions.mcp.server.McpJsonSerializer.METHOD_COMPLETION_COMPLETE;
import static io.helidon.extensions.mcp.server.McpJsonSerializer.METHOD_INITIALIZE;
import static io.helidon.extensions.mcp.server.McpJsonSerializer.METHOD_LOGGING_SET_LEVEL;
import static io.helidon.extensions.mcp.server.McpJsonSerializer.METHOD_NOTIFICATION_CANCELED;
import static io.helidon.extensions.mcp.server.McpJsonSerializer.METHOD_NOTIFICATION_INITIALIZED;
import static io.helidon.extensions.mcp.server.McpJsonSerializer.METHOD_NOTIFICATION_ROOTS_LIST_CHANGED;
import static io.helidon.extensions.mcp.server.McpJsonSerializer.METHOD_PING;
import static io.helidon.extensions.mcp.server.McpJsonSerializer.METHOD_PROMPT_GET;
import static io.helidon.extensions.mcp.server.McpJsonSerializer.METHOD_PROMPT_LIST;
import static io.helidon.extensions.mcp.server.McpJsonSerializer.METHOD_RESOURCES_LIST;
import static io.helidon.extensions.mcp.server.McpJsonSerializer.METHOD_RESOURCES_READ;
import static io.helidon.extensions.mcp.server.McpJsonSerializer.METHOD_RESOURCES_SUBSCRIBE;
import static io.helidon.extensions.mcp.server.McpJsonSerializer.METHOD_RESOURCES_TEMPLATES_LIST;
import static io.helidon.extensions.mcp.server.McpJsonSerializer.METHOD_RESOURCES_UNSUBSCRIBE;
import static io.helidon.extensions.mcp.server.McpJsonSerializer.METHOD_SESSION_DISCONNECT;
import static io.helidon.extensions.mcp.server.McpJsonSerializer.METHOD_TOOLS_CALL;
import static io.helidon.extensions.mcp.server.McpJsonSerializer.METHOD_TOOLS_LIST;
import static io.helidon.extensions.mcp.server.McpJsonSerializer.isResponse;
import static io.helidon.extensions.mcp.server.McpJsonSerializer.prettyPrint;
import static io.helidon.extensions.mcp.server.McpSession.State.INITIALIZING;
import static io.helidon.extensions.mcp.server.McpStreamableHttpTransport.SESSION_ID_HEADER;
import static io.helidon.jsonrpc.core.JsonRpcError.INTERNAL_ERROR;
import static io.helidon.jsonrpc.core.JsonRpcError.INVALID_PARAMS;
import static io.helidon.jsonrpc.core.JsonRpcError.INVALID_REQUEST;

/**
 * Actual MCP server implemented as a Helidon {@link io.helidon.webserver.http.HttpFeature}.
 */
@RuntimeType.PrototypedBy(McpServerConfig.class)
public final class McpServerFeature implements HttpFeature, RuntimeType.Api<McpServerConfig> {
    private static final int SESSION_CACHE_SIZE = 1000;
    private static final int RESOURCE_NOT_FOUND_CODE = -32002;
    private static final List<String> PROTOCOL_VERSIONS = List.of("2024-11-05", "2025-03-26");
    private static final String DEFAULT_OIDC_METADATA_URI = "/.well-known/openid-configuration";
    private static final System.Logger LOGGER = System.getLogger(McpServerFeature.class.getName());

    private final String endpoint;
    private final McpServerConfig config;
    private final JsonRpcHandlers jsonRpcHandlers;
    private final McpPagination<McpTool> tools;
    private final McpPagination<McpPrompt> prompts;
    private final McpPagination<McpResource> resources;
    private final McpPagination<McpResourceTemplate> resourceTemplates;
    private final Set<McpCapability> capabilities = new HashSet<>();
    private final McpSessions sessions = new McpSessions(SESSION_CACHE_SIZE);
    private final Map<String, McpCompletion> promptCompletions = new ConcurrentHashMap<>();
    private final Map<String, McpCompletion> resourceCompletions = new ConcurrentHashMap<>();

    private McpServerFeature(McpServerConfig config) {
        List<McpTool> tools = new CopyOnWriteArrayList<>(config.tools());
        List<McpPrompt> prompts = new CopyOnWriteArrayList<>(config.prompts());
        List<McpResource> resources = new CopyOnWriteArrayList<>();
        List<McpResourceTemplate> templates = new CopyOnWriteArrayList<>();
        JsonRpcHandlers.Builder builder = JsonRpcHandlers.builder();

        this.config = config;
        this.endpoint = removeTrailingSlash(config.path());
        for (McpResource resource : config.resources()) {
            if (isTemplate(resource)) {
                templates.add(new McpResourceTemplate(resource));
            } else {
                resources.add(resource);
            }
        }
        for (McpCompletion completion : config.completions()) {
            switch (completion.referenceType()) {
                case PROMPT -> promptCompletions.put(completion.reference(), completion);
                case RESOURCE -> resourceCompletions.put(completion.reference(), completion);
                default -> throw new IllegalStateException("Unknown reference type: " + completion.referenceType());
            }
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
        builder.method(METHOD_COMPLETION_COMPLETE, this::completionRpc);

        builder.method(METHOD_SESSION_DISCONNECT, this::disconnect);
        builder.method(METHOD_NOTIFICATION_CANCELED, this::notificationCancelRpc);
        builder.method(METHOD_NOTIFICATION_INITIALIZED, this::notificationInitRpc);
        builder.method(METHOD_NOTIFICATION_ROOTS_LIST_CHANGED, this::notificationRootsListRpc);

        builder.errorHandler(this::handleErrorRequest);
        builder.exception(McpInternalException.class, this::mcpInternalExceptionHandler);
        builder.exception(McpException.class, this::mcpExceptionHandler);
        builder.exception(Throwable.class, this::throwableExceptionHandler);

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
        var cors = CorsSupport.builder()
                .addCrossOrigin(CrossOriginConfig.create())
                .build();
        // add all the JSON-RPC routes first
        JsonRpcRouting jsonRpcRouting = JsonRpcRouting.builder()
                .register(endpoint + "/message", jsonRpcHandlers)
                .register(endpoint, jsonRpcHandlers)        // streamable HTTP
                .build();
        jsonRpcRouting.routing(routing);

        // additional HTTP routes for SSE and session disconnect
        routing.get(DEFAULT_OIDC_METADATA_URI, cors, this::mcpMetadata)
                .get(endpoint, this::sse)
                .delete(endpoint, this::disconnect);
    }

    private void mcpMetadata(ServerRequest request, ServerResponse response) {
        var config = Services.get(Config.class);
        var providers = config.get("security.providers").asList(Config.class);

        if (providers.isEmpty()) {
            response.status(Status.NOT_FOUND_404);
            response.send();
            if (LOGGER.isLoggable(Level.DEBUG)) {
                LOGGER.log(Level.DEBUG, "Security is not enabled, add OIDC security provider to the configuration");
            }
            return;
        }
        for (Config provider : providers.get()) {
            var identity = provider.get("oidc.identity-uri");
            if (identity.exists()) {
                String identityUri = identity.asString().map(this::removeTrailingSlash).orElse(null);
                response.header(HeaderNames.LOCATION, identityUri + DEFAULT_OIDC_METADATA_URI);
                response.status(Status.SEE_OTHER_303);
                response.send();
                return;
            }
        }
        if (LOGGER.isLoggable(Level.DEBUG)) {
            LOGGER.log(Level.DEBUG, "Cannot find \"oidc.identity-uri\" property");
        }
        response.status(Status.NOT_FOUND_404);
        response.send();
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
        foundSession.get().onDisconnect(response);
        response.send();
    }

    private void sse(ServerRequest request, ServerResponse response) {
        // check if using streamable http
        if (response.headers().contains(SESSION_ID_HEADER)) {
            Optional<McpSession> session = findSession(request);
            if (session.isEmpty()) {
                response.status(Status.NOT_FOUND_404).send();
                return;
            }
            // streamable HTTP and active session
            response.status(Status.METHOD_NOT_ALLOWED_405).send();
        } else {
            String sessionId = UUID.randomUUID().toString();
            McpTransportLifecycle ssePost = new McpSsePostTransport(endpoint, sessionId);
            McpSession session = new McpSession(sessions, ssePost, config, sessionId);
            sessions.put(sessionId, session);
            session.onConnect(response);
        }
    }

    private void initializeRpc(JsonRpcRequest req, JsonRpcResponse res) {
        JsonValue requestId = req.rpcId().orElseThrow(() -> new McpInternalException("request id is required"));
        Optional<McpSession> foundSession = findSession(requestId, req, res);
        McpSession session;

        // is this streamable HTTP?
        if (foundSession.isEmpty()) {
            // create a new session
            String sessionId = UUID.randomUUID().toString();
            McpTransportLifecycle streamableHttp = new McpStreamableHttpTransport(res, sessionId);
            session = new McpSession(sessions, streamableHttp, config, sessionId);
            sessions.put(sessionId, session);
            session.onRequest(requestId, req, res);
            session.onConnect(res);
        } else {
            session = foundSession.get();
        }
        McpParameters params = new McpParameters(req.params(), req.params().asJsonObject());
        McpProtocolVersion protocolVersion = params.get("protocolVersion")
                .asString()
                .map(McpProtocolVersion::find)
                .orElseGet(McpProtocolVersion::lastest);
        session.protocolVersion(protocolVersion);

        var clientCapabilities = params.get("capabilities");
        clientCapabilities.get(McpCapability.SAMPLING.text())
                .ifPresent(it -> session.capability(McpCapability.SAMPLING));
        clientCapabilities.get(McpCapability.ROOTS.text())
                .get("listChanged")
                .asBoolean()
                .filter(Boolean::booleanValue)
                .ifPresent(it -> session.capability(McpCapability.ROOTS));
        session.state(INITIALIZING);
        var payload = session.serializer().toJson(capabilities, config);
        res.result(payload.build());
        session.send(requestId, res);
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
        Optional<McpSession> foundSession = findSession(req);
        if (foundSession.isEmpty()) {
            res.status(Status.NOT_FOUND_404).send();
            if (LOGGER.isLoggable(Level.TRACE)) {
                LOGGER.log(Level.TRACE, "No session found for cancellation request: %s".formatted(req.asJsonObject()));
            }
            return;
        }
        McpSession session = foundSession.get();
        Optional<JsonValue> reason = req.params().find("reason");
        Optional<JsonValue> requestId = req.params().find("requestId");
        // Ignore malformed request
        if (requestId.isEmpty()
                || reason.isEmpty()
                || !JsonValue.ValueType.STRING.equals(reason.get().getValueType())) {
            if (LOGGER.isLoggable(Level.TRACE)) {
                LOGGER.log(Level.TRACE, "Malformed cancellation request: %s".formatted(req.asJsonObject()));
            }
            return;
        }
        String cancelReason = ((JsonString) reason.get()).getString();
        session.findFeatures(requestId.get())
                .map(McpFeatures::cancellation)
                .ifPresent(cancellation -> cancellation.cancel(cancelReason, requestId.get()));
    }

    private void notificationRootsListRpc(JsonRpcRequest req, JsonRpcResponse res) {
        Optional<McpSession> session = findSession(req);
        if (session.isEmpty()) {
            if (LOGGER.isLoggable(Level.TRACE)) {
                LOGGER.log(Level.TRACE, "No session found for roots update: %s".formatted(req.asJsonObject()));
            }
            return;
        }
        session.get()
                .context()
                .register(McpRoots.McpRootClassifier.class, true);
    }

    private void pingRpc(JsonRpcRequest req, JsonRpcResponse res) {
        JsonValue requestId = req.rpcId().orElseThrow(() -> new McpInternalException("request id is required"));
        Optional<McpSession> foundSession = findSession(requestId, req, res);
        if (foundSession.isEmpty()) {
            res.status(Status.NOT_FOUND_404).send();
            return;
        }
        McpSession session = foundSession.get();
        res.result(JsonValue.EMPTY_JSON_OBJECT);
        session.send(requestId, res);
    }

    private void toolsListRpc(JsonRpcRequest req, JsonRpcResponse res) {
        JsonValue requestId = req.rpcId().orElseThrow(() -> new McpInternalException("request id is required"));
        Optional<McpSession> foundSession = findSession(requestId, req, res);
        if (foundSession.isEmpty()) {
            res.status(Status.NOT_FOUND_404).send();
            return;
        }
        McpSession session = foundSession.get();
        McpPage<McpTool> page = tools.page(req.params());
        res.result(session.serializer().listTools(page));
        session.send(requestId, res);
    }

    private void toolsCallRpc(JsonRpcRequest req, JsonRpcResponse res) {
        JsonValue requestId = req.rpcId().orElseThrow(() -> new McpInternalException("request id is required"));
        Optional<McpSession> foundSession = findSession(requestId, req, res);
        if (foundSession.isEmpty()) {
            res.status(Status.NOT_FOUND_404).send();
            return;
        }
        boolean error = false;
        List<McpToolContent> contents;
        McpSession session = foundSession.get();
        McpParameters parameters = new McpParameters(req.params(), req.params().asJsonObject());

        String name = parameters.get("name").asString().orElse("");
        Optional<McpTool> tool = tools.content().stream()
                .filter(t -> name.equals(t.name()))
                .findFirst();

        if (tool.isEmpty()) {
            res.error(INVALID_PARAMS, "Tool with name %s is not available".formatted(name));
            session.send(requestId, res);
            return;
        }

        McpFeatures features = session.createFeatures(requestId, req, res);
        session.beforeFeatureRequest(parameters, requestId);
        try {
            contents = tool.get()
                    .tool()
                    .apply(McpRequest.builder()
                                   .parameters(parameters.get("arguments"))
                                   .features(features)
                                   .protocolVersion(session.protocolVersion().text())
                                   .sessionContext(session.context())
                                   .requestContext(req.context())
                                   .build());
        } catch (McpToolErrorException e) {
            error = true;
            contents = e.contents();
        }
        session.afterFeatureRequest(parameters, requestId);

        var toolCall = session.serializer().toolCall(error, contents);
        res.result(toolCall);
        session.send(requestId, res);
    }

    private void resourcesListRpc(JsonRpcRequest req, JsonRpcResponse res) {
        JsonValue requestId = req.rpcId().orElseThrow(() -> new McpInternalException("request id is required"));
        Optional<McpSession> foundSession = findSession(requestId, req, res);
        if (foundSession.isEmpty()) {
            res.status(Status.NOT_FOUND_404).send();
            return;
        }
        McpSession session = foundSession.get();
        var resourceList = session.serializer().listResources(resources.page(req.params()));
        res.result(resourceList);
        session.send(requestId, res);
    }

    private void resourcesReadRpc(JsonRpcRequest req, JsonRpcResponse res) {
        JsonValue requestId = req.rpcId().orElseThrow(() -> new McpInternalException("request id is required"));
        Optional<McpSession> foundSession = findSession(requestId, req, res);
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
                res.error(RESOURCE_NOT_FOUND_CODE, "Resource not found");
                session.send(requestId, res);
                return;
            }

            McpResourceTemplate template = templates.get();
            parameters = template.parameters(req.params(), resourceUri);
            resource = templates.map(Function.identity());
        }

        McpFeatures features = session.createFeatures(requestId, req, res);
        session.beforeFeatureRequest(parameters, requestId);
        List<McpResourceContent> contents = resource.get()
                .resource()
                .apply(McpRequest.builder()
                               .parameters(parameters)
                               .features(features)
                               .protocolVersion(session.protocolVersion().text())
                               .sessionContext(session.context())
                               .requestContext(req.context())
                               .build());
        session.afterFeatureRequest(parameters, requestId);
        var readResource = session.serializer().readResource(resourceUri, contents);
        res.result(readResource);
        session.send(requestId, res);
    }

    private void resourceSubscribeRpc(JsonRpcRequest req, JsonRpcResponse res) {
        JsonValue requestId = req.rpcId().orElseThrow(() -> new McpInternalException("request id is required"));
        Optional<McpSession> foundSession = findSession(requestId, req, res);
        if (foundSession.isEmpty()) {
            res.status(Status.NOT_FOUND_404).send();
            return;
        }
        McpSession session = foundSession.get();
        McpParameters parameters = new McpParameters(req.params(), req.params().asJsonObject());
        String resourceUri = parameters.get("uri").asString()
                .orElseThrow(() -> new McpInternalException("uri is required"));

        // check if resource exists
        Optional<McpResource> resource = config.resources().stream()
                .filter(r -> resourceUri.equals(r.uri()))
                .findFirst();
        if (resource.isEmpty()) {
            res.error(INVALID_PARAMS, "Unable to find resource");
            session.send(requestId, res);
            return;
        }

        // update session with new subscription
        session.features()
                .subscriptions()
                .subscribe(requestId, resourceUri);

        // if subscriber exists then call it
        Optional<McpResourceSubscriber> subscriber = config.resourceSubscribers().stream()
                .filter(r -> resourceUri.equals(r.uri()))
                .findFirst();
        if (subscriber.isPresent()) {
            McpFeatures features = session.createFeatures(requestId, req, res);
            session.beforeFeatureRequest(parameters, requestId);
            subscriber.get()
                    .subscribe()
                    .accept(McpRequest.builder()
                                    .parameters(parameters)
                                    .features(features)
                                    .protocolVersion(session.protocolVersion().text())
                                    .sessionContext(session.context())
                                    .requestContext(req.context())
                                    .build());
            session.afterFeatureRequest(parameters, requestId);
            // send final response using active SSE sink
            res.result(JsonValue.EMPTY_JSON_OBJECT);
            session.send(requestId, res);
            return;
        }

        session.features()
                .subscriptions()
                .blockSubscribe(resourceUri);
        // send final response after unblocking
        res.result(JsonValue.EMPTY_JSON_OBJECT);
        session.send(requestId, res);
    }

    private void resourceUnsubscribeRpc(JsonRpcRequest req, JsonRpcResponse res) {
        JsonValue requestId = req.rpcId().orElseThrow(() -> new McpInternalException("request id is required"));
        Optional<McpSession> foundSession = findSession(requestId, req, res);
        if (foundSession.isEmpty()) {
            res.status(Status.NOT_FOUND_404).send();
            return;
        }
        McpSession session = foundSession.get();
        McpParameters parameters = new McpParameters(req.params(), req.params().asJsonObject());
        String resourceUri = parameters.get("uri").asString()
                .orElseThrow(() -> new McpInternalException("uri is required"));

        // if unsubscriber exists then call it
        Optional<McpResourceUnsubscriber> unsubscriber = config.resourceUnsubscribers().stream()
                .filter(r -> resourceUri.equals(r.uri()))
                .findFirst();
        // invoke user method to unsubscribe
        if (unsubscriber.isPresent()) {
            McpFeatures features = session.createFeatures(requestId, req, res);
            session.beforeFeatureRequest(parameters, requestId);
            unsubscriber.get()
                    .unsubscribe()
                    .accept(McpRequest.builder()
                                    .parameters(parameters)
                                    .features(features)
                                    .protocolVersion(session.protocolVersion().text())
                                    .sessionContext(session.context())
                                    .requestContext(req.context())
                                    .build());
            session.afterFeatureRequest(parameters, requestId);
        }

        session.features()
                .subscriptions()
                .unsubscribe(resourceUri);
        res.result(JsonValue.EMPTY_JSON_OBJECT);
        session.send(requestId, res);
    }

    private void resourceTemplateListRpc(JsonRpcRequest req, JsonRpcResponse res) {
        JsonValue requestId = req.rpcId().orElseThrow(() -> new McpInternalException("request id is required"));
        Optional<McpSession> foundSession = findSession(requestId, req, res);
        if (foundSession.isEmpty()) {
            res.status(Status.NOT_FOUND_404).send();
            return;
        }
        McpSession session = foundSession.get();
        var page = resourceTemplates.page(req.params());
        var payload = session.serializer().listResourceTemplates(page);
        res.result(payload);
        session.send(requestId, res);
    }

    private void promptsListRpc(JsonRpcRequest req, JsonRpcResponse res) {
        JsonValue requestId = req.rpcId().orElseThrow(() -> new McpInternalException("request id is required"));
        Optional<McpSession> foundSession = findSession(requestId, req, res);
        if (foundSession.isEmpty()) {
            res.status(Status.NOT_FOUND_404).send();
            return;
        }
        McpSession session = foundSession.get();
        var page = prompts.page(req.params());
        var payload = session.serializer().listPrompts(page);
        res.result(payload);
        session.send(requestId, res);
    }

    private void promptsGetRpc(JsonRpcRequest req, JsonRpcResponse res) {
        JsonValue requestId = req.rpcId().orElseThrow(() -> new McpInternalException("request id is required"));
        Optional<McpSession> foundSession = findSession(requestId, req, res);
        if (foundSession.isEmpty()) {
            res.status(Status.NOT_FOUND_404).send();
            return;
        }
        McpSession session = foundSession.get();
        McpParameters parameters = new McpParameters(req.params(), req.params().asJsonObject());

        String name = parameters.get("name").asString().orElse(null);
        if (name == null) {
            res.error(INVALID_REQUEST, "Prompt name is missing from request " + req.id());
            session.send(requestId, res);
            return;
        }

        Optional<McpPrompt> prompt = prompts.content().stream()
                .filter(p -> name.equals(p.name()))
                .findFirst();
        if (prompt.isEmpty()) {
            res.error(INVALID_PARAMS, "Wrong prompt name: " + name);
            session.send(requestId, res);
            return;
        }

        McpFeatures features = session.createFeatures(requestId, req, res);
        session.beforeFeatureRequest(parameters, requestId);
        List<McpPromptContent> contents = prompt.get()
                .prompt()
                .apply(McpRequest.builder()
                               .parameters(parameters.get("arguments"))
                               .features(features)
                               .protocolVersion(session.protocolVersion().text())
                               .sessionContext(session.context())
                               .requestContext(req.context())
                               .build());
        session.afterFeatureRequest(parameters, requestId);
        var payload = session.serializer().toJson(contents, prompt.get().description());
        res.result(payload);
        session.send(requestId, res);
    }

    private void loggingLogLevelRpc(JsonRpcRequest req, JsonRpcResponse res) {
        JsonValue requestId = req.rpcId().orElseThrow(() -> new McpInternalException("request id is required"));
        Optional<McpSession> foundSession = findSession(requestId, req, res);
        if (foundSession.isEmpty()) {
            res.status(Status.NOT_FOUND_404).send();
            return;
        }

        McpSession session = foundSession.get();
        McpParameters parameters = new McpParameters(req.params(), req.params().asJsonObject());
        OptionalValue<String> level = parameters.get("level").asString();
        if (level.isPresent()) {
            try {
                McpLogger.Level logLevel = McpLogger.Level.valueOf(level.get().toUpperCase());
                session.createFeatures(requestId, req, res).logger().setLevel(logLevel);
                res.result(JsonValue.EMPTY_JSON_OBJECT);
                session.send(requestId, res);
                return;
            } catch (IllegalArgumentException e) {
                if (LOGGER.isLoggable(Level.TRACE)) {
                    LOGGER.log(Level.TRACE, "Invalid log level provided", e);
                }
                // falls through
            }
        }
        res.error(INVALID_PARAMS, "Invalid log level");
        session.send(requestId, res);
    }

    private void completionRpc(JsonRpcRequest req, JsonRpcResponse res) {
        JsonValue requestId = req.rpcId().orElseThrow(() -> new McpInternalException("request id is required"));
        Optional<McpSession> foundSession = findSession(requestId, req, res);
        if (foundSession.isEmpty()) {
            res.status(Status.NOT_FOUND_404).send();
            return;
        }
        McpSession session = foundSession.get();
        McpParameters parameters = new McpParameters(req.params(), req.params().asJsonObject());
        McpParameters ref = parameters.get("ref");
        String referenceType = ref.get("type").asString().orElse(null);
        if (referenceType != null) {
            McpCompletionType type = McpCompletionType.fromString(referenceType);
            McpCompletion completion = switch (type) {
                case PROMPT -> ref.get("name")
                        .asString()
                        .map(promptCompletions::get)
                        .orElseThrow(() -> new McpInternalException(INVALID_PARAMS, "No prompt completion found"));
                case RESOURCE -> ref.get("uri")
                        .asString()
                        .map(resourceCompletions::get)
                        .orElseThrow(() -> new McpInternalException(INVALID_PARAMS, "No resource completion found"));
            };
            McpFeatures features = session.createFeatures(requestId, req, res);
            session.beforeFeatureRequest(parameters, requestId);
            McpCompletionContent result = completion.completion()
                    .apply(McpRequest.builder()
                                   .parameters(parameters.get("argument"))
                                   .features(features)
                                   .protocolVersion(session.protocolVersion().text())
                                   .sessionContext(session.context())
                                   .requestContext(req.context())
                                   .build());
            session.afterFeatureRequest(parameters, requestId);
            var payload = session.serializer().toJson(result);
            res.result(payload);
            session.send(requestId, res);
            return;
        }

        // unable to process completion request
        res.error(INVALID_PARAMS, "Invalid completion request");
        session.send(requestId, res);
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
                if (LOGGER.isLoggable(Level.DEBUG)) {
                    LOGGER.log(Level.DEBUG, "Client response:\n" + prettyPrint(object));
                }
                session.get().acceptResponse(object);
                return Optional.empty();
            }
        }
        if (LOGGER.isLoggable(Level.TRACE)) {
            LOGGER.log(Level.TRACE, "Wrong format message received:\n" + prettyPrint(object));
        }
        return Optional.of(JsonRpcError.create(INVALID_REQUEST, "Invalid request"));
    }

    private Optional<JsonRpcError> throwableExceptionHandler(JsonRpcRequest request,
                                                             JsonRpcResponse response,
                                                             Throwable throwable) {
        return sendError(request, response, throwable, INTERNAL_ERROR);
    }

    private Optional<JsonRpcError> mcpExceptionHandler(JsonRpcRequest request,
                                                       JsonRpcResponse response,
                                                       Throwable throwable) {
        if (throwable instanceof McpException exception) {
            return sendError(request, response, exception, exception.code());
        }
        return throwableExceptionHandler(request, response, throwable);
    }

    private Optional<JsonRpcError> mcpInternalExceptionHandler(JsonRpcRequest request,
                                                               JsonRpcResponse response,
                                                               Throwable throwable) {
        if (throwable instanceof McpInternalException exception) {
            return sendError(request, response, exception, exception.code());
        }
        return throwableExceptionHandler(request, response, throwable);
    }

    private Optional<JsonRpcError> sendError(JsonRpcRequest request,
                                             JsonRpcResponse response,
                                             Throwable throwable,
                                             int errorCode) {
        if (LOGGER.isLoggable(Level.DEBUG)) {
            LOGGER.log(Level.DEBUG, "Send error response because of: ", throwable);
        }

        // Look up session to send an error to the client
        JsonValue requestId = request.rpcId().orElse(JsonValue.NULL);
        var session = findSession(requestId, request, response);
        if (session.isEmpty()) {
            response.header(HeaderValues.CONTENT_TYPE_JSON);
            return Optional.of(JsonRpcError.create(INTERNAL_ERROR, "Session not found"));
        }
        response.error(errorCode, throwable.getMessage());

        // If streamable HTTP transport and did not switch to SSE
        // the handler manages the response
        var transport = session.get()
                .transport(requestId)
                .filter(it -> it instanceof McpStreamableHttpTransport)
                .map(McpStreamableHttpTransport.class::cast)
                .filter(it -> !it.openedSseChannel());
        if (transport.isPresent()) {
            if (LOGGER.isLoggable(Level.DEBUG)) {
                LOGGER.log(Level.DEBUG, "Streamable HTTP:\n" + prettyPrint(response.asJsonObject()));
            }
            session.get().clearRequest(requestId);
            response.header(HeaderValues.CONTENT_TYPE_JSON);
            return response.error();
        }
        session.get().send(requestId, response);
        return Optional.empty();
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
            String sessionId = req.headers().contains(SESSION_ID_HEADER)
                    ? req.headers().get(SESSION_ID_HEADER).values()
                    : req.query().get("sessionId");
            return sessions.get(sessionId);
        } catch (NoSuchElementException e) {
            if (LOGGER.isLoggable(Level.TRACE)) {
                LOGGER.log(Level.TRACE, "Session not found");
            }
            return Optional.empty();
        }
    }

    private Optional<McpSession> findSession(JsonValue id, JsonRpcRequest request, JsonRpcResponse response) {
        return findSession(request).map(session -> session.onRequest(id, request, response));
    }

    private String removeTrailingSlash(String path) {
        return path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }

    private boolean isTemplate(McpResource resource) {
        String uri = resource.uri();
        return uri.contains("{") || uri.contains("}");
    }

    /**
     * Checks if using streamable HTTP transport.
     *
     * @param headers the request headers
     * @return outcome of test
     */
    private boolean isStreamableHttp(ServerRequestHeaders headers) {
        return headers.contains(SESSION_ID_HEADER);
    }
}
