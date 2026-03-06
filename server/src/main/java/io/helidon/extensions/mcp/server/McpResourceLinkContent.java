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

import java.util.Optional;

import io.helidon.common.media.type.MediaType;

/**
 * MCP resource link content.
 */
interface McpResourceLinkContent extends McpContent {
    /**
     * Resource URI.
     *
     * @return uri
     */
    String uri();

    /**
     * Resource name.
     *
     * @return name
     */
    String name();

    /**
     * Human-readable resource title.
     *
     * @return the resource title
     */
    Optional<String> title();

    /**
     * Resource description.
     *
     * @return description
     */
    Optional<String> description();

    /**
     * Resource mime type.
     *
     * @return type
     */
    Optional<MediaType> mediaType();

    /**
     * Resource data size.
     *
     * @return size
     */
    Optional<Long> size();

    @Override
    default McpContentType type() {
        return McpContentType.RESOURCE_LINK;
    }
}
