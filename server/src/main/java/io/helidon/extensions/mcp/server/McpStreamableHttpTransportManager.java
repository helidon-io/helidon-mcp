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

import io.helidon.http.HeaderName;
import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.webserver.http.ServerResponse;
import io.helidon.webserver.jsonrpc.JsonRpcRequest;
import io.helidon.webserver.jsonrpc.JsonRpcResponse;

import static io.helidon.extensions.mcp.server.McpJsonSerializer.prettyPrint;
import static io.helidon.jsonrpc.core.JsonRpcError.INTERNAL_ERROR;

final class McpStreamableHttpTransportManager implements McpTransportManager {
    static final HeaderName SESSION_ID_HEADER = HeaderNames.create("Mcp-Session-Id");
    private static final HeaderName MCP_PROTOCOL_VERSION = HeaderNames.create("Mcp-Protocol-Version");
    private static final System.Logger LOGGER = System.getLogger(McpStreamableHttpTransportManager.class.getName());
    private final McpSessions sessions;
    private final String sessionId;

    McpStreamableHttpTransportManager(McpSessions sessions, String sessionId) {
        this.sessions = sessions;
        this.sessionId = sessionId;
    }

    @Override
    public McpTransport create(JsonRpcRequest request, JsonRpcResponse response) {
        return new McpStreamableHttpTransport(response);
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
    public void onRequest(JsonRpcRequest request, JsonRpcResponse response) {
        if (LOGGER.isLoggable(System.Logger.Level.DEBUG)) {
            LOGGER.log(System.Logger.Level.DEBUG, "Streamable HTTP Request:\n" + prettyPrint(request.asJsonObject()));
        }
        McpSession session = sessions.get(sessionId)
                .orElseThrow(() -> new McpInternalException("No session with id " + sessionId));
        if (isNotValidNegotiatedVersion(request, session)) {
            response.status(Status.BAD_REQUEST_400)
                    .error(INTERNAL_ERROR, "Wrong MCP protocol version");
        }
    }

    @Override
    public void onNotification(JsonRpcRequest request, JsonRpcResponse response) {
        McpSession session = sessions.get(sessionId)
                .orElseThrow(() -> new McpInternalException("No session with id " + sessionId));
        if (isNotValidNegotiatedVersion(request, session)) {
            response.status(Status.BAD_REQUEST_400);
            throw new McpInternalException("Wrong MCP protocol version");
        }
    }

    /**
     * Validate the negotiated MCP version with the one sent by the client.
     *
     * @param req       the client request
     * @param session   the client session
     * @return          {@code true} if the version is not valid, {@code false} otherwise
     */
    private boolean isNotValidNegotiatedVersion(JsonRpcRequest req, McpSession session) {
        if (req.headers().contains(MCP_PROTOCOL_VERSION)) {
            String providedMcpVersion = req.headers().get(MCP_PROTOCOL_VERSION).get();
            return !session.protocolVersion().text().equals(providedMcpVersion);
        }
        // for backward compatibility, we do not enforce this check
        return false;
    }
}
