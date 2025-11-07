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

import java.util.Optional;

/**
 * Configuration of an MCP sampling response.
 */
public sealed interface McpSamplingResponse permits McpSamplingResponseImpl {
    /**
     * Sampling response message.
     *
     * @return response
     */
    McpSamplingMessage message();

    /**
     * Returns sampling response message as text message.
     *
     * @return message as text
     * @throws McpSamplingException if the message is not a text
     */
    McpSamplingTextMessage asTextMessage() throws McpSamplingException;

    /**
     * Returns sampling response message as image message.
     *
     * @return message as image
     * @throws McpSamplingException if the message is not an image
     */
    McpSamplingImageMessage asImageMessage() throws McpSamplingException;

    /**
     * Returns sampling response message as audio message.
     *
     * @return message as audio
     * @throws McpSamplingException if the message is not an audio
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
     * @return stop reason
     */
    Optional<McpStopReason> stopReason();
}
