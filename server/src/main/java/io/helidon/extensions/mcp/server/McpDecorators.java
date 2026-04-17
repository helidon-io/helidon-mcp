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

import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.helidon.builder.api.Prototype;

import static io.helidon.extensions.mcp.server.McpPagination.DEFAULT_PAGE_SIZE;

/**
 * Placeholder for the MCP configuration decorators.
 */
final class McpDecorators {
    private McpDecorators() {
    }

    /**
     * Enforce positive page size.
     * <p>
     * See {@link io.helidon.extensions.mcp.server.McpPagination}.
     */
    static class PageSizeDecorator implements Prototype.OptionDecorator<McpServerConfig.BuilderBase<?, ?>, Integer> {
        @Override
        public void decorate(McpServerConfig.BuilderBase<?, ?> builder, Integer pageSize) {
            if (pageSize < DEFAULT_PAGE_SIZE) {
                throw new IllegalArgumentException("Page size must be greater than zero");
            }
        }
    }

    /**
     * Enforce intelligence priority value between 0 and 1.
     * <p>
     * See {@link io.helidon.extensions.mcp.server.McpSamplingRequest}.
     */
    static class IntelligencePriorityDecorator implements Prototype.OptionDecorator<McpSamplingRequest.BuilderBase<?, ?>, Optional<Double>> {
        @Override
        public void decorate(McpSamplingRequest.BuilderBase<?, ?> builder, Optional<Double> value) {
            value.filter(McpDecorators::isPositiveAndLessThanOne)
                    .orElseThrow(() -> new IllegalArgumentException("Intelligence priority must be in range [0, 1]"));
        }
    }

    /**
     * Enforce speed priority value between 0 and 1.
     * <p>
     * See {@link io.helidon.extensions.mcp.server.McpSamplingRequest}.
     */
    static class SpeedPriorityDecorator implements Prototype.OptionDecorator<McpSamplingRequest.BuilderBase<?, ?>, Optional<Double>> {
        @Override
        public void decorate(McpSamplingRequest.BuilderBase<?, ?> builder, Optional<Double> value) {
            value.filter(McpDecorators::isPositiveAndLessThanOne)
                    .orElseThrow(() -> new IllegalArgumentException("Speed priority must be in range [0, 1]"));
        }
    }

    /**
     * Enforce cost priority value between 0 and 1.
     * <p>
     * See {@link io.helidon.extensions.mcp.server.McpSamplingRequest}.
     */
    static class CostPriorityDecorator implements Prototype.OptionDecorator<McpSamplingRequest.BuilderBase<?, ?>, Optional<Double>> {
        @Override
        public void decorate(McpSamplingRequest.BuilderBase<?, ?> builder, Optional<Double> value) {
            value.filter(McpDecorators::isPositiveAndLessThanOne)
                    .orElseThrow(() -> new IllegalArgumentException("Cost priority must be in range [0, 1]"));
        }
    }

    /**
     * The URI scheme must be {@code file} when creating an MCP root.
     */
    static class RootUriDecorator implements Prototype.OptionDecorator<McpRoot.BuilderBase<?, ?>, URI> {
        @Override
        public void decorate(McpRoot.BuilderBase<?, ?> builder, URI uri) {
            if (!uri.getScheme().equals("file")) {
                throw new McpRootException("Root URI scheme must be file");
            }
        }
    }

    /**
     * Number of suggestions must not exceed 100 items.
     */
    static class CompletionValuesDecorator implements Prototype.OptionDecorator<McpCompletionResult.BuilderBase<?, ?>, String> {
        @Override
        public void decorate(McpCompletionResult.BuilderBase<?, ?> builder, String value) {
        }

        @Override
        public void decorateSetList(McpCompletionResult.BuilderBase<?, ?> builder, List<String> values) {
            lessThan100Items(values);
        }

        @Override
        public void decorateAddList(McpCompletionResult.BuilderBase<?, ?> builder, List<String> values) {
            lessThan100Items(values);
        }

        @Override
        public void decorateSetSet(McpCompletionResult.BuilderBase<?, ?> builder, Set<String> values) {
            lessThan100Items(values);
        }

        @Override
        public void decorateAddSet(McpCompletionResult.BuilderBase<?, ?> builder, Set<String> values) {
            lessThan100Items(values);
        }

        private void lessThan100Items(Collection<String> values) {
            if (values.size() > 100) {
                throw new IllegalArgumentException("Completion values must be less than 100");
            }
        }
    }

    static class PositiveValueDecorator implements Prototype.OptionDecorator<McpServerConfig.BuilderBase<?, ?>, Integer> {
        @Override
        public void decorate(McpServerConfig.BuilderBase<?, ?> builder, Integer value) {
            if (value < 0) {
                throw new IllegalArgumentException("value must be greater than zero");
            }
        }
    }

    static boolean isPositiveAndLessThanOne(Double value) {
        return 0 <= value && value <= 1.0;
    }
}
