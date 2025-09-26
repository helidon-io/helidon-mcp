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

package io.helidon.extensions.mcp.server;

import java.time.Duration;
import java.util.List;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

import static io.helidon.extensions.mcp.server.McpPagination.DEFAULT_PAGE_SIZE;

/**
 * Configuration of an MCP server.
 */
@Prototype.Blueprint
@Prototype.Configured(McpServerConfigBlueprint.CONFIG_ROOT)
interface McpServerConfigBlueprint extends Prototype.Factory<McpServerFeature> {
    String CONFIG_ROOT = "mcp.server";

    /**
     * Server path configuration.
     *
     * @return path
     */
    @Option.Configured
    @Option.Default("/mcp")
    String path();

    /**
     * Server name configuration.
     *
     * @return name
     */
    @Option.Configured
    @Option.Default("mcp-server")
    String name();

    /**
     * Server version configuration.
     *
     * @return version
     */
    @Option.Configured
    @Option.Default("0.0.1")
    String version();

    /**
     * Server logging configuration.
     *
     * @return logging
     */
    @Option.Configured
    @Option.DefaultBoolean(true)
    boolean logging();

    /**
     * Server tools page size configuration.
     *
     * @return page size
     */
    @Option.Configured
    @Option.DefaultInt(DEFAULT_PAGE_SIZE)
    @Option.Decorator(McpPagination.PageSizeDecorator.class)
    int toolsPageSize();

    /**
     * Server prompts page size configuration.
     *
     * @return page size
     */
    @Option.Configured
    @Option.DefaultInt(DEFAULT_PAGE_SIZE)
    @Option.Decorator(McpPagination.PageSizeDecorator.class)
    int promptsPageSize();

    /**
     * Server resources page size configuration.
     *
     * @return page size
     */
    @Option.Configured
    @Option.DefaultInt(DEFAULT_PAGE_SIZE)
    @Option.Decorator(McpPagination.PageSizeDecorator.class)
    int resourcesPageSize();

    /**
     * Server resource templates page size configuration.
     *
     * @return page size
     */
    @Option.Configured
    @Option.DefaultInt(DEFAULT_PAGE_SIZE)
    @Option.Decorator(McpPagination.PageSizeDecorator.class)
    int resourceTemplatesPageSize();

    /**
     * Timeout for resource subscriptions without a corresponding {@link McpResourceSubscriber}
     * that can control expiration.
     *
     * @return subscription timeout
     */
    @Option.Configured
    @Option.Default("PT2M")
    Duration subscriptionTimeout();

    /**
     * List of tools registered to this server.
     *
     * @return tools
     */
    @Option.Singular
    List<McpTool> tools();

    /**
     * List of prompts registered to this server.
     *
     * @return prompts
     */
    @Option.Singular
    List<McpPrompt> prompts();

    /**
     * List of resources registered to this server.
     *
     * @return resources
     */
    @Option.Singular
    List<McpResource> resources();

    /**
     * List of completions registered to this server.
     *
     * @return completions
     */
    @Option.Singular
    List<McpCompletion> completions();

    /**
     * List of resource subscribers.
     *
     * @return subscribers
     */
    @Option.Singular
    List<McpResourceSubscriber> resourceSubscribers();

    /**
     * List of resource unsubscribers.
     *
     * @return unsubscribers
     */
    @Option.Singular
    List<McpResourceUnsubscriber> resourceUnsubscribers();
}
