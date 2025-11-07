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
package io.helidon.extensions.mcp.server;

import java.nio.charset.StandardCharsets;

import io.helidon.common.media.type.MediaTypes;

import org.junit.jupiter.api.Test;

import static io.helidon.extensions.mcp.server.McpSamplingMessages.audioMessage;
import static io.helidon.extensions.mcp.server.McpSamplingMessages.imageMessage;
import static io.helidon.extensions.mcp.server.McpSamplingMessages.textMessage;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class McpSamplingResponseTest {

    @Test
    void testSamplingResponseTextMessage() {
        var message = textMessage("text", McpRole.USER);
        McpSamplingResponse response = new McpSamplingResponseImpl(message, "helidon-model", McpStopReason.END_TURN);

        assertThat(response.model(), is("helidon-model"));
        assertThat(response.stopReason().isPresent(), is(true));
        assertThat(response.stopReason().get(), is(McpStopReason.END_TURN));
        assertThat(response.message(), instanceOf(McpSamplingTextMessage.class));

        McpSamplingTextMessage text = response.asTextMessage();
        assertThat(text.role(), is(McpRole.USER));
        assertThat(text.text(), is("text"));

        assertThrows(McpSamplingException.class, response::asImageMessage);
        assertThrows(McpSamplingException.class, response::asAudioMessage);
    }

    @Test
    void testSamplingResponseImageMessage() {
        var data = "data".getBytes(StandardCharsets.UTF_8);
        var message = imageMessage(data, MediaTypes.TEXT_PLAIN, McpRole.USER);
        McpSamplingResponse response = new McpSamplingResponseImpl(message, "helidon-model", McpStopReason.END_TURN);

        assertThat(response.model(), is("helidon-model"));
        assertThat(response.stopReason().isPresent(), is(true));
        assertThat(response.stopReason().get(), is(McpStopReason.END_TURN));
        assertThat(response.message(), instanceOf(McpSamplingImageMessage.class));

        McpSamplingImageMessage image = response.asImageMessage();
        assertThat(image.role(), is(McpRole.USER));
        assertThat(image.data(), is(data));

        assertThrows(McpSamplingException.class, response::asTextMessage);
        assertThrows(McpSamplingException.class, response::asAudioMessage);
    }

    @Test
    void testSamplingResponseAudioMessage() {
        var data = "data".getBytes(StandardCharsets.UTF_8);
        var message = audioMessage(data, MediaTypes.TEXT_PLAIN, McpRole.USER);
        McpSamplingResponse response = new McpSamplingResponseImpl(message, "helidon-model", McpStopReason.END_TURN);

        assertThat(response.model(), is("helidon-model"));
        assertThat(response.stopReason().isPresent(), is(true));
        assertThat(response.stopReason().get(), is(McpStopReason.END_TURN));
        assertThat(response.message(), instanceOf(McpSamplingAudioMessage.class));

        McpSamplingAudioMessage image = response.asAudioMessage();
        assertThat(image.role(), is(McpRole.USER));
        assertThat(image.data(), is(data));

        assertThrows(McpSamplingException.class, response::asTextMessage);
        assertThrows(McpSamplingException.class, response::asImageMessage);
    }
}
