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
package io.helidon.extensions.mcp.tests;

import java.time.Duration;
import java.util.Map;

import io.helidon.extensions.mcp.tests.common.StatelessServer;
import io.helidon.http.HeaderName;
import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.webclient.jsonrpc.JsonRpcClient;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.exception.ToolExecutionException;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.McpException;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import jakarta.json.JsonObject;
import jakarta.json.spi.JsonProvider;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ServerTest
class StatelessServerTest {
    private static final JsonProvider JSON_PROVIDER = JsonProvider.provider();
    private static final String SUCCESS_TEXT = "Success";
    private static final String TOOL_NAME = "stateless-tool";
    private static final String PROMPT_NAME = "stateless-prompt";
    private static final String RESOURCE_URI = "https://foo";
    private static final String RESOURCE_NAME = "stateless-resource";
    private static final String COMPLETION_NAME = "stateless-completion";
    private static final HeaderName MCP_PROTOCOL_VERSION = HeaderNames.create("Mcp-Protocol-Version");
    private final McpClient statefulClient;
    private final JsonRpcClient statelessClient;

    StatelessServerTest(WebServer server, JsonRpcClient client) {
        McpTransport transport = new StreamableHttpMcpTransport.Builder()
                .url("http://localhost:" + server.port())
                .logRequests(true)
                .logResponses(true)
                .timeout(Duration.ofSeconds(1))
                .build();
        this.statefulClient = new DefaultMcpClient.Builder()
                .transport(transport)
                .initializationTimeout(Duration.ofSeconds(1))
                .build();
        this.statelessClient = client;
    }

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        StatelessServer.setUpRoute(builder);
    }

    @Test
    void statefulToolsList() {
        var tools = statefulClient.listTools();
        assertThat(tools.size(), is(1));
        assertThat(tools.getFirst().name(), is(TOOL_NAME));
    }

    @Test
    void statelessToolsList() {
        try (var response = statelessClient.rpcMethod("tools/list")
                .rpcId(1)
                .submit()) {
            assertThat(response.error().isEmpty(), is(true));
            assertThat(response.result().isEmpty(), is(false));
            var tools = response.result().orElseThrow().asJsonObject().getJsonArray("tools");
            assertThat(tools.size(), is(1));
            assertThat(tools.getJsonObject(0).getString("name"), is(TOOL_NAME));
        }
    }

    @Test
    void statefulPromptsList() {
        var prompts = statefulClient.listPrompts();
        assertThat(prompts.size(), is(1));
        assertThat(prompts.getFirst().name(), is(PROMPT_NAME));
    }

    @Test
    void statelessPromptsList() {
        try (var response = statelessClient.rpcMethod("prompts/list")
                .rpcId(2)
                .submit()) {
            assertThat(response.error().isEmpty(), is(true));
            assertThat(response.result().isEmpty(), is(false));
            var prompts = response.result().orElseThrow().asJsonObject().getJsonArray("prompts");
            assertThat(prompts.size(), is(1));
            assertThat(prompts.getJsonObject(0).getString("name"), is(PROMPT_NAME));
        }
    }

    @Test
    void statefulResourcesList() {
        var resources = statefulClient.listResources();
        assertThat(resources.size(), is(1));
        assertThat(resources.getFirst().name(), is(RESOURCE_NAME));
        assertThat(resources.getFirst().uri(), is(RESOURCE_URI));
    }

    @Test
    void statelessResourcesList() {
        try (var response = statelessClient.rpcMethod("resources/list")
                .rpcId(3)
                .submit()) {
            assertThat(response.error().isEmpty(), is(true));
            assertThat(response.result().isEmpty(), is(false));
            var resources = response.result().orElseThrow().asJsonObject().getJsonArray("resources");
            assertThat(resources.size(), is(1));
            assertThat(resources.getJsonObject(0).getString("name"), is(RESOURCE_NAME));
            assertThat(resources.getJsonObject(0).getString("uri"), is(RESOURCE_URI));
        }
    }

    @Test
    void statefulToolCall() {
        var exception = assertThrows(ToolExecutionException.class, () -> statefulClient.executeTool(ToolExecutionRequest.builder()
                                                                                                             .name(TOOL_NAME)
                                                                                                             .arguments("{}")
                                                                                                             .build()));
        assertThat(exception.getMessage(), containsString("Roots is enabled"));
    }

    @Test
    void statelessToolCall() {
        try (var response = statelessClient.rpcMethod("tools/call")
                .rpcId(4)
                .param("name", TOOL_NAME)
                .param("arguments", JSON_PROVIDER.createObjectBuilder().build())
                .submit()) {
            assertThat(response.error().isEmpty(), is(true));
            assertThat(response.result().isEmpty(), is(false));
            var result = response.result().orElseThrow().asJsonObject();
            assertThat(result.getJsonArray("content").getJsonObject(0).getString("text"), is(SUCCESS_TEXT));
        }
    }

    @Test
    void statefulPromptCall() {
        var exception = assertThrows(McpException.class, () -> statefulClient.getPrompt(PROMPT_NAME, Map.of()));
        assertThat(exception.getMessage(), containsString("Roots is enabled"));
    }

    @Test
    void statelessPromptCall() {
        try (var response = statelessClient.rpcMethod("prompts/get")
                .rpcId(5)
                .param("name", PROMPT_NAME)
                .param("arguments", JSON_PROVIDER.createObjectBuilder().build())
                .submit()) {
            assertThat(response.error().isEmpty(), is(true));
            assertThat(response.result().isEmpty(), is(false));
            var prompt = response.result().orElseThrow().asJsonObject();
            assertThat(prompt.getJsonArray("messages").getJsonObject(0).getJsonObject("content").getString("text"),
                       is(SUCCESS_TEXT));
        }
    }

    @Test
    void statefulResourceCall() {
        var exception = assertThrows(McpException.class, () -> statefulClient.readResource(RESOURCE_URI));
        assertThat(exception.getMessage(), containsString("Roots is enabled"));
    }

    @Test
    void statelessResourceCall() {
        try (var response = statelessClient.rpcMethod("resources/read")
                .rpcId(6)
                .param("uri", RESOURCE_URI)
                .submit()) {
            assertThat(response.error().isEmpty(), is(true));
            assertThat(response.result().isEmpty(), is(false));
            var resource = response.result().orElseThrow().asJsonObject();
            assertThat(resource.getJsonArray("contents").getJsonObject(0).getString("text"), is(SUCCESS_TEXT));
        }
    }

    @Test
    void statelessCompletion() {
        JsonObject ref = JSON_PROVIDER.createObjectBuilder()
                .add("type", "ref/prompt")
                .add("name", COMPLETION_NAME)
                .build();
        JsonObject argument = JSON_PROVIDER.createObjectBuilder()
                .add("name", "arg")
                .add("value", "suc")
                .build();
        try (var response = statelessClient.rpcMethod("completion/complete")
                .rpcId(7)
                .param("ref", ref)
                .param("argument", argument)
                .submit()) {
            assertThat(response.error().isEmpty(), is(true));
            assertThat(response.result().isEmpty(), is(false));
        }
    }

    @Test
    void statelessInitializedNotificationWithoutSession() {
        try (var response = statelessClient.rpcMethod("notifications/initialized")
                .header(MCP_PROTOCOL_VERSION, "2025-12-34")
                .submit()) {
            assertThat(response.status(), is(Status.ACCEPTED_202));
        }
    }

    @Test
    void statelessCancelledNotificationWithoutSession() {
        try (var response = statelessClient.rpcMethod("notifications/cancelled")
                .param("requestId", 42)
                .param("reason", "test")
                .submit()) {
            assertThat(response.status(), is(Status.ACCEPTED_202));
        }
    }

    @Test
    void statelessRootsChangedNotificationWithoutSession() {
        try (var response = statelessClient.rpcMethod("notifications/roots/list_changed")
                .submit()) {
            assertThat(response.status(), is(Status.ACCEPTED_202));
        }
    }
}
