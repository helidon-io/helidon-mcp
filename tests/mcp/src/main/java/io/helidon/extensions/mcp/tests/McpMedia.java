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

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import io.helidon.common.media.type.MediaType;
import io.helidon.common.media.type.MediaTypes;

class McpMedia {
    public static final MediaType IMAGE_PNG = MediaTypes.create("image/png");
    public static final MediaType AUDIO_WAV = MediaTypes.create("audio/wav");
    public static final String IMAGE_PNG_VALUE = IMAGE_PNG.text();
    public static final String AUDIO_WAV_VALUE = AUDIO_WAV.text();

    private static final ReentrantLock LOCK = new ReentrantLock();
    private static final Map<String, byte[]> MEDIA = new HashMap<>();

    private McpMedia() {
    }

    /**
     * Get a media resource from an internal cache or load it as a resource using
     * this class' classloader.
     *
     * @param name name of resource
     * @return byte array representation or {@code null} if not found
     */
    static byte[] media(String name) {
        LOCK.lock();
        try {
            if (MEDIA.containsKey(name)) {
                return MEDIA.get(name);
            }
            try (InputStream is = McpMedia.class.getClassLoader().getResourceAsStream(name)) {
                if (is != null) {
                    byte[] data = is.readAllBytes();
                    MEDIA.put(name, data);
                    return data;
                }
            } catch (IOException e) {
                // falls through
            }
            return null;
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * Get a media resource in base64 encoding.
     *
     * @param name name of resource
     * @return string representation in base64 or {@code null} if not found
     */
    static String base64Media(String name) {
        byte[] data = media(name);
        return data != null ? Base64.getEncoder().encodeToString(data) : null;
    }
}
