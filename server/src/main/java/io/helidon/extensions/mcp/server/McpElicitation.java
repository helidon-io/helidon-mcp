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

import java.util.function.Consumer;

import jakarta.json.JsonObject;

/**
 * Elicitation feature.
 */
public final class McpElicitation extends McpFeature {
    private final boolean enabled;

    McpElicitation(McpSession session, McpTransport transport) {
        super(session, transport);
        this.enabled = session.capabilities().contains(McpCapability.ELICITATION);
    }

    /**
     * Whether the connected client supports elicitation feature.
     *
     * @return {@code true} if the connected client supports elicitation feature,
     * {@code false} otherwise.
     */
    public boolean enabled() {
        return enabled;
    }

    /**
     * Send the provided elicitation request to the client and return its response.
     *
     * @param request elicitation request
     * @return elicitation response
     * @throws io.helidon.extensions.mcp.server.McpElicitationException when an error occurs
     */
    public McpElicitationResponse request(Consumer<McpElicitationRequest.Builder> request) throws McpElicitationException {
        var builder = McpElicitationRequest.builder();
        request.accept(builder);
        return request(builder.build());
    }

    /**
     * Send the provided elicitation request to the client and return its response.
     *
     * @param request elicitation request
     * @return elicitation response
     * @throws io.helidon.extensions.mcp.server.McpElicitationException when an error occurs
     */
    public McpElicitationResponse request(McpElicitationRequest request) throws McpElicitationException {
        if (!enabled) {
            throw new McpElicitationException("Elicitation feature is not supported by client");
        }
        long id = session().jsonRpcId();
        JsonObject payload = session().serializer().createElicitationRequest(id, request);
        transport().send(payload);
        JsonObject response = session().pollResponse(id, request.timeout());
        return session().serializer().createElicitationResponse(response);
    }
}
