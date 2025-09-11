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

import java.net.URI;
import java.util.Objects;

import io.helidon.common.media.type.MediaType;

/**
 * {@link McpToolContent} factory.
 */
public final class McpToolContents {
    private McpToolContents() {
    }

    /**
     * Create a text tool content instance.
     *
     * @param text text
     * @return instance
     */
    public static McpToolContent textContent(String text) {
        Objects.requireNonNull(text, "Tool content text must not be null");
        return new McpToolTextContent(text);
    }

    /**
     * Create a image tool content instance.
     *
     * @param data data
     * @param type media type
     * @return instance
     */
    public static McpToolContent imageContent(byte[] data, MediaType type) {
        Objects.requireNonNull(data, "Tool image content data must not be null");
        Objects.requireNonNull(type, "Tool image content MIME type must not be null");
        return new McpToolImageContent(data, type);
    }

    /**
     * Create a resource tool content instance.
     *
     * @param uri resource uri
     * @param content resource content
     * @return instance
     */
    public static McpToolContent resourceContent(URI uri, McpResourceContent content) {
        Objects.requireNonNull(uri, "Tool resource URI must not be null");
        Objects.requireNonNull(content, "Tool resource content must not be null");
        return new McpToolResourceContent(uri, content);
    }

    /**
     * Create an audio tool content instance.
     *
     * @param data data
     * @param type media type
     * @return instance
     */
    public static McpToolContent audioContent(byte[] data, MediaType type) {
        Objects.requireNonNull(data, "Tool audio content data must not be null");
        Objects.requireNonNull(type, "Tool audio content MIME type must not be null");
        return new McpToolAudioContent(data, type);
    }
}
