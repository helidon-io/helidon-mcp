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

/**
 * Configuration of an MCP sampling response.
 */
public sealed interface McpSamplingResponse permits McpSamplingResponseImpl {
    /**
     * First sampling response message.
     *
     * @return response
     * @throws McpSamplingException if the response does not contain any messages
     */
    McpSamplingMessage message();

    /**
     * All sampling response messages in wire order.
     *
     * @return immutable response messages
     */
    default List<McpSamplingMessage> messages() {
        return List.of(message());
    }

    /**
     * Returns the first sampling response message as a text message.
     *
     * @return message as text
     * @throws McpSamplingException if the response is empty or the first message is not text
     */
    McpSamplingTextMessage asTextMessage() throws McpSamplingException;

    /**
     * Returns the first sampling response message as an image message.
     *
     * @return message as image
     * @throws McpSamplingException if the response is empty or the first message is not an image
     */
    McpSamplingImageMessage asImageMessage() throws McpSamplingException;

    /**
     * Returns the first sampling response message as an audio message.
     *
     * @return message as audio
     * @throws McpSamplingException if the response is empty or the first message is not audio
     */
    McpSamplingAudioMessage asAudioMessage() throws McpSamplingException;

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
