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

import java.util.Objects;

import io.helidon.common.media.type.MediaType;

/**
 * {@link io.helidon.extensions.mcp.server.McpSamplingMessage} factory class.
 */
public final class McpSamplingMessages {
    private McpSamplingMessages() {
    }

    /**
     * Create a sampling text message.
     *
     * @param text text
     * @param role role
     * @return a sampling text message
     */
    public static McpSamplingMessage textMessage(String text, McpRole role) {
        Objects.requireNonNull(role, "role must not be null");
        Objects.requireNonNull(text, "text must not be null");
        return new McpSamplingTextMessageImpl(text, role);
    }

    /**
     * Create a sampling image message.
     *
     * @param data data
     * @param mediaType media type
     * @param role role
     * @return a sampling image message
     */
    public static McpSamplingMessage imageMessage(byte[] data, MediaType mediaType, McpRole role) {
        Objects.requireNonNull(role, "role must not be null");
        Objects.requireNonNull(data, "data must not be null");
        Objects.requireNonNull(mediaType, "media type must not be null");
        return new McpSamplingImageMessageImpl(data, mediaType, role);
    }

    /**
     * Create a sampling audio message.
     *
     * @param data data
     * @param mediaType media type
     * @param role role
     * @return a sampling audio message
     */
    public static McpSamplingMessage audioMessage(byte[] data, MediaType mediaType, McpRole role) {
        Objects.requireNonNull(data, "data must not be null");
        Objects.requireNonNull(role, "role must not be null");
        Objects.requireNonNull(mediaType, "media type must not be null");
        return new McpSamplingAudioMessageImpl(data, mediaType, role);
    }
}
