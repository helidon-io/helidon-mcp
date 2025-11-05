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

import java.util.Optional;

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

    static boolean isPositiveAndLessThanOne(Double value) {
        return 0 <= value && value <= 1.0;
    }
}
