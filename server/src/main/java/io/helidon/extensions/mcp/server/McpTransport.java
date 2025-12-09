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

import io.helidon.webserver.jsonrpc.JsonRpcResponse;

import jakarta.json.JsonObject;

/**
 * MCP transport provides a way to send data to the connected client.
 */
sealed interface McpTransport extends McpTransportLifecycle permits McpSsePostTransport,
                                                                    McpStreamableHttpTransport {
    /**
     * Send a JSON object to the client. The payload has to follow
     * the JSON-RPC 2.0 specification.
     *
     * @param object payload
     */
    void send(JsonObject object);

    /**
     * Send a JSON-RPC response.
     *
     * @param response the response
     */
    void send(JsonRpcResponse response);

    /**
     * Block the current request for the provided duration.
     *
     * @param timeout the timeout
     */
    void block(Duration timeout);

    /**
     * Unblock the current request.
     */
    void unblock();
}
