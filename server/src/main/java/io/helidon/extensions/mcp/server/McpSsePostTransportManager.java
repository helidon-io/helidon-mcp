/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
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

import io.helidon.webserver.http.ServerResponse;
import io.helidon.webserver.jsonrpc.JsonRpcRequest;
import io.helidon.webserver.jsonrpc.JsonRpcResponse;

import static io.helidon.extensions.mcp.server.McpJsonSerializer.prettyPrint;

final class McpSsePostTransportManager implements McpTransportManager {
    private static final System.Logger LOGGER = System.getLogger(McpSsePostTransportManager.class.getName());
    private final McpSsePostTransport transport;

    McpSsePostTransportManager(String endpoint, String sessionId) {
        this.transport = new McpSsePostTransport(endpoint, sessionId);
    }

    @Override
    public McpTransport create(JsonRpcRequest request, JsonRpcResponse response) {
        return transport;
    }

    @Override
    public void onConnect(ServerResponse response) {
        transport.onConnect(response);
    }

    @Override
    public void onDisconnect(ServerResponse response) {
        transport.onDisconnect();
    }

    @Override
    public void onRequest(JsonRpcRequest request, JsonRpcResponse response) {
        if (LOGGER.isLoggable(System.Logger.Level.DEBUG)) {
            LOGGER.log(System.Logger.Level.DEBUG, "SSE Request:\n" + prettyPrint(request.asJsonObject()));
        }
    }

    @Override
    public void onNotification(JsonRpcRequest request, JsonRpcResponse response) {
        if (LOGGER.isLoggable(System.Logger.Level.DEBUG)) {
            LOGGER.log(System.Logger.Level.DEBUG, "SSE Notification:\n" + prettyPrint(request.asJsonObject()));
        }
    }
}
