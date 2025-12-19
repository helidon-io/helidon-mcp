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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.helidon.http.HeaderName;
import io.helidon.http.HeaderNames;
import io.helidon.http.HeaderValues;
import io.helidon.http.Status;
import io.helidon.http.sse.SseEvent;
import io.helidon.webserver.http.ServerResponse;
import io.helidon.webserver.jsonrpc.JsonRpcRequest;
import io.helidon.webserver.jsonrpc.JsonRpcResponse;
import io.helidon.webserver.sse.SseSink;

import jakarta.json.JsonObject;

import static io.helidon.extensions.mcp.server.McpJsonSerializer.prettyPrint;

final class McpStreamableHttpTransport implements McpTransport {
    static final HeaderName SESSION_ID_HEADER = HeaderNames.create("Mcp-Session-Id");
    private static final System.Logger LOGGER = System.getLogger(McpStreamableHttpTransport.class.getName());
    private final String sessionId;
    private final CountDownLatch latch;
    private final JsonRpcResponse response;
    private SseSink sseSink;

    McpStreamableHttpTransport(JsonRpcResponse response, String sessionId) {
        this.response = response;
        this.sessionId = sessionId;
        this.latch = new CountDownLatch(1);
    }

    @Override
    public void send(JsonObject object) {
        if (LOGGER.isLoggable(System.Logger.Level.DEBUG)) {
            LOGGER.log(System.Logger.Level.DEBUG, "Streamable Http:\n" + prettyPrint(object));
        }
        sink().emit(SseEvent.builder()
                            .name("message")
                            .data(object)
                            .build());
    }

    @Override
    public void send(JsonRpcResponse response) {
        if (LOGGER.isLoggable(System.Logger.Level.DEBUG)) {
            LOGGER.log(System.Logger.Level.DEBUG, "Streamable Http:\n" + prettyPrint(response.asJsonObject()));
        }
        if (sseSink != null) {
            sseSink.emit(SseEvent.builder()
                                 .name("message")
                                 .data(response.asJsonObject())
                                 .build());
            sseSink.close();
            return;
        }
        response.header(HeaderValues.CONTENT_TYPE_JSON);
        response.send();
    }

    @Override
    public void block(Duration timeout) {
        try {
            boolean completed = latch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!completed) {
                if (LOGGER.isLoggable(System.Logger.Level.TRACE)) {
                    LOGGER.log(System.Logger.Level.TRACE, "Blocking timeout reached");
                }
            }
        } catch (InterruptedException e) {
            if (LOGGER.isLoggable(System.Logger.Level.TRACE)) {
                LOGGER.log(System.Logger.Level.TRACE, "Interrupted while blocking", e);
            }
        }
    }

    @Override
    public void unblock() {
        latch.countDown();
    }

    boolean openedSseChannel() {
        return sseSink != null;
    }

    @Override
    public void onConnect(ServerResponse response) {
        response.header(SESSION_ID_HEADER, sessionId);
    }

    @Override
    public void onDisconnect(ServerResponse response) {
        response.status(Status.ACCEPTED_202);
    }

    @Override
    public McpTransport onRequest(JsonRpcRequest request, JsonRpcResponse response) {
        if (LOGGER.isLoggable(System.Logger.Level.DEBUG)) {
            LOGGER.log(System.Logger.Level.DEBUG, "Streamable HTTP Request:\n" + prettyPrint(request.asJsonObject()));
        }
        return new McpStreamableHttpTransport(response, sessionId);
    }

    private SseSink sink() {
        if (sseSink == null) {
            response.header(HeaderValues.CONTENT_TYPE_EVENT_STREAM);
            sseSink = response.sink(SseSink.TYPE);
        }
        return sseSink;
    }
}
