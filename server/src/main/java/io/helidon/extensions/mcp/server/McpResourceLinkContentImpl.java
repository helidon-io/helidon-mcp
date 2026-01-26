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

final class McpResourceLinkContentImpl implements McpResourceLinkContent {
    private final Long size;
    private final String uri;
    private final String name;
    private final String title;
    private final String description;
    private final MediaType mediaType;

    McpResourceLinkContentImpl(Builder builder) {
        this.uri = builder.uri();
        this.name = builder.name();
        this.size = builder.size();
        this.title = builder.title();
        this.mediaType = builder.mediaType();
        this.description = builder.description();
    }

    @Override
    public ContentType type() {
        return ContentType.RESOURCE_LINK;
    }

    @Override
    public String uri() {
        return uri;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Optional<Long> size() {
        return Optional.ofNullable(size);
    }

    @Override
    public Optional<String> title() {
        return Optional.ofNullable(title);
    }

    @Override
    public Optional<String> description() {
        return Optional.ofNullable(description);
    }

    @Override
    public Optional<MediaType> mediaType() {
        return Optional.ofNullable(mediaType);
    }
}
