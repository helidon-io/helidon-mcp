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

import java.nio.charset.StandardCharsets;

import io.helidon.common.media.type.MediaType;
import io.helidon.common.media.type.MediaTypes;

final class McpResourceTextContent implements McpResourceContent {

    private final String text;

    McpResourceTextContent(String text) {
        this.text = text;
    }

    @Override
    public byte[] data() {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public MediaType mimeType() {
        return MediaTypes.TEXT_PLAIN;
    }

    @Override
    public ContentType type() {
        return ContentType.RESOURCE;
    }
}
