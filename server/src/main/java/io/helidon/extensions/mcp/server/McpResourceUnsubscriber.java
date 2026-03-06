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

import java.util.function.Consumer;

import io.helidon.builder.api.RuntimeType;

/**
 * MCP Resource Unsubscriber.
 */
@RuntimeType.PrototypedBy(McpResourceUnsubscriberConfig.class)
public interface McpResourceUnsubscriber extends RuntimeType.Api<McpResourceUnsubscriberConfig> {
    /**
     * Create a resource unsubscriber configuration builder.
     *
     * @return builder
     */
    static McpResourceUnsubscriberConfig.Builder builder() {
        return McpResourceUnsubscriberConfig.builder();
    }

    /**
     * Create a resource unsubscriber from its configuration.
     *
     * @param configuration resource unsubscriber configuration
     * @return resource unsubscriber instance
     */
    static McpResourceUnsubscriber create(McpResourceUnsubscriberConfig configuration) {
        return new McpResourceUnsubscriberImpl(configuration);
    }

    /**
     * Create a resource unsubscriber from its configuration builder.
     *
     * @param consumer resource unsubscriber configuration
     * @return resource unsubscriber instance
     */
    static McpResourceUnsubscriber create(Consumer<McpResourceUnsubscriberConfig.Builder> consumer) {
        return builder().update(consumer).build();
    }

    /**
     * Resource URI.
     *
     * @return uri
     */
    String uri();

    /**
     * Resource unsubscriber.
     *
     * @param request unsubscribe request
     */
    void unsubscribe(McpUnsubscribeRequest request);

    @Override
    default McpResourceUnsubscriberConfig prototype() {
        return McpResourceUnsubscriberConfig.builder()
                .uri(uri())
                .unsubscribe(this::unsubscribe)
                .buildPrototype();
    }
}
