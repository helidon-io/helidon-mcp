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

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class McpCompletionContentTest {

    @Test
    void testDefaultMcpCompletion() {
        var content = McpCompletionContents.completion(List.of());
        assertThat(content.total(), is(0));
        assertThat(content.hasMore(), is(false));
        assertThat(content.values(), is(List.of()));
    }

    @Test
    void testDefaultArrayMcpCompletion() {
        var content = McpCompletionContents.completion();
        assertThat(content.total(), is(0));
        assertThat(content.hasMore(), is(false));
        assertThat(content.values(), is(List.of()));
    }

    @Test
    void testMcpCompletion() {
        var content = McpCompletionContents.completion(List.of("foo"));
        assertThat(content.total(), is(1));
        assertThat(content.hasMore(), is(false));
        assertThat(content.values(), is(List.of("foo")));
    }

    @Test
    void testArrayMcpCompletion() {
        var content = McpCompletionContents.completion("foo");
        assertThat(content.total(), is(1));
        assertThat(content.hasMore(), is(false));
        assertThat(content.values(), is(List.of("foo")));
    }
}
