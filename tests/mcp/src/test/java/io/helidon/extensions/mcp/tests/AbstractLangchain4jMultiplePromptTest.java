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

import java.util.List;
import java.util.Map;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.McpEmbeddedResource;
import dev.langchain4j.mcp.client.McpGetPromptResult;
import dev.langchain4j.mcp.client.McpImageContent;
import dev.langchain4j.mcp.client.McpPrompt;
import dev.langchain4j.mcp.client.McpPromptMessage;
import dev.langchain4j.mcp.client.McpRole;
import dev.langchain4j.mcp.client.McpTextContent;
import dev.langchain4j.mcp.client.McpTextResourceContents;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

abstract class AbstractLangchain4jMultiplePromptTest {
    protected static McpClient client;

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        MultiplePrompt.setUpRoute(builder);
    }

    @AfterAll
    static void closeClient() throws Exception {
        client.close();
    }

    @Test
    void listPrompts() {
        List<McpPrompt> prompts = client.listPrompts();

        assertThat(prompts.size(), is(4));
    }

    @Test
    void testPrompt1() {
        McpGetPromptResult prompt = client.getPrompt("prompt1", Map.of());
        assertThat(prompt.description(), is("Prompt 1"));

        List<McpPromptMessage> messages = prompt.messages();
        assertThat(messages.size(), is(1));

        McpPromptMessage message = messages.getFirst();
        assertThat(message.role(), is(McpRole.USER));

        McpTextContent content = (McpTextContent) message.content();
        assertThat(content.text(), is("text"));
    }

    @Test
    void testPrompt2() {
        McpGetPromptResult prompt = client.getPrompt("prompt2", Map.of());
        assertThat(prompt.description(), is("Prompt 2"));

        List<McpPromptMessage> messages = prompt.messages();
        assertThat(messages.size(), is(1));

        McpPromptMessage message = messages.getFirst();
        assertThat(message.role(), is(McpRole.ASSISTANT));

        McpImageContent content = (McpImageContent) message.content();
        assertThat(content.data(), is("binary"));
        assertThat(content.mimeType(), is(MediaTypes.APPLICATION_OCTET_STREAM_VALUE));
    }

    @Test
    void testPrompt3() {
        McpGetPromptResult prompt = client.getPrompt("prompt3", Map.of());
        assertThat(prompt.description(), is("Prompt 3"));

        List<McpPromptMessage> messages = prompt.messages();
        assertThat(messages.size(), is(1));

        McpPromptMessage message = messages.getFirst();
        assertThat(message.role(), is(McpRole.ASSISTANT));

        McpEmbeddedResource content = (McpEmbeddedResource) message.content();
        McpTextResourceContents resource = (McpTextResourceContents) content.resource();
        assertThat(resource.text(), is("resource"));
        assertThat(resource.mimeType(), is(MediaTypes.TEXT_PLAIN_VALUE));
    }

    @Test
    void testPrompt4() {
        McpGetPromptResult prompt = client.getPrompt("prompt4", Map.of("argument1", "text"));
        assertThat(prompt.description(), is("Prompt 4"));

        List<McpPromptMessage> messages = prompt.messages();
        assertThat(messages.size(), is(3));

        McpPromptMessage first = messages.getFirst();
        McpPromptMessage second = messages.get(1);
        McpPromptMessage third = messages.get(2);
        assertThat(first.role(), is(McpRole.USER));
        assertThat(second.role(), is(McpRole.USER));
        assertThat(third.role(), is(McpRole.USER));

        McpImageContent image = (McpImageContent) first.content();
        assertThat(image.data(), is("binary"));
        assertThat(image.mimeType(), is(MediaTypes.APPLICATION_OCTET_STREAM_VALUE));

        McpTextContent text = (McpTextContent) second.content();
        assertThat(text.text(), is("text"));

        McpEmbeddedResource resource = (McpEmbeddedResource) third.content();
        McpTextResourceContents content = (McpTextResourceContents) resource.resource();
        assertThat(content.text(), is("resource"));
        assertThat(content.mimeType(), is(MediaTypes.TEXT_PLAIN_VALUE));
    }
}
