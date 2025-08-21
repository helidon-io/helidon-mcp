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

package io.helidon.extensions.mcp.codegen;

import io.helidon.common.types.TypeName;

final class McpTypes {

    private McpTypes() {
    }

    //Annotations
    static final TypeName MCP_NAME = TypeName.create("io.helidon.extensions.mcp.server.Mcp.Name");
    static final TypeName MCP_PATH = TypeName.create("io.helidon.extensions.mcp.server.Mcp.Path");
    static final TypeName MCP_ROLE = TypeName.create("io.helidon.extensions.mcp.server.Mcp.Role");
    static final TypeName MCP_TOOL = TypeName.create("io.helidon.extensions.mcp.server.Mcp.Tool");
    static final TypeName MCP_SERVER = TypeName.create("io.helidon.extensions.mcp.server.Mcp.Server");
    static final TypeName MCP_PROMPT = TypeName.create("io.helidon.extensions.mcp.server.Mcp.Prompt");
    static final TypeName MCP_VERSION = TypeName.create("io.helidon.extensions.mcp.server.Mcp.Version");
    static final TypeName MCP_RESOURCE = TypeName.create("io.helidon.extensions.mcp.server.Mcp.Resource");
    static final TypeName MCP_COMPLETION = TypeName.create("io.helidon.extensions.mcp.server.Mcp.Completion");
    static final TypeName MCP_JSON_SCHEMA = TypeName.create("io.helidon.extensions.mcp.server.Mcp.JsonSchema");
    static final TypeName MCP_DESCRIPTION = TypeName.create("io.helidon.extensions.mcp.server.Mcp.Description");
    //Implementations
    static final TypeName MCP_LOGGER = TypeName.create("io.helidon.extensions.mcp.server.McpLogger");
    static final TypeName MCP_ROLE_ENUM = TypeName.create("io.helidon.extensions.mcp.server.McpRole");
    static final TypeName MCP_FEATURES = TypeName.create("io.helidon.extensions.mcp.server.McpFeatures");
    static final TypeName MCP_PROGRESS = TypeName.create("io.helidon.extensions.mcp.server.McpProgress");
    static final TypeName MCP_TOOL_INTERFACE = TypeName.create("io.helidon.extensions.mcp.server.McpTool");
    static final TypeName MCP_PARAMETERS = TypeName.create("io.helidon.extensions.mcp.server.McpParameters");
    static final TypeName MCP_PROMPT_INTERFACE = TypeName.create("io.helidon.extensions.mcp.server.McpPrompt");
    static final TypeName MCP_SERVER_CONFIG = TypeName.create("io.helidon.extensions.mcp.server.McpServerConfig");
    static final TypeName MCP_TOOL_CONTENTS = TypeName.create("io.helidon.extensions.mcp.server.McpToolContents");
    static final TypeName MCP_RESOURCE_INTERFACE = TypeName.create("io.helidon.extensions.mcp.server.McpResource");
    static final TypeName MCP_PROMPT_CONTENTS = TypeName.create("io.helidon.extensions.mcp.server.McpPromptContents");
    static final TypeName MCP_PROMPT_ARGUMENT = TypeName.create("io.helidon.extensions.mcp.server.McpPromptArgument");
    static final TypeName MCP_COMPLETION_INTERFACE = TypeName.create("io.helidon.extensions.mcp.server.McpCompletion");
    static final TypeName MCP_RESOURCE_CONTENTS = TypeName.create("io.helidon.extensions.mcp.server.McpResourceContents");
    static final TypeName MCP_COMPLETION_CONTENTS = TypeName.create("io.helidon.extensions.mcp.server.McpCompletionContents");
    //others
    static final TypeName HTTP_FEATURE = TypeName.create("io.helidon.webserver.http.HttpFeature");
    static final TypeName HELIDON_MEDIA_TYPE = TypeName.create("io.helidon.common.media.type.MediaType");
    static final TypeName HELIDON_MEDIA_TYPES = TypeName.create("io.helidon.common.media.type.MediaTypes");
    static final TypeName HTTP_ROUTING_BUILDER = TypeName.create("io.helidon.webserver.http.HttpRouting.Builder");
    static final TypeName SET_MCP_PROMPT_ARGUMENT = TypeName.create(
            "java.util.Set<io.helidon.extensions.mcp.server.McpPromptArgument>");
    static final TypeName FUNCTION_REQUEST_COMPLETION_CONTENT = TypeName.create(
            "java.util.function.Function<"
                    + "io.helidon.extensions.mcp.server.McpRequest, "
                    + "io.helidon.extensions.mcp.server.McpCompletionContent>");
    static final TypeName FUNCTION_REQUEST_LIST_RESOURCE_CONTENT = TypeName.create(
            "java.util.function.Function<"
                    + "io.helidon.extensions.mcp.server.McpRequest, "
                    + "java.util.List<io.helidon.extensions.mcp.server.McpResourceContent>>");
    static final TypeName FUNCTION_REQUEST_LIST_TOOL_CONTENT = TypeName.create(
            "java.util.function.Function<"
                    + "io.helidon.extensions.mcp.server.McpRequest, "
                    + "java.util.List<io.helidon.extensions.mcp.server.McpToolContent>>");
    static final TypeName FUNCTION_REQUEST_LIST_PROMPT_CONTENT = TypeName.create(
            "java.util.function.Function<"
                    + "io.helidon.extensions.mcp.server.McpRequest, "
                    + "java.util.List<io.helidon.extensions.mcp.server.McpPromptContent>>");
}
