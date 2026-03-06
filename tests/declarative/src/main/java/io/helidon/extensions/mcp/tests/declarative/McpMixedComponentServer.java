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
import io.helidon.extensions.mcp.server.McpCompletionResult;
import io.helidon.extensions.mcp.server.McpPromptResult;
import io.helidon.extensions.mcp.server.McpResourceResult;
import io.helidon.extensions.mcp.server.McpToolResult;
import io.helidon.json.schema.JsonSchema;

@Mcp.Server("mcp-weather-server")
class McpMixedComponentServer {

    @Mcp.Tool("Tool description")
    McpToolResult weatherAlert(String state, Alert alert) {
        return McpToolResult.builder()
                .addTextContent("state: %s, alert name: %s".formatted(state, alert.name))
                .build();
    }

    @Mcp.Prompt("Prompt description")
    McpPromptResult weatherInTown(@Mcp.Description("town's name") String town) {
        return McpPromptResult.builder().addTextContent("Town: " + town).build();
    }

    @Mcp.Resource(uri = "resource:resource",
                  mediaType = MediaTypes.TEXT_PLAIN_VALUE,
                  description = "Resource description")
    McpResourceResult weatherAlerts() {
        return McpResourceResult.builder().addTextContent("Resource content").build();
    }

    @Mcp.Completion("weatherInTown")
    McpCompletionResult completion() {
        return McpCompletionResult.create();
    }

    @JsonSchema.Schema
    public static class Alert {
        public String name;
        public int priority;
        public Location location;
    }

    @JsonSchema.Schema
    public static class Location {
        public int latitude;
        public int longitude;
    }
}
