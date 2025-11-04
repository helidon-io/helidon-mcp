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
 * Cursor points to the next page. The last page has a blank cursor.
 *
 * @param <T> MCP component such as Tool, Prompt, Resource.
 */
class McpPage<T> {
    private final List<T> components;
    private final String cursor;
    private final boolean isLast;

    /**
     * Create a page with provided parameters.
     *
     * @param components the page components
     * @param cursor the cursor pointing to the next page
     * @param isLast {@code true} if this is the last page, {@code false} otherwise
     */
    McpPage(List<T> components, String cursor, boolean isLast) {
        this.cursor = cursor;
        this.isLast = isLast;
        this.components = components;
    }

    /**
     * Create a single page with provided content.
     *
     * @param components the page components
     */
    McpPage(List<T> components) {
        this(components, "", true);
    }

    String cursor() {
        return cursor;
    }

    List<T> components() {
        return components;
    }

    boolean isLast() {
        return isLast;
    }
}
