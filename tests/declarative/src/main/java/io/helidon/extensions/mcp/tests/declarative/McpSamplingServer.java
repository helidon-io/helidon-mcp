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
import io.helidon.extensions.mcp.server.McpSampling;
import io.helidon.extensions.mcp.server.McpToolResult;

@Mcp.Server
@Mcp.Path("/sampling")
class McpSamplingServer {

    @Mcp.Tool("Sampling tool")
    McpToolResult tool(McpSampling sampling) {
        return McpToolResult.create();
    }

    @Mcp.Tool("Sampling tool")
    McpToolResult tool1(McpSampling sampling, String value) {
        return McpToolResult.create();
    }

    @Mcp.Tool("Sampling tool")
    String tool4(McpSampling sampling) {
        return "";
    }

    @Mcp.Tool("Sampling tool")
    String tool5(McpSampling sampling, String value) {
        return "";
    }

    @Mcp.Prompt("Sampling prompt")
    McpPromptResult prompt(McpSampling sampling) {
        return McpPromptResult.create();
    }

    @Mcp.Prompt("Sampling prompt")
    McpPromptResult prompt1(McpSampling sampling, String value) {
        return McpPromptResult.create();
    }

    @Mcp.Prompt("Sampling prompt")
    String prompt4(McpSampling sampling) {
        return "";
    }

    @Mcp.Prompt("Sampling prompt")
    String prompt5(McpSampling sampling, String value) {
        return "";
    }

    @Mcp.Resource(uri = "https://example.com",
                  description = "Sampling resource",
                  mediaType = MediaTypes.TEXT_PLAIN_VALUE)
    McpResourceResult resource(McpSampling sampling) {
        return McpResourceResult.create();
    }

    @Mcp.Resource(uri = "https://example.com",
                  description = "Sampling resource",
                  mediaType = MediaTypes.TEXT_PLAIN_VALUE)
    McpResourceResult resource1(McpSampling sampling, McpRequest request) {
        return McpResourceResult.create();
    }

    @Mcp.Resource(uri = "https://example.com",
                  description = "Sampling resource",
                  mediaType = MediaTypes.TEXT_PLAIN_VALUE)
    String resource4(McpSampling sampling) {
        return "";
    }

    @Mcp.Resource(uri = "https://example.com",
                  description = "Sampling resource",
                  mediaType = MediaTypes.TEXT_PLAIN_VALUE)
    String resource5(McpSampling sampling, McpRequest request) {
        return "";
    }
}
