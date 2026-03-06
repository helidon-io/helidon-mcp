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

import java.util.Objects;
import java.util.function.Consumer;

import io.helidon.builder.api.Prototype;

final class McpServerFeatureSupport {
    private McpServerFeatureSupport() {
    }

    /**
     * Create a new {@link io.helidon.extensions.mcp.server.McpTool} from its configuration
     * and register it to this server.
     *
     * @param builder server configuration builder
     * @param config  tool configuration
     */
    @Prototype.BuilderMethod
    static void addTool(McpServerConfig.BuilderBase<?, ?> builder, McpToolConfig config) {
        Objects.requireNonNull(config, "config is null");
        builder.addTool(new McpToolImpl(config));
    }

    /**
     * Create a new {@link io.helidon.extensions.mcp.server.McpTool} from its configuration builder
     * and register it to this server.
     *
     * @param builder  server configuration builder
     * @param consumer tool configuration builder consumer
     */
    @Prototype.BuilderMethod
    static void addTool(McpServerConfig.BuilderBase<?, ?> builder, Consumer<McpToolConfig.Builder> consumer) {
        Objects.requireNonNull(consumer, "consumer is null");
        McpToolConfig config = McpToolConfig.builder().update(consumer).build();
        builder.addTool(new McpToolImpl(config));
    }

    /**
     * Create a new {@link io.helidon.extensions.mcp.server.McpPrompt} from its configuration
     * and register it to this server.
     *
     * @param builder server configuration builder
     * @param config  prompt configuration
     */
    @Prototype.BuilderMethod
    static void addPrompt(McpServerConfig.BuilderBase<?, ?> builder, McpPromptConfig config) {
        Objects.requireNonNull(config, "config is null");
        builder.addPrompt(new McpPromptImpl(config));
    }

    /**
     * Create a new {@link io.helidon.extensions.mcp.server.McpPrompt} from its configuration builder
     * and register it to this server.
     *
     * @param builder  server configuration builder
     * @param consumer prompt configuration builder consumer
     */
    @Prototype.BuilderMethod
    static void addPrompt(McpServerConfig.BuilderBase<?, ?> builder, Consumer<McpPromptConfig.Builder> consumer) {
        Objects.requireNonNull(consumer, "consumer is null");
        McpPromptConfig config = McpPromptConfig.builder().update(consumer).build();
        builder.addPrompt(new McpPromptImpl(config));
    }

    /**
     * Create a new {@link io.helidon.extensions.mcp.server.McpResource} from its configuration
     * and register it to this server.
     *
     * @param builder server configuration builder
     * @param config  resource configuration
     */
    @Prototype.BuilderMethod
    static void addResource(McpServerConfig.BuilderBase<?, ?> builder, McpResourceConfig config) {
        Objects.requireNonNull(config, "config is null");
        builder.addResource(new McpResourceImpl(config));
    }

    /**
     * Create a new {@link io.helidon.extensions.mcp.server.McpResource} from its configuration builder
     * and register it to this server.
     *
     * @param builder  server configuration builder
     * @param consumer resource configuration builder consumer
     */
    @Prototype.BuilderMethod
    static void addResource(McpServerConfig.BuilderBase<?, ?> builder, Consumer<McpResourceConfig.Builder> consumer) {
        Objects.requireNonNull(consumer, "consumer is null");
        McpResourceConfig config = McpResourceConfig.builder().update(consumer).build();
        builder.addResource(new McpResourceImpl(config));
    }

    /**
     * Create a new {@link io.helidon.extensions.mcp.server.McpCompletion} from its configuration
     * and register it to this server.
     *
     * @param builder server configuration builder
     * @param config  completion configuration
     */
    @Prototype.BuilderMethod
    static void addCompletion(McpServerConfig.BuilderBase<?, ?> builder, McpCompletionConfig config) {
        Objects.requireNonNull(config, "config is null");
        builder.addCompletion(new McpCompletionImpl(config));
    }

    /**
     * Create a new {@link io.helidon.extensions.mcp.server.McpCompletion} from its configuration builder
     * and register it to this server.
     *
     * @param builder  server configuration builder
     * @param consumer completion configuration builder consumer
     */
    @Prototype.BuilderMethod
    static void addCompletion(McpServerConfig.BuilderBase<?, ?> builder, Consumer<McpCompletionConfig.Builder> consumer) {
        Objects.requireNonNull(consumer, "consumer is null");
        McpCompletionConfig config = McpCompletionConfig.builder().update(consumer).build();
        builder.addCompletion(new McpCompletionImpl(config));
    }
}
