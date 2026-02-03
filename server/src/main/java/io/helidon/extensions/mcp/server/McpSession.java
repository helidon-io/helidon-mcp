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

import java.lang.System.Logger.Level;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import io.helidon.common.LazyValue;
import io.helidon.common.LruCache;
import io.helidon.common.UncheckedException;
import io.helidon.common.context.Context;
import io.helidon.webserver.http.ServerResponse;
import io.helidon.webserver.jsonrpc.JsonRpcRequest;
import io.helidon.webserver.jsonrpc.JsonRpcResponse;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import static io.helidon.extensions.mcp.server.McpJsonSerializer.prettyPrint;
import static io.helidon.extensions.mcp.server.McpSession.State.UNINITIALIZED;

class McpSession {
    private static final System.Logger LOGGER = System.getLogger(McpSession.class.getName());

    private final String id;
    private final McpSessions sessions;
    private final McpTransportManager manager;
    private final Context context = Context.create();
    private final Set<McpCapability> clientCapabilities;
    private final AtomicLong jsonRpcId = new AtomicLong(0);
    private final List<McpFeatureLifecycle> featureListeners;
    private final LazyValue<McpSessionFeatures> sessionFeatures;
    private final AtomicBoolean active = new AtomicBoolean(true);
    private final LruCache<JsonValue, McpFeatures> features = LruCache.create();
    private final LruCache<JsonValue, McpTransport> transports = LruCache.create();
    private final BlockingQueue<JsonObject> responses = new LinkedBlockingQueue<>();

    private McpJsonSerializer serializer;
    private volatile State state = UNINITIALIZED;
    private volatile McpProtocolVersion protocolVersion;

    McpSession(McpSessions sessions, McpTransportManager manager, McpServerConfig config, String id) {
        this.id = id;
        this.manager = manager;
        this.sessions = sessions;
        this.clientCapabilities = new HashSet<>();
        this.featureListeners = new CopyOnWriteArrayList<>();
        this.featureListeners.add(new McpProgress.McpProgressListener());
        this.sessionFeatures = LazyValue.create(() -> new McpSessionFeatures(this));
        context.register(McpServerConfigBlueprint.class, config);
    }

    void send(JsonValue id, JsonRpcResponse response) {
        transports.get(id)
                .orElseThrow(() -> new McpInternalException("No transport for id " + id))
                .send(response);
        transports.remove(id);
    }

    void onConnect(ServerResponse response) {
        context.register(McpRoots.McpRootClassifier.class, true);
        manager.onConnect(response);
    }

    void onDisconnect(ServerResponse response) {
        active.compareAndSet(true, false);
        sessions.remove(id);
        manager.onDisconnect(response);
    }

    McpSession onRequest(JsonValue id, JsonRpcRequest req, JsonRpcResponse res) {
        if (transports.get(id).isEmpty()) {
            McpTransport transport = this.manager.create(req, res);
            transports.put(id, transport);
        }
        manager.onRequest(req, res);
        return this;
    }

    McpSession onNotification(JsonRpcRequest req, JsonRpcResponse res) {
        manager.onNotification(req, res);
        return this;
    }

    void beforeFeatureRequest(McpParameters parameters, JsonValue requestId) {
        features.get(requestId).ifPresent(feature -> {
            for (McpFeatureLifecycle listener : featureListeners) {
                listener.beforeRequest(parameters, feature);
            }
        });
    }

    void afterFeatureRequest(McpParameters parameters, JsonValue requestId) {
        features.get(requestId).ifPresent(feature -> {
            for (McpFeatureLifecycle listener : featureListeners) {
                listener.beforeRequest(parameters, feature);
            }
        });
    }

    void acceptResponse(JsonObject response) {
        try {
            responses.put(response);
        } catch (InterruptedException e) {
            throw new UncheckedException(e);
        }
    }

    McpFeatures createFeatures(JsonValue requestId, JsonRpcRequest request, JsonRpcResponse response) {
        var transport = transports.get(requestId)
                .orElseThrow(() -> new McpInternalException("No transport for request id " + requestId));
        McpFeatures feat = new McpFeatures(this, transport);
        features.put(requestId, feat);
        return feat;
    }

    JsonObject pollResponse(long requestId, Duration timeout) {
        while (active.get()) {
            try {
                JsonObject response = responses.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
                if (response != null) {
                    if (LOGGER.isLoggable(System.Logger.Level.DEBUG)) {
                        LOGGER.log(System.Logger.Level.DEBUG, "Response:\n" + prettyPrint(response));
                    }
                    long id = response.getJsonNumber("id").longValue();
                    if (id == requestId) {
                        return response;
                    }
                } else {
                    return serializer.timeoutResponse(requestId);
                }
            } catch (ClassCastException e) {
                if (LOGGER.isLoggable(Level.TRACE)) {
                    LOGGER.log(Level.TRACE, "Received a response with wrong request id type", e);
                }
            } catch (InterruptedException e) {
                throw new McpInternalException("Session interrupted.", e);
            }
        }
        throw new McpInternalException("Session disconnected");
    }

    /**
     * Generates a unique JSON-RPC {@code id} for an outbound request to the client.
     * The returned identifier is guaranteed to be unused by any prior request in this session.
     *
     * @return a new request id
     */
    long jsonRpcId() {
        return jsonRpcId.getAndIncrement();
    }

    void clearRequest(JsonValue requestId) {
        features.remove(requestId);
        transports.remove(requestId);
    }

    Optional<McpFeatures> findFeatures(JsonValue requestId) {
        return features.get(requestId);
    }

    McpSessionFeatures features() {
        return sessionFeatures.get();
    }

    McpSessions sessions() {
        return sessions;
    }

    void capability(McpCapability capability) {
        clientCapabilities.add(capability);
    }

    Set<McpCapability> capability() {
        return clientCapabilities;
    }

    Context context() {
        return context;
    }

    void protocolVersion(McpProtocolVersion protocolVersion) {
        this.protocolVersion = protocolVersion;
        this.serializer = McpJsonSerializer.create(protocolVersion);
    }

    McpProtocolVersion protocolVersion() {
        return protocolVersion;
    }

    McpJsonSerializer serializer() {
        return serializer;
    }

    Optional<McpTransport> transport(JsonValue id) {
        return transports.get(id);
    }

    void state(State state) {
        this.state = state;
    }

    State state() {
        return state;
    }

    enum State {
        INITIALIZED,
        INITIALIZING,
        UNINITIALIZED
    }
}
