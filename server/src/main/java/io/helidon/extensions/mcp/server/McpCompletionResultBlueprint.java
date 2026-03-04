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
package io.helidon.extensions.mcp.server;

import java.util.List;
import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

@Prototype.Blueprint
@Prototype.CustomMethods(McpCompletionSupport.class)
interface McpCompletionResultBlueprint {
    /**
     * Completion suggestions returned for the current request.
     * <p>
     * The list is capped at 100 elements by the MCP protocol. If you have more than 100
     * suggestions available, return the first 100 items, set {@link #total()} to the total
     * number of available suggestions, and set {@link #hasMore()} to {@code true}.
     *
     * @return completion suggestion values (max 100 items)
     */
    @Option.Singular
    @Option.Decorator(McpDecorators.CompletionValuesDecorator.class)
    List<String> values();

    /**
     * Total number of completion suggestions available.
     * <p>
     * This is typically set when {@link #values()} is truncated to the MCP limit (100 items).
     * For example, if you return the first 100 suggestions out of 250 available, set this to 250.
     * <p>
     * If the total number of suggestions is unknown or not applicable, leave this empty.
     *
     * @return total number of available suggestions, if known
     */
    Optional<Integer> total();

    /**
     * Indicates whether additional completion suggestions exist beyond {@link #values()}.
     * <p>
     * Set this to {@code true} when you have more suggestions than were returned in {@link #values()}.
     * This is typically used together with {@link #total()}.
     *
     * @return {@code true} if more suggestions exist beyond {@link #values()}, {@code false} otherwise
     */
    Optional<Boolean> hasMore();
}
