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

import java.time.Duration;
import java.util.List;

import io.helidon.common.media.type.MediaTypes;

import jakarta.json.JsonValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class McpSamplingRequestTest {

    @Test
    void testDefaultValues() {
        McpSamplingRequest request = McpSamplingRequest.create();

        assertThat(request.maxTokens(), is(100));
        assertThat(request.hints().isEmpty(), is(true));
        assertThat(request.metadata().isEmpty(), is(true));
        assertThat(request.temperature().isEmpty(), is(true));
        assertThat(request.textMessages().isEmpty(), is(true));
        assertThat(request.costPriority().isEmpty(), is(true));
        assertThat(request.systemPrompt().isEmpty(), is(true));
        assertThat(request.imageMessages().isEmpty(), is(true));
        assertThat(request.audioMessages().isEmpty(), is(true));
        assertThat(request.stopSequences().isEmpty(), is(true));
        assertThat(request.speedPriority().isEmpty(), is(true));
        assertThat(request.includeContext().isEmpty(), is(true));
        assertThat(request.timeout(), is(Duration.ofSeconds(5)));
        assertThat(request.intelligencePriority().isEmpty(), is(true));
    }

    @Test
    void testCustomValues() {
        McpSamplingRequest request = McpSamplingRequest.builder()
                .maxTokens(1)
                .temperature(0.1)
                .costPriority(0.1)
                .speedPriority(0.1)
                .hints(List.of("hint1"))
                .metadata(JsonValue.TRUE)
                .intelligencePriority(0.1)
                .systemPrompt("system prompt")
                .timeout(Duration.ofSeconds(10))
                .stopSequences(List.of("stop1"))
                .includeContext(McpIncludeContext.NONE)
                .addTextMessage(message -> message.text("text").role(McpRole.USER))
                .addImageMessage(message -> message.data(new byte[0]).mediaType(MediaTypes.TEXT_PLAIN).role(McpRole.ASSISTANT))
                .addAudioMessage(message -> message.data(new byte[0]).mediaType(MediaTypes.TEXT_PLAIN).role(McpRole.ASSISTANT))
                .build();

        assertThat(request.maxTokens(), is(1));
        assertThat(request.timeout(), is(Duration.ofSeconds(10)));

        assertThat(request.hints().isEmpty(), is(false));
        assertThat(request.hints().get(), is(List.of("hint1")));

        assertThat(request.textMessages().isEmpty(), is(false));
        assertThat(request.textMessages().size(), is(1));

        McpSamplingTextMessage message = request.textMessages().getFirst();
        assertThat(message.text(), is("text"));
        assertThat(message.role(), is(McpRole.USER));

        assertThat(request.imageMessages().isEmpty(), is(false));
        assertThat(request.imageMessages().size(), is(1));

        McpSamplingImageMessage image = request.imageMessages().getFirst();
        assertThat(image.data(), is(new byte[0]));
        assertThat(image.role(), is(McpRole.ASSISTANT));
        assertThat(image.mediaType(), is(MediaTypes.TEXT_PLAIN));

        assertThat(request.audioMessages().isEmpty(), is(false));
        assertThat(request.audioMessages().size(), is(1));

        McpSamplingAudioMessage audio = request.audioMessages().getFirst();
        assertThat(audio.data(), is(new byte[0]));
        assertThat(audio.role(), is(McpRole.ASSISTANT));
        assertThat(audio.mediaType(), is(MediaTypes.TEXT_PLAIN));

        assertThat(request.metadata().isEmpty(), is(false));
        assertThat(request.metadata().get(), is(JsonValue.TRUE));

        assertThat(request.includeContext().isEmpty(), is(false));
        assertThat(request.includeContext().get(), is(McpIncludeContext.NONE));

        assertThat(request.systemPrompt().isEmpty(), is(false));
        assertThat(request.systemPrompt().get(), is("system prompt"));

        assertThat(request.stopSequences().isEmpty(), is(false));
        assertThat(request.stopSequences().get(), is(List.of("stop1")));

        assertThat(request.temperature().isEmpty(), is(false));
        assertThat(request.temperature().get(), is(0.1));

        assertThat(request.costPriority().isEmpty(), is(false));
        assertThat(request.costPriority().get(), is(0.1));

        assertThat(request.speedPriority().isEmpty(), is(false));
        assertThat(request.speedPriority().get(), is(0.1));

        assertThat(request.intelligencePriority().isEmpty(), is(false));
        assertThat(request.intelligencePriority().get(), is(0.1));
    }

    @ParameterizedTest
    @ValueSource(doubles = {1.1, -1.1})
    void testIntelligencePriorityDecorator(double value) {
        try {
            McpSamplingRequest.builder()
                    .intelligencePriority(value)
                    .build();
            assertThat("Setting a value outside of range [0, 1] must throw an exception", true, is(false));
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Intelligence priority must be in range [0, 1]"));
        }
    }

    @ParameterizedTest
    @ValueSource(doubles = {1.1, -1.1})
    void testCostPriorityDecorator(double value) {
        try {
            McpSamplingRequest.builder()
                    .costPriority(value)
                    .build();
            assertThat("Setting a value outside of range [0, 1] must throw an exception", true, is(false));
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Cost priority must be in range [0, 1]"));
        }
    }

    @ParameterizedTest
    @ValueSource(doubles = {1.1, -1.1})
    void testSpeedPriorityDecorator(double value) {
        try {
            McpSamplingRequest.builder()
                    .speedPriority(value)
                    .build();
            assertThat("Setting a value outside of range [0, 1] must throw an exception", true, is(false));
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Speed priority must be in range [0, 1]"));
        }
    }
}
