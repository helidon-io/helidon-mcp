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

import java.util.Objects;
import java.util.Optional;

final class McpSamplingResponseImpl implements McpSamplingResponse {
    private final String model;
    private final String rawStopReason;
    private final McpSamplingMessage message;

    McpSamplingResponseImpl(McpSamplingMessage message, String model, McpStopReason stopReason) {
        this(message, model, stopReason.text());
    }

    McpSamplingResponseImpl(McpSamplingMessage message, String model) {
        this(message, model, (String) null);
    }

    McpSamplingResponseImpl(McpSamplingMessage message, String model, String rawStopReason) {
        this.message = Objects.requireNonNull(message, "message is null");
        this.model = Objects.requireNonNull(model, "model is null");
        this.rawStopReason = rawStopReason;
    }

    @Override
    public McpSamplingMessage message() {
        return message;
    }

    @Override
    public McpSamplingTextContent asTextContent() throws McpSamplingException {
        McpSamplingContent content = content();
        if (content instanceof McpSamplingTextContent text) {
            return text;
        }
        throw new McpSamplingException("Sampling content is not text");
    }

    @Override
    public McpSamplingImageContent asImageContent() throws McpSamplingException {
        McpSamplingContent content = content();
        if (content instanceof McpSamplingImageContent image) {
            return image;
        }
        throw new McpSamplingException("Sampling content is not an image");
    }

    @Override
    public McpSamplingAudioContent asAudioContent() throws McpSamplingException {
        McpSamplingContent content = content();
        if (content instanceof McpSamplingAudioContent audio) {
            return audio;
        }
        throw new McpSamplingException("Sampling content is not audio");
    }

    @Override
    public McpSamplingToolUseContent asToolUseContent() throws McpSamplingException {
        McpSamplingContent content = content();
        if (content instanceof McpSamplingToolUseContent toolUse) {
            return toolUse;
        }
        throw new McpSamplingException("Sampling content is not a tool use");
    }

    @Override
    public String model() {
        return model;
    }

    @Override
    public Optional<McpStopReason> stopReason() {
        return rawStopReason().flatMap(McpStopReason::from);
    }

    @Override
    public Optional<String> rawStopReason() {
        return Optional.ofNullable(rawStopReason);
    }

    private McpSamplingContent content() {
        if (message.contents().isEmpty()) {
            throw new McpSamplingException("Sampling response message does not contain content");
        }
        return message.contents().getFirst();
    }
}
