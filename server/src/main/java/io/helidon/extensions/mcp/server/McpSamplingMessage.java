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

/**
 * Mcp sampling message interface.
 */
public sealed interface McpSamplingMessage extends McpContent permits McpSamplingTextContent,
                                                                      McpSamplingImageContent,
                                                                      McpSamplingAudioContent {
    /**
     * Sampling message role.
     *
     * @return role
     */
    McpRole role();

    /**
     * Sampling message as text content.
     *
     * @return text content
     */
    default McpSamplingTextContent asText() {
        throw new IllegalArgumentException("Not a text");
    }

    /**
     * Sampling message as image content.
     *
     * @return image content
     */
    default McpSamplingImageContent asImage() {
        throw new IllegalArgumentException("Not an image");
    }

    /**
     * Sampling message as audio content.
     *
     * @return audio content
     */
    default McpSamplingAudioContent asAudio() {
        throw new IllegalArgumentException("Not an audio");
    }
}
