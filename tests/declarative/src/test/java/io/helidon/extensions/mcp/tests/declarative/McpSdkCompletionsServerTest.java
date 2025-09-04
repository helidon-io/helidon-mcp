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
class McpSdkCompletionsServerTest {
    private static McpSyncClient client;

    McpSdkCompletionsServerTest(WebServer server) {
        client = McpClient.sync(HttpClientSseClientTransport.builder("http://localhost:" + server.port())
                                        .sseEndpoint("/completions")
                                        .build())
                .capabilities(McpSchema.ClientCapabilities.builder().build())
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
    @ValueSource(strings = {
            "prompt1", "prompt2", "prompt3", "prompt4", "prompt5",
            "prompt6", "prompt7", "prompt8", "prompt9", "prompt10",
            "prompt11", "prompt12", "prompt13", "prompt14",
    })
    void testCompletionPrompt(String name) {
        McpSchema.CompleteResult.CompleteCompletion result = client.completeCompletion(new McpSchema.CompleteRequest(
                        new McpSchema.PromptReference(name),
                        new McpSchema.CompleteRequest.CompleteArgument("argument", name)))
                .completion();

        assertThat(result.hasMore(), is(false));
        assertThat(result.total(), is(1));
        assertThat(result.values().getFirst(), is(name));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "resource/{path1}", "resource/{path2}", "resource/{path3}",
            "resource/{path4}", "resource/{path5}", "resource/{path6}",
            "resource/{path7}", "resource/{path8}", "resource/{path9}",
            "resource/{path10}"
    })
    void testCompletionResource(String name) {
        String argument = name.substring(10, name.length() - 1);
        McpSchema.CompleteResult.CompleteCompletion result = client.completeCompletion(new McpSchema.CompleteRequest(
                        new McpSchema.ResourceReference(name),
                        new McpSchema.CompleteRequest.CompleteArgument(argument, argument)))
                .completion();

        assertThat(result.hasMore(), is(false));
        assertThat(result.total(), is(1));
        assertThat(result.values().getFirst(), is(argument));
    }
}
