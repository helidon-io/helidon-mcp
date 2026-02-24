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
import io.helidon.extensions.mcp.server.McpFeatures;
import io.helidon.extensions.mcp.server.McpLogger;
import io.helidon.extensions.mcp.server.McpPromptResult;
import io.helidon.extensions.mcp.server.McpResourceResult;
import io.helidon.extensions.mcp.server.McpToolResult;

@Mcp.Server
@Mcp.Path("/logging")
class McpLoggingFeatureServer {

    @Mcp.Tool("Tool description")
    String loggingTool(McpFeatures features) {
        features.logger().info("Logging notification");
        return "Hello World";
    }

    @Mcp.Tool("Tool description")
    McpToolResult loggerTool(McpLogger logger) {
        logger.info("Logging notification");
        return McpToolResult.builder().addTextContent("Hello World").build();
    }

    @Mcp.Prompt("Prompt description")
    String loggingPrompt(McpFeatures features) {
        features.logger().info("Logging notification");
        return "Hello World";
    }

    @Mcp.Prompt("Prompt description")
    McpPromptResult loggerPrompt(McpLogger logger) {
        logger.info("Logging notification");
        return McpPromptResult.builder().addTextContent("Hello World").build();
    }

    @Mcp.Resource(
            uri = "file://hello/world",
            mediaType = MediaTypes.TEXT_PLAIN_VALUE,
            description = "Resource description")
    String loggingResource(McpFeatures features) {
        features.logger().info("Logging notification");
        return "Hello World";
    }

    @Mcp.Resource(
            uri = "file://hello/world1",
            mediaType = MediaTypes.TEXT_PLAIN_VALUE,
            description = "Resource description")
    McpResourceResult loggerResource(McpLogger logger) {
        logger.info("Logging notification");
        return McpResourceResult.builder().addTextContent("Hello World").build();
    }
}
