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
package io.helidon.extensions.mcp.tests.declarative;

import java.time.Duration;
import java.util.List;

import io.helidon.webserver.WebServer;
import io.helidon.webserver.testing.junit5.ServerTest;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ServerTest
class McpSdkNameServerTest {
    private static McpSyncClient client;

    McpSdkNameServerTest(WebServer server) {
        client = McpClient.sync(HttpClientSseClientTransport.builder("http://localhost:" + server.port())
                                        .sseEndpoint("/name")
                                        .build())
                .requestTimeout(Duration.ofSeconds(1))
                .build();
        client.initialize();
    }

    @Test
    void testNamedTool() {
        List<McpSchema.Tool> tools = client.listTools().tools();
        assertThat(tools.size(), is(1));

        McpSchema.Tool tool = tools.getFirst();
        assertThat(tool.name(), is("my-tool"));
    }

    @Test
    void testNamedPrompt() {
        List<McpSchema.Prompt> prompts = client.listPrompts().prompts();
        assertThat(prompts.size(), is(1));

        McpSchema.Prompt prompt = prompts.getFirst();
        assertThat(prompt.name(), is("my-prompt"));
    }

    @Test
    void testNamedResource() {
        List<McpSchema.Resource> resources = client.listResources().resources();
        assertThat(resources.size(), is(1));

        McpSchema.Resource resource = resources.getFirst();
        assertThat(resource.name(), is("my-resource"));
    }
}
