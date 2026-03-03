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

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import io.helidon.builder.api.RuntimeType;

/**
 * Configuration of an MCP Prompt.
 */
@RuntimeType.PrototypedBy(McpPromptConfig.class)
public interface McpPrompt extends RuntimeType.Api<McpPromptConfig> {
    /**
     * Create a prompt configuration builder.
     *
     * @return builder
     */
    static McpPromptConfig.Builder builder() {
        return McpPromptConfig.builder();
    }

    /**
     * Create a prompt from its configuration.
     *
     * @param configuration prompt configuration
     * @return prompt instance
     */
    static McpPrompt create(McpPromptConfig configuration) {
        return new McpPromptImpl(configuration);
    }

    /**
     * Create a prompt from its configuration builder.
     *
     * @param consumer prompt configuration
     * @return prompt instance
     */
    static McpPrompt create(Consumer<McpPromptConfig.Builder> consumer) {
        return builder().update(consumer).build();
    }

    /**
     * Prompt name.
     *
     * @return name
     */
    String name();

    /**
     * Prompt description.
     *
     * @return description
     */
    String description();

    /**
     * A {@link List} of prompt arguments.
     *
     * @return {@link List} of arguments
     */
    List<McpPromptArgument> arguments();

    /**
     * Prompt template processing.
     *
     * @param request prompt request
     * @return Prompt content
     */
    McpPromptResult prompt(McpPromptRequest request);

    /**
     * Human-readable prompt title.
     *
     * @return the prompt title
     */
    default Optional<String> title() {
        return Optional.empty();
    }

    @Override
    default McpPromptConfig prototype() {
        return McpPromptConfig.builder()
                .name(name())
                .title(title())
                .description(description())
                .arguments(arguments())
                .prompt(this::prompt)
                .buildPrototype();
    }
}
