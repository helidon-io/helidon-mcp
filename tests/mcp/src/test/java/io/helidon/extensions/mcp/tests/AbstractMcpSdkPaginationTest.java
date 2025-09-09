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

package io.helidon.extensions.mcp.tests;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

abstract class AbstractMcpSdkPaginationTest extends AbstractMcpSdkTest {

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        PaginationServer.setUpRoute(builder);
    }

    @Test
    void testListToolsWithPagination() {
        var result = client().listTools(null);
        var tools = result.tools();
        String cursor = result.nextCursor();
        assertThat(tools.size(), is(1));
        assertThat(cursor, notNullValue());

        var tool = tools.getFirst();
        assertThat(tool.name(), is("tool-1"));
        assertThat(tool.description(), is("Tool description"));

        result = client().listTools(cursor);
        tools = result.tools();
        assertThat(tools.size(), is(1));
        assertThat(result.nextCursor(), nullValue());

        tool = tools.getFirst();
        assertThat(tool.name(), is("tool-2"));
        assertThat(tool.description(), is("Tool description"));
    }

    @Test
    void testListPromptsWithPagination() {
        var result = client().listPrompts(null);
        var prompts = result.prompts();
        String cursor = result.nextCursor();
        assertThat(prompts.size(), is(1));
        assertThat(cursor, notNullValue());

        var prompt = prompts.getFirst();
        assertThat(prompt.name(), is("prompt-1"));
        assertThat(prompt.description(), is("Prompt description"));

        result = client().listPrompts(cursor);
        prompts = result.prompts();
        assertThat(prompts.size(), is(1));
        assertThat(result.nextCursor(), nullValue());

        prompt = prompts.getFirst();
        assertThat(prompt.name(), is("prompt-2"));
        assertThat(prompt.description(), is("Prompt description"));
    }

    @Test
    void testListResourcesWithPagination() {
        var result = client().listResources(null);
        var resources = result.resources();
        String cursor = result.nextCursor();
        assertThat(resources.size(), is(1));
        assertThat(cursor, notNullValue());

        var resource = resources.getFirst();
        assertThat(resource.name(), is("Resource"));
        assertThat(resource.uri(), is("https://path1"));
        assertThat(resource.description(), is("Resource description"));
        assertThat(resource.mimeType(), is(MediaTypes.TEXT_PLAIN_VALUE));

        result = client().listResources(cursor);
        resources = result.resources();
        assertThat(resources.size(), is(1));
        assertThat(result.nextCursor(), nullValue());

        resource = resources.getFirst();
        assertThat(resource.name(), is("Resource"));
        assertThat(resource.uri(), is("https://path2"));
        assertThat(resource.description(), is("Resource description"));
        assertThat(resource.mimeType(), is(MediaTypes.TEXT_PLAIN_VALUE));
    }

    @Test
    void testListResourceTemplatesWithPagination() {
        var result = client().listResourceTemplates(null);
        var resources = result.resourceTemplates();
        String cursor = result.nextCursor();
        assertThat(resources.size(), is(1));
        assertThat(cursor, notNullValue());

        var resource = resources.getFirst();
        assertThat(resource.name(), is("ResourceTemplate"));
        assertThat(resource.uriTemplate(), is("https://{path1}"));
        assertThat(resource.description(), is("Resource Template description"));
        assertThat(resource.mimeType(), is(MediaTypes.TEXT_PLAIN_VALUE));

        result = client().listResourceTemplates(cursor);
        resources = result.resourceTemplates();
        assertThat(resources.size(), is(1));
        assertThat(result.nextCursor(), nullValue());

        resource = resources.getFirst();
        assertThat(resource.name(), is("ResourceTemplate"));
        assertThat(resource.uriTemplate(), is("https://{path2}"));
        assertThat(resource.description(), is("Resource Template description"));
        assertThat(resource.mimeType(), is(MediaTypes.TEXT_PLAIN_VALUE));
    }
}
