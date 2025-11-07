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

import java.util.Base64;

import io.helidon.common.media.type.MediaType;

/**
 * MCP sampling audio content.
 */
final class McpSamplingAudioMessageImpl implements McpSamplingAudioMessage {
    private final byte[] data;
    private final McpRole role;
    private final MediaType type;

    McpSamplingAudioMessageImpl(byte[] data, MediaType type, McpRole role) {
        this.data = data;
        this.role = role;
        this.type = type;
    }

    @Override
    public McpSamplingMessageType type() {
        return McpSamplingMessageType.AUDIO;
    }

    @Override
    public McpRole role() {
        return role;
    }

    @Override
    public MediaType mediaType() {
        return type;
    }

    @Override
    public byte[] data() {
        return data;
    }

    @Override
    public byte[] decodeBase64Data() {
        return Base64.getDecoder().decode(data);
    }

    @Override
    public String encodeBase64Data() {
        return Base64.getEncoder().encodeToString(data);
    }
}
