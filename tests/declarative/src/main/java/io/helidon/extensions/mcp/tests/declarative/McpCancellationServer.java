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
import io.helidon.extensions.mcp.server.McpCancellation;
import io.helidon.extensions.mcp.server.McpLogger;
import io.helidon.extensions.mcp.server.McpPromptContent;
import io.helidon.extensions.mcp.server.McpPromptContents;
import io.helidon.extensions.mcp.server.McpRequest;
import io.helidon.extensions.mcp.server.McpResourceContent;
import io.helidon.extensions.mcp.server.McpResourceContents;
import io.helidon.extensions.mcp.server.McpRole;
import io.helidon.extensions.mcp.server.McpToolContent;
import io.helidon.extensions.mcp.server.McpToolContents;

@Mcp.Server
@Mcp.Path("/cancellation")
class McpCancellationServer {

    @Mcp.Tool("Cancellation Tool")
    List<McpToolContent> cancellationTool(McpCancellation cancellation) {
        String reason = cancellation.result().reason();
        return List.of(McpToolContents.textContent(reason));
    }

    @Mcp.Tool("Cancellation Tool")
    String cancellationTool1(McpRequest request, McpCancellation cancellation, McpLogger logger) {
        return request.features().cancellation().result().reason();
    }

    @Mcp.Prompt("Cancellation Prompt")
    List<McpPromptContent> cancellationPrompt(McpCancellation cancellation) {
        String reason = cancellation.result().reason();
        return List.of(McpPromptContents.textContent(reason, McpRole.USER));
    }

    @Mcp.Prompt("Cancellation Prompt")
    String cancellationPrompt1(McpRequest request, McpCancellation cancellation, McpLogger logger) {
        return request.features().cancellation().result().reason();
    }

    @Mcp.Resource(uri = "file://cancellation",
                  mediaType = MediaTypes.TEXT_PLAIN_VALUE,
                  description = "Cancellation Resource")
    List<McpResourceContent> cancellationResource(McpCancellation cancellation) {
        String reason = cancellation.result().reason();
        return List.of(McpResourceContents.textContent(reason));
    }

    @Mcp.Resource(uri = "file://cancellation1",
                  mediaType = MediaTypes.TEXT_PLAIN_VALUE,
                  description = "Cancellation Resource")
    String cancellationResource1(McpRequest request, McpCancellation cancellation, McpLogger logger) {
        return request.features().cancellation().result().reason();
    }
}
