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

import io.helidon.common.LazyValue;

import jakarta.json.JsonValue;

/**
 * The MCP Cancellation feature enables verification of whether a client
 * has issued a cancellation request. Such requests are typically made when
 * a process is taking an extended amount of time, and the client opts not
 * to wait for the completion of the operation.
 */
public final class McpCancellation {
    private static final System.Logger LOGGER = System.getLogger(McpServerFeature.class.getName());
    private final Runnable noop = () -> {};
    private volatile McpCancellationResult result;
    private LazyValue<Runnable> hook = LazyValue.create(() -> noop);

    McpCancellation() {
        result = new McpCancellationResult(false, "No cancellation requested");
    }

    /**
     * Check whether a cancellation request was made.
     *
     * @return cancellation result
     */
    public McpCancellationResult result() {
        return result;
    }

    /**
     * Actions to be performed when cancellation get triggered.
     *
     * @param hook cancellation hook
     */
    public void registerCancellationHook(Runnable hook) {
        this.hook = LazyValue.create(() -> hook);
    }

    /**
     * Cancel the current operation. This method can be triggered only once and
     * additional call are ignored.
     *
     * @param reason cancellation reason
     * @param requestId request ID to be cancelled
     */
    void cancel(String reason, JsonValue requestId) {
        if (!hook.isLoaded()) {
            LOGGER.log(Level.DEBUG, "Cancelling task with request id: %s", requestId);
            result = new McpCancellationResult(true, reason);
            hook.get().run();
        }
    }
}
