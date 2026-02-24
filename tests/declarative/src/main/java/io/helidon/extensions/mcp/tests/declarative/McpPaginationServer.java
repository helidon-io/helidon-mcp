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

package io.helidon.extensions.mcp.tests.declarative;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.extensions.mcp.server.Mcp;
import io.helidon.extensions.mcp.server.McpPromptResult;
import io.helidon.extensions.mcp.server.McpResourceResult;
import io.helidon.extensions.mcp.server.McpToolResult;

@Mcp.Server
@Mcp.Path("/pagination")
@Mcp.ToolsPageSize(1)
@Mcp.PromptsPageSize(1)
@Mcp.ResourcesPageSize(1)
@Mcp.ResourceTemplatesPageSize(1)
class McpPaginationServer {

    @Mcp.Tool("Tool description")
    McpToolResult tool1() {
        return McpToolResult.builder().addTextContent("text1").build();
    }

    @Mcp.Tool("Tool description")
    McpToolResult tool2() {
        return McpToolResult.builder().addTextContent("text2").build();
    }

    @Mcp.Prompt("Prompt description")
    McpPromptResult prompt1() {
        return McpPromptResult.builder().addTextContent("text1").build();
    }

    @Mcp.Prompt("Prompt description")
    McpPromptResult prompt2() {
        return McpPromptResult.builder().addTextContent("text2").build();
    }

    @Mcp.Resource(
            uri = "https://path1",
            mediaType = MediaTypes.TEXT_PLAIN_VALUE,
            description = "Resource description")
    McpResourceResult resource1() {
        return McpResourceResult.builder().addTextContent("text1").build();
    }

    @Mcp.Resource(
            uri = "https://path2",
            mediaType = MediaTypes.TEXT_PLAIN_VALUE,
            description = "Resource description")
    McpResourceResult resource2() {
        return McpResourceResult.builder().addTextContent("text2").build();
    }

    @Mcp.Resource(
            uri = "https://{path1}",
            mediaType = MediaTypes.TEXT_PLAIN_VALUE,
            description = "Resource Template description")
    McpResourceResult resourceTemplate1() {
        return McpResourceResult.builder().addTextContent("text1").build();
    }

    @Mcp.Resource(
            uri = "https://{path2}",
            mediaType = MediaTypes.TEXT_PLAIN_VALUE,
            description = "Resource Template description")
    McpResourceResult resourceTemplate2() {
        return McpResourceResult.builder().addTextContent("text2").build();
    }
}
