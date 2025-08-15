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

package io.helidon.mcp.codegen;

import io.helidon.common.types.TypeName;

final class McpTypes {

    private McpTypes() {
    }

    //Annotations
    static final TypeName MCP_URI = TypeName.create("io.helidon.mcp.server.Mcp.URI");
    static final TypeName MCP_NAME = TypeName.create("io.helidon.mcp.server.Mcp.Name");
    static final TypeName MCP_PATH = TypeName.create("io.helidon.mcp.server.Mcp.Path");
    static final TypeName MCP_ROLE = TypeName.create("io.helidon.mcp.server.Mcp.Role");
    static final TypeName MCP_TOOL = TypeName.create("io.helidon.mcp.server.Mcp.Tool");
    static final TypeName MCP_PARAM = TypeName.create("io.helidon.mcp.server.Mcp.Param");
    static final TypeName MCP_SERVER = TypeName.create("io.helidon.mcp.server.Mcp.Server");
    static final TypeName MCP_PROMPT = TypeName.create("io.helidon.mcp.server.Mcp.Prompt");
    static final TypeName MCP_VERSION = TypeName.create("io.helidon.mcp.server.Mcp.Version");
    static final TypeName MCP_RESOURCE = TypeName.create("io.helidon.mcp.server.Mcp.Resource");
    static final TypeName MCP_SUBSCRIBE = TypeName.create("io.helidon.mcp.server.Mcp.Subscribe");
    static final TypeName MCP_MEDIA_TYPE = TypeName.create("io.helidon.mcp.server.Mcp.MediaType");
    static final TypeName MCP_COMPLETION = TypeName.create("io.helidon.mcp.server.Mcp.Completion");
    static final TypeName MCP_JSON_SCHEMA = TypeName.create("io.helidon.mcp.server.Mcp.JsonSchema");
    static final TypeName MCP_DESCRIPTION = TypeName.create("io.helidon.mcp.server.Mcp.Description");
    static final TypeName MCP_NOTIFICATION = TypeName.create("io.helidon.mcp.server.Mcp.Notification");
    //Implementations
    static final TypeName MCP_ROLE_ENUM = TypeName.create("io.helidon.mcp.server.McpRole");
    static final TypeName MCP_LOGGER = TypeName.create("io.helidon.mcp.server.McpLogger");
    static final TypeName MCP_REQUEST = TypeName.create("io.helidon.mcp.server.McpRequest");
    static final TypeName MCP_PROGRESS = TypeName.create("io.helidon.mcp.server.McpProgress");
    static final TypeName MCP_TOOL_INTERFACE = TypeName.create("io.helidon.mcp.server.McpTool");
    static final TypeName MCP_FEATURES = TypeName.create("io.helidon.mcp.server.McpFeatures");
    static final TypeName MCP_PROMPT_INTERFACE = TypeName.create("io.helidon.mcp.server.McpPrompt");
    static final TypeName MCP_TOOL_CONTENT = TypeName.create("io.helidon.mcp.server.McpToolContent");
    static final TypeName MCP_PARAMETERS = TypeName.create("io.helidon.mcp.server.McpParameters");
    static final TypeName MCP_TOOL_CONTENTS = TypeName.create("io.helidon.mcp.server.McpToolContents");
    static final TypeName MCP_RESOURCE_INTERFACE = TypeName.create("io.helidon.mcp.server.McpResource");
    static final TypeName MCP_PROMPT_CONTENT = TypeName.create("io.helidon.mcp.server.McpPromptContent");
    static final TypeName MCP_PROMPT_CONTENTS = TypeName.create("io.helidon.mcp.server.McpPromptContents");
    static final TypeName MCP_COMPLETION_INTERFACE = TypeName.create("io.helidon.mcp.server.McpCompletion");
    static final TypeName MCP_RESOURCE_CONTENT = TypeName.create("io.helidon.mcp.server.McpResourceContent");
    static final TypeName MCP_RESOURCE_CONTENTS = TypeName.create("io.helidon.mcp.server.McpResourceContents");
    static final TypeName MCP_COMPLETION_CONTENT = TypeName.create("io.helidon.mcp.server.McpCompletionContent");
    static final TypeName MCP_COMPLETION_CONTENTS = TypeName.create("io.helidon.mcp.server.McpCompletionContents");
}
