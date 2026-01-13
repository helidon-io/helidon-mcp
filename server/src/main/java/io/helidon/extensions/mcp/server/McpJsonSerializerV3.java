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
package io.helidon.extensions.mcp.server;

import java.util.Set;

import jakarta.json.JsonObjectBuilder;

class McpJsonSerializerV3 extends McpJsonSerializerV2 {

    @Override
    public JsonObjectBuilder toJson(Set<McpCapability> capabilities, McpServerConfig config) {
        return super.toJson(capabilities, config)
                .add("protocolVersion", McpProtocolVersion.VERSION_2025_06_18.text());
    }

    @Override
    public JsonObjectBuilder toJson(McpTool tool) {
        var builder = super.toJson(tool);
        if (!tool.title().isBlank()) {
            builder.add("title", tool.title());
        }
        return builder;
    }

    @Override
    public JsonObjectBuilder toJson(McpResource resource) {
        var builder = super.toJson(resource);
        if (!resource.title().isBlank()) {
            builder.add("title", resource.title());
        }
        return builder;
    }

    @Override
    public JsonObjectBuilder toJson(McpPrompt prompt) {
        var builder = super.toJson(prompt);
        if (!prompt.title().isBlank()) {
            builder.add("title", prompt.title());
        }
        return builder;
    }

    @Override
    public JsonObjectBuilder toJson(McpPromptArgument argument) {
        var builder = super.toJson(argument);
        if (!argument.title().isBlank()) {
            builder.add("title", argument.title());
        }
        return builder;
    }
}
