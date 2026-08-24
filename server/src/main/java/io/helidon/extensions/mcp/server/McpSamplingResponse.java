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

import java.util.Optional;

/**
 * Configuration of an MCP sampling response.
 */
public sealed interface McpSamplingResponse permits McpSamplingResponseImpl {
    /**
     * Sampling response message.
     *
     * @return response message
     */
    McpSamplingMessage message();

    /**
     * Returns the first sampling response content block as text content.
     *
     * @return text content
     * @throws McpSamplingException if the message is empty or its first content block is not text
     */
    McpSamplingTextContent asTextContent() throws McpSamplingException;

    /**
     * Returns the first sampling response content block as image content.
     *
     * @return image content
     * @throws McpSamplingException if the message is empty or its first content block is not an image
     */
    McpSamplingImageContent asImageContent() throws McpSamplingException;

    /**
     * Returns the first sampling response content block as audio content.
     *
     * @return audio content
     * @throws McpSamplingException if the message is empty or its first content block is not audio
     */
    McpSamplingAudioContent asAudioContent() throws McpSamplingException;

    /**
     * Returns the first sampling response content block as tool use content.
     *
     * @return tool use content
     * @throws McpSamplingException if the message is empty or its first content block is not a tool use
     */
    McpSamplingToolUseContent asToolUseContent() throws McpSamplingException;

    /**
     * Sampling model used.
     *
     * @return model
     */
    String model();

    /**
     * Sampling stop reason.
     *
     * @return matching standard stop reason, or empty if the client omitted the reason or returned a non-standard reason
     */
    Optional<McpStopReason> stopReason();

    /**
     * Sampling stop reason exactly as received from the client.
     *
     * @return raw stop reason, or empty if the client omitted it
     */
    default Optional<String> rawStopReason() {
        return Optional.empty();
    }
}
