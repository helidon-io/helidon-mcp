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

package io.helidon.extensions.mcp.tests;

import java.util.Map;
import java.util.function.Consumer;

import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import io.modelcontextprotocol.spec.McpSchema;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isOneOf;

abstract class AbstractMcpSdkLoggingTest extends AbstractMcpSdkTest {

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        LoggingNotifications.setUpRoute(builder);
    }

    @Test
    void testMcpSdkLogging() {
        client().callTool(new McpSchema.CallToolRequest("logging", Map.of()));
    }

    protected class LoggingConsumer implements Consumer<McpSchema.LoggingMessageNotification> {

        private final McpSchema.LoggingLevel[] levels;

        LoggingConsumer(McpSchema.LoggingLevel... levels) {
            this.levels = levels;
        }

        @Override
        public void accept(McpSchema.LoggingMessageNotification notification) {
            assertThat(notification.level(), isOneOf(levels));
            assertThat(notification.logger(), Matchers.is("helidon-logger"));
            assertThat(notification.data(), Matchers.is("Logging data"));
            latch().countDown();
        }
    }
}
