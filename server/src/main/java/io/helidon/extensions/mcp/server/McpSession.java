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
import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.helidon.common.LruCache;
import io.helidon.common.UncheckedException;
import io.helidon.common.context.Context;
import io.helidon.http.Status;
import io.helidon.webserver.jsonrpc.JsonRpcRequest;
import io.helidon.webserver.jsonrpc.JsonRpcResponse;
import io.helidon.webserver.sse.SseSink;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import static io.helidon.extensions.mcp.server.McpJsonRpc.timeoutResponse;
import static io.helidon.extensions.mcp.server.McpServerFeature.isStreamableHttp;
import static io.helidon.extensions.mcp.server.McpSession.State.UNINITIALIZED;

class McpSession {
    private static final System.Logger LOGGER = System.getLogger(McpSession.class.getName());

    private final McpSessions sessions;
    private final McpServerConfig config;
    private final Set<McpCapability> capabilities;
    private final Context context = Context.create();
    private final AtomicLong jsonRpcId = new AtomicLong(0);
    private final AtomicBoolean active = new AtomicBoolean(true);
    private final BlockingQueue<JsonObject> queue = new LinkedBlockingQueue<>();
    private final BlockingQueue<JsonObject> responses = new LinkedBlockingQueue<>();
    private final LruCache<JsonValue, McpFeatures> features = LruCache.create();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<String, McpSession> sseSubscriptions = new HashMap<>();
    private final Map<String, SseSink> streamableSubscriptions = new HashMap<>();
    private final Map<String, CountDownLatch> threadSubscriptions = new ConcurrentHashMap<>();

    private volatile String protocolVersion;
    private volatile State state = UNINITIALIZED;

    McpSession(McpSessions sessions, McpServerConfig config) {
        this(sessions, new HashSet<>(), config);
    }

    McpSession(McpSessions sessions, Set<McpCapability> capabilities, McpServerConfig config) {
        this.sessions = sessions;
        this.capabilities = capabilities;
        this.config = config;
    }

    McpSessions sessions() {
        return sessions;
    }

    void poll(Consumer<JsonObject> consumer) {
        while (active.get()) {
            try {
                JsonObject message = queue.take();
                if (message.getBoolean("disconnect", false)) {
                    log(Level.TRACE, () -> "Session disconnected.");
                    break;
                }
                consumer.accept(message);
            } catch (Exception e) {
                log(Level.TRACE, () -> "Session interrupted.");
            }
        }
    }

    JsonObject pollResponse(long requestId, Duration timeout) {
        while (active.get()) {
            try {
                JsonObject response = responses.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
                if (response != null) {
                    long id = response.getJsonNumber("id").longValue();
                    if (id == requestId) {
                        return response;
                    }
                } else {
                    return timeoutResponse(requestId);
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

    void send(JsonObject message) {
        try {
            queue.put(message);
        } catch (InterruptedException e) {
            throw new UncheckedException(e);
        }
    }

    void send(JsonRpcResponse response) {
        send(response.status(Status.ACCEPTED_202).asJsonObject());
    }

    void sendResponse(JsonObject response) {
        try {
            responses.put(response);
        } catch (InterruptedException e) {
            throw new UncheckedException(e);
        }
    }

    void disconnect() {
        if (active.compareAndSet(true, false)) {
            queue.add(McpJsonRpc.disconnectSession());
        }
    }

    McpFeatures createFeatures(JsonValue requestId) {
        McpFeatures feat = new McpFeatures(this);
        features.put(requestId, feat);
        return feat;
    }

    McpFeatures createFeatures(JsonRpcResponse res, JsonValue requestId) {
        McpFeatures feat = new McpFeatures(this, res);
        features.put(requestId, feat);
        return feat;
    }

    McpFeatures createFeatures(JsonRpcResponse res, JsonValue requestId, SseSink sseSink) {
        McpFeatures feat = new McpFeatures(this, res, sseSink);
        features.put(requestId, feat);
        return feat;
    }

    Optional<McpFeatures> features(JsonValue requestId) {
        return features.get(requestId);
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
    }

    void capabilities(McpCapability capability) {
        capabilities.add(capability);
    }

    Set<McpCapability> capabilities() {
        return capabilities;
    }

    State state() {
        return state;
    }

    Context context() {
        return context;
    }

    void state(State state) {
        this.state = state;
    }

    String protocolVersion() {
        return protocolVersion;
    }

    void protocolVersion(String protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    boolean hasSubscription(String uri) {
        return streamableSubscriptions.containsKey(uri) || sseSubscriptions.containsKey(uri);
    }

    Optional<SseSink> findSubscription(String uri) {
        lock.readLock().lock();
        try {
            if (streamableSubscriptions.containsKey(uri)) {
                return Optional.of(streamableSubscriptions.get(uri));
            }
            if (sseSubscriptions.containsKey(uri)) {
                return Optional.empty();
            }
            throw new IllegalArgumentException("Subscription not found: " + uri);
        } finally {
            lock.readLock().unlock();
        }
    }

    Optional<SseSink> subscribe(JsonRpcRequest req, JsonRpcResponse res, String uri) {
        if (!active.get()) {
            return Optional.empty();
        }

        lock.writeLock().lock();
        try {
            SseSink sseSink = null;
            if (isStreamableHttp(req.headers())) {
                SseSink existing = streamableSubscriptions.get(uri);
                if (existing != null) {
                    existing.close();       // close old one
                    log(Level.DEBUG, () -> "Removed existing subscription for " + uri);
                }
                sseSink = res.sink(SseSink.TYPE);
                streamableSubscriptions.put(uri, sseSink);
            } else {
                McpSession existing = sseSubscriptions.get(uri);
                if (existing != null) {
                    log(Level.DEBUG, () -> "Found existing subscription for " + uri);
                    return Optional.empty();
                }
                sseSubscriptions.put(uri, this);
            }
            log(Level.DEBUG, () -> "New subscription for " + uri);
            return Optional.ofNullable(sseSink);
        } finally {
            lock.writeLock().unlock();
        }
    }

    Optional<SseSink> unsubscribe(JsonRpcRequest req, String uri) {
        if (!active.get()) {
            return Optional.empty();
        }

        lock.writeLock().lock();
        try {
            SseSink sseSink = null;
            if (isStreamableHttp(req.headers())) {
                sseSink = streamableSubscriptions.remove(uri);
                if (sseSink == null) {
                    log(Level.DEBUG, () -> "No subscription found for " + uri);
                }
            } else {
                McpSession session = sseSubscriptions.remove(uri);
                if (session == null) {
                    log(Level.DEBUG, () -> "No subscription found for " + uri);
                }
            }
            log(Level.DEBUG, () -> "Removed subscription for " + uri);
            return Optional.ofNullable(sseSink);
        } finally {
            lock.writeLock().unlock();
        }
    }

    void blockSubscribe(String uri) throws InterruptedException {
        if (active.get()) {
            CountDownLatch latch = threadSubscriptions.computeIfAbsent(uri, k -> new CountDownLatch(1));
            Duration timeout = config.subscriptionTimeout();
            try {
                boolean completed = latch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
                if (!completed) {
                    log(Level.TRACE, () -> "Timed out waiting subscription for " + uri);
                }
            } catch (InterruptedException e) {
                log(Level.TRACE, () -> "Interrupted while waiting for subscription");
            }
        }
    }

    void unblockSubscribe(String uri) {
        if (active.get()) {
            CountDownLatch latch = threadSubscriptions.remove(uri);
            if (latch != null) {
                latch.countDown();
            }
        }
    }

    private void log(Level level, Supplier<String> message) {
        if (LOGGER.isLoggable(level)) {
            LOGGER.log(level, message);
        }
    }

    enum State {
        INITIALIZED,
        INITIALIZING,
        UNINITIALIZED
    }
}
