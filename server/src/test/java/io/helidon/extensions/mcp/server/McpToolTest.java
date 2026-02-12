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
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class McpToolTest {
    @Test
    void testMcpToolCustom() {
        McpTool tool = McpTool.builder()
                .name("name")
                .title("title")
                .schema("schema")
                .description("description")
                .outputSchema("outputSchema")
                .tool((request) -> null)
                .build();
        assertThat(tool.name(), is("name"));
        assertThat(tool.schema(), is("schema"));
        assertThat(tool.title().orElse(""), is("title"));
        assertThat(tool.description(), is("description"));
        assertThat(tool.outputSchema().orElse(""), is("outputSchema"));
    }

    @Test
    void testMcpToolDefault() {
        McpTool tool = McpTool.builder()
                .name("name")
                .schema("schema")
                .description("description")
                .tool((request) -> null)
                .build();
        assertThat(tool.name(), is("name"));
        assertThat(tool.schema(), is("schema"));
        assertThat(tool.title().isEmpty(), is(true));
        assertThat(tool.description(), is("description"));
        assertThat(tool.outputSchema().isPresent(), is(false));
    }

    @Test
    void testMcpToolImplementation() {
        Foo foo = new Foo();
        assertThat(foo.name(), is("name"));
        assertThat(foo.schema(), is("schema"));
        assertThat(foo.title().isEmpty(), is(true));
        assertThat(foo.description(), is("description"));
        assertThat(foo.outputSchema().isPresent(), is(false));
    }

    static class Foo implements McpTool {
        public String name() {
            return "name";
        }

        public String description() {
            return "description";
        }

        public String schema() {
            return "schema";
        }

        public Function<McpRequest, McpToolResult> tool() {
            return request -> McpToolResult.builder()
                    .contents(List.of())
                    .build();
        }
    }
}
