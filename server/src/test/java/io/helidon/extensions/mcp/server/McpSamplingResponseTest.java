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
package io.helidon.extensions.mcp.server;

import java.nio.charset.StandardCharsets;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.json.JsonObject;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;

class McpSamplingResponseTest {

    @Test
    void testSamplingResponseTextContent() {
        McpSamplingTextContent content = McpSamplingTextContent.create("text");
        McpSamplingMessage message = message(McpRole.USER, content);
        McpSamplingResponse response = new McpSamplingResponseImpl(message, "helidon-model", McpStopReason.END_TURN);

        assertResponse(response, message);
        assertThat(response.message().role(), is(McpRole.USER));
        assertThat(response.asTextContent(), sameInstance(content));
        assertThat(response.asTextContent().text(), is("text"));

        assertThrows(McpSamplingException.class, response::asImageContent);
        assertThrows(McpSamplingException.class, response::asAudioContent);
        assertThrows(McpSamplingException.class, response::asToolUseContent);
    }

    @Test
    void testSamplingResponseImageContent() {
        byte[] data = "data".getBytes(StandardCharsets.UTF_8);
        McpSamplingImageContent content = McpSamplingImageContent.builder()
                .data(data)
                .mediaType(MediaTypes.TEXT_PLAIN)
                .build();
        McpSamplingMessage message = message(McpRole.USER, content);
        McpSamplingResponse response = new McpSamplingResponseImpl(message, "helidon-model", McpStopReason.END_TURN);

        assertResponse(response, message);
        assertThat(response.asImageContent(), sameInstance(content));
        assertThat(response.asImageContent().data(), is(data));

        assertThrows(McpSamplingException.class, response::asTextContent);
        assertThrows(McpSamplingException.class, response::asAudioContent);
        assertThrows(McpSamplingException.class, response::asToolUseContent);
    }

    @Test
    void testSamplingResponseAudioContent() {
        byte[] data = "data".getBytes(StandardCharsets.UTF_8);
        McpSamplingAudioContent content = McpSamplingAudioContent.builder()
                .data(data)
                .mediaType(MediaTypes.TEXT_PLAIN)
                .build();
        McpSamplingMessage message = message(McpRole.USER, content);
        McpSamplingResponse response = new McpSamplingResponseImpl(message, "helidon-model", McpStopReason.END_TURN);

        assertResponse(response, message);
        assertThat(response.asAudioContent(), sameInstance(content));
        assertThat(response.asAudioContent().data(), is(data));

        assertThrows(McpSamplingException.class, response::asTextContent);
        assertThrows(McpSamplingException.class, response::asImageContent);
        assertThrows(McpSamplingException.class, response::asToolUseContent);
    }

    @Test
    void testSamplingResponseToolUseContent() {
        McpSamplingToolUseContent content = McpSamplingToolUseContent.builder()
                .id("call-1")
                .name("weather")
                .input(new McpParameters(JsonObject.empty()))
                .build();
        McpSamplingMessage message = message(McpRole.ASSISTANT, content);
        McpSamplingResponse response = new McpSamplingResponseImpl(message, "helidon-model", McpStopReason.TOOL_USE);

        assertThat(response.model(), is("helidon-model"));
        assertThat(response.rawStopReason().orElseThrow(), is("toolUse"));
        assertThat(response.stopReason().orElseThrow(), is(McpStopReason.TOOL_USE));
        assertThat(response.message(), sameInstance(message));
        assertThat(response.asToolUseContent(), sameInstance(content));

        assertThrows(McpSamplingException.class, response::asTextContent);
        assertThrows(McpSamplingException.class, response::asImageContent);
        assertThrows(McpSamplingException.class, response::asAudioContent);
    }

    @Test
    void testEmptySamplingResponseContent() {
        McpSamplingResponse response = new McpSamplingResponseImpl(McpSamplingMessage.builder()
                                                                          .role(McpRole.ASSISTANT)
                                                                          .build(),
                                                                  "helidon-model");

        assertThrows(McpSamplingException.class, response::asTextContent);
    }

    private static McpSamplingMessage message(McpRole role, McpSamplingContent content) {
        return McpSamplingMessage.builder()
                .role(role)
                .addContent(content)
                .build();
    }

    private static void assertResponse(McpSamplingResponse response, McpSamplingMessage message) {
        assertThat(response.model(), is("helidon-model"));
        assertThat(response.rawStopReason().orElseThrow(), is("endTurn"));
        assertThat(response.stopReason().orElseThrow(), is(McpStopReason.END_TURN));
        assertThat(response.message(), sameInstance(message));
    }
}
