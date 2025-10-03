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

import io.helidon.jsonrpc.core.JsonRpcError;

/**
 * MCP protocol exception.
 */
public class McpException extends RuntimeException {
    private final int code;

    /**
     * Create an exception with a message and default {@code Internal Error} error code.
     *
     * @param message exception message
     */
    public McpException(String message) {
        super(message);
        this.code = JsonRpcError.INTERNAL_ERROR;
    }

    /**
     * Create an exception with a message, cause and default {@code Internal Error} error code.
     *
     * @param message exception message
     * @param cause exception cause
     */
    public McpException(String message, Throwable cause) {
        super(message, cause);
        this.code = JsonRpcError.INTERNAL_ERROR;
    }

    /**
     * Create an exception with a message and custom error code.
     *
     * @param code exception error code
     * @param message exception message
     */
    public McpException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * Create an exception with a message, cause and custom error code.
     *
     * @param code exception error code
     * @param message exception message
     * @param cause exception cause
     */
    public McpException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    int code() {
        return code;
    }
}
