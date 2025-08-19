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

/**
 * General content type for all MCP component contents.
 */
public sealed interface McpContent permits McpEmbeddedResource, McpImageContent, McpResourceContent, McpTextContent {
    /**
     * Content type.
     *
     * @return type
     */
    ContentType type();

    /**
     * Content types.
     */
    enum ContentType {
        /**
         * Text.
         */
        TEXT,

        /**
         * Image.
         */
        IMAGE,

        /**
         * RESOURCE.
         */
        RESOURCE;

        /**
         * Return text representation.
         *
         * @return text representation
         */
        public String text() {
            return this.name().toLowerCase();
        }
    }
}
