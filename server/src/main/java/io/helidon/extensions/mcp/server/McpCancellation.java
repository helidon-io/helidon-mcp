/*
 * Copyright (c) 2025, 2026 Oracle and/or its affiliates.
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.helidon.json.JsonValue;

/**
 * The MCP Cancellation feature enables verification of whether a client
 * has issued a cancellation request. Such requests are typically made when
 * a process is taking an extended amount of time, and the client opts not
 * to wait for the completion of the operation.
 */
public final class McpCancellation {
    private static final System.Logger LOGGER = System.getLogger(McpCancellation.class.getName());

    private final Lock lock = new ReentrantLock();
    private final List<Runnable> hooks = new ArrayList<>();
    private volatile McpCancellationResult result;

    McpCancellation() {
        result = new McpCancellationResultImpl(false);
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
     * Register an action to perform when cancellation is triggered. Each registered hook is invoked once. If cancellation
     * was already requested, the hook is invoked before this method returns.
     *
     * @param hook cancellation hook
     * @throws NullPointerException if the hook is {@code null}
     */
    public void registerCancellationHook(Runnable hook) {
        Objects.requireNonNull(hook, "hook is null");
        boolean runNow;
        lock.lock();
        try {
            runNow = result.isRequested();
            if (!runNow) {
                hooks.add(hook);
            }
        } finally {
            lock.unlock();
        }
        if (runNow) {
            runHook(hook);
        }
    }

    void unregisterCancellationHook(Runnable hook) {
        Objects.requireNonNull(hook, "hook is null");
        lock.lock();
        try {
            hooks.remove(hook);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Cancel the current operation. This method can be triggered only once and
     * additional calls are ignored.
     *
     * @param reason cancellation reason
     * @param requestId request ID to be canceled
     */
    void cancel(String reason, JsonValue requestId) {
        cancel(new McpCancellationResultImpl(true, reason), requestId);
    }

    /**
     * Cancel the current operation without a reason. This method can be triggered only once and
     * additional calls are ignored.
     *
     * @param requestId request ID to be canceled
     */
    void cancel(JsonValue requestId) {
        cancel(new McpCancellationResultImpl(true), requestId);
    }

    private void cancel(McpCancellationResult cancellationResult, JsonValue requestId) {
        List<Runnable> registeredHooks;
        lock.lock();
        try {
            if (result.isRequested()) {
                return;
            }
            result = cancellationResult;
            registeredHooks = List.copyOf(hooks);
            hooks.clear();
        } finally {
            lock.unlock();
        }
        if (LOGGER.isLoggable(Level.DEBUG)) {
            LOGGER.log(Level.DEBUG, "Cancelling task with request id: %s", requestId);
        }
        for (Runnable hook : registeredHooks) {
            runHook(hook);
        }
    }

    private static void runHook(Runnable hook) {
        try {
            hook.run();
        } catch (RuntimeException e) {
            LOGGER.log(Level.WARNING, "Cancellation hook failed", e);
        }
    }
}
