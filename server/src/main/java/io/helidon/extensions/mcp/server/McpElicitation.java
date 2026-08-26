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

import java.time.Duration;
import java.util.Objects;
import java.util.function.Consumer;

import io.helidon.json.JsonObject;

/**
 * Elicitation feature.
 */
public final class McpElicitation extends McpFeature {
    private static final String CANCELLATION_MESSAGE = "Elicitation request cancelled";

    private final boolean enabled;
    private final boolean enabledUrl;
    private final McpFeatures features;

    McpElicitation(McpSession session, McpTransport transport, McpFeatures features) {
        super(session, transport);
        this.enabled = session.capabilities().contains(McpCapability.ELICITATION_FORM);
        this.enabledUrl = session.capabilities().contains(McpCapability.ELICITATION_URL);
        this.features = features;
    }

    /**
     * Whether the connected client supports form elicitation.
     *
     * @return {@code true} if the connected client supports form elicitation,
     * {@code false} otherwise.
     */
    public boolean enabled() {
        return enabled;
    }

    /**
     * Whether the connected client supports URL elicitation.
     *
     * @return {@code true} if the connected client supports URL elicitation,
     * {@code false} otherwise.
     */
    public boolean enabledUrl() {
        return enabledUrl;
    }

    /**
     * Send the provided elicitation request to the client and return its response.
     * Starting with MCP protocol version {@code 2025-11-25}, this method creates a form elicitation request.
     * Use {@link #requestUrl(Consumer)} to create a URL elicitation request.
     *
     * @param request form elicitation request
     * @return form elicitation response; content is present only when the action is
     *         {@link McpElicitationAction#ACCEPT}
     * @throws McpElicitationException when an error occurs
     */
    public McpElicitationResponse request(Consumer<McpElicitationRequest.Builder> request) throws McpElicitationException {
        McpElicitationRequest builder = McpElicitationRequest.builder().update(request).build();
        return request(builder);
    }

    /**
     * Send the provided elicitation request to the client and return its response.
     * Starting with MCP protocol version {@code 2025-11-25}, this method sends a form elicitation request.
     * Use {@link #requestUrl(McpElicitationUrlRequest)} to send a URL elicitation request.
     *
     * @param request form elicitation request
     * @return form elicitation response; content is present only when the action is
     *         {@link McpElicitationAction#ACCEPT}
     * @throws McpElicitationException when an error occurs
     */
    public McpElicitationResponse request(McpElicitationRequest request) throws McpElicitationException {
        if (!enabled) {
            throw new McpElicitationException("Elicitation feature is not supported by client");
        }
        long id = session().jsonRpcId();
        JsonObject payload = session().serializer().createElicitationRequest(id, request);
        McpResponse response = requestClientResponse(id, payload, request.timeout());
        return session().serializer().createElicitationResponse(response.asJsonObject());
    }

    /**
     * Send the provided URL elicitation request to the client and return its response.
     *
     * @param request URL elicitation request
     * @return URL elicitation response without content
     * @throws McpElicitationException when an error occurs
     */
    public McpElicitationResponse requestUrl(Consumer<McpElicitationUrlRequest.Builder> request)
            throws McpElicitationException {
        McpElicitationUrlRequest builder = McpElicitationUrlRequest.builder().update(request).build();
        return requestUrl(builder);
    }

    /**
     * Send the provided URL elicitation request to the client and return its response.
     *
     * @param request URL elicitation request
     * @return URL elicitation response without content
     * @throws NullPointerException if request is {@code null}
     * @throws McpElicitationException when an error occurs
     */
    public McpElicitationResponse requestUrl(McpElicitationUrlRequest request) throws McpElicitationException {
        Objects.requireNonNull(request, "request is null");
        if (!enabledUrl) {
            throw new McpElicitationException("URL elicitation feature is not supported by client");
        }
        long id = session().jsonRpcId();
        JsonObject payload = session().serializer().createElicitationRequest(id, request);
        McpResponse response = requestClientResponse(id, payload, request.timeout());
        return session().serializer().createElicitationUrlResponse(response.asJsonObject());
    }

    private McpResponse requestClientResponse(long id, JsonObject payload, Duration timeout) {
        McpCancellation cancellation = features.cancellation();
        Runnable cancellationHook = () -> session()
                .abortResponse(id, new McpElicitationException(CANCELLATION_MESSAGE));
        try {
            session().prepareResponse(id);
            cancellation.registerCancellationHook(cancellationHook);
            if (session().state() == McpSession.State.DISCONNECTED) {
                throw new McpInternalException("Session disconnected");
            }
            if (cancellation.result().isRequested()) {
                cancellation.result()
                        .reason()
                        .ifPresentOrElse(
                                this::throwCancellationException,
                                () -> throwCancellationException(CANCELLATION_MESSAGE));
            }
            transport().send(payload);
            return session().pollResponse(id, timeout)
                    .orElseThrow(() -> new McpElicitationException("response timeout"));
        } finally {
            cancellation.unregisterCancellationHook(cancellationHook);
            session().discardResponse(id);
        }
    }

    private void throwCancellationException(String reason) {
        throw new McpElicitationException(reason);
    }
}
