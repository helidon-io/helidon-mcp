/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
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

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import io.helidon.builder.api.Prototype;
import io.helidon.common.media.type.MediaType;

class McpPromptSupport {
    private McpPromptSupport() {
    }

    /**
     * Create a {@link io.helidon.extensions.mcp.server.McpPromptResult} instance
     * with a prompt text content based on provided text.
     *
     * @param text text content
     * @return a prompt result instance
     */
    @Prototype.FactoryMethod
    static McpPromptResult create(String text) {
        return McpPromptResult.builder().addTextContent(text).build();
    }

    /**
     * Add a prompt text content to the prompt result from the provided text.
     *
     * @param builder prompt result builder
     * @param text    text content
     */
    @Prototype.BuilderMethod
    static void addTextContent(McpPromptResult.BuilderBase<?, ?> builder, String text) {
        Objects.requireNonNull(text, "text is null");
        builder.addTextContent(b -> b.text(text).role(McpRole.USER));
    }

    /**
     * Add a prompt image content to the prompt result from the provided data and media type.
     *
     * @param builder prompt result builder
     * @param data    prompt image data
     * @param type    prompt image media type
     */
    @Prototype.BuilderMethod
    static void addImageContent(McpPromptResult.BuilderBase<?, ?> builder, byte[] data, MediaType type) {
        Objects.requireNonNull(data, "data is null");
        Objects.requireNonNull(type, "media type is null");
        builder.addImageContent(image -> image.data(data).mediaType(type).role(McpRole.USER));
    }

    /**
     * Add a prompt audio content to the prompt result from the provided data and media type.
     *
     * @param builder prompt result builder
     * @param data    prompt audio data
     * @param type    prompt audio media type
     */
    @Prototype.BuilderMethod
    static void addAudioContent(McpPromptResult.BuilderBase<?, ?> builder, byte[] data, MediaType type) {
        Objects.requireNonNull(data, "data is null");
        Objects.requireNonNull(type, "media type is null");
        builder.addAudioContent(audio -> audio.data(data).mediaType(type).role(McpRole.USER));
    }

    /**
     * Aggregate contents of a prompt result into a single list.
     *
     * @param result prompt result
     * @return list of prompt content
     */
    static List<McpPromptContent> aggregateContent(McpPromptResult result) {
        List<McpPromptContent> contents = new LinkedList<>();
        contents.addAll(result.textContents());
        contents.addAll(result.imageContents());
        contents.addAll(result.audioContents());
        contents.addAll(result.textResourceContents());
        contents.addAll(result.binaryResourceContents());
        return contents;
    }
}
