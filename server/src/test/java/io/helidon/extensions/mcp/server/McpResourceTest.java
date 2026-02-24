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

import io.helidon.common.media.type.MediaType;
import io.helidon.common.media.type.MediaTypes;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class McpResourceTest {

    @Test
    void testDefaultMcpResource() {
        McpResource resource = McpResource.builder()
                .uri("uri")
                .name("name")
                .mediaType(MediaTypes.TEXT_PLAIN)
                .resource(request -> McpResourceResult.create())
                .build();

        assertThat(resource.uri(), is("uri"));
        assertThat(resource.name(), is("name"));
        assertThat(resource.title().isEmpty(), is(true));
        assertThat(resource.mediaType(), is(MediaTypes.TEXT_PLAIN));
        assertThat(resource.description(), is("No description available"));
    }

    @Test
    void testCustomMcpResource() {
        McpResource resource = McpResource.builder()
                .uri("uri")
                .name("name")
                .title("title")
                .description("description")
                .mediaType(MediaTypes.TEXT_PLAIN)
                .resource(request -> McpResourceResult.create())
                .build();

        assertThat(resource.uri(), is("uri"));
        assertThat(resource.name(), is("name"));
        assertThat(resource.title().orElse(""), is("title"));
        assertThat(resource.description(), is("description"));
        assertThat(resource.mediaType(), is(MediaTypes.TEXT_PLAIN));
    }

    @Test
    void testMcpResourceImplementation() {
        Foo foo = new Foo();
        assertThat(foo.uri(), is("uri"));
        assertThat(foo.name(), is("name"));
        assertThat(foo.title().orElse(""), is("title"));
        assertThat(foo.description(), is("description"));
        assertThat(foo.mediaType(), is(MediaTypes.TEXT_PLAIN));
    }

    static class Foo implements McpResource {
        @Override
        public String uri() {
            return "uri";
        }

        @Override
        public String name() {
            return "name";
        }

        @Override
        public String description() {
            return "description";
        }

        @Override
        public MediaType mediaType() {
            return MediaTypes.TEXT_PLAIN;
        }

        @Override
        public McpResourceResult resource(McpResourceRequest request) {
            return McpResourceResult.create();
        }

        @Override
        public Optional<String> title() {
            return Optional.of("title");
        }
    }
}
