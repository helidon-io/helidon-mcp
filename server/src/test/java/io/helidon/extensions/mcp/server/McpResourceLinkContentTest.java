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

import io.helidon.common.media.type.MediaTypes;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class McpResourceLinkContentTest {

    @Test
    void testDefaultResourceLinkContent() {
        var content = McpResourceLinkContent.builder()
                .name("name")
                .uri("https://foo")
                .build();

        assertThat(content.name(), is("name"));
        assertThat(content.uri(), is("https://foo"));
        assertThat(content.size().isPresent(), is(false));
        assertThat(content.title().isPresent(), is(false));
        assertThat(content.mediaType().isPresent(), is(false));
        assertThat(content.description().isPresent(), is(false));
    }

    @Test
    void testCustomResourceLinkContent() {
        var content = McpResourceLinkContent.builder()
                .size(10)
                .name("name")
                .title("title")
                .uri("https://foo")
                .description("description")
                .mediaType(MediaTypes.APPLICATION_JSON)
                .build();

        assertThat(content.name(), is("name"));
        assertThat(content.uri(), is("https://foo"));
        assertThat(content.size().orElse(null), is(10L));
        assertThat(content.title().orElse(null), is("title"));
        assertThat(content.description().orElse(null), is("description"));
        assertThat(content.mediaType().orElse(null), is(MediaTypes.APPLICATION_JSON));
    }
}
