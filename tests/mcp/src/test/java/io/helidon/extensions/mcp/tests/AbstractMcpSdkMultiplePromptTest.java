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

import io.helidon.common.media.type.MediaTypes;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

abstract class AbstractMcpSdkMultiplePromptTest extends AbstractMcpSdkTest {

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        MultiplePrompt.setUpRoute(builder);
    }

    @Test
    void testListPrompts() {
        McpSchema.ListPromptsResult result = client().listPrompts();
        assertThat(result.prompts().size(), is(5));
    }

    @Test
    void testPrompt1() {
        McpSchema.GetPromptResult prompt1 = client().getPrompt(new McpSchema.GetPromptRequest("prompt1", Map.of()));
        assertThat(prompt1.messages().size(), is(1));

        McpSchema.PromptMessage message = prompt1.messages().getFirst();
        assertThat(message.content().type(), is("text"));
        assertThat(message.role(), is(McpSchema.Role.USER));

        McpSchema.TextContent text = (McpSchema.TextContent) message.content();
        assertThat(text.text(), is("text"));
    }

    @Test
    void testPrompt2() {
        McpSchema.GetPromptResult prompt2 = client().getPrompt(new McpSchema.GetPromptRequest("prompt2", Map.of()));
        assertThat(prompt2.messages().size(), is(1));

        McpSchema.PromptMessage message = prompt2.messages().getFirst();
        assertThat(message.content().type(), is("image"));
        assertThat(message.role(), is(McpSchema.Role.ASSISTANT));

        var image = (McpSchema.ImageContent) message.content();
        assertThat(image.data(), is(McpMedia.base64Media("helidon.png")));
        assertThat(image.mimeType(), is(McpMedia.IMAGE_PNG_VALUE));
    }

    @Test
    void testPrompt3() {
        McpSchema.GetPromptResult prompt3 = client().getPrompt(new McpSchema.GetPromptRequest("prompt3", Map.of()));
        assertThat(prompt3.messages().size(), is(1));

        McpSchema.PromptMessage message = prompt3.messages().getFirst();
        McpSchema.Content first = message.content();
        assertThat(first.type(), is("resource"));
        assertThat(message.role(), is(McpSchema.Role.ASSISTANT));

        var resource = (McpSchema.EmbeddedResource) first;
        var text = (McpSchema.TextResourceContents) resource.resource();
        assertThat(text.text(), is("resource"));
        assertThat(text.uri(), is("http://resource"));
        assertThat(text.mimeType(), is(MediaTypes.TEXT_PLAIN_VALUE));
    }

    @Test
    void testPrompt4() {
        McpSchema.GetPromptResult prompt4 = client().getPrompt(new McpSchema.GetPromptRequest("prompt4",
                                                                                            Map.of("argument1", "text")));
        assertThat(prompt4.messages().size(), is(3));

        McpSchema.Content first = prompt4.messages().getFirst().content();
        McpSchema.Content second = prompt4.messages().get(1).content();
        McpSchema.Content third = prompt4.messages().get(2).content();
        assertThat(first.type(), is("image"));
        assertThat(second.type(), is("text"));
        assertThat(third.type(), is("resource"));

        McpSchema.ImageContent image = (McpSchema.ImageContent) first;
        McpSchema.TextContent text = (McpSchema.TextContent) second;
        McpSchema.EmbeddedResource resource = (McpSchema.EmbeddedResource) third;
        assertThat(text.text(), is("text"));
        assertThat(image.data(), is(McpMedia.base64Media("helidon.png")));
        assertThat(image.mimeType(), is(McpMedia.IMAGE_PNG_VALUE));;
        assertThat(resource.resource().uri(), is("http://resource"));
        assertThat(resource.resource().mimeType(), is(MediaTypes.TEXT_PLAIN_VALUE));
    }
}
