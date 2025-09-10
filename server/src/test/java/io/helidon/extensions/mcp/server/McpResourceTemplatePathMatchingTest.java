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
import java.util.regex.PatternSyntaxException;

import io.helidon.common.media.type.MediaTypes;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

class McpResourceTemplatePathMatchingTest {
    private final McpResource.Builder builder = McpResource.builder()
            .name("name")
            .description("description")
            .mediaType(MediaTypes.TEXT_PLAIN)
            .resource(this::resource);

    @Test
    void testSingleVariablePath() {
        var resource = builder.uri("https://{path}").build();
        McpResourceTemplate template = new McpResourceTemplate(resource);

        assertThat(template.matches("https://foo"), is(true));
        assertThat(template.matches("https://foo/"), is(false));
        assertThat(template.matches("https://foo-bar"), is(true));
        assertThat(template.matches("https://foo-bar/"), is(false));
        assertThat(template.matches("https://foo bar"), is(true));
        assertThat(template.matches("https://foo bar/"), is(false));

        assertThat(template.matches("https://foo/bar"), is(false));
        assertThat(template.matches("https://foo/bar/"), is(false));
        assertThat(template.matches("https:/foo/bar/"), is(false));
        assertThat(template.matches("https:/foo/bar"), is(false));
    }

    @Test
    void testMultipleVariablePath() {
        var resource = builder.uri("https://{path}/{path1}").build();
        McpResourceTemplate template = new McpResourceTemplate(resource);

        assertThat(template.matches("https://foo/bar"), is(true));
        assertThat(template.matches("https://foo-bar/foo-bar"), is(true));
        assertThat(template.matches("https://foo bar/ foo bar"), is(true));
        assertThat(template.matches("https://foo bar/ "), is(true));

        assertThat(template.matches("https://foo/bar/foo"), is(false));
        assertThat(template.matches("https://foo/"), is(false));
        assertThat(template.matches("http:/foo/bar/"), is(false));
        assertThat(template.matches("http:/foo bar/"), is(false));
        assertThat(template.matches("https:/foo-bar/bar/foo"), is(false));
    }

    @Test
    void testWrongVariablePath() {
        var resource = builder.uri("https://{path/path1}").build();
        try {
            McpResourceTemplate template = new McpResourceTemplate(resource);
        } catch (PatternSyntaxException e) {
            assertThat(e.getMessage(), startsWith("Illegal repetition near index 9"));
        }
    }

    @Test
    void testWrongPath() {
        var resource = builder.uri("https://{path/path1").build();
        try {
            McpResourceTemplate template = new McpResourceTemplate(resource);
        } catch (PatternSyntaxException e) {
            assertThat(e.getMessage(), startsWith("Illegal repetition near index 9"));
        }
    }

    private List<McpResourceContent> resource(McpRequest request) {
        return List.of();
    }
}
