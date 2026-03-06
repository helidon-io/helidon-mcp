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
 * MCP tool result.
 */
@Prototype.Blueprint
@Prototype.CustomMethods(McpToolSupport.class)
interface McpToolResultBlueprint {
    /**
     * Text content.
     *
     * @return list of tool text content
     */
    @Option.Singular
    List<McpToolTextContent> textContents();

    /**
     * Image content.
     *
     * @return list of tool image content
     */
    @Option.Singular
    List<McpToolImageContent> imageContents();

    /**
     * Audio content.
     *
     * @return list of tool audio content
     */
    @Option.Singular
    List<McpToolAudioContent> audioContents();

    /**
     * Embedded resource text content.
     *
     * @return list of tool embedded resource text content
     */
    @Option.Singular
    List<McpToolTextResourceContent> textResourceContents();

    /**
     * Embedded resource binary content.
     *
     * @return list of tool embedded resource binary content
     */
    @Option.Singular
    List<McpToolBinaryResourceContent> binaryResourceContents();

    /**
     * Resource link content.
     *
     * @return list of tool resource link content
     */
    @Option.Singular
    List<McpToolResourceLinkContent> resourceLinkContents();

    /**
     * Structured tool result content. If specified, the tool definition
     * must contain an output schema.
     *
     * @return structured content
     */
    Optional<Object> structuredContent();

    /**
     * Tool result error.
     *
     * @return error
     */
    @Option.DefaultBoolean(false)
    boolean error();
}
