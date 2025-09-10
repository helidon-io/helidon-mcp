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

import io.helidon.common.media.type.MediaTypes;
import io.helidon.jsonrpc.core.JsonRpcParams;

import jakarta.json.JsonValue;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class McpResourceTemplateParameterTest {
    private final McpResource.Builder builder = McpResource.builder()
            .name("name")
            .description("description")
            .mediaType(MediaTypes.TEXT_PLAIN)
            .resource(request -> List.of());

    @Test
    void testSimpleParameter() {
        JsonRpcParams params = JsonRpcParams.create(JsonValue.EMPTY_JSON_OBJECT);
        var resource = builder.uri("https://{path}").build();
        McpResourceTemplate template = new McpResourceTemplate(resource);
        McpParameters parameters = template.parameters(params, "https://foo");

        assertThat(parameters.get("path").asString().get(), is("foo"));
    }

    @Test
    void testTwoParameter() {
        JsonRpcParams params = JsonRpcParams.create(JsonValue.EMPTY_JSON_OBJECT);
        var resource = builder.uri("https://{foo}/{bar}").build();
        McpResourceTemplate template = new McpResourceTemplate(resource);
        McpParameters parameters = template.parameters(params, "https://foo/bar");

        assertThat(parameters.get("foo").asString().get(), is("foo"));
        assertThat(parameters.get("bar").asString().get(), is("bar"));
    }

    @Test
    void testSpaceParameter() {
        JsonRpcParams params = JsonRpcParams.create(JsonValue.EMPTY_JSON_OBJECT);
        var resource = builder.uri("https://{foo}/{bar}").build();
        McpResourceTemplate template = new McpResourceTemplate(resource);
        McpParameters parameters = template.parameters(params, "https://foo foo/bar bar");

        assertThat(parameters.get("foo").asString().get(), is("foo foo"));
        assertThat(parameters.get("bar").asString().get(), is("bar bar"));
    }

    @Test
    void testMiddleParameter() {
        JsonRpcParams params = JsonRpcParams.create(JsonValue.EMPTY_JSON_OBJECT);
        var resource = builder.uri("https://{foo}/path").build();
        McpResourceTemplate template = new McpResourceTemplate(resource);
        McpParameters parameters = template.parameters(params, "https://foo/path");

        assertThat(parameters.get("foo").asString().get(), is("foo"));
    }

    @Test
    void testProtocolParameter() {
        JsonRpcParams params = JsonRpcParams.create(JsonValue.EMPTY_JSON_OBJECT);
        var resource = builder.uri("{protocol}://{foo}").build();
        McpResourceTemplate template = new McpResourceTemplate(resource);
        McpParameters parameters = template.parameters(params, "https://foo");

        assertThat(parameters.get("foo").asString().get(), is("foo"));
        assertThat(parameters.get("protocol").asString().get(), is("https"));
    }
}
