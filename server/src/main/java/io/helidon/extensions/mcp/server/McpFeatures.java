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

/**
 * Support for optional client features like {@link McpProgress} and {@link McpLogger}.
 */
public final class McpFeatures {
    private final McpProgress progress;
    private final McpLogger logger;

    McpFeatures(McpSession session) {
        this.logger = new McpLogger(session);
        this.progress = new McpProgress(session);
    }

    /**
     * Get a {@link McpProgress} feature.
     *
     * @return progress
     */
    public McpProgress progress() {
        return progress;
    }

    /**
     * Get a {@link McpLogger} feature.
     *
     * @return logging
     */
    public McpLogger logger() {
        return logger;
    }
}
