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
package io.helidon.extensions.mcp.tests;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import io.helidon.common.media.type.MediaType;
import io.helidon.common.media.type.MediaTypes;

class McpMedia {
    public static final MediaType IMAGE_PNG = MediaTypes.create("image/png");
    public static final MediaType AUDIO_WAV = MediaTypes.create("audio/wav");
    public static final String IMAGE_PNG_VALUE = IMAGE_PNG.text();
    public static final String AUDIO_WAV_VALUE = AUDIO_WAV.text();

    private static final Map<String, String> MEDIA = new HashMap<>();

    private McpMedia() {
    }

    /**
     * Get a media resource from an internal cache or load it as a resource using
     * this class' classloader.
     *
     * @param name name of resource
     * @return base64 encoded representation of media or {@code null} if not found
     */
    static String base64Media(String name) {
        if (MEDIA.containsKey(name)) {
            return MEDIA.get(name);
        }
        try (InputStream is = McpMedia.class.getClassLoader().getResourceAsStream(name)) {
            if (is != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                is.transferTo(baos);
                String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
                MEDIA.put(name, base64);
                baos.close();
                return base64;
            }
        } catch (IOException e) {
            // falls through
        }
        return null;
    }
}
