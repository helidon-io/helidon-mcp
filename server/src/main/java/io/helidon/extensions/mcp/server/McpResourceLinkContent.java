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
public sealed interface McpResourceLinkContent extends McpContent permits McpResourceLinkContentImpl {
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

    /**
     * Create an MCP resource link content builder instance.
     *
     * @return resource link content builder instance
     */
    static McpResourceLinkContent.Builder builder() {
        return new McpResourceLinkContent.Builder();
    }

    /**
     * MCP resource link content builder.
     */
    final class Builder {
        private Long size;
        private String uri;
        private String name;
        private String title;
        private String description;
        private MediaType mediaType;

        /**
         * Set resource link uri.
         *
         * @param uri the uri
         * @return this builder
         */
        public Builder uri(String uri) {
            this.uri = uri;
            return this;
        }

        /**
         * Set resource link name.
         *
         * @param name the name
         * @return this builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Set resource link size.
         *
         * @param size the size
         * @return this builder
         */
        public Builder size(long size) {
            this.size = size;
            return this;
        }

        /**
         * Set resource link title.
         *
         * @param title the title
         * @return this builder
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }

        /**
         * Set resource link description.
         *
         * @param description the description
         * @return this builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * Set resource link media type.
         *
         * @param mediaType the media type
         * @return this builder
         */
        public Builder mediaType(MediaType mediaType) {
            this.mediaType = mediaType;
            return this;
        }

        /**
         * Get resource link uri.
         *
         * @return the uri
         */
        public String uri() {
            return uri;
        }

        /**
         * Get resource link name.
         *
         * @return the name
         */
        public String name() {
            return name;
        }

        /**
         * Get resource link size.
         *
         * @return the size
         */
        public Long size() {
            return size;
        }

        /**
         * Get resource link title.
         *
         * @return the title
         */
        public String title() {
            return title;
        }

        /**
         * Get resource link description.
         *
         * @return the description
         */
        public String description() {
            return description;
        }

        /**
         * Get resource link media type.
         *
         * @return the media type
         */
        public MediaType mediaType() {
            return mediaType;
        }

        /**
         * Build an instance of {@link io.helidon.extensions.mcp.server.McpResourceLinkContent}.
         *
         * @return the instance
         */
        public McpResourceLinkContent build() {
            return new McpResourceLinkContentImpl(this);
        }
    }
}
