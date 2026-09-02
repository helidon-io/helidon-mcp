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
package io.helidon.extensions.mcp.tests.declarative;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.extensions.mcp.server.Mcp;
import io.helidon.extensions.mcp.server.McpIconTheme;

@Mcp.Server("icon-metadata")
@Mcp.Icon(
        value = "https://example.com/server.svg",
        mimeType = "image/svg+xml",
        sizes = "any",
        theme = McpIconTheme.LIGHT)
@Mcp.Icon("data:image/png;base64,iVBORw0KGgo=")
@Mcp.Path("/icon-metadata")
@Mcp.Stateless
class McpIconMetadataServer {
    @Mcp.Tool("Tool with icons")
    @Mcp.Icon(
            value = "https://example.com/tool.svg",
            mimeType = "image/svg+xml",
            sizes = {"48x48", "96x96"},
            theme = McpIconTheme.DARK)
    @Mcp.Icon("data:image/png;base64,iVBORw0KGgo=")
    String tool() {
        return "tool";
    }

    @Mcp.Prompt("Prompt with an icon")
    @Mcp.Icon(
            value = "https://example.com/prompt.png",
            mimeType = "image/png",
            theme = McpIconTheme.LIGHT)
    String prompt() {
        return "prompt";
    }

    @Mcp.Resource(
            uri = "https://example.com/resource",
            mediaType = MediaTypes.TEXT_PLAIN_VALUE,
            description = "Resource with an icon")
    @Mcp.Icon("https://example.com/resource.svg")
    String resource() {
        return "resource";
    }

    @Mcp.Resource(
            uri = "https://example.com/resource/{id}",
            mediaType = MediaTypes.TEXT_PLAIN_VALUE,
            description = "Resource template with an icon")
    @Mcp.Icon(
            value = "https://example.com/template.svg",
            sizes = "any")
    String resourceTemplate(String id) {
        return id;
    }
}
