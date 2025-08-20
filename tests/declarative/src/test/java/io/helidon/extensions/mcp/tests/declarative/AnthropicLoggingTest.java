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

package io.helidon.extensions.mcp.tests.declarative;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.helidon.webserver.WebServer;
import io.helidon.webserver.testing.junit5.ServerTest;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ServerTest
class AnthropicLoggingTest {
    private static McpSyncClient client;
    private CountDownLatch latch;

    AnthropicLoggingTest(WebServer server) {
        client = McpClient.sync(HttpClientSseClientTransport.builder("http://localhost:" + server.port())
                                        .sseEndpoint("/logging")
                                        .build())
                .loggingConsumer(notification -> {
                    assertThat(notification.data(), is("Logging notification"));
                    assertThat(notification.logger(), is("helidon-logger"));
                    assertThat(notification.level(), is(McpSchema.LoggingLevel.INFO));
                    latch.countDown();
                })
                .requestTimeout(Duration.ofSeconds(1))
                .build();
        client.initialize();
    }

    @AfterAll
    static void afterAll() {
        if (client != null) {
            client.close();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"loggingTool", "loggerTool"})
    void testLoggingTool(String name) throws InterruptedException {
        latch = new CountDownLatch(1);
        var result = client.callTool(new McpSchema.CallToolRequest(name, Map.of()));
        assertThat(latch.await(1, TimeUnit.SECONDS), is(true));

        List<McpSchema.Content> contents = result.content();
        assertThat(contents.size(), is(1));

        McpSchema.TextContent text = (McpSchema.TextContent) contents.getFirst();
        assertThat(text.text(), is("Hello World"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"loggingPrompt", "loggerPrompt"})
    void testLoggingPrompt(String name) throws InterruptedException {
        latch = new CountDownLatch(1);
        var result = client.getPrompt(new McpSchema.GetPromptRequest(name, Map.of()));
        assertThat(latch.await(1, TimeUnit.SECONDS), is(true));

        var contents = result.messages();
        assertThat(contents.size(), is(1));

        var text = (McpSchema.TextContent) contents.getFirst().content();
        assertThat(text.text(), is("Hello World"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"file://hello/world", "file://hello/world1"})
    void testLoggingResource(String uri) throws InterruptedException {
        latch = new CountDownLatch(1);
        var result = client.readResource(new McpSchema.ReadResourceRequest(uri));
        assertThat(latch.await(1, TimeUnit.SECONDS), is(true));

        var contents = result.contents();
        assertThat(contents.size(), is(1));

        var text = (McpSchema.TextResourceContents) contents.getFirst();
        assertThat(text.text(), is("Hello World"));
    }
}
