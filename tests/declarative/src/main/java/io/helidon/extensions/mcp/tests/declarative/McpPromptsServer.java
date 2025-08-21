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

import io.helidon.extensions.mcp.server.Mcp;
import io.helidon.extensions.mcp.server.McpFeatures;
import io.helidon.extensions.mcp.server.McpPromptContent;
import io.helidon.extensions.mcp.server.McpPromptContents;
import io.helidon.extensions.mcp.server.McpRole;

@Mcp.Server
@Mcp.Path("/prompts")
class McpPromptsServer {
    public static final String PROMPT_CONTENT = "prompt content";
    public static final String PROMPT_DESCRIPTION = "prompt description";

    @Mcp.Prompt(PROMPT_DESCRIPTION)
    String prompt(String prompt) {
        return PROMPT_CONTENT;
    }

    @Mcp.Prompt(PROMPT_DESCRIPTION)
    String prompt1(McpFeatures features) {
        return PROMPT_CONTENT;
    }

    @Mcp.Prompt(PROMPT_DESCRIPTION)
    String prompt2(String prompt, McpFeatures features) {
        return PROMPT_CONTENT;
    }

    @Mcp.Prompt(PROMPT_DESCRIPTION)
    String prompt3() {
        return PROMPT_CONTENT;
    }

    @Mcp.Prompt(PROMPT_DESCRIPTION)
    @Mcp.Role(McpRole.USER)
    String promptRoleUser() {
        return PROMPT_CONTENT;
    }

    @Mcp.Prompt(PROMPT_DESCRIPTION)
    @Mcp.Role(McpRole.ASSISTANT)
    String promptRoleAssistant() {
        return PROMPT_CONTENT;
    }

    @Mcp.Prompt(PROMPT_DESCRIPTION)
    @Mcp.Role
    String promptRoleDefault() {
        return PROMPT_CONTENT;
    }

    @Mcp.Prompt(PROMPT_DESCRIPTION)
    List<McpPromptContent> prompt4(String prompt) {
        return List.of(McpPromptContents.textContent(PROMPT_CONTENT, McpRole.USER));
    }

    @Mcp.Prompt(PROMPT_DESCRIPTION)
    List<McpPromptContent> prompt5(McpFeatures features) {
        return List.of(McpPromptContents.textContent(PROMPT_CONTENT, McpRole.USER));
    }

    @Mcp.Prompt(PROMPT_DESCRIPTION)
    List<McpPromptContent> prompt6(McpFeatures features) {
        return List.of(McpPromptContents.textContent(PROMPT_CONTENT, McpRole.USER));
    }
}
