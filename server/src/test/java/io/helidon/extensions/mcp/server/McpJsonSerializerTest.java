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

import java.util.Map;

import jakarta.json.Json;
import jakarta.json.JsonBuilderFactory;
import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

class McpJsonSerializerTest {
    private final JsonBuilderFactory JSON_BUILDER_FACTORY = Json.createBuilderFactory(Map.of());

    @Test
    void testMcpJsonSerializerV1() {
        McpJsonSerializer mjs = McpJsonSerializer.create(McpProtocolVersion.VERSION_2024_11_05);
        assertThat(mjs, instanceOf(McpJsonSerializerV1.class));
    }

    @Test
    void testMcpJsonSerializerV2() {
        McpJsonSerializer mjs = McpJsonSerializer.create(McpProtocolVersion.VERSION_2025_03_26);
        assertThat(mjs, instanceOf(McpJsonSerializerV2.class));
    }

    @Test
    void testMcpJsonSerializerV3() {
        McpJsonSerializer mjs = McpJsonSerializer.create(McpProtocolVersion.VERSION_2025_06_18);
        assertThat(mjs, instanceOf(McpJsonSerializerV3.class));
    }

    @Test
    void testIsResponse() {
        JsonObject payload = JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("jsonrpc", 2.0)
                .add("id", 1)
                .add("result", 2)
                .build();

        boolean response = McpJsonSerializer.isResponse(payload);
        assertThat(response, is(true));
    }

    @Test
    void testIsNotResponse() {
        JsonObject payload = JSON_BUILDER_FACTORY.createObjectBuilder()
                .add("jsonrpc", 2.0)
                .add("id", 1)
                .add("method", "foo/bar")
                .build();

        boolean response = McpJsonSerializer.isResponse(payload);
        assertThat(response, is(false));
    }
}
