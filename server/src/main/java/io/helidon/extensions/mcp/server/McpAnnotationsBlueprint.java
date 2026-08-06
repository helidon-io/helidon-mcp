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

import java.util.List;
import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Optional annotations for MCP content.
 */
@Prototype.Blueprint
interface McpAnnotationsBlueprint {
    /**
     * Roles for which the content is intended.
     *
     * @return audience roles
     */
    @Option.Singular
    List<McpRole> audience();

    /**
     * Content importance from zero to one.
     *
     * @return priority
     */
    @Option.Decorator(McpDecorators.AnnotationsPriorityDecorator.class)
    Optional<Double> priority();

    /**
     * Last modification time as an ISO 8601 string.
     *
     * @return last modification time
     */
    Optional<String> lastModified();
}
