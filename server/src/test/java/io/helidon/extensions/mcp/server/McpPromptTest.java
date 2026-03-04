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

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class McpPromptTest {
    @Test
    void testMcpPromptCustom() {
        McpPromptConfig config = McpPromptConfig.builder()
                .name("name")
                .title("title")
                .description("description")
                .prompt(request -> McpPromptResult.create())
                .build();
        McpPrompt prompt = new McpPromptImpl(config);
        assertThat(prompt.name(), is("name"));
        assertThat(prompt.title().orElse(""), is("title"));
        assertThat(prompt.description(), is("description"));
    }

    @Test
    void testMcpPromptDefault() {
        McpPromptConfig config = McpPromptConfig.builder()
                .name("name")
                .description("description")
                .prompt(request -> McpPromptResult.create())
                .build();
        McpPrompt prompt = new McpPromptImpl(config);
        assertThat(prompt.name(), is("name"));
        assertThat(prompt.title().isEmpty(), is(true));
        assertThat(prompt.description(), is("description"));
    }

    @Test
    void testMcpPromptImplementation() {
        Foo foo = new Foo();
        assertThat(foo.name(), is("name"));
        assertThat(foo.title().isEmpty(), is(false));
        assertThat(foo.description(), is("description"));
        assertThat(foo.arguments().size(), is(1));
    }

    private static class Foo implements McpPrompt {
        @Override
        public String name() {
            return "name";
        }

        @Override
        public String description() {
            return "description";
        }

        @Override
        public List<McpPromptArgument> arguments() {
            return List.of(McpPromptArgument.builder()
                                   .name("name")
                                   .description("description")
                                   .build());
        }

        @Override
        public McpPromptResult prompt(McpPromptRequest request) {
            return McpPromptResult.create();
        }

        @Override
        public Optional<String> title() {
            return Optional.of("title");
        }
    }
}
