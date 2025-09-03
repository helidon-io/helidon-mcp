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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.helidon.builder.api.Prototype;

/**
 * Support for MCP pagination feature.
 * <p>
 * Pagination is supported by the following JSON-RPC methods:
 * <ul>
 *     <li>
 *         {@link io.helidon.extensions.mcp.server.McpJsonRpc#METHOD_TOOLS_LIST}
 *         List the tools registered on the server.
 *     </li>
 *     <li>
 *         {@link io.helidon.extensions.mcp.server.McpJsonRpc#METHOD_PROMPT_LIST}
 *         List the prompts registered on the server.
 *     </li>
 *     <li>
 *         {@link io.helidon.extensions.mcp.server.McpJsonRpc#METHOD_RESOURCES_LIST}
 *         List the resources registered on the server.
 *     </li>
 *     <li>
 *         {@link io.helidon.extensions.mcp.server.McpJsonRpc#METHOD_RESOURCES_TEMPLATES_LIST}
 *         List the resource templates registered on the server.
 *     </li>
 * </ul>
 * <p>
 * Pagination enables the server to return results in smaller, manageable chunks rather than
 * delivering the entire dataset at once. The size of each chunk is configured via the {@code page-size}
 * property. This class maintains a map of pages, where each key represents a unique cursor associated
 * with a specific page. Each page also contains a cursor pointing to the next page in the sequence.
 *
 * @param <T> MCP components type
 */
class McpPagination<T> {
    static final int DEFAULT_PAGE_SIZE = 0;
    private final Map<String, T> components;
    private final Map<String, McpPage<T>> pages;
    private final String initialCursor = UUID.randomUUID().toString();

    McpPagination(Map<String, T> mcpComponents, int pageSize) {
        this.components = mcpComponents;
        this.pages = new ConcurrentHashMap<>();
        List<T> components = mcpComponents.values()
                .stream()
                .toList();
        createPages(components, pageSize);
    }

    McpPagination(List<T> components, int pageSize) {
        this.pages = new ConcurrentHashMap<>();
        this.components = new ConcurrentHashMap<>();
        for (int i = 0; i < components.size(); i++) {
            this.components.put("key" + i, components.get(i));
        }
        createPages(components, pageSize);
    }

    private void createPages(List<T> components, int pageSize) {
        String prevCursor = initialCursor;
        int total = components.size();

        // Pagination is disabled
        if (pageSize == DEFAULT_PAGE_SIZE) {
            pages.put(prevCursor, new McpPage<>(components, "", true));
            return;
        }

        for (int i = pageSize; i <= total; i += pageSize) {
            String nextCursor = UUID.randomUUID().toString();
            List<T> pageItems = components.subList(i - pageSize, i);
            boolean isLast = i == total;
            String cursor = isLast ? "" : nextCursor;
            pages.put(prevCursor, new McpPage<>(pageItems, cursor, isLast));
            prevCursor = nextCursor;
        }

        if (total % pageSize != 0) {
            int lastPageStart = total - (total % pageSize);
            List<T> lastPage = components.subList(lastPageStart, total);
            pages.put(prevCursor, new McpPage<>(lastPage, "", true));
        }
    }

    McpPage<T> firstPage() {
        return pages.get(initialCursor);
    }

    McpPage<T> page(String cursor) {
        return pages.get(cursor);
    }

    T get(String key) {
        return components.get(key);
    }

    Collection<T> content() {
        return components.values();
    }

    static class PageSizeDecorator implements Prototype.OptionDecorator<McpServerConfig.BuilderBase<?, ?>, Integer> {
        @Override
        public void decorate(McpServerConfig.BuilderBase<?, ?> builder, Integer pageSize) {
            if (pageSize < DEFAULT_PAGE_SIZE) {
                throw new IllegalArgumentException("Page size must be greater than zero");
            }
        }
    }
}
