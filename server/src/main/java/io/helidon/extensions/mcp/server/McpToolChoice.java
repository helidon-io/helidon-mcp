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

import java.util.Locale;

/**
 * Tool selection behavior for an MCP sampling request.
 */
public enum McpToolChoice {
    /**
     * The model decides whether to use tools.
     */
    AUTO,

    /**
     * The model must use at least one tool.
     */
    REQUIRED,

    /**
     * The model must not use tools.
     */
    NONE;

    /**
     * Returns the protocol representation of this tool choice.
     *
     * @return protocol representation
     */
    String text() {
        return name().toLowerCase(Locale.ROOT);
    }
}
