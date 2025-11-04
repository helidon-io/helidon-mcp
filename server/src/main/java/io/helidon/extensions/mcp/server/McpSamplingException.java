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
 * MCP sampling exception thrown during a sampling request to the client.
 */
public class McpSamplingException extends RuntimeException {
    /**
     * Creates a new MCP sampling exception with specified details message.
     *
     * @param message exception message
     */
    McpSamplingException(String message) {
        super(message);
    }

    /**
     * Creates a new MCP sampling exception with specified details message and its cause.
     *
     * @param message exception message
     * @param cause exception cause
     */
    McpSamplingException(String message, Throwable cause) {
        super(message, cause);
    }
}
