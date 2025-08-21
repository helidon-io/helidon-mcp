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
 * MCP protocol exception.
 */
public class McpException extends RuntimeException {

    /**
     * Create a {@link McpException} instance.
     *
     * @param message exception message
     */
    public McpException(String message) {
        super(message);
    }

    /**
     * Create a {@link McpException} instance.
     *
     * @param message exception message
     * @param cause exception cause
     */
    public McpException(String message, Throwable cause) {
        super(message, cause);
    }
}
