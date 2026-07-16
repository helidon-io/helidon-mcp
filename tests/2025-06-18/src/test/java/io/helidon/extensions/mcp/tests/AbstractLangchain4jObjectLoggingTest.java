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

package io.helidon.extensions.mcp.tests;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.helidon.extensions.mcp.tests.common.LoggingNotifications;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.logging.McpLogLevel;
import dev.langchain4j.mcp.client.logging.McpLogMessage;
import dev.langchain4j.mcp.client.logging.McpLogMessageHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

abstract class AbstractLangchain4jObjectLoggingTest {
    private static final AtomicReference<AssertionError> LOG_MESSAGE_FAILURE = new AtomicReference<>();
    private static volatile CountDownLatch logMessageLatch;

    protected static McpClient client;

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        LoggingNotifications.setUpRoute(builder);
    }

    @AfterAll
    static void closeClient() throws Exception {
        client.close();
    }

    @BeforeEach
    void prepareLogMessageLatch() {
        LOG_MESSAGE_FAILURE.set(null);
        logMessageLatch = new CountDownLatch(1);
    }

    @AfterEach
    void verifyLogMessage() throws InterruptedException {
        assertThat(logMessageLatch.await(20, TimeUnit.SECONDS), is(true));
        AssertionError failure = LOG_MESSAGE_FAILURE.get();
        if (failure != null) {
            throw failure;
        }
    }

    @Test
    void testObjectLoggingData() {
        client.executeTool(ToolExecutionRequest.builder().name("object-logging").build());
    }

    @Test
    void testClassLoggingData() {
        client.executeTool(ToolExecutionRequest.builder().name("class-logging").build());
    }

    protected static class ObjectLogMessageHandler implements McpLogMessageHandler {
        @Override
        public void handleLogMessage(McpLogMessage message) {
            try {
                assertThat(message.level(), is(McpLogLevel.INFO));
                assertThat(message.logger(), is("helidon-logger"));
                assertThat(message.data().get("message").asText(), is("Logging data"));
                assertThat(message.data().get("count").asInt(), is(1));
            } catch (AssertionError e) {
                LOG_MESSAGE_FAILURE.set(e);
            } finally {
                logMessageLatch.countDown();
            }
        }
    }
}
