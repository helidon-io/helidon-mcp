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

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import io.helidon.common.UncheckedException;
import io.helidon.http.HeaderValues;
import io.helidon.http.sse.SseEvent;
import io.helidon.webserver.http.ServerResponse;
import io.helidon.webserver.jsonrpc.JsonRpcRequest;
import io.helidon.webserver.jsonrpc.JsonRpcResponse;
import io.helidon.webserver.sse.SseSink;

import jakarta.json.JsonObject;

import static io.helidon.extensions.mcp.server.McpJsonSerializer.JSON_BUILDER_FACTORY;
import static io.helidon.extensions.mcp.server.McpJsonSerializer.prettyPrint;

/**
 * Implementation of the MCP {@code SSE + POST} transport.
 */
final class McpSsePostTransport implements McpTransport {
    private static final System.Logger LOGGER = System.getLogger(McpSsePostTransport.class.getName());
    private final String endpoint;
    private final String sessionId;
    /**
     * Keeps connection active while {@code true}.
     */
    private final AtomicBoolean active;
    /**
     * JSON-RPC message sender, can be requests or notifications.
     */
    private final BlockingQueue<JsonObject> messages = new LinkedBlockingQueue<>();

    McpSsePostTransport(String endpoint, String sessionId) {
        this.endpoint = endpoint;
        this.sessionId = sessionId;
        this.active = new AtomicBoolean(true);
    }

    @Override
    public void send(JsonObject object) {
        try {
            if (LOGGER.isLoggable(System.Logger.Level.DEBUG)) {
                LOGGER.log(System.Logger.Level.DEBUG, "SSE: " + prettyPrint(object));
            }
            messages.put(object);
        } catch (InterruptedException e) {
            throw new UncheckedException(e);
        }
    }

    @Override
    public void send(JsonRpcResponse response) {
        send(response.asJsonObject());
    }

    @Override
    public void onConnect(ServerResponse response) {
        response.header(HeaderValues.CONTENT_TYPE_EVENT_STREAM);
        try (SseSink sink = response.sink(SseSink.TYPE)) {
            sink.emit(SseEvent.builder()
                              .name("endpoint")
                              .data(endpoint + "/message?sessionId=" + sessionId)
                              .build());
            poll(message -> sink.emit(SseEvent.builder()
                                              .name("message")
                                              .data(message)
                                              .build()));
        }
    }

    @Override
    public void onDisconnect(ServerResponse response) {
        if (active.compareAndSet(true, false)) {
            var disconnect = JSON_BUILDER_FACTORY.createObjectBuilder()
                    .add("disconnect", true)
                    .build();
            messages.add(disconnect);
        }
    }

    @Override
    public void block(Duration timeout) {
    }

    @Override
    public void unblock() {
    }

    @Override
    public McpTransport onRequest(JsonRpcRequest request, JsonRpcResponse response) {
        if (LOGGER.isLoggable(System.Logger.Level.DEBUG)) {
            LOGGER.log(System.Logger.Level.DEBUG, "SSE Request:\n" + prettyPrint(request.asJsonObject()));
        }
        return this;
    }

    private void poll(Consumer<JsonObject> consumer) {
        while (active.get()) {
            try {
                JsonObject message = messages.take();
                if (message.getBoolean("disconnect", false)) {
                    if (LOGGER.isLoggable(System.Logger.Level.TRACE)) {
                        LOGGER.log(System.Logger.Level.TRACE, "Session disconnected.");
                    }
                    break;
                }
                consumer.accept(message);
            } catch (Exception e) {
                if (LOGGER.isLoggable(System.Logger.Level.TRACE)) {
                    LOGGER.log(System.Logger.Level.TRACE, "Session interrupted.", e);
                }
                break;
            }
        }
    }
}
