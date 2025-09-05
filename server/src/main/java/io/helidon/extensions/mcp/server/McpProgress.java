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

import java.util.Objects;

import io.helidon.http.sse.SseEvent;
import io.helidon.webserver.sse.SseSink;

/**
 * Progress notification to the client.
 */
public final class McpProgress {
    private final McpSession session;
    private final SseSink sseSink;

    private int total;
    private int tokenInt;
    private String token;
    private boolean isSending;

    McpProgress(McpSession session) {
        Objects.requireNonNull(session, "session is null");
        this.session = session;
        this.sseSink = null;
        this.token = "";
    }

    McpProgress(SseSink sseSink) {
        Objects.requireNonNull(sseSink, "sseSink is null");
        this.session = null;
        this.sseSink = sseSink;
        this.token = "";
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
        if (progress > total) {
            return;
        }
        if (isSending) {
            if (sseSink != null) {
                sseSink.emit(SseEvent.builder()
                                     .name("message")
                                     .data(McpJsonRpc.toJson(this, progress))
                                     .build());
            } else if (session != null) {
                session.send(McpJsonRpc.toJson(this, progress));
            } else {
                throw new IllegalStateException("Session and sink are null");
            }
        }
        if (progress >= total) {
            isSending = false;
        }
    }

    void token(String token) {
        this.token = token;
        isSending = true;
    }

    void token(int token) {
        this.tokenInt = token;
        isSending = true;
    }

    String token() {
        return token;
    }

    int tokenInt() {
        return tokenInt;
    }

    int total() {
        return total;
    }

    void stopSending() {
        token = "";
        isSending = false;
    }
}
