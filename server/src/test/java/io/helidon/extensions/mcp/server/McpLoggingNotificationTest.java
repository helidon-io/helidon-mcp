/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
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

import java.util.Map;

import io.helidon.json.JsonObject;
import io.helidon.json.binding.Json;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class McpLoggingNotificationTest {

    @ParameterizedTest
    @EnumSource(McpProtocolVersion.class)
    void testLoggingNotificationStringData(McpProtocolVersion version) {
        McpJsonSerializer mjs = McpJsonSerializer.create(version);
        JsonObject payload = mjs.createLoggingNotification(McpLogger.Level.INFO, "helidon-logger", "Logging data");

        JsonObject params = payload.objectValue("params").orElseThrow();
        assertThat(payload.stringValue("method").orElseThrow(), is("notifications/message"));
        assertThat(params.stringValue("level").orElseThrow(), is("info"));
        assertThat(params.stringValue("logger").orElseThrow(), is("helidon-logger"));
        assertThat(params.stringValue("data").orElseThrow(), is("Logging data"));
    }

    @ParameterizedTest
    @EnumSource(McpProtocolVersion.class)
    void testLoggingNotificationObjectData(McpProtocolVersion version) {
        McpJsonSerializer mjs = McpJsonSerializer.create(version);
        JsonObject payload = mjs.createLoggingNotification(McpLogger.Level.INFO,
                                                           "helidon-logger",
                                                           Map.of("message", "Logging data", "count", 1));

        JsonObject params = payload.objectValue("params").orElseThrow();
        JsonObject objectData = params.objectValue("data").orElseThrow();
        assertThat(payload.stringValue("method").orElseThrow(), is("notifications/message"));
        assertThat(params.stringValue("level").orElseThrow(), is("info"));
        assertThat(params.stringValue("logger").orElseThrow(), is("helidon-logger"));
        assertThat(objectData.stringValue("message").orElseThrow(), is("Logging data"));
        assertThat(objectData.intValue("count").orElseThrow(), is(1));
    }

    @ParameterizedTest
    @EnumSource(McpProtocolVersion.class)
    void testLoggingNotificationClassData(McpProtocolVersion version) {
        McpJsonSerializer mjs = McpJsonSerializer.create(version);
        JsonObject payload = mjs.createLoggingNotification(McpLogger.Level.INFO,
                                                           "helidon-logger",
                                                           new LoggingData("Logging data", 1));

        JsonObject params = payload.objectValue("params").orElseThrow();
        JsonObject objectData = params.objectValue("data").orElseThrow();
        assertThat(payload.stringValue("method").orElseThrow(), is("notifications/message"));
        assertThat(params.stringValue("level").orElseThrow(), is("info"));
        assertThat(params.stringValue("logger").orElseThrow(), is("helidon-logger"));
        assertThat(objectData.stringValue("message").orElseThrow(), is("Logging data"));
        assertThat(objectData.intValue("count").orElseThrow(), is(1));
    }

    @ParameterizedTest
    @EnumSource(McpProtocolVersion.class)
    void testLoggingNotificationJsonValueData(McpProtocolVersion version) {
        McpJsonSerializer mjs = McpJsonSerializer.create(version);
        JsonObject payload = mjs.createLoggingNotification(McpLogger.Level.INFO,
                                                           "helidon-logger",
                                                           JsonObject.builder()
                                                                   .set("message", "Logging data")
                                                                   .set("count", 1)
                                                                   .build());

        JsonObject params = payload.objectValue("params").orElseThrow();
        JsonObject objectData = params.objectValue("data").orElseThrow();
        assertThat(payload.stringValue("method").orElseThrow(), is("notifications/message"));
        assertThat(params.stringValue("level").orElseThrow(), is("info"));
        assertThat(params.stringValue("logger").orElseThrow(), is("helidon-logger"));
        assertThat(objectData.stringValue("message").orElseThrow(), is("Logging data"));
        assertThat(objectData.intValue("count").orElseThrow(), is(1));
    }

    /**
     * Logging data object.
     *
     * @param message message
     * @param count count
     */
    @Json.Entity
    record LoggingData(String message, int count) {
    }
}
