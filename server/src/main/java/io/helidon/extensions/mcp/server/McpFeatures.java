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

import io.helidon.common.LazyValue;

/**
 * Support for optional client features:
 * <p>
 * <ul>
 *     <li>
 *         {@link io.helidon.extensions.mcp.server.McpProgress} - MCP Progress feature.
 *         Sends progress notifications to the client.
 *     </li>
 *     <li>
 *         {@link io.helidon.extensions.mcp.server.McpLogger} - MCP Logging feature.
 *         Sends logging notifications to the client.
 *     </li>
 *     <li>
 *         {@link io.helidon.extensions.mcp.server.McpCancellation} - MCP Cancellation feature.
 *         Check whether the client made a cancellation request.
 *     </li>
 *     <li>
 *         {@link io.helidon.extensions.mcp.server.McpSubscriptions} - MCP subscription feature.
 *         Sends notifications to the subscribed clients.
 *     </li>
 *     <li>
 *         {@link io.helidon.extensions.mcp.server.McpSampling} - MCP Sampling feature.
 *         Send sampling messages to client.
 *     </li>
 *     <li>
 *         {@link io.helidon.extensions.mcp.server.McpRoots} - MCP Roots feature.
 *         List the available filesystem root from client.
 *     </li>
 * </ul>
 */
public final class McpFeatures {
    private final McpSession session;
    private final LazyValue<McpRoots> roots;
    private final LazyValue<McpLogger> logger;
    private final LazyValue<McpSampling> sampling;
    private final LazyValue<McpProgress> progress;
    private final LazyValue<McpCancellation> cancellation;

    McpFeatures(McpSession session, McpTransport transport) {
        Objects.requireNonNull(session, "session is null");
        Objects.requireNonNull(transport, "transport is null");
        this.session = session;
        this.cancellation = LazyValue.create(McpCancellation::new);
        this.roots = LazyValue.create(() -> new McpRoots(session, transport));
        this.logger = LazyValue.create(() -> new McpLogger(session, transport));
        this.sampling = LazyValue.create(() -> new McpSampling(session, transport));
        this.progress = LazyValue.create(() -> new McpProgress(session, transport));
    }

    /**
     * Get a {@link io.helidon.extensions.mcp.server.McpProgress} feature.
     *
     * @return the MCP progress
     */
    public McpProgress progress() {
        return progress.get();
    }

    /**
     * Get a {@link io.helidon.extensions.mcp.server.McpLogger} feature.
     *
     * @return the MCP logger
     */
    public McpLogger logger() {
        return logger.get();
    }

    /**
     * Get a {@link io.helidon.extensions.mcp.server.McpRoots} feature.
     *
     * @return the MCP roots
     */
    public McpRoots roots() {
        return roots.get();
    }

    /**
     * Get a {@link io.helidon.extensions.mcp.server.McpSampling} feature.
     *
     * @return the MCP sampling
     */
    public McpSampling sampling() {
        return sampling.get();
    }

    /**
     * Get a {@link io.helidon.extensions.mcp.server.McpCancellation} feature.
     *
     * @return the MCP cancellation
     */
    public McpCancellation cancellation() {
        return cancellation.get();
    }

    /**
     * Get a {@link io.helidon.extensions.mcp.server.McpSubscriptions} feature.
     *
     * @return the MCP subscriptions
     */
    public McpSubscriptions subscriptions() {
        return session.features().subscriptions();
    }
}
