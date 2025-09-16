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
 * The MCP Cancellation feature enables verification of whether a client
 * has issued a cancellation request. Such requests are typically made when
 * a process is taking an extended amount of time, and the client opts not
 * to wait for the completion of the operation.
 */
public final class McpCancellation {
    private volatile McpCancellationResult result;

    McpCancellation() {
        result = new McpCancellationResult(false, "No cancellation requested");
    }

    /**
     * Check whether a cancellation request was made.
     *
     * @return cancellation result
     */
    public McpCancellationResult verify() {
        return result;
    }

    void cancel(String reason) {
        result = new McpCancellationResult(true, reason);
    }
}
