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

import java.util.Optional;
import java.util.function.Consumer;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.RuntimeType;

/**
 * An MCP Tool.
 */
@RuntimeType.PrototypedBy(McpToolConfig.class)
public interface McpTool extends RuntimeType.Api<McpToolConfig> {
    /**
     * Create a tool configuration builder.
     *
     * @return builder
     */
    static McpToolConfig.Builder builder() {
        return McpToolConfig.builder();
    }

    /**
     * Create a tool from its configuration.
     *
     * @param configuration tool configuration
     * @return tool instance
     */
    static McpTool create(McpToolConfig configuration) {
        return new McpToolImpl(configuration);
    }

    /**
     * Create a tool from its configuration builder.
     *
     * @param consumer tool configuration
     * @return tool instance
     */
    static McpTool create(Consumer<McpToolConfig.Builder> consumer) {
        return builder().update(consumer).build();
    }

    /**
     * Tool name.
     *
     * @return name
     */
    String name();

    /**
     * Tool description.
     *
     * @return description
     */
    @Option.Default("No description available")
    String description();

    /**
     * JSON schema describing tool inputs. String must be compliant with
     * <a href="https://json-schema.org/draft/2020-12">JSON Schema Version 2020-12</a>.
     * An empty string is mapped to a schema of type object without any properties.
     *
     * @return JSON schema as a string
     */
    String schema();

    /**
     * Tool execution.
     *
     * @param request tool request
     * @return tool execution result
     */
    McpToolResult tool(McpToolRequest request);

    /**
     * Human-readable tool title.
     *
     * @return the tool title
     */
    default Optional<String> title() {
        return Optional.empty();
    }

    /**
     * Annotations for this tool.
     *
     * @return set of annotations
     */
    default Optional<McpToolAnnotations> annotations() {
        return Optional.empty();
    }

    /**
     * Tool output schema. Describes this tool response format.
     *
     * @return the tool output schema
     */
    default Optional<String> outputSchema() {
        return Optional.empty();
    }

    @Override
    default McpToolConfig prototype() {
        return McpToolConfig.builder()
                .name(name())
                .description(description())
                .schema(schema())
                .annotations(annotations())
                .title(title())
                .outputSchema(outputSchema())
                .tool(this::tool)
                .buildPrototype();
    }
}
