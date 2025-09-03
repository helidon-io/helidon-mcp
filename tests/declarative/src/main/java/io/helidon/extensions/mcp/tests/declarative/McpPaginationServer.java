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
import io.helidon.extensions.mcp.server.McpPromptContent;
import io.helidon.extensions.mcp.server.McpPromptContents;
import io.helidon.extensions.mcp.server.McpResourceContent;
import io.helidon.extensions.mcp.server.McpResourceContents;
import io.helidon.extensions.mcp.server.McpRole;
import io.helidon.extensions.mcp.server.McpToolContent;
import io.helidon.extensions.mcp.server.McpToolContents;

@Mcp.Server
@Mcp.Path("/pagination")
@Mcp.ToolsPageSize(1)
@Mcp.PromptsPageSize(1)
@Mcp.ResourcesPageSize(1)
@Mcp.ResourceTemplatesPageSize(1)
class McpPaginationServer {

    @Mcp.Tool("Tool description")
    List<McpToolContent> tool1() {
        return List.of(McpToolContents.textContent("text1"));
    }

    @Mcp.Tool("Tool description")
    List<McpToolContent> tool2() {
        return List.of(McpToolContents.textContent("text2"));
    }

    @Mcp.Prompt("Prompt description")
    List<McpPromptContent> prompt1() {
        return List.of(McpPromptContents.textContent("text1", McpRole.USER));
    }

    @Mcp.Prompt("Prompt description")
    List<McpPromptContent> prompt2() {
        return List.of(McpPromptContents.textContent("text2", McpRole.USER));
    }

    @Mcp.Resource(
            uri = "https://path1",
            mediaType = MediaTypes.TEXT_PLAIN_VALUE,
            description = "Resource description")
    List<McpResourceContent> resource1() {
        return List.of(McpResourceContents.textContent("text1"));
    }

    @Mcp.Resource(
            uri = "https://path2",
            mediaType = MediaTypes.TEXT_PLAIN_VALUE,
            description = "Resource description")
    List<McpResourceContent> resource2() {
        return List.of(McpResourceContents.textContent("text2"));
    }

    @Mcp.Resource(
            uri = "https://{path1}",
            mediaType = MediaTypes.TEXT_PLAIN_VALUE,
            description = "Resource Template description")
    List<McpResourceContent> resourceTemplate1() {
        return List.of(McpResourceContents.textContent("text1"));
    }

    @Mcp.Resource(
            uri = "https://{path2}",
            mediaType = MediaTypes.TEXT_PLAIN_VALUE,
            description = "Resource Template description")
    List<McpResourceContent> resourceTemplate2() {
        return List.of(McpResourceContents.textContent("text2"));
    }
}
