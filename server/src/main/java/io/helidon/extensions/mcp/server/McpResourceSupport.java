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
import io.helidon.common.media.type.MediaTypes;

class McpResourceSupport {
    private McpResourceSupport() {
    }

    /**
     * Create a {@link io.helidon.extensions.mcp.server.McpResourceResult} instance
     * with a resource text content based on provided text.
     *
     * @param text text content
     * @return a resource result instance
     */
    @Prototype.FactoryMethod
    static McpResourceResult create(String text) {
        return McpResourceResult.builder().addTextContent(text).build();
    }

    /**
     * Add a resource text content to the resource result from the provided text.
     *
     * @param builder resource builder result
     * @param text    text content
     */
    @Prototype.BuilderMethod
    static void addTextContent(McpResourceResult.BuilderBase<?, ?> builder, String text) {
        Objects.requireNonNull(text, "text is null");
        builder.addTextContent(b -> b.text(text).mimeType(MediaTypes.TEXT_PLAIN));
    }

    /**
     * Add a resource binary content to the resource result from the provided data and media type.
     *
     * @param builder resource builder result
     * @param data    resource binary data
     * @param type    resource media type
     */
    @Prototype.BuilderMethod
    static void addBinaryContent(McpResourceResult.BuilderBase<?, ?> builder, byte[] data, MediaType type) {
        Objects.requireNonNull(data, "data is null");
        Objects.requireNonNull(type, "media type is null");
        builder.addBinaryContent(binary -> binary.data(data).mimeType(type));
    }

    /**
     * Aggregate contents of a resource result into a single list.
     *
     * @param result resource result
     * @return list of resource content
     */
    static List<McpResourceContent> aggregateContent(McpResourceResult result) {
        List<McpResourceContent> contents = new LinkedList<>();
        contents.addAll(result.textContents());
        contents.addAll(result.binaryContents());
        return contents;
    }
}
