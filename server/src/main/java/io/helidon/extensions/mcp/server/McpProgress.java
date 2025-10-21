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
import java.util.Objects;

import io.helidon.http.sse.SseEvent;
import io.helidon.webserver.sse.SseSink;

import static io.helidon.extensions.mcp.server.McpJsonRpc.toJson;

/**
 * Progress notification to the client.
 */
public final class McpProgress extends McpFeature {
    private static final System.Logger LOGGER = System.getLogger(McpProgress.class.getName());

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
        sendProgress(progress, null);
    }

    /**
     * Send a progress notification with a message to the client. Ignores the message
     * if using an older specification that does support it.
     *
     * @param progress the progress
     * @param message the notification
     */
    public void send(int progress, String message) {
        Objects.requireNonNull(message, "message is null");
        String protocolVersion = session().protocolVersion();
        if (protocolVersion.startsWith("2024")) {
            if (LOGGER.isLoggable(Level.DEBUG)) {
                LOGGER.log(Level.DEBUG, () -> "Ignoring message with protocol version " + protocolVersion);
            }
            sendProgress(progress, null);
        } else {
            sendProgress(progress, message);
        }
    }

    void sendProgress(int progress, String message) {
        if (progress > total) {
            return;
        }
        if (isSending) {
            if (sseSink().isPresent()) {
                sseSink().get().emit(SseEvent.builder()
                                             .name("message")
                                             .data(toJson(this, progress, message))
                                             .build());
            } else {
                session().send(toJson(this, progress, message));
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
