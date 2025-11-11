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

import java.lang.System.Logger.Level;
import java.util.function.Consumer;

import io.helidon.http.sse.SseEvent;
import io.helidon.webserver.sse.SseSink;

import jakarta.json.JsonObject;

import static io.helidon.extensions.mcp.server.McpJsonRpc.createSamplingRequest;
import static io.helidon.extensions.mcp.server.McpJsonRpc.createSamplingResponse;
import static io.helidon.extensions.mcp.server.McpJsonRpc.prettyPrint;

/**
 * MCP Sampling feature.
 */
public final class McpSampling extends McpFeature {
    private static final System.Logger LOGGER = System.getLogger(McpSampling.class.getName());
    private final boolean enabled;

    McpSampling(McpSession session) {
        super(session);
        this.enabled = session().capabilities().contains(McpCapability.SAMPLING);
    }

    McpSampling(McpSession session, SseSink sseSink) {
        super(session, sseSink);
        this.enabled = session().capabilities().contains(McpCapability.SAMPLING);
    }

    /**
     * Whether the connected client supports sampling feature.
     *
     * @return {@code true} if the connected client supports sampling feature,
     * {@code false} otherwise.
     */
    public boolean enabled() {
        return enabled;
    }

    /**
     * Send the provided sampling request to the client and return its response.
     *
     * @param request sampling request
     * @return sampling response
     * @throws io.helidon.extensions.mcp.server.McpSamplingException when an error occurs
     */
    public McpSamplingResponse request(Consumer<McpSamplingRequest.Builder> request) throws McpSamplingException {
        var builder = McpSamplingRequest.builder();
        request.accept(builder);
        return request(builder.build());
    }

    /**
     * Send the provided sampling request to the client and return its response.
     *
     * @param request sampling request
     * @return sampling response
     * @throws io.helidon.extensions.mcp.server.McpSamplingException when an error occurs
     */
    public McpSamplingResponse request(McpSamplingRequest request) throws McpSamplingException {
        if (!enabled) {
            throw new McpSamplingException("Sampling feature is not supported by client");
        }
        long id = session().jsonRpcId();
        JsonObject payload = createSamplingRequest(id, request);

        if (LOGGER.isLoggable(Level.DEBUG)) {
            LOGGER.log(Level.DEBUG, "Sampling request:\n" + prettyPrint(payload));
        }
        sseSink().ifPresentOrElse(sink -> sink.emit(SseEvent.builder()
                                            .name("message")
                                            .data(payload)
                                            .build()),
                                  () -> session().send(payload));
        JsonObject response = session().pollResponse(id, request.timeout());
        if (LOGGER.isLoggable(Level.DEBUG)) {
            LOGGER.log(Level.DEBUG, "Sampling response:\n" + prettyPrint(response));
        }
        return createSamplingResponse(response);
    }
}
