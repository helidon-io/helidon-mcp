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

import java.util.Arrays;
import java.util.List;

import io.helidon.builder.api.Prototype;

final class McpCompletionSupport {
    private McpCompletionSupport() {
    }

    /**
     * Create a {@link io.helidon.extensions.mcp.server.McpCompletionResult} instance
     * from the list of suggestion values.
     *
     * @param values suggestions
     * @return completion result
     */
    @Prototype.FactoryMethod
    static McpCompletionResult create(List<String> values) {
        return McpCompletionResult.builder()
                .values(values)
                .total(values.size())
                .hasMore(false)
                .build();
    }

    /**
     * Create a {@link io.helidon.extensions.mcp.server.McpCompletionResult} instance
     * from the list of suggestion values.
     *
     * @param values suggestions
     * @return completion result
     */
    @Prototype.FactoryMethod
    static McpCompletionResult create(String... values) {
        return McpCompletionResult.builder()
                .values(Arrays.asList(values))
                .total(values.length)
                .hasMore(false)
                .build();
    }
}
