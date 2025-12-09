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

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.json.JsonValue;

/**
 * Subscriptions feature used to send resource update notifications
 * to subscribed clients.
 */
public final class McpSubscriptions extends McpFeature {
    private static final System.Logger LOGGER = System.getLogger(McpSubscriptions.class.getName());
    private final Duration timeout;
    private final Map<String, McpTransport> subscriptions;

    McpSubscriptions(McpSession session) {
        super(session, null);
        this.subscriptions = new ConcurrentHashMap<>();
        this.timeout = session.context()
                .get(McpServerConfigBlueprint.class, McpServerConfig.class)
                .orElseThrow(() -> new McpInternalException("MCP server configuration not found"))
                .subscriptionTimeout();
    }

    /**
     * Resource update for subscribers in all active sessions.
     *
     * @param uri the resource URI
     */
    public void sendUpdate(String uri) {
        McpSessions sessions = session().sessions();
        for (McpSession session : sessions) {
            session.features().subscriptions().sendSessionUpdate(uri);
        }
    }

    /**
     * Resource update for a subscriber in the current session. There can only
     * be a single subscriber per session/resource.
     *
     * @param uri the resource URI
     */
    public void sendSessionUpdate(String uri) {
        if (subscriptions.containsKey(uri)) {
            var notification = session().serializer().createUpdateNotification(uri);
            subscriptions.get(uri).send(notification);
        }
    }

    void subscribe(JsonValue id, String uri) {
        if (subscriptions.get(uri) != null) {
            if (LOGGER.isLoggable(System.Logger.Level.DEBUG)) {
                LOGGER.log(System.Logger.Level.DEBUG, "Found existing subscription for " + uri);
            }
            return;
        }
        McpTransport transport = session().transport(id).orElseThrow(() -> new McpInternalException("Transport not found"));
        subscriptions.putIfAbsent(uri, transport);
        if (LOGGER.isLoggable(System.Logger.Level.DEBUG)) {
            LOGGER.log(System.Logger.Level.DEBUG, "New subscription for " + uri);
        }
    }

    void unsubscribe(String uri) {
        McpTransport transport = subscriptions.remove(uri);
        if (transport == null) {
            if (LOGGER.isLoggable(System.Logger.Level.DEBUG)) {
                LOGGER.log(System.Logger.Level.DEBUG, "No subscription found for " + uri);
            }
            return;
        }
        transport.unblock();
        if (LOGGER.isLoggable(System.Logger.Level.DEBUG)) {
            LOGGER.log(System.Logger.Level.DEBUG, "Removed subscription for " + uri);
        }
    }

    void blockSubscribe(String uri) {
        if (subscriptions.containsKey(uri)) {
            subscriptions.get(uri).block(timeout);
        }
    }

    static final class McpSubscriptionsQualifier {
    }
}
