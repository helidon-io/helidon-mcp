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
import java.util.function.Function;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Configuration of an MCP Prompt.
 */
@Prototype.Blueprint
@Prototype.IncludeDefaultMethods("title")
interface McpPromptBlueprint {

    /**
     * Prompt name.
     *
     * @return name
     */
    String name();

    /**
     * Human-readable prompt title.
     *
     * @return the prompt title
     */
    @Option.Default("")
    default String title() {
        return "";
    }

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
    @Option.Singular
    List<McpPromptArgument> arguments();

    /**
     * Prompt template processing.
     *
     * @return Prompt content
     */
    Function<McpRequest, List<McpPromptContent>> prompt();
}
