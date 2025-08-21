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

package io.helidon.mcp.tests.declarative;

import java.util.List;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.mcp.server.Mcp;
import io.helidon.mcp.server.McpCompletionContent;
import io.helidon.mcp.server.McpCompletionContents;
import io.helidon.mcp.server.McpPromptContent;
import io.helidon.mcp.server.McpPromptContents;
import io.helidon.mcp.server.McpResourceContent;
import io.helidon.mcp.server.McpResourceContents;
import io.helidon.mcp.server.McpRole;
import io.helidon.mcp.server.McpToolContent;
import io.helidon.mcp.server.McpToolContents;

@Mcp.Server("mcp-weather-server")
class McpMixedComponentServer {

    @Mcp.Tool("Tool description")
    List<McpToolContent> weatherAlert(String state, Alert alert) {
        return List.of(McpToolContents.textContent("state: %s, alert name: %s".formatted(state, alert.name)));
    }

    @Mcp.Prompt("Prompt description")
    List<McpPromptContent> weatherInTown(@Mcp.Description("town's name") String town) {
        return List.of(McpPromptContents.textContent("Town: " + town, McpRole.USER));
    }

    @Mcp.Resource(uri = "resource:resource",
                  mediaType = MediaTypes.TEXT_PLAIN_VALUE,
                  description = "Resource description")
    List<McpResourceContent> weatherAlerts() {
        return List.of(McpResourceContents.textContent("Resource content"));
    }

    @Mcp.Completion("weatherInTown")
    McpCompletionContent completion() {
        return McpCompletionContents.completion();
    }

    @Mcp.JsonSchema
    @Mcp.Description("Weather alert")
    public static class Alert {
        public String name;
        public int priority;
        public Location location;
    }

    @Mcp.JsonSchema
    public static class Location {
        public int latitude;
        public int longitude;
    }
}
