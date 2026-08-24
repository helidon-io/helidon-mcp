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

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import io.helidon.json.JsonObject;
import io.helidon.json.JsonValue;

/**
 * MCP 2025-11-25 protocol error indicating that URL elicitations must complete before the original request can be retried.
 *
 * @see <a href="https://modelcontextprotocol.io/specification/2025-11-25/client/elicitation#url-elicitation-required-error">
 *      MCP URL elicitation required error</a>
 */
public final class McpUrlElicitationRequiredException extends RuntimeException {
    /**
     * URL elicitation required JSON-RPC error code.
     */
    public static final int ERROR_CODE = -32042;

    private static final String ERROR_MESSAGE = "URL elicitation required";

    /**
     * Structured JSON-RPC error data.
     */
    private final JsonObject data;

    /**
     * Required URL elicitations.
     */
    private final List<McpElicitationUrlRequest> elicitations;

    /**
     * Create an exception for one required URL elicitation.
     *
     * @param elicitation required URL elicitation
     * @throws NullPointerException if the elicitation is {@code null}
     */
    public McpUrlElicitationRequiredException(McpElicitationUrlRequest elicitation) {
        this(List.of(Objects.requireNonNull(elicitation, "elicitation is null")));
    }

    /**
     * Create an exception for required URL elicitations.
     *
     * @param elicitations required URL elicitations
     * @throws NullPointerException if the collection or an elicitation is {@code null}
     * @throws IllegalArgumentException if the collection is empty
     */
    public McpUrlElicitationRequiredException(Collection<McpElicitationUrlRequest> elicitations) {
        this(List.copyOf(Objects.requireNonNull(elicitations, "elicitations are null")));
    }

    private McpUrlElicitationRequiredException(List<McpElicitationUrlRequest> elicitations) {
        super(ERROR_MESSAGE);
        this.data = createData(elicitations);
        this.elicitations = elicitations;
    }

    /**
     * URL elicitations that must complete before retrying the original request.
     *
     * @return required URL elicitations
     */
    public List<McpElicitationUrlRequest> elicitations() {
        return elicitations;
    }

    JsonObject errorData() {
        return data;
    }

    private static JsonObject createData(List<McpElicitationUrlRequest> elicitations) {
        if (elicitations.isEmpty()) {
            throw new IllegalArgumentException("At least one URL elicitation is required");
        }
        List<JsonValue> values = elicitations.stream()
                .map(elicitation -> JsonObject.builder()
                        .set("mode", "url")
                        .set("message", elicitation.message())
                        .set("elicitationId", elicitation.elicitationId())
                        .set("url", elicitation.url().toASCIIString())
                        .build())
                .map(JsonValue.class::cast)
                .toList();
        return JsonObject.builder()
                .setValues("elicitations", values)
                .build();
    }
}
