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

package io.helidon.extensions.mcp.tests;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import io.helidon.common.media.type.MediaType;
import io.helidon.common.media.type.MediaTypes;
import io.helidon.extensions.mcp.server.McpPrompt;
import io.helidon.extensions.mcp.server.McpPromptArgument;
import io.helidon.extensions.mcp.server.McpPromptContent;
import io.helidon.extensions.mcp.server.McpPromptContents;
import io.helidon.extensions.mcp.server.McpRequest;
import io.helidon.extensions.mcp.server.McpResource;
import io.helidon.extensions.mcp.server.McpResourceContent;
import io.helidon.extensions.mcp.server.McpResourceContents;
import io.helidon.extensions.mcp.server.McpRole;
import io.helidon.extensions.mcp.server.McpServerFeature;
import io.helidon.extensions.mcp.server.McpTool;
import io.helidon.extensions.mcp.server.McpToolContent;
import io.helidon.extensions.mcp.server.McpToolContents;
import io.helidon.webserver.http.HttpRouting;

class PaginationServer {
    private PaginationServer() {
    }

    static void setUpRoute(HttpRouting.Builder builder) {
        builder.addFeature(McpServerFeature.builder()
                                   .path("/pagination")
                                   .name("pagination-mcp-server")
                                   .toolsPageSize(1)
                                   .promptsPageSize(1)
                                   .resourcesPageSize(1)
                                   .resourceTemplatesPageSize(1)
                                   .addTool(new Tool("tool-1"))
                                   .addTool(new Tool("tool-2"))
                                   .addPrompt(new Prompt("prompt-1"))
                                   .addPrompt(new Prompt("prompt-2"))
                                   .addResource(new Resource("https://path1"))
                                   .addResource(new Resource("https://path2"))
                                   .addResource(new ResourceTemplate("https://{path1}"))
                                   .addResource(new ResourceTemplate("https://{path2}"))
        );
    }

    private record Tool(String name) implements McpTool {

        @Override
        public String name() {
            return name;
        }

        @Override
        public String description() {
            return "Tool description";
        }

        @Override
        public String schema() {
            return """
                    {
                        "type": "object",
                        "properties": {
                            "foo": {
                                "type": "string"
                            }
                        }
                    }
                    """;
        }

        @Override
        public Function<McpRequest, List<McpToolContent>> tool() {
            return request -> List.of(McpToolContents.textContent("text"));
        }
    }

    private record Prompt(String name) implements McpPrompt {
        @Override
        public String description() {
            return "Prompt description";
        }

        @Override
        public Set<McpPromptArgument> arguments() {
            return Set.of();
        }

        @Override
        public Function<McpRequest, List<McpPromptContent>> prompt() {
            return request -> List.of(McpPromptContents.textContent("text", McpRole.USER));
        }
    }

    private record Resource(String uri) implements McpResource {
        @Override
        public String name() {
            return "Resource";
        }

        @Override
        public String description() {
            return "Resource description";
        }

        @Override
        public MediaType mediaType() {
            return MediaTypes.TEXT_PLAIN;
        }

        @Override
        public Function<McpRequest, List<McpResourceContent>> resource() {
            return request -> List.of(McpResourceContents.textContent("text"));
        }
    }

    private record ResourceTemplate(String template) implements McpResource {
        @Override
        public String uri() {
            return template;
        }

        @Override
        public String name() {
            return "ResourceTemplate";
        }

        @Override
        public String description() {
            return "Resource Template description";
        }

        @Override
        public MediaType mediaType() {
            return MediaTypes.TEXT_PLAIN;
        }

        @Override
        public Function<McpRequest, List<McpResourceContent>> resource() {
            return null;
        }
    }
}
