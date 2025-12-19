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

/**
 * Progress notification to the client.
 */
public final class McpProgress extends McpFeature {
    private static final System.Logger LOGGER = System.getLogger(McpProgress.class.getName());

    private final McpSession session;
    private int total;
    private int tokenInt;
    private String token;
    private boolean isSending;

    McpProgress(McpSession session, McpTransport transport) {
        super(session, transport);
        this.session = session;
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
        send(progress, "");
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
        if (progress > total) {
            return;
        }
        if (isSending) {
            var notification = session.serializer().toJson(this, progress, message);
            transport().send(notification);
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

    static class McpProgressListener implements McpFeatureLifecycle {

        @Override
        public void beforeRequest(McpParameters parameters, McpFeatures features) {
            var progressToken = parameters.get("_meta").get("progressToken");
            if (progressToken.isNumber()) {
                features.progress().token(progressToken.asInteger().get());
            }
            if (progressToken.isString()) {
                features.progress().token(progressToken.asString().get());
            }
        }

        @Override
        public void afterRequest(McpParameters parameters, McpFeatures features) {
            features.progress().stopSending();
        }
    }
}
