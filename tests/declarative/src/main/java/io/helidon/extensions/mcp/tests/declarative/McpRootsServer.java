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
import io.helidon.extensions.mcp.server.McpRequest;
import io.helidon.extensions.mcp.server.McpResourceContent;
import io.helidon.extensions.mcp.server.McpResourceContents;
import io.helidon.extensions.mcp.server.McpRole;
import io.helidon.extensions.mcp.server.McpRoots;
import io.helidon.extensions.mcp.server.McpToolContent;
import io.helidon.extensions.mcp.server.McpToolContents;

@Mcp.Server
@Mcp.Path("/roots")
class McpRootsServer {
    @Mcp.Tool("Sampling tool")
    List<McpToolContent> tool(McpRoots sampling) {
        return List.of(McpToolContents.textContent(""));
    }

    @Mcp.Tool("Sampling tool")
    List<McpToolContent> tool1(McpRoots sampling, String value) {
        return List.of(McpToolContents.textContent(""));
    }

    @Mcp.Tool("Sampling tool")
    String tool2(McpRoots sampling) {
        return "";
    }

    @Mcp.Tool("Sampling tool")
    String tool3(McpRoots sampling, String value) {
        return "";
    }

    @Mcp.Prompt("Sampling prompt")
    List<McpPromptContent> prompt(McpRoots sampling) {
        return List.of(McpPromptContents.textContent("", McpRole.USER));
    }

    @Mcp.Prompt("Sampling prompt")
    List<McpPromptContent> prompt1(McpRoots sampling, String value) {
        return List.of(McpPromptContents.textContent("", McpRole.USER));
    }

    @Mcp.Prompt("Sampling prompt")
    String prompt2(McpRoots sampling) {
        return "";
    }

    @Mcp.Prompt("Sampling prompt")
    String prompt3(McpRoots sampling, String value) {
        return "";
    }

    @Mcp.Resource(uri = "https://resource",
                  description = "Sampling resource",
                  mediaType = MediaTypes.TEXT_PLAIN_VALUE)
    List<McpResourceContent> resource(McpRoots sampling) {
        return List.of(McpResourceContents.textContent(""));
    }

    @Mcp.Resource(uri = "https://resource1",
                  description = "Sampling resource",
                  mediaType = MediaTypes.TEXT_PLAIN_VALUE)
    List<McpResourceContent> resource1(McpRoots sampling, McpRequest request) {
        return List.of(McpResourceContents.textContent(""));
    }

    @Mcp.Resource(uri = "https://resource2",
                  description = "Sampling resource",
                  mediaType = MediaTypes.TEXT_PLAIN_VALUE)
    String resource2(McpRoots sampling) {
        return "";
    }

    @Mcp.Resource(uri = "https://resource3",
                  description = "Sampling resource",
                  mediaType = MediaTypes.TEXT_PLAIN_VALUE)
    String resource3(McpRoots sampling, McpRequest request) {
        return "";
    }
}
