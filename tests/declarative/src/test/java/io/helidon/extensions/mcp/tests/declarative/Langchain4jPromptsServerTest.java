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

import java.util.Map;

import io.helidon.webserver.WebServer;
import io.helidon.webserver.testing.junit5.ServerTest;

import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.McpGetPromptResult;
import dev.langchain4j.mcp.client.McpPromptContent;
import dev.langchain4j.mcp.client.McpRole;
import dev.langchain4j.mcp.client.McpTextContent;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static io.helidon.extensions.mcp.tests.declarative.McpPromptsServer.PROMPT_CONTENT;
import static io.helidon.extensions.mcp.tests.declarative.McpPromptsServer.PROMPT_DESCRIPTION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ServerTest
class Langchain4jPromptsServerTest {
    private static McpClient client;

    Langchain4jPromptsServerTest(WebServer server) {
        McpTransport transport = new HttpMcpTransport.Builder()
                .sseUrl("http://localhost:" + server.port() + "/prompts")
                .logRequests(true)
                .logResponses(true)
                .build();
        client = new DefaultMcpClient.Builder()
                .transport(transport)
                .build();
    }

    @AfterAll
    static void afterAll() throws Exception {
        if (client != null) {
            client.close();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "prompt1", "prompt2", "prompt3",
    })
    void assistantTest(String promptName) {
        runTest(promptName, McpRole.ASSISTANT);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "prompt4", "prompt5", "prompt6",
    })
    void userTest(String promptName) {
        runTest(promptName, McpRole.USER);
    }

    void runTest(String promptName, McpRole role) {
        McpGetPromptResult result = client.getPrompt(promptName, Map.of("prompt", "prompt"));
        assertThat(result.description(), is(PROMPT_DESCRIPTION));

        var messages = result.messages();
        assertThat(messages.size(), is(1));

        var message = messages.getFirst();
        assertThat(message.role(), is(role));

        var text = (McpTextContent) message.content();
        assertThat(text.type(), is(McpPromptContent.Type.TEXT));
        assertThat(text.text(), is(PROMPT_CONTENT));
    }
}
