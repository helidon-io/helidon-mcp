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

import java.util.Optional;

import io.helidon.json.JsonObject;
import io.helidon.json.binding.Json;

/**
 * MCP component that supports optional protocol metadata.
 */
public interface McpMetadata {
    /**
     * MCP protocol metadata field name.
     */
    String META = "_meta";

    /**
     * Optional protocol metadata represented by the {@value #META} field. Values supplied to builders are serialized using
     * Helidon JSON binding and must produce a JSON object. Custom types must have a Helidon JSON converter, for example by
     * annotating the type with {@link Json.Entity} and enabling Helidon JSON code generation.
     *
     * <p>Wire metadata is deserialized using Helidon JSON binding and represented by {@link JsonObject}. JSON-B annotations
     * and unregistered POJOs are not supported.
     *
     * @return protocol metadata
     */
    Optional<Object> metadata();
}
