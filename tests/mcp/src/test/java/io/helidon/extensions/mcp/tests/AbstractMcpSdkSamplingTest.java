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

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static io.modelcontextprotocol.spec.McpSchema.CreateMessageResult.StopReason.STOP_SEQUENCE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

abstract class AbstractMcpSdkSamplingTest extends AbstractMcpSdkTest {
    private static final String SAMPLING_CLIENT_TEXT = "samplingMessage";
    private static final String SAMPLING_ERROR_MESSAGE = "sampling error message";

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        SamplingServer.setUpRoute(builder);
    }

    protected McpSchema.CreateMessageResult samplingHandler(McpSchema.CreateMessageRequest request) {
        var messages = request.messages();
        assertThat(messages.size(), is(1));

        var message = messages.getFirst();
        return switch (message.content().type()) {
            case "text" -> testTextMessage(message);
            case "image" -> testImageMessage(message);
            case "audio" -> testAudioMessage(message);
            default -> throw new IllegalStateException("Wrong sampling message type");
        };
    }

    private McpSchema.CreateMessageResult testAudioMessage(McpSchema.SamplingMessage message) {
        assertThat(message.content(), instanceOf(McpSchema.AudioContent.class));
        assertThat(message.role(), is(McpSchema.Role.USER));

        var audio = (McpSchema.AudioContent) message.content();
        assertThat(decode(audio.data()), is(SAMPLING_CLIENT_TEXT));
        assertThat(audio.mimeType(), is(MediaTypes.TEXT_PLAIN_VALUE));

        var annotations = new McpSchema.Annotations(List.of(), 1.0);
        var result = new McpSchema.AudioContent(annotations, SAMPLING_CLIENT_TEXT, MediaTypes.TEXT_PLAIN_VALUE);
        return new McpSchema.CreateMessageResult(McpSchema.Role.USER,
                                                 result,
                                                 "test-model",
                                                 STOP_SEQUENCE);
    }

    private McpSchema.CreateMessageResult testImageMessage(McpSchema.SamplingMessage message) {
        assertThat(message.content(), instanceOf(McpSchema.ImageContent.class));
        assertThat(message.role(), is(McpSchema.Role.USER));

        var image = (McpSchema.ImageContent) message.content();
        assertThat(decode(image.data()), is(SAMPLING_CLIENT_TEXT));
        assertThat(image.mimeType(), is(MediaTypes.TEXT_PLAIN_VALUE));

        var annotations = new McpSchema.Annotations(List.of(), 1.0);
        var result = new McpSchema.ImageContent(annotations, SAMPLING_CLIENT_TEXT, MediaTypes.TEXT_PLAIN_VALUE);
        return new McpSchema.CreateMessageResult(McpSchema.Role.USER, result, "test-model", STOP_SEQUENCE);
    }

    private McpSchema.CreateMessageResult testTextMessage(McpSchema.SamplingMessage message) {
        assertThat(message.content(), instanceOf(McpSchema.TextContent.class));
        assertThat(message.role(), is(McpSchema.Role.USER));

        var text = (McpSchema.TextContent) message.content();
        var result = new McpSchema.TextContent(SAMPLING_CLIENT_TEXT);

        if ("timeout".equals(text.text())) {
            try {
                TimeUnit.SECONDS.sleep(4);
                return new McpSchema.CreateMessageResult(McpSchema.Role.USER, result, "test-model", STOP_SEQUENCE);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        if ("error".equals(text.text())) {
            throw new RuntimeException(SAMPLING_ERROR_MESSAGE);
        }

        return new McpSchema.CreateMessageResult(McpSchema.Role.USER, result, "test-model", STOP_SEQUENCE);
    }

    private String decode(String data) {
        return new String(Base64.getDecoder().decode(data));
    }

    @ParameterizedTest
    @ValueSource(strings = {"image", "audio"})
    void testContentTypeSamplingTool(String type) {
        var result = client().callTool(McpSchema.CallToolRequest.builder()
                                               .name("sampling-tool")
                                               .arguments(Map.of("type", type))
                                               .build());
        List<McpSchema.Content> contents = result.content();
        assertThat(contents.size(), is(1));

        McpSchema.Content content = contents.getFirst();
        assertThat(content, instanceOf(McpSchema.TextContent.class));

        McpSchema.TextContent textContent = (McpSchema.TextContent) content;
        assertThat(textContent.text(), is(SAMPLING_CLIENT_TEXT));
    }

    @Test
    void testTextContentTypeSamplingTool() {
        var result = client().callTool(McpSchema.CallToolRequest.builder()
                                               .name("sampling-tool")
                                               .arguments(Map.of("type", "text"))
                                               .build());
        List<McpSchema.Content> contents = result.content();
        assertThat(contents.size(), is(1));

        McpSchema.Content content = contents.getFirst();
        assertThat(content, instanceOf(McpSchema.TextContent.class));

        McpSchema.TextContent textContent = (McpSchema.TextContent) content;
        assertThat(textContent.text(), is(SAMPLING_CLIENT_TEXT));
    }

    @Test
    void testEnabledTool() {
        var result = client().callTool(McpSchema.CallToolRequest.builder()
                                               .name("enabled-tool")
                                               .arguments(Map.of("type", "text"))
                                               .build());
        List<McpSchema.Content> contents = result.content();
        assertThat(contents.size(), is(1));

        McpSchema.Content content = contents.getFirst();
        assertThat(content, instanceOf(McpSchema.TextContent.class));

        McpSchema.TextContent textContent = (McpSchema.TextContent) content;
        assertThat(textContent.text(), is(SAMPLING_CLIENT_TEXT));
    }

    @Test
    void testTimeoutTool() {
        var result = client().callTool(McpSchema.CallToolRequest.builder()
                                               .name("timeout-tool")
                                               .arguments(Map.of("type", "text"))
                                               .build());
        assertThat(result.isError(), is(true));

        var contents = result.content();
        assertThat(result.content().size(), is(1));

        McpSchema.Content content = contents.getFirst();
        assertThat(content, instanceOf(McpSchema.TextContent.class));

        McpSchema.TextContent textContent = (McpSchema.TextContent) content;
        assertThat(textContent.text(), is("response timeout"));
    }

    @Test
    void testErrorTool() {
        var result = client().callTool(McpSchema.CallToolRequest.builder()
                                               .name("error-tool")
                                               .arguments(Map.of("type", "text"))
                                               .build());
        assertThat(result.isError(), is(true));

        var contents = result.content();
        assertThat(result.content().size(), is(1));

        McpSchema.Content content = contents.getFirst();
        assertThat(content, instanceOf(McpSchema.TextContent.class));

        McpSchema.TextContent textContent = (McpSchema.TextContent) content;
        assertThat(textContent.text(), is(SAMPLING_ERROR_MESSAGE));
    }
}
