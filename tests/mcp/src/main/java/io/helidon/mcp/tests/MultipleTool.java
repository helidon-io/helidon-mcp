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
import java.util.function.Function;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.mcp.server.McpParameters;
import io.helidon.mcp.server.McpRequest;
import io.helidon.mcp.server.McpResourceContents;
import io.helidon.mcp.server.McpServerFeature;
import io.helidon.mcp.server.McpTool;
import io.helidon.mcp.server.McpToolContent;
import io.helidon.webserver.http.HttpRouting;

import static io.helidon.mcp.server.McpToolContents.imageContent;
import static io.helidon.mcp.server.McpToolContents.resourceContent;
import static io.helidon.mcp.server.McpToolContents.textContent;

class MultipleTool {

    static final String SIMPLE_SCHEMA = """
                {
                    "type": "object",
                    "properties": {
                        "schema": { "type" : "string" }
                    }
                }
            """;

    private MultipleTool() {
    }

    static void setUpRoute(HttpRouting.Builder builder) {
        builder.addFeature(McpServerFeature.builder()
                                   .path("/")
                                   .addTool(tool -> tool.name("tool1")
                                           .description("Tool 1")
                                           .schema(SIMPLE_SCHEMA)
                                           .tool(request ->
                                                         List.of(imageContent("binary",
                                                                              MediaTypes.APPLICATION_OCTET_STREAM))))
                                   .addTool(tool -> tool.name("tool2")
                                           .description("Tool 2")
                                           .schema(SIMPLE_SCHEMA)
                                           .tool(request ->
                                                         List.of(resourceContent(
                                                                 "http://resource",
                                                                 McpResourceContents.textContent(
                                                                         "resource")))))
                                   .addTool(tool -> tool.name("tool3")
                                           .description("Tool 3")
                                           .schema(SIMPLE_SCHEMA)
                                           .tool(request -> List.of(
                                                   imageContent("binary", MediaTypes.APPLICATION_OCTET_STREAM),
                                                   resourceContent("http://resource",
                                                                   McpResourceContents.textContent("resource")),
                                                   textContent("text"))))
                                   .addTool(new TownTool()));
    }

    static final class TownTool implements McpTool {

        @Override
        public String name() {
            return "tool4";
        }

        @Override
        public String description() {
            return "Tool 4";
        }

        @Override
        public String schema() {
            return """
                    { "type" : "object",
                      "properties" : {
                        "name": { "type" : "string" },
                        "population": { "type" : "number" }
                      }
                    }
                    """;
        }

        @Override
        public Function<McpRequest, List<McpToolContent>> tool() {
            return this::process;
        }

        List<McpToolContent> process(McpRequest request) {
            McpParameters parameters = request.parameters();
            String name = parameters.get("name").asString().orElse("unknown");
            int population = parameters.get("population").asInt().orElse(-1);
            String content = String.format("%s has a population of %d inhabitants", name, population);
            return List.of(textContent(content));
        }
    }
}
