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

package io.helidon.mcp.server;

import java.util.Objects;

import io.helidon.common.media.type.MediaType;

/**
 * {@link McpPromptContent} factory.
 */
public final class McpPromptContents {
    private McpPromptContents() {
    }

    /**
     * Create a prompt text content.
     *
     * @param prompt content
     * @param role role
     * @return text prompt content
     */
    public static McpPromptContent textContent(String prompt, McpRole role) {
        Objects.requireNonNull(prompt, "Prompt text content must not be null");
        Objects.requireNonNull(role, "Prompt Role must not be null");
        return new McpPromptTextContent(prompt, role);
    }

    /**
     * Create a prompt image content.
     *
     * @param data content
     * @param type media type
     * @param role role
     * @return image prompt content instance
     */
    public static McpPromptContent imageContent(String data, MediaType type, McpRole role) {
        Objects.requireNonNull(data, "Prompt image data must not be null");
        Objects.requireNonNull(type, "Prompt image MIME type must not be null");
        Objects.requireNonNull(role, "Prompt role must not be null");
        return new McpPromptImageContent(data.getBytes(), type, role);
    }

    /**
     * Create a prompt resource content.
     *
     * @param uriTemplate resource uri template
     * @param role role
     * @param content resource content
     * @return prompt resource content instance
     */
    public static McpPromptContent resourceContent(String uriTemplate, McpResourceContent content, McpRole role) {
        Objects.requireNonNull(role, "Prompt role must not be null");
        Objects.requireNonNull(uriTemplate, "Prompt resource URI must not be null");
        Objects.requireNonNull(content, "Prompt resource content must not be null");
        return new McpPromptResourceContent(uriTemplate, content, role);
    }
}
