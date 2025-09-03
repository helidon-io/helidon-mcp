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
 * Mcp logger to send notification to the client.
 */
public final class McpLogger {
    private final String name;
    private final McpSession session;
    private Level level;

    McpLogger(McpSession session) {
        this.session = session;
        this.level = Level.INFO;
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
        if (level.ordinal() >= this.level.ordinal()) {
            session.send(McpJsonRpc.createLoggingNotification(level, name, message));
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
     * Send an info notification to the client.
     *
     * @param message notification
     */
    public void info(String message) {
        log(Level.INFO, message);
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
     * Send a warning notification to the client.
     *
     * @param message notification
     */
    public void warn(String message) {
        log(Level.WARNING, message);
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
     * Send a critical notification to the client.
     *
     * @param message notification
     */
    public void critical(String message) {
        log(Level.CRITICAL, message);
    }

    /**
     * Send an alert notification to the client.
     *
     * @param message notification
     */
    public void alert(String message) {
        log(Level.ALERT, message);
    }

    void setLevel(McpLogger.Level level) {
        this.level = level;
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
        ALERT;

        String text() {
            return this.name().toLowerCase();
        }
    }
}
