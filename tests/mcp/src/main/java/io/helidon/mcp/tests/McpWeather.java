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

package io.helidon.mcp.tests;

import java.util.List;

import io.helidon.common.media.type.MediaType;
import io.helidon.common.media.type.MediaTypes;
import io.helidon.mcp.server.McpCompletionContents;
import io.helidon.mcp.server.McpFeatures;
import io.helidon.mcp.server.McpPromptContent;
import io.helidon.mcp.server.McpPromptContents;
import io.helidon.mcp.server.McpRequest;
import io.helidon.mcp.server.McpResourceContent;
import io.helidon.mcp.server.McpResourceContents;
import io.helidon.mcp.server.McpRole;
import io.helidon.mcp.server.McpServerFeature;
import io.helidon.mcp.server.McpToolContent;
import io.helidon.mcp.server.McpToolContents;
import io.helidon.webserver.http.HttpRouting;

class McpWeather {
    static final String PROTOCOL_VERSION = "2024-11-05";
    static final String SERVER_VERSION = "0.0.1";
    static final String SERVER_NAME = "helidon-mcp-server";
    static final String TOOL_NAME = "weather-alerts";
    static final String TOOL_DESCRIPTION = "Get weather from town";
    static final String PROMPT_ARGUMENT_NAME = "town";
    static final String PROMPT_NAME = "weather-in-town";
    static final String PROMPT_ARGUMENT_DESCRIPTION = "town's name";
    static final String PROMPT_DESCRIPTION = "Get the weather in a specific town";
    static final String RESOURCE_NAME = "alerts-list";
    static final String RESOURCE_URI = "file:///documents/alerts.txt";
    static final String RESOURCE_DESCRIPTION = "Get the list of all weather alerts";
    static final MediaType RESOURCE_MIME_TYPE = MediaTypes.TEXT_PLAIN;

    private McpWeather() {
    }

    static void setUpRoute(HttpRouting.Builder builder) {
        builder.addFeature(McpServerFeature.builder()
                                   .name(SERVER_NAME)
                                   .version(SERVER_VERSION)
                                   .path("/")
                                   .addTool(tool -> tool.name(TOOL_NAME)
                                           .description(TOOL_DESCRIPTION)
                                           .tool(McpWeather::process)
                                           .schema("""
                                                 {
                                                   "type": "object",
                                                   "properties": {
                                                     "town": {
                                                       "type": "string"
                                                     }
                                                   },
                                                   "required": [ "town" ]
                                                  }"""))

                                   .addResource(resource -> resource.name(RESOURCE_NAME)
                                           .description(RESOURCE_DESCRIPTION)
                                           .uri(RESOURCE_URI)
                                           .mediaType(RESOURCE_MIME_TYPE)
                                           .resource(McpWeather::read))

                                   .addPrompt(prompt -> prompt.name(PROMPT_NAME)
                                           .description(PROMPT_DESCRIPTION)
                                           .addArgument(arg -> arg.name(PROMPT_ARGUMENT_NAME)
                                                   .description(PROMPT_ARGUMENT_DESCRIPTION)
                                                   .required(true))
                                           .prompt(McpWeather::prompt))

                                   .addCompletion(completion -> completion
                                           .reference(PROMPT_NAME)
                                           .completion(request -> McpCompletionContents.completion("foo"))));
    }

    static List<McpToolContent> process(McpRequest request) {
        String town = request.parameters().get("town").asString().orElse("unknown");
        return List.of(McpToolContents.textContent("There is a hurricane in " + town));
    }

    static List<McpPromptContent> prompt(McpRequest request) {
        String town = request.parameters().get("town").asString().orElse("unknown");
        String content = "What is the weather like in %s ?".formatted(town);
        return List.of(McpPromptContents.textContent(content, McpRole.USER));
    }

    static List<McpResourceContent> read(McpFeatures features) {
        return List.of(McpResourceContents.textContent("There are severe weather alerts in Praha"));
    }
}
