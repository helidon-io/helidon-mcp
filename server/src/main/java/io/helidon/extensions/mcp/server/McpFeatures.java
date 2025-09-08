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

import java.util.Objects;
import java.util.Optional;

import io.helidon.webserver.jsonrpc.JsonRpcResponse;
import io.helidon.webserver.sse.SseSink;

/**
 * Support for optional client features like {@link McpProgress} and {@link McpLogger}.
 */
public final class McpFeatures {
    private final JsonRpcResponse response;
    private final McpSession session;

    private McpProgress progress;
    private McpLogger logger;
    private SseSink sseSink;

    McpFeatures(McpSession session) {
        Objects.requireNonNull(session, "session is null");
        this.session = session;
        this.response = null;
    }

    McpFeatures(McpSession session, JsonRpcResponse response) {
        Objects.requireNonNull(response, "response is null");
        Objects.requireNonNull(session, "session is null");
        this.response = response;
        this.session = session;
    }

    /**
     * Get a {@link McpProgress} feature.
     *
     * @return progress the MCP progress
     */
    public McpProgress progress() {
        if (progress == null) {
            if (response != null) {
                sseSink = getOrCreateSseSink();
                progress = new McpProgress(session, sseSink);
            } else {
                progress = new McpProgress(session);
            }
        }
        return progress;
    }

    /**
     * Get a {@link McpLogger} feature.
     *
     * @return logging the MCP logger
     */
    public McpLogger logger() {
        if (logger == null) {
            if (response != null) {
                sseSink = getOrCreateSseSink();
                logger = new McpLogger(session, sseSink);
            } else {
                logger = new McpLogger(session);
            }
        }
        return logger;
    }

    /**
     * Get access to underlying SSE sink, if available. This method is package private.
     *
     * @return optional SSE sink
     */
    Optional<SseSink> sseSink() {
        return Optional.ofNullable(sseSink);
    }

    /**
     * Get or create an SSE sink for this instance.
     *
     * @return the SSE sink
     */
    private SseSink getOrCreateSseSink() {
        Objects.requireNonNull(response, "response is null");
        return sseSink != null ? sseSink : response.sink(SseSink.TYPE);
    }
}
