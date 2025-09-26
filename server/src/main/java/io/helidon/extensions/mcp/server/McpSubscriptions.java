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

import java.util.Optional;

import io.helidon.http.sse.SseEvent;
import io.helidon.webserver.sse.SseSink;

import jakarta.json.JsonObject;

/**
 * Subscriptions feature used to send resource update notifications
 * to subscribed clients.
 */
public final class McpSubscriptions extends McpFeature {

    McpSubscriptions(McpSession session) {
        super(session);
    }

    /**
     * Resource update for subscribers in all active sessions.
     *
     * @param uri the resource URI
     */
    public void sendUpdate(String uri) {
        McpSessions sessions = session().sessions();
        for (McpSession session : sessions) {
            sendNotification(session, uri);
        }
    }

    /**
     * Resource update for a subscriber in the current session. There can only
     * be a single subscriber per session/resource.
     *
     * @param uri the resource URI
     */
    public void sendSessionUpdate(String uri) {
        sendNotification(session(), uri);
    }

    /**
     * Resource update for a subscriber in the given session.
     *
     * @param session the session
     * @param uri resource URI
     */
    private void sendNotification(McpSession session, String uri) {
        if (session.hasSubscription(uri)) {
            Optional<SseSink> sseSink = session.findSubscription(uri);
            JsonObject notification = McpJsonRpc.createUpdateNotification(uri);
            SseEvent event = SseEvent.builder()
                    .name("message")
                    .data(notification)
                    .build();
            if (sseSink.isPresent()) {
                sseSink.get().emit(event);
            } else {
                session().send(notification);
            }
        }
    }
}
