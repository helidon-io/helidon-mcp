/*
 * Copyright (c) 2025, 2026 Oracle and/or its affiliates.
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
 * Supported MCP protocol version.
 */
enum McpProtocolVersion {
    /**
     * 2025-06-18 protocol version.
     */
    VERSION_2025_06_18,
    /**
     * 2025-03-26 protocol version.
     */
    VERSION_2025_03_26,
    /**
     * 2024-11-05 protocol version.
     */
    VERSION_2024_11_05;

    /**
     * Return the protocol version text.
     *
     * @return text version
     */
    String text() {
        return name().replace("VERSION_", "")
                .replace("_", "-");
    }

    /**
     * Map the provided version to a {@code McpProtocolVersion}. If the value
     * does not exist, return the latest supported version.
     *
     * @param version the tested version
     * @return list of supported versions
     */
    static McpProtocolVersion find(String version) {
        if (version == null) {
            return lastest();
        }
        for (McpProtocolVersion v : McpProtocolVersion.values()) {
            if (v.text().equals(version)) {
                return v;
            }
        }
        return lastest();
    }

    /**
     * Returns lastest supported MCP protocol version.
     *
     * @return the lastest supported protocol version
     */
    static McpProtocolVersion lastest() {
        return VERSION_2025_06_18;
    }
}
