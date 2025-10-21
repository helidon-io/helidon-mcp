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
 * MCP logger sends notification to the client.
 */
public final class McpLogger extends McpFeature {
    private final String name;

    McpLogger(McpSession session) {
        super(session);
        this.name = "helidon-logger";
    }

    McpLogger(McpSession session, SseSink sseSink) {
        super(session, sseSink);
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

        if (level.ordinal() >= level().ordinal()) {
            if (sseSink().isPresent()) {
                sseSink().get().emit(SseEvent.builder()
                                     .name("message")
                                     .data(McpJsonRpc.createLoggingNotification(level, name, message))
                                     .build());
            } else {
                session().send(McpJsonRpc.createLoggingNotification(level, name, message));
            }
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

    /**
     * Send an emergency notification to the client.
     *
     * @param message notification
     */
    public void emergency(String message) {
        log(Level.EMERGENCY, message);
    }

    /**
     * Get level for this logger.
     *
     * @return the level
     */
    McpLogger.Level level() {
        return context().get(ContextClassifier.class, Level.class).orElse(Level.INFO);
    }

    /**
     * Set level on the session since there could be multiple instances of this
     * class with streamable HTTP.
     *
     * @param level the level
     */
    void setLevel(McpLogger.Level level) {
        context().register(ContextClassifier.class, level);
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
