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

package io.helidon.extensions.mcp.tests.declarative;

import java.util.List;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.extensions.mcp.server.Mcp;
import io.helidon.extensions.mcp.server.McpFeatures;
import io.helidon.extensions.mcp.server.McpResourceContent;
import io.helidon.extensions.mcp.server.McpResourceContents;

@Mcp.Server
@Mcp.Path("/resources")
class McpResourcesServer {
    public static final String RESOURCE_CONTENT = "resource content";
    public static final String RESOURCE_DESCRIPTION = "Resource description";
    public static final String RESOURCE_MEDIA_TYPE = MediaTypes.TEXT_PLAIN_VALUE;

    @Mcp.Resource(
            uri = "resource",
            mediaType = RESOURCE_MEDIA_TYPE,
            description = RESOURCE_DESCRIPTION)
    String resource() {
        return RESOURCE_CONTENT;
    }

    @Mcp.Resource(
            uri = "resource1",
            mediaType = RESOURCE_MEDIA_TYPE,
            description = RESOURCE_DESCRIPTION)
    String resource1(McpFeatures features) {
        return RESOURCE_CONTENT;
    }

    @Mcp.Resource(
            uri = "resource2",
            mediaType = RESOURCE_MEDIA_TYPE,
            description = RESOURCE_DESCRIPTION)
    List<McpResourceContent> resource2() {
        return List.of(McpResourceContents.textContent(RESOURCE_CONTENT));
    }

    @Mcp.Resource(
            uri = "resource3",
            mediaType = RESOURCE_MEDIA_TYPE,
            description = RESOURCE_DESCRIPTION)
    List<McpResourceContent> resource3(McpFeatures features) {
        return List.of(McpResourceContents.textContent(RESOURCE_CONTENT));
    }
}
