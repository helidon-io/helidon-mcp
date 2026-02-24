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
package io.helidon.extensions.mcp.tests.common;

import java.util.Optional;

import io.helidon.extensions.mcp.server.McpServerConfig;
import io.helidon.extensions.mcp.server.McpTool;
import io.helidon.extensions.mcp.server.McpToolRequest;
import io.helidon.extensions.mcp.server.McpToolResult;
import io.helidon.json.schema.Schema;
import io.helidon.json.schema.SchemaObject;
import io.helidon.json.schema.SchemaString;
import io.helidon.webserver.http.HttpRouting;

/**
 * Output schema tool testing.
 */
public class OutputSchemaTools {
    private OutputSchemaTools() {
    }

    /**
     * Setup webserver routing.
     *
     * @param builder routing builder
     */
    public static void setUpRoute(HttpRouting.Builder builder) {
        builder.addFeature(McpServerConfig.builder()
                                   .path("/")
                                   .addTool(new EmptyOutputSchemaTool())
                                   .addTool(new ValidOutputSchemaTool())
                                   .addTool(new InvalidOutputSchemaTool())
                                   .addTool(new OutputSchemaAndContentTool()));
    }

    private static class EmptyOutputSchemaTool implements McpTool {
        @Override
        public String name() {
            return "empty-output-schema";
        }

        @Override
        public String description() {
            return "Tool with an empty output schema";
        }

        @Override
        public String schema() {
            return "";
        }

        @Override
        public McpToolResult tool(McpToolRequest request) {
            return McpToolResult.create();
        }

        @Override
        public Optional<String> outputSchema() {
            return Optional.of(Schema.builder()
                                       .rootObject(SchemaObject.Builder::build)
                                       .build()
                                       .generate());
        }
    }

    private static class ValidOutputSchemaTool implements McpTool {
        @Override
        public String name() {
            return "valid-output-schema";
        }

        @Override
        public String description() {
            return "Tool with a valid output schema";
        }

        @Override
        public String schema() {
            return Schema.builder()
                    .rootObject(object -> object.addStringProperty("foo", SchemaString.create()))
                    .build()
                    .generate();
        }

        @Override
        public McpToolResult tool(McpToolRequest request) {
            // When structured content is set without text content, Helidon must serialize it
            // and add it as text content to preserve backward compatibility.
            return request.arguments()
                    .get("foo")
                    .asString()
                    .map(StructuredContentPojo::new)
                    .flatMap(value -> Optional.of(McpToolResult.builder().structuredContent(value).build()))
                    .orElse(McpToolResult.create("Unknown"));
        }

        @Override
        public Optional<String> outputSchema() {
            return Optional.of(Schema.builder()
                                       .rootObject(object -> object.addStringProperty("foo", SchemaString.create()))
                                       .build()
                                       .generate());
        }
    }

    private static class InvalidOutputSchemaTool implements McpTool {

        @Override
        public String name() {
            return "invalid-output-schema";
        }

        @Override
        public String description() {
            return """
                    Tool should return a structured content. This scenario work
                    until schema validation is implemented.
                    """;
        }

        @Override
        public String schema() {
            return "";
        }

        @Override
        public McpToolResult tool(McpToolRequest request) {
            return McpToolResult.create();
        }

        @Override
        public Optional<String> outputSchema() {
            return Optional.of(Schema.builder()
                                       .rootObject(object -> object.addStringProperty("foo", SchemaString.create()))
                                       .build()
                                       .generate());
        }
    }

    private static class OutputSchemaAndContentTool implements McpTool {

        @Override
        public String name() {
            return "output-schema-and-content";
        }

        @Override
        public String description() {
            return "Tool returns text content and structured content";
        }

        @Override
        public String schema() {
            return "";
        }

        @Override
        public McpToolResult tool(McpToolRequest request) {
            return McpToolResult.builder()
                    .addTextContent("content")
                    .structuredContent(new StructuredContentPojo("bar"))
                    .build();
        }

        @Override
        public Optional<String> outputSchema() {
            return Optional.of(Schema.builder()
                                       .rootObject(object -> object.addStringProperty("foo", SchemaString.create()))
                                       .build()
                                       .generate());
        }
    }

    /**
     * Structured output content.
     *
     * @param foo foo
     */
    public record StructuredContentPojo(String foo) {
    }
}
