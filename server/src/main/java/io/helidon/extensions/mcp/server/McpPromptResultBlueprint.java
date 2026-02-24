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

import java.util.List;
import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * MCP prompt result.
 */
@Prototype.Blueprint
@Prototype.CustomMethods(McpPromptSupport.class)
interface McpPromptResultBlueprint {
    /**
     * Prompt text content.
     *
     * @return list of text content
     */
    @Option.Singular
    List<McpPromptTextContent> textContents();

    /**
     * Prompt image content.
     *
     * @return list of image content
     */
    @Option.Singular
    List<McpPromptImageContent> imageContents();

    /**
     * Prompt audio content.
     *
     * @return list of audio content
     */
    @Option.Singular
    List<McpPromptAudioContent> audioContents();

    /**
     * Prompt embedded text resource content.
     *
     * @return list of embedded text resource content
     */
    @Option.Singular
    List<McpPromptTextResourceContent> textResourceContents();

    /**
     * Prompt embedded binary resource content.
     *
     * @return list of embedded binary resource content
     */
    @Option.Singular
    List<McpPromptBinaryResourceContent> binaryResourceContents();

    /**
     * Prompt result description.
     *
     * @return description
     */
    Optional<String> description();
}
