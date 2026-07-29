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

import java.util.List;
import java.util.Optional;

final class McpSamplingResponseImpl implements McpSamplingResponse {
    private final String model;
    private final McpStopReason stopReason;
    private final List<McpSamplingMessage> messages;

    McpSamplingResponseImpl(McpSamplingMessage message, String model, McpStopReason stopReason) {
        this(List.of(message), model, stopReason);
    }

    McpSamplingResponseImpl(List<McpSamplingMessage> messages, String model, McpStopReason stopReason) {
        this.messages = List.copyOf(messages);
        this.model = model;
        this.stopReason = stopReason;
    }

    @Override
    public McpSamplingMessage message() {
        if (messages.isEmpty()) {
            throw new McpSamplingException("Sampling response does not contain a message");
        }
        return messages.getFirst();
    }

    @Override
    public List<McpSamplingMessage> messages() {
        return messages;
    }

    @Override
    public McpSamplingTextMessage asTextMessage() throws McpSamplingException {
        McpSamplingMessage message = message();
        if (message instanceof McpSamplingTextMessage text) {
            return text;
        }
        throw new McpSamplingException("Sampling message is not text");
    }

    @Override
    public McpSamplingImageMessage asImageMessage() throws McpSamplingException {
        McpSamplingMessage message = message();
        if (message instanceof McpSamplingImageMessage image) {
            return image;
        }
        throw new McpSamplingException("Sampling message is not an image");
    }

    @Override
    public McpSamplingAudioMessage asAudioMessage() throws McpSamplingException {
        McpSamplingMessage message = message();
        if (message instanceof McpSamplingAudioMessage audio) {
            return audio;
        }
        throw new McpSamplingException("Sampling message is not an audio");
    }

    @Override
    public String model() {
        return model;
    }

    @Override
    public Optional<McpStopReason> stopReason() {
        return Optional.ofNullable(stopReason);
    }
}
