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

import java.net.URI;
import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * Roots define the boundaries of where servers can operate within the filesystem.
 */
@Prototype.Blueprint
interface McpRootBlueprint {
    /**
     * Unique identifier for the root. This MUST be a {@code file://} URI
     * in the current specification.
     *
     * @return root uri
     */
    @Option.Decorator(McpDecorators.RootUriDecorator.class)
    URI uri();

    /**
     * The root name.
     *
     * @return root name
     */
    Optional<String> name();
}
