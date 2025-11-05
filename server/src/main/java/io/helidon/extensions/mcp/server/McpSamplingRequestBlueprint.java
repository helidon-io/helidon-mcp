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
import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

import jakarta.json.JsonValue;

/**
 * Configuration of an MCP sampling request.
 */
@Prototype.Blueprint
interface McpSamplingRequestBlueprint {
    /**
     * Sampling messages sent to the client.
     *
     * @return messages
     */
    @Option.Singular
    List<McpSamplingMessage> messages();

    /**
     * Sampling model hints.
     *
     * @return hints
     */
    Optional<List<String>> hints();

    /**
     * Sampling cost priority.
     *
     * @return cost priority
     */
    @Option.Decorator(McpDecorators.CostPriorityDecorator.class)
    Optional<Double> costPriority();

    /**
     * Sampling speed priority.
     *
     * @return speed priority
     */
    @Option.Decorator(McpDecorators.SpeedPriorityDecorator.class)
    Optional<Double> speedPriority();

    /**
     * Sampling intelligence priority.
     *
     * @return intelligence priority
     */
    @Option.Decorator(McpDecorators.IntelligencePriorityDecorator.class)
    Optional<Double> intelligencePriority();

    /**
     * Sampling system prompt.
     *
     * @return system prompt
     */
    Optional<String> systemPrompt();

    /**
     * Sampling temperature.
     *
     * @return temperature
     */
    Optional<Double> temperature();

    /**
     * Sampling max tokens.
     *
     * @return max tokens
     */
    @Option.DefaultInt(100)
    Integer maxTokens();

    /**
     * Sampling stop sequence.
     *
     * @return stop sequence
     */
    Optional<List<String>> stopSequences();

    /**
     * Sampling include context.
     *
     * @return include context
     */
    Optional<McpIncludeContext> includeContext();

    /**
     * Optional metadata to pass through to the LLM provider.
     * The format of this metadata is provider-specific.
     *
     * @return metadata
     */
    Optional<JsonValue> metadata();

    /**
     * Sampling request timeout. Default is five seconds.
     *
     * @return timeout
     */
    @Option.Default("PT5S")
    Duration timeout();
}
