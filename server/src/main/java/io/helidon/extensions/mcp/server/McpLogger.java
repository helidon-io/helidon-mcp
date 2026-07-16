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

import java.util.Objects;

import io.helidon.json.binding.Json;

/**
 * MCP logger sends notification to the client.
 */
public final class McpLogger extends McpFeature {
    private final String name;
    private final McpSession session;

    McpLogger(McpSession session, McpTransport transport) {
        super(session, transport);
        this.session = session;
        this.name = "helidon-logger";
    }

    /**
     * Send a notification to the client with provided message and logging level.
     *
     * @param level   notification level
     * @param message notification
     */
    public void log(Level level, String message) {
        Objects.requireNonNull(level, "level must not be null");
        Objects.requireNonNull(message, "message must not be null");

        sendNotification(level, message);
    }

    /**
     * Send a notification to the client with provided data and logging level.
     * Data is serialized using Helidon JSON binding. Custom types must have a Helidon JSON converter,
     * for example by annotating the type with {@link Json.Entity} and enabling Helidon JSON code generation.
     *
     * @param level notification level
     * @param data  notification data
     */
    public void log(Level level, Object data) {
        Objects.requireNonNull(level, "level must not be null");
        Objects.requireNonNull(data, "data must not be null");

        sendNotification(level, data);
    }

    private void sendNotification(Level level, Object data) {
        if (level.ordinal() >= level().ordinal()) {
            var notification = session.serializer().createLoggingNotification(level, name, data);
            transport().send(notification);
        }
    }

    /**
     * Send a debug notification to the client.
     *
     * @param message notification
     */
    public void debug(String message) {
        log(Level.DEBUG, message);
    }

    /**
     * Send a debug notification to the client.
     *
     * @param data notification data
     */
    public void debug(Object data) {
        log(Level.DEBUG, data);
    }

    /**
     * Send an info notification to the client.
     *
     * @param message notification
     */
    public void info(String message) {
        log(Level.INFO, message);
    }

    /**
     * Send an info notification to the client.
     *
     * @param data notification data
     */
    public void info(Object data) {
        log(Level.INFO, data);
    }

    /**
     * Send a notice notification to the client.
     *
     * @param message notification
     */
    public void notice(String message) {
        log(Level.NOTICE, message);
    }

    /**
     * Send a notice notification to the client.
     *
     * @param data notification data
     */
    public void notice(Object data) {
        log(Level.NOTICE, data);
    }

    /**
     * Send a warning notification to the client.
     *
     * @param message notification
     */
    public void warn(String message) {
        log(Level.WARNING, message);
    }

    /**
     * Send a warning notification to the client.
     *
     * @param data notification data
     */
    public void warn(Object data) {
        log(Level.WARNING, data);
    }

    /**
     * Send an error notification to the client.
     *
     * @param message notification
     */
    public void error(String message) {
        log(Level.ERROR, message);
    }

    /**
     * Send an error notification to the client.
     *
     * @param data notification data
     */
    public void error(Object data) {
        log(Level.ERROR, data);
    }

    /**
     * Send a critical notification to the client.
     *
     * @param message notification
     */
    public void critical(String message) {
        log(Level.CRITICAL, message);
    }

    /**
     * Send a critical notification to the client.
     *
     * @param data notification data
     */
    public void critical(Object data) {
        log(Level.CRITICAL, data);
    }

    /**
     * Send an alert notification to the client.
     *
     * @param message notification
     */
    public void alert(String message) {
        log(Level.ALERT, message);
    }

    /**
     * Send an alert notification to the client.
     *
     * @param data notification data
     */
    public void alert(Object data) {
        log(Level.ALERT, data);
    }

    /**
     * Send an emergency notification to the client.
     *
     * @param message notification
     */
    public void emergency(String message) {
        log(Level.EMERGENCY, message);
    }

    /**
     * Send an emergency notification to the client.
     *
     * @param data notification data
     */
    public void emergency(Object data) {
        log(Level.EMERGENCY, data);
    }

    /**
     * Get level for this logger.
     *
     * @return the level
     */
    McpLogger.Level level() {
        return session.context().get(ContextClassifier.class, Level.class).orElse(Level.INFO);
    }

    /**
     * Set level on the session since there could be multiple instances of this
     * class with streamable HTTP.
     *
     * @param level the level
     */
    void setLevel(McpLogger.Level level) {
        session.context().register(ContextClassifier.class, level);
    }

    /**
     * Logger log levels.
     */
    public enum Level {
        /**
         * Debug.
         */
        DEBUG,
        /**
         * Info.
         */
        INFO,
        /**
         * Notice.
         */
        NOTICE,
        /**
         * Warning.
         */
        WARNING,
        /**
         * Error.
         */
        ERROR,
        /**
         * Critical.
         */
        CRITICAL,
        /**
         * Alert.
         */
        ALERT,
        /**
         * Emergency.
         */
        EMERGENCY;

        String text() {
            return this.name().toLowerCase();
        }
    }

    /**
     * Must be private.
     */
    private static class ContextClassifier {
    }
}
