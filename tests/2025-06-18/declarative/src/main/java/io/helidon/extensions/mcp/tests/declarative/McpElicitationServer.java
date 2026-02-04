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

import java.util.List;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.extensions.mcp.server.Mcp;
import io.helidon.extensions.mcp.server.McpCompletionContent;
import io.helidon.extensions.mcp.server.McpCompletionContents;
import io.helidon.extensions.mcp.server.McpElicitation;
import io.helidon.extensions.mcp.server.McpPromptContent;
import io.helidon.extensions.mcp.server.McpPromptContents;
import io.helidon.extensions.mcp.server.McpResourceContent;
import io.helidon.extensions.mcp.server.McpResourceContents;
import io.helidon.extensions.mcp.server.McpRole;
import io.helidon.extensions.mcp.server.McpToolContents;
import io.helidon.extensions.mcp.server.McpToolResult;

@Mcp.Server
@Mcp.Path("/elicitation")
class McpElicitationServer {

    @Mcp.Tool("Elicitation Tool")
    McpToolResult elicitationTool(McpElicitation elicitation) {
        return McpToolResult.builder().addContent(McpToolContents.textContent("foo")).build();
    }

    @Mcp.Prompt("Elicitation Prompt")
    List<McpPromptContent> elicitationPrompt(McpElicitation elicitation) {
        return List.of(McpPromptContents.textContent("foo", McpRole.USER));
    }

    @Mcp.Resource(uri = "https://foo",
                  description = "Elicitation Resource",
                  mediaType = MediaTypes.TEXT_PLAIN_VALUE)
    List<McpResourceContent> elicitationResource(McpElicitation elicitation) {
        return List.of(McpResourceContents.textContent("foo"));
    }

    @Mcp.Completion("foo")
    McpCompletionContent elicitationCompletion(McpElicitation elicitation) {
        return McpCompletionContents.completion("foo");
    }
}
