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
package io.helidon.extensions.mcp.server;

import io.helidon.builder.api.Prototype;

/**
 * MCP sampling tool use content.
 */
@Prototype.Blueprint
interface McpSamplingToolUseContentBlueprint extends McpSamplingContent {
    /**
     * Unique identifier for this tool use.
     *
     * @return tool use identifier
     */
    String id();

    /**
     * Name of the tool to invoke.
     *
     * @return tool name
     */
    String name();

    /**
     * Arguments to pass to the tool.
     *
     * @return tool arguments
     */
    McpParameters input();

    @Override
    default McpSamplingContentType type() {
        return McpSamplingContentType.TOOL_USE;
    }
}
