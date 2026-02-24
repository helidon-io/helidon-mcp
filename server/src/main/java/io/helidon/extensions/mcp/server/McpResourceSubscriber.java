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

import java.util.function.Consumer;

import io.helidon.builder.api.RuntimeType;

/**
 * Resource subscriber.
 */
@RuntimeType.PrototypedBy(McpResourceSubscriberConfig.class)
public interface McpResourceSubscriber extends RuntimeType.Api<McpResourceSubscriberConfig> {
    /**
     * Create a resource subscriber configuration builder.
     *
     * @return builder
     */
    static McpResourceSubscriberConfig.Builder builder() {
        return McpResourceSubscriberConfig.builder();
    }

    /**
     * Create a resource subscriber from its configuration.
     *
     * @param configuration resource subscriber configuration
     * @return tool instance
     */
    static McpResourceSubscriber create(McpResourceSubscriberConfig configuration) {
        return new McpResourceSubscriberImpl(configuration);
    }

    /**
     * Create a resource subscriber from its configuration builder.
     *
     * @param consumer resource subscriber configuration
     * @return resource subscriber instance
     */
    static McpResourceSubscriber create(Consumer<McpResourceSubscriberConfig.Builder> consumer) {
        return builder().update(consumer).build();
    }

    /**
     * Resource URI.
     *
     * @return uri
     */
    String uri();

    /**
     * Resource subscriber.
     *
     * @param request subscribe request
     */
    void subscribe(McpSubscribeRequest request);

    @Override
    default McpResourceSubscriberConfig prototype() {
        return McpResourceSubscriberConfig.builder()
                .uri(uri())
                .subscribe(this::subscribe)
                .buildPrototype();
    }
}
