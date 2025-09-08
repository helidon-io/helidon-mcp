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

import io.helidon.http.sse.SseEvent;
import io.helidon.webserver.sse.SseSink;

/**
 * Progress notification to the client.
 */
public final class McpProgress extends McpFeature {

    private int total;
    private int tokenInt;
    private String token;
    private boolean isSending;

    McpProgress(McpSession session) {
        super(session);
        this.token = "";
    }

    McpProgress(McpSession session, SseSink sseSink) {
        super(session, sseSink);
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
            if (sseSink().isPresent()) {
                sseSink().get().emit(SseEvent.builder()
                                     .name("message")
                                     .data(McpJsonRpc.toJson(this, progress))
                                     .build());
            } else {
                session().send(McpJsonRpc.toJson(this, progress));
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
