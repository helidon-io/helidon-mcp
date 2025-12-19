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

import io.helidon.webserver.http.ServerResponse;
import io.helidon.webserver.jsonrpc.JsonRpcRequest;
import io.helidon.webserver.jsonrpc.JsonRpcResponse;

/**
 * Basic MCP transport operation lifecycle.
 */
interface McpTransportLifecycle {
    /**
     * On new connection request.
     *
     * @param response server response on new connection
     */
    void onConnect(ServerResponse response);

    /**
     * On connection disconnect.
     *
     * @param response server response on disconnect
     */
    void onDisconnect(ServerResponse response);

    /**
     * On new client request.
     *
     * @param request the request
     * @param response the response
     * @return an instance of McpTransport
     */
    McpTransport onRequest(JsonRpcRequest request, JsonRpcResponse response);
}
