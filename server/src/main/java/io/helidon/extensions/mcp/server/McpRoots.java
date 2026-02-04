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

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.json.JsonObject;

import static io.helidon.extensions.mcp.server.McpJsonSerializer.METHOD_ROOTS_LIST;

/**
 * MCP roots feature.
 */
public final class McpRoots extends McpFeature {
    private final boolean enabled;
    private final Duration timeout;
    private final List<McpRoot> roots = new CopyOnWriteArrayList<>();

    McpRoots(McpSession session, McpTransport transport) {
        super(session, transport);
        this.timeout = session.context()
                .get(McpServerConfigBlueprint.class, McpServerConfig.class)
                .orElseThrow(() -> new McpInternalException("Server configuration is not set"))
                .rootListTimeout();
        this.enabled = session.capabilities().contains(McpCapability.ROOTS);
    }

    /**
     * Whether the connected client supports roots feature.
     *
     * @return {@code true} if the connected client supports roots feature,
     * {@code false} otherwise.
     */
    public boolean enabled() {
        return enabled;
    }

    /**
     * Get the current list of root available from client.
     *
     * @return list of root
     * @throws io.helidon.extensions.mcp.server.McpRootException if an error occurs
     */
    public List<McpRoot> listRoots() throws McpRootException {
        if (!enabled) {
            throw new McpRootException("Roots feature is not supported by the client");
        }
        boolean updateRoot = session().context()
                .get(McpRootClassifier.class, Boolean.class)
                .orElse(false);
        return updateRoot ? sendListRoot(timeout) : roots;
    }

    /**
     * Sends a {@code roots/list} request and update the list of root.
     *
     * @return list of root
     */
    private List<McpRoot> sendListRoot(Duration timeout) {
        long id = session().jsonRpcId();
        JsonObject request = session().serializer()
                .createJsonRpcRequest(id, METHOD_ROOTS_LIST)
                .build();
        transport().send(request);
        JsonObject response = session().pollResponse(id, timeout);
        List<McpRoot> updatedRoots = session().serializer().parseRoots(response);
        roots.clear();
        roots.addAll(updatedRoots);
        session().context().register(McpRootClassifier.class, false);
        return roots;
    }

    static class McpRootClassifier {
    }
}
