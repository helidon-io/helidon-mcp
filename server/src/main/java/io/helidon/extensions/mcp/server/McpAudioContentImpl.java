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

import java.util.Base64;

import io.helidon.common.media.type.MediaType;

final class McpAudioContentImpl implements McpAudioContent {
    private final byte[] data;
    private final MediaType type;

    private String base64Data;

    McpAudioContentImpl(byte[] data, MediaType type) {
        this.data = data;
        this.type = type;
    }

    @Override
    public byte[] data() {
        return data;
    }

    @Override
    public String base64Data() {
        if (base64Data == null) {
            base64Data = Base64.getEncoder().encodeToString(data);
        }
        return base64Data;
    }

    @Override
    public MediaType mediaType() {
        return type;
    }

    @Override
    public ContentType type() {
        return ContentType.AUDIO;
    }
}
