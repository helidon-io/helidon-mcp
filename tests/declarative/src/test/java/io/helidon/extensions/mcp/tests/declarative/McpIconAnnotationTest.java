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
package io.helidon.extensions.mcp.tests.declarative;

import io.helidon.json.JsonArray;
import io.helidon.json.JsonObject;
import io.helidon.json.JsonParser;
import io.helidon.json.JsonValue;
import io.helidon.webclient.jsonrpc.JsonRpcClient;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.testing.junit5.ServerTest;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ServerTest
class McpIconAnnotationTest {
    private static final JsonArray SERVER_ICONS = JsonParser.create("""
            [
              {
                "src": "https://example.com/server.svg",
                "mimeType": "image/svg+xml",
                "sizes": ["any"],
                "theme": "light"
              },
              {
                "src": "data:image/png;base64,iVBORw0KGgo="
              }
            ]
            """).readJsonArray();
    private static final JsonArray TOOL_ICONS = JsonParser.create("""
            [
              {
                "src": "https://example.com/tool.svg",
                "mimeType": "image/svg+xml",
                "sizes": ["48x48", "96x96"],
                "theme": "dark"
              },
              {
                "src": "data:image/png;base64,iVBORw0KGgo="
              }
            ]
            """).readJsonArray();
    private static final JsonArray PROMPT_ICONS = JsonParser.create("""
            [
              {
                "src": "https://example.com/prompt.png",
                "mimeType": "image/png",
                "theme": "light"
              }
            ]
            """).readJsonArray();
    private static final JsonArray RESOURCE_ICONS = JsonParser.create("""
            [
              {
                "src": "https://example.com/resource.svg"
              }
            ]
            """).readJsonArray();
    private static final JsonArray TEMPLATE_ICONS = JsonParser.create("""
            [
              {
                "src": "https://example.com/template.svg",
                "sizes": ["any"]
              }
            ]
            """).readJsonArray();

    private final JsonRpcClient client;

    McpIconAnnotationTest(WebServer server) {
        this.client = JsonRpcClient.create(config -> config.baseUri("http://localhost:"
                                                                           + server.port()
                                                                           + "/icon-metadata"));
    }

    @Test
    void generatesServerIcons() {
        JsonObject clientInfo = JsonObject.builder()
                .set("name", "test-client")
                .set("version", "1.0.0")
                .build();

        try (var response = client.rpcMethod("initialize")
                .rpcId(1)
                .param("protocolVersion", "2025-11-25")
                .param("capabilities", JsonObject.empty())
                .param("clientInfo", clientInfo)
                .submit()) {
            JsonObject serverInfo = response.result()
                    .map(result -> result.asJsonObject())
                    .flatMap(result -> result.objectValue("serverInfo"))
                    .orElseThrow();

            assertThat(serverInfo.arrayValue("icons").orElseThrow(), is(SERVER_ICONS));
        }
    }

    @Test
    void generatesToolIcons() {
        JsonObject tool = listComponent("tools/list", "tools");

        assertThat(tool.arrayValue("icons").orElseThrow(), is(TOOL_ICONS));
    }

    @Test
    void generatesPromptIcons() {
        JsonObject prompt = listComponent("prompts/list", "prompts");

        assertThat(prompt.arrayValue("icons").orElseThrow(), is(PROMPT_ICONS));
    }

    @Test
    void generatesResourceIcons() {
        JsonObject resource = listComponent("resources/list", "resources");

        assertThat(resource.arrayValue("icons").orElseThrow(), is(RESOURCE_ICONS));
    }

    @Test
    void generatesResourceTemplateIcons() {
        JsonObject template = listComponent("resources/templates/list", "resourceTemplates");

        assertThat(template.arrayValue("icons").orElseThrow(), is(TEMPLATE_ICONS));
    }

    private JsonObject listComponent(String method, String property) {
        try (var response = client.rpcMethod(method)
                .rpcId(1)
                .submit()) {
            JsonArray components = response.result()
                    .map(result -> result.asJsonObject())
                    .flatMap(result -> result.arrayValue(property))
                    .orElseThrow();
            return components.get(0)
                    .map(JsonValue::asObject)
                    .orElseThrow();
        }
    }
}
