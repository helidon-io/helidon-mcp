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

package io.helidon.mcp.server;

/**
 * Progress notification to the client.
 */
public final class McpProgress {
    private final McpSession session;
    private final boolean isActive;
    private int total;
    private String token;
    private boolean isSending;

    McpProgress(McpSession session, boolean isActive) {
        this.session = session;
        this.isActive = isActive;
    }

    /**
     * Set total progression amount.
     *
     * @param total total
     */
    public void total(int total) {
        this.total = total;
    }

    /**
     * Send a progress notification to the client.
     *
     * @param progress progress
     */
    public void send(int progress) {
        if (!isActive || progress > total) {
            return;
        }
        if (isSending) {
            session.send(McpJsonRpc.toJson(this, progress));
        }
        if (progress == total) {
            isSending = false;
        }
    }

    void token(String token) {
        this.token = token;
        isSending = true;
    }

    String token() {
        return token;
    }

    int total() {
        return total;
    }

    void stopSending() {
        isSending = false;
    }
}
