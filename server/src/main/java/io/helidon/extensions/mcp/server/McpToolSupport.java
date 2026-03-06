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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.helidon.builder.api.Prototype;
import io.helidon.common.media.type.MediaType;

class McpToolSupport {
    private McpToolSupport() {
    }

    /**
     * Create a {@link io.helidon.extensions.mcp.server.McpToolResult} instance with
     * a text content based on the provided string.
     *
     * @param text text content
     * @return tool result instance
     */
    @Prototype.FactoryMethod
    static McpToolResult create(String text) {
        return McpToolResult.builder().addTextContent(text).build();
    }

    /**
     * Add a tool text content to the tool result from the provided text.
     *
     * @param builder tool result builder
     * @param text    text content
     */
    @Prototype.BuilderMethod
    static void addTextContent(McpToolResult.BuilderBase<?, ?> builder, String text) {
        Objects.requireNonNull(text, "text is null");
        builder.addTextContent(b -> b.text(text));
    }

    /**
     * Add a tool image content to the tool result from the provided data and media type.
     *
     * @param builder tool result builder
     * @param data    tool image data
     * @param type    tool image media type
     */
    @Prototype.BuilderMethod
    static void addImageContent(McpToolResult.BuilderBase<?, ?> builder, byte[] data, MediaType type) {
        Objects.requireNonNull(data, "data is null");
        Objects.requireNonNull(type, "media type is null");
        builder.addImageContent(image -> image.data(data).mediaType(type));
    }

    /**
     * Add a tool resource link content to the tool result from the provided name and uri.
     *
     * @param builder tool result builder
     * @param name    tool resource link name
     * @param uri     tool resource link uri
     */
    @Prototype.BuilderMethod
    static void addResourceLinkContent(McpToolResult.BuilderBase<?, ?> builder, String name, String uri) {
        Objects.requireNonNull(uri, "uri is null");
        Objects.requireNonNull(name, "name is null");
        builder.addResourceLinkContent(link -> link.name(name).uri(uri));
    }

    /**
     * Add a tool audio content to the tool result from the provided data and media type.
     *
     * @param builder tool result builder
     * @param data    tool audio data
     * @param type    tool audio media type
     */
    @Prototype.BuilderMethod
    static void addAudioContent(McpToolResult.BuilderBase<?, ?> builder, byte[] data, MediaType type) {
        Objects.requireNonNull(data, "data is null");
        Objects.requireNonNull(type, "media type is null");
        builder.addAudioContent(audio -> audio.data(data).mediaType(type));
    }

    /**
     * Aggregate contents of a tool result into a single list.
     *
     * @param result tool result
     * @return list of tool content
     */
    static List<McpToolContent> aggregateContent(McpToolResult result) {
        List<McpToolContent> contents = new ArrayList<>();
        contents.addAll(result.textContents());
        contents.addAll(result.imageContents());
        contents.addAll(result.audioContents());
        contents.addAll(result.textResourceContents());
        contents.addAll(result.binaryResourceContents());
        contents.addAll(result.resourceLinkContents());
        return contents;
    }
}
