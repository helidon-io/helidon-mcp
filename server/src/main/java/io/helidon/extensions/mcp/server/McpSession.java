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

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import io.helidon.common.UncheckedException;
import io.helidon.http.Status;
import io.helidon.webserver.jsonrpc.JsonRpcResponse;

import jakarta.json.JsonObject;

import static io.helidon.extensions.mcp.server.McpSession.State.INITIALIZED;
import static io.helidon.extensions.mcp.server.McpSession.State.UNINITIALIZED;

class McpSession {
    private final Set<McpCapability> capabilities;
    private final AtomicBoolean active = new AtomicBoolean(true);
    private final BlockingQueue<JsonObject> queue = new LinkedBlockingQueue<>();

    private volatile McpFeatures features;
    private volatile State state = UNINITIALIZED;

    McpSession(Set<McpCapability> capabilities) {
        this.capabilities = capabilities;
    }

    void poll(Consumer<JsonObject> consumer) {
        while (active.get()) {
            try {
                JsonObject message = queue.take();
                if (message.getBoolean("disconnect", false)) {
                    break;
                }
                consumer.accept(message);
            } catch (InterruptedException e) {
                throw new McpException("Session interrupted.", e);
            }
        }
    }

    void send(JsonObject message) {
        try {
            queue.put(message);
        } catch (InterruptedException e) {
            throw new UncheckedException(e);
        }
    }

    void send(JsonRpcResponse response) {
        send(response.status(Status.ACCEPTED_202).asJsonObject());
    }

    void disconnect() {
        if (active.compareAndSet(true, false)) {
            queue.add(McpJsonRpc.disconnectSession());
        }
    }

    void capabilities(McpCapability capability) {
        capabilities.add(capability);
    }

    McpFeatures features() {
        return features;
    }

    State state() {
        return state;
    }

    void state(State state) {
        this.state = state;
        if (state == INITIALIZED) {
            this.features = new McpFeatures(this, capabilities);
        }
    }

    enum State {
        INITIALIZED,
        INITIALIZING,
        UNINITIALIZED
    }
}
