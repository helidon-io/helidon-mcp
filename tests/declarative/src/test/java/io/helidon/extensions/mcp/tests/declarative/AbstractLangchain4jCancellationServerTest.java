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

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.McpGetPromptResult;
import dev.langchain4j.mcp.client.McpReadResourceResult;
import dev.langchain4j.mcp.client.McpTextContent;
import dev.langchain4j.mcp.client.McpTextResourceContents;
import dev.langchain4j.service.tool.ToolExecutionResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

abstract class AbstractLangchain4jCancellationServerTest {
    protected static McpClient client;

    @AfterAll
    static void afterAll() throws Exception {
        if (client != null) {
            client.close();
        }
    }

    @Test
    void testCancellationTool() {
        ToolExecutionResult result = client.executeTool(ToolExecutionRequest.builder()
                                                   .name("cancellationTool")
                                                   .build());
        assertThat(result.resultText(), is("No cancellation requested"));
    }

    @Test
    void testCancellationTool1() {
        ToolExecutionResult result = client.executeTool(ToolExecutionRequest.builder()
                                                   .name("cancellationTool1")
                                                   .build());
        assertThat(result.resultText(), is("No cancellation requested"));
    }

    @Test
    void testCancellationPrompt() {
        McpGetPromptResult result = client.getPrompt("cancellationPrompt", Map.of());
        McpTextContent text = (McpTextContent) result.messages().getFirst().content();
        assertThat(text.text(), is("No cancellation requested"));
    }

    @Test
    void testCancellationPrompt1() {
        McpGetPromptResult result = client.getPrompt("cancellationPrompt1", Map.of());
        McpTextContent text = (McpTextContent) result.messages().getFirst().content();
        assertThat(text.text(), is("No cancellation requested"));
    }

    @Test
    void testCancellationResource() {
        McpReadResourceResult result = client.readResource("file://cancellation");
        McpTextResourceContents text = (McpTextResourceContents) result.contents().getFirst();
        assertThat(text.text(), is("No cancellation requested"));
    }

    @Test
    void testCancellationResource1() {
        McpReadResourceResult result = client.readResource("file://cancellation1");
        McpTextResourceContents text = (McpTextResourceContents) result.contents().getFirst();
        assertThat(text.text(), is("No cancellation requested"));
    }
}
