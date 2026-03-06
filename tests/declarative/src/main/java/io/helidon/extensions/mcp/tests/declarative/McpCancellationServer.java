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
import io.helidon.extensions.mcp.server.McpCancellation;
import io.helidon.extensions.mcp.server.McpLogger;
import io.helidon.extensions.mcp.server.McpPromptResult;
import io.helidon.extensions.mcp.server.McpRequest;
import io.helidon.extensions.mcp.server.McpResourceResult;
import io.helidon.extensions.mcp.server.McpToolResult;

@Mcp.Server
@Mcp.Path("/cancellation")
class McpCancellationServer {

    @Mcp.Tool("Cancellation Tool")
    McpToolResult cancellationTool(McpCancellation cancellation) {
        String reason = cancellation.result().reason();
        return McpToolResult.builder().addTextContent(reason).build();
    }

    @Mcp.Tool("Cancellation Tool")
    String cancellationTool1(McpRequest request, McpCancellation cancellation, McpLogger logger) {
        return request.features().cancellation().result().reason();
    }

    @Mcp.Prompt("Cancellation Prompt")
    McpPromptResult cancellationPrompt(McpCancellation cancellation) {
        String reason = cancellation.result().reason();
        return McpPromptResult.builder().addTextContent(reason).build();
    }

    @Mcp.Prompt("Cancellation Prompt")
    String cancellationPrompt1(McpRequest request, McpCancellation cancellation, McpLogger logger) {
        return request.features().cancellation().result().reason();
    }

    @Mcp.Resource(uri = "file://cancellation",
                  mediaType = MediaTypes.TEXT_PLAIN_VALUE,
                  description = "Cancellation Resource")
    McpResourceResult cancellationResource(McpCancellation cancellation) {
        String reason = cancellation.result().reason();
        return McpResourceResult.builder().addTextContent(reason).build();
    }

    @Mcp.Resource(uri = "file://cancellation1",
                  mediaType = MediaTypes.TEXT_PLAIN_VALUE,
                  description = "Cancellation Resource")
    String cancellationResource1(McpRequest request, McpCancellation cancellation, McpLogger logger) {
        return request.features().cancellation().result().reason();
    }
}
