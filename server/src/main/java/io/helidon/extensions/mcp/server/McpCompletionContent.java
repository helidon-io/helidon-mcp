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

/**
 * Completion result content.
 */
public sealed interface McpCompletionContent permits McpCompletionContents.McpCompletionContentImpl {

    /**
     * List of completion values.
     *
     * @return values
     */
    List<String> values();

    /**
     * Total number of values.
     *
     * @return total
     */
    int total();

    /**
     * Whether there is more values.
     *
     * @return {code true} if there is more values, {code false} otherwise
     */
    boolean hasMore();
}
