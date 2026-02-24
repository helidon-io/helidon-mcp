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
package io.helidon.extensions.mcp.tests.common;

import java.net.URI;
import java.util.Optional;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.extensions.mcp.server.McpParameters;
import io.helidon.extensions.mcp.server.McpServerFeature;
import io.helidon.extensions.mcp.server.McpTool;
import io.helidon.extensions.mcp.server.McpToolRequest;
import io.helidon.extensions.mcp.server.McpToolResult;
import io.helidon.json.schema.Schema;
import io.helidon.json.schema.SchemaNumber;
import io.helidon.json.schema.SchemaString;
import io.helidon.webserver.http.HttpRouting;


/**
 * Tool server test.
 */
public class MultipleTool {
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

    /**
     * Setup webserver routing.
     *
     * @param builder routing builder
     */
    public static void setUpRoute(HttpRouting.Builder builder) {
        builder.addFeature(McpServerFeature.builder()
                                   .path("/")
                                   .addTool(tool -> tool.name("tool1")
                                           .description("Tool 1")
                                           .schema(SIMPLE_SCHEMA)
                                           .tool(request -> McpToolResult.builder()
                                                   .addImageContent(McpMedia.media("helidon.png"), McpMedia.IMAGE_PNG)
                                                   .build()))
                                   .addTool(tool -> tool.name("tool2")
                                           .description("Tool 2")
                                           .schema(SIMPLE_SCHEMA)
                                           .tool(request -> McpToolResult.builder()
                                                   .addTextResourceContent(resource -> resource.uri(URI.create("http://resource"))
                                                           .text("resource")
                                                           .mimeType(MediaTypes.TEXT_PLAIN))
                                                   .build()))
                                   .addTool(tool -> tool.name("tool3")
                                           .description("Tool 3")
                                           .schema(SIMPLE_SCHEMA)
                                           .title("Tool 3 Title")
                                           .tool(request -> McpToolResult.builder()
                                                   .addImageContent(McpMedia.media("helidon.png"), McpMedia.IMAGE_PNG)
                                                   .addTextResourceContent(resource -> resource.uri(URI.create("http://resource"))
                                                           .text("resource")
                                                           .mimeType(MediaTypes.TEXT_PLAIN))
                                                   .addTextContent("text")
                                                   .addAudioContent(McpMedia.media("helidon.wav"), McpMedia.AUDIO_WAV)
                                                   .addResourceLinkContent("resource-link-default", "https://foo")
                                                   .addResourceLinkContent(link -> link.name("resource-link-custom")
                                                           .size(10)
                                                           .title("title")
                                                           .uri("https://foo")
                                                           .description("description")
                                                           .mediaType(MediaTypes.TEXT_PLAIN))
                                                   .build()))
                                   .addTool(new TownTool())
                                   .addTool(tool -> tool.name("tool5")
                                           .schema("")
                                           .description("Tool 5")
                                           .tool(request -> McpToolResult.builder()
                                                   .structuredContent(new StructuredContent("foo"))
                                                   .build())));
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
            return Schema.builder()
                    .rootObject(root -> root.addStringProperty("name", SchemaString.Builder::build)
                            .addNumberProperty("population", SchemaNumber.Builder::build))
                    .build()
                    .generate();
        }

        @Override
        public McpToolResult tool(McpToolRequest request) {
            McpParameters parameters = request.arguments();
            String name = parameters.get("name").asString().orElse("unknown");
            int population = parameters.get("population").asInteger().orElse(-1);
            String content = String.format("%s has a population of %d inhabitants", name, population);
            return McpToolResult.builder()
                    .addTextContent(content)
                    .build();
        }

        @Override
        public Optional<String> title() {
            return Optional.of("Tool 4 Title");
        }
    }

    public static class StructuredContent {
        private String foo;

        StructuredContent(String foo) {
            this.foo = foo;
        }

        public String getFoo() {
            return foo;
        }

        public void setFoo(String foo) {
            this.foo = foo;
        }
    }
}
