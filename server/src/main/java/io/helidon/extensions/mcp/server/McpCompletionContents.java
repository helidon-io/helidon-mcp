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

import java.util.List;
import java.util.Objects;

/**
 * {@link McpCompletionContent} factory.
 */
public final class McpCompletionContents {
    private McpCompletionContents() {
    }

    /**
     * Create a completion content from the provided list of string.
     *
     * @param values completion values
     * @return completion content
     */
    public static McpCompletionContent completion(String... values) {
        return new McpCompletionContentImpl(List.of(values));
    }

    /**
     * Completion content default implementation.
     */
    static final class McpCompletionContentImpl implements McpCompletionContent {
        private final List<String> values;

        private McpCompletionContentImpl(List<String> values) {
            Objects.requireNonNull(values, "values must not be null");
            if (values.size() > 100) {
                throw new McpInternalException("Cannot contain more than 100 values");
            }
            this.values = values;
        }

        @Override
        public List<String> values() {
            return values;
        }

        @Override
        public int total() {
            return values.size();
        }

        @Override
        public boolean hasMore() {
            return false;
        }
    }
}
