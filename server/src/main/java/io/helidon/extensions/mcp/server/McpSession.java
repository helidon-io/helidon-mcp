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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.helidon.common.LruCache;
import io.helidon.common.UncheckedException;
import io.helidon.common.context.Context;
import io.helidon.http.Status;
import io.helidon.webserver.jsonrpc.JsonRpcRequest;
import io.helidon.webserver.jsonrpc.JsonRpcResponse;
import io.helidon.webserver.sse.SseSink;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import static io.helidon.extensions.mcp.server.McpServerFeature.isStreamableHttp;
import static io.helidon.extensions.mcp.server.McpSession.State.UNINITIALIZED;

class McpSession {
    private static final Logger LOGGER = Logger.getLogger(McpSession.class.getName());

    private final Set<McpCapability> capabilities;
    private final Context context = Context.create();
    private final AtomicBoolean active = new AtomicBoolean(true);
    private final BlockingQueue<JsonObject> queue = new LinkedBlockingQueue<>();
    private final LruCache<JsonValue, McpFeatures> features = LruCache.create();

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<String, McpSession> sseSubscriptions = new HashMap<>();
    private final Map<String, SseSink> streamableSubscriptions = new HashMap<>();

    private volatile String protocolVersion;
    private volatile State state = UNINITIALIZED;

    McpSession() {
        this(new HashSet<>());
    }

    McpSession(Set<McpCapability> capabilities) {
        this.capabilities = capabilities;
    }

    void poll(Consumer<JsonObject> consumer) {
        while (active.get()) {
            try {
                JsonObject message = queue.take();
                if (message.getBoolean("disconnect", false)) {
                    break;
                }
                consumer.accept(message);
            } catch (Exception e) {
                throw new McpException("Session interrupted.", e);
            }
        }
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

    Optional<McpFeatures> features(JsonValue requestId) {
        return features.get(requestId);
    }

    void clearRequest(JsonValue requestId) {
        features.remove(requestId);
    }

    void capabilities(McpCapability capability) {
        capabilities.add(capability);
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
        }  finally {
            lock.readLock().unlock();
        }
    }

    Optional<SseSink> subscribe(JsonRpcRequest req, JsonRpcResponse res, McpResourceSubscriber subscriber) {
        if (!active.get()) {
            return Optional.empty();
        }

        lock.writeLock().lock();
        try {
            SseSink sseSink = null;
            if (isStreamableHttp(req.headers())) {
                SseSink existing = streamableSubscriptions.get(subscriber.uri());
                if (existing != null) {
                    existing.close();       // close old one
                    LOGGER.log(Level.FINE, () -> "Removed existing subscription for " + subscriber.uri());
                }
                sseSink = res.sink(SseSink.TYPE);
                streamableSubscriptions.put(subscriber.uri(), sseSink);
            } else {
                McpSession existing = sseSubscriptions.get(subscriber.uri());
                if (existing != null) {
                    LOGGER.log(Level.FINE, () -> "Found existing subscription for " + subscriber.uri());
                    return Optional.empty();
                }
                sseSubscriptions.put(subscriber.uri(), this);
            }
            LOGGER.log(Level.FINE, () -> "New subscription for " + subscriber.uri());
            return Optional.ofNullable(sseSink);
        } finally {
            lock.writeLock().unlock();
        }
    }

    Optional<SseSink> unsubscribe(JsonRpcRequest req, McpResourceUnsubscriber unsubscriber) {
        if (!active.get()) {
            return Optional.empty();
        }

        lock.writeLock().lock();
        try {
            SseSink sseSink = null;
            if (isStreamableHttp(req.headers())) {
                sseSink = streamableSubscriptions.remove(unsubscriber.uri());
                if (sseSink == null) {
                    LOGGER.log(Level.FINE, () -> "No subscription found for " + unsubscriber.uri());
                }
            } else {
                McpSession session = sseSubscriptions.remove(unsubscriber.uri());
                if (session == null) {
                    LOGGER.log(Level.FINE, () -> "No subscription found for " + unsubscriber.uri());
                }
            }
            LOGGER.log(Level.FINE, () -> "Removed subscription for " + unsubscriber.uri());
            return Optional.ofNullable(sseSink);
        }  finally {
            lock.writeLock().unlock();
        }
    }

    enum State {
        INITIALIZED,
        INITIALIZING,
        UNINITIALIZED
    }
}
