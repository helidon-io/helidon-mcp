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

import jakarta.json.JsonObject;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

class McpJsonSerializerV3Test {
    private static final McpJsonSerializer MJS = McpJsonSerializer.create(McpProtocolVersion.VERSION_2025_06_18);

    @Test
    void testSerializeTool() {
        McpTool tool = McpTool.builder()
                .name("name")
                .title("title")
                .schema("")
                .description("description")
                .tool(request -> null)
                .build();

        JsonObject payload = MJS.toJson(tool).build();
        assertThat(payload.getString("name"), is("name"));
        assertThat(payload.getString("title"), is("title"));
        assertThat(payload.getString("description"), is("description"));
        assertThat(payload.getJsonObject("inputSchema"), notNullValue());
    }

    @Test
    void testSerializeResource() {
        McpResource resource = McpResource.builder()
                .uri("https://foo")
                .name("name")
                .title("title")
                .description("description")
                .mediaType(MediaTypes.APPLICATION_JSON)
                .resource(request -> null)
                .build();

        JsonObject payload = MJS.toJson(resource).build();
        assertThat(payload.getString("name"), is("name"));
        assertThat(payload.getString("title"), is("title"));
        assertThat(payload.getString("uri"), is("https://foo"));
        assertThat(payload.getString("description"), is("description"));
        assertThat(payload.getString("mimeType"), is(MediaTypes.APPLICATION_JSON.text()));
    }

    @Test
    void testSerializePrompt() {
        McpPrompt prompt = McpPrompt.builder()
                .name("name")
                .title("title")
                .description("description")
                .addArgument(argument -> argument.name("name")
                        .title("title")
                        .description("description")
                        .required(true))
                .prompt(request -> null)
                .build();

        JsonObject payload = MJS.toJson(prompt).build();
        assertThat(payload.getString("name"), is("name"));
        assertThat(payload.getString("title"), is("title"));
        assertThat(payload.getString("description"), is("description"));

        JsonObject argument = payload.getJsonArray("arguments").getJsonObject(0);
        assertThat(argument.getString("name"), is("name"));
        assertThat(argument.getString("title"), is("title"));
        assertThat(argument.getBoolean("required"), is(true));
        assertThat(argument.getString("description"), is("description"));
    }

    @Test
    void testSerializePromptArgument() {
        McpPromptArgument argument = McpPromptArgument.builder()
                .name("name")
                .title("title")
                .description("description")
                .required(true)
                .build();

        JsonObject payload = MJS.toJson(argument).build();
        assertThat(payload.getString("name"), is("name"));
        assertThat(payload.getString("title"), is("title"));
        assertThat(payload.getBoolean("required"), is(true));
        assertThat(payload.getString("description"), is("description"));
    }
}
