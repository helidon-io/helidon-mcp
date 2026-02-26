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

import java.util.Base64;

import io.helidon.common.media.type.MediaType;

/**
 * MCP sampling media content.
 */
public interface McpSamplingMediaMessage extends McpSamplingMessage {
    /**
     * Image content raw data.
     *
     * @return content
     */
    byte[] data();

    /**
     * Image content MIME type.
     *
     * @return MIME type
     */
    MediaType mediaType();

    /**
     * Returns the decoded image data using base64 decoder.
     *
     * @return decoded content.
     */
    default byte[] decodeBase64Data() {
        return Base64.getDecoder().decode(data());
    }

    /**
     * Returns the encoded image data using base64 encoder.
     *
     * @return content in base64.
     */
    default String encodeBase64Data() {
        return Base64.getEncoder().encodeToString(data());
    }
}
