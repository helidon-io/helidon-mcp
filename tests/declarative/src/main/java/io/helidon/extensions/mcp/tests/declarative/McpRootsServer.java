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
import io.helidon.extensions.mcp.server.McpRequest;
import io.helidon.extensions.mcp.server.McpResourceResult;
import io.helidon.extensions.mcp.server.McpRoots;
import io.helidon.extensions.mcp.server.McpToolResult;

@Mcp.Server
@Mcp.Path("/roots")
class McpRootsServer {
    @Mcp.Tool("Roots tool")
    McpToolResult tool(McpRoots roots) {
        return McpToolResult.builder().addTextContent("").build();
    }

    @Mcp.Tool("Roots tool")
    McpToolResult tool1(McpRoots roots, String value) {
        return McpToolResult.builder().addTextContent("").build();
    }

    @Mcp.Tool("Roots tool")
    String tool2(McpRoots roots) {
        return "";
    }

    @Mcp.Tool("Roots tool")
    String tool3(McpRoots roots, String value) {
        return "";
    }

    @Mcp.Prompt("Roots prompt")
    McpPromptResult prompt(McpRoots roots) {
        return McpPromptResult.builder().addTextContent("").build();
    }

    @Mcp.Prompt("Roots prompt")
    McpPromptResult prompt1(McpRoots roots, String value) {
        return McpPromptResult.builder().addTextContent("").build();
    }

    @Mcp.Prompt("Roots prompt")
    String prompt2(McpRoots roots) {
        return "";
    }

    @Mcp.Prompt("Roots prompt")
    String prompt3(McpRoots roots, String value) {
        return "";
    }

    @Mcp.Resource(uri = "https://resource",
                  description = "Roots resource",
                  mediaType = MediaTypes.TEXT_PLAIN_VALUE)
    McpResourceResult resource(McpRoots roots) {
        return McpResourceResult.create("");
    }

    @Mcp.Resource(uri = "https://resource1",
                  description = "Roots resource",
                  mediaType = MediaTypes.TEXT_PLAIN_VALUE)
    McpResourceResult resource1(McpRoots roots, McpRequest request) {
        return McpResourceResult.create("");
    }

    @Mcp.Resource(uri = "https://resource2",
                  description = "Roots resource",
                  mediaType = MediaTypes.TEXT_PLAIN_VALUE)
    String resource2(McpRoots roots) {
        return "";
    }

    @Mcp.Resource(uri = "https://resource3",
                  description = "Roots resource",
                  mediaType = MediaTypes.TEXT_PLAIN_VALUE)
    String resource3(McpRoots roots, McpRequest request) {
        return "";
    }
}
