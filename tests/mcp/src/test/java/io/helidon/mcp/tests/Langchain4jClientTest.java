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
package io.helidon.mcp.tests;

import java.util.Map;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.McpPromptContent;
import dev.langchain4j.mcp.client.McpResourceContents;
import dev.langchain4j.mcp.client.McpRole;
import dev.langchain4j.mcp.client.McpTextContent;
import dev.langchain4j.mcp.client.McpTextResourceContents;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static io.helidon.mcp.tests.McpWeather.PROMPT_ARGUMENT_DESCRIPTION;
import static io.helidon.mcp.tests.McpWeather.PROMPT_ARGUMENT_NAME;
import static io.helidon.mcp.tests.McpWeather.PROMPT_DESCRIPTION;
import static io.helidon.mcp.tests.McpWeather.PROMPT_NAME;
import static io.helidon.mcp.tests.McpWeather.RESOURCE_DESCRIPTION;
import static io.helidon.mcp.tests.McpWeather.RESOURCE_NAME;
import static io.helidon.mcp.tests.McpWeather.RESOURCE_URI;
import static io.helidon.mcp.tests.McpWeather.TOOL_DESCRIPTION;
import static io.helidon.mcp.tests.McpWeather.TOOL_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * {@link McpWeather} tests using Langchain4j client.
 */
@ServerTest
class Langchain4jClientTest {

    private static McpClient client;

    Langchain4jClientTest(WebServer server) {
        McpTransport transport = new HttpMcpTransport.Builder()
                .sseUrl("http://localhost:" + server.port())
                .logRequests(true)
                .logResponses(true)
                .build();

        client = new DefaultMcpClient.Builder()
                .transport(transport)
                .build();
    }

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        McpWeather.setUpRoute(builder);
    }

    @AfterAll
    static void closeClient() throws Exception {
        client.close();
    }

    @Test
    void testPing() {
        client.checkHealth();
    }

    @Test
    void testToolList() {
        var result = client.listTools();
        assertThat(result.size(), is(1));

        var tool = result.getFirst();
        assertThat(tool.name(), is(TOOL_NAME));
        assertThat(tool.description(), is(TOOL_DESCRIPTION));

        var parameters = tool.parameters();
        assertThat(parameters.properties().size(), is(1));

        var required = parameters.required();
        assertThat(required.size(), is(1));
        assertThat(required.getFirst(), is("town"));

        var parameter = parameters.properties().get("town");
        assertThat(parameter, notNullValue());
    }

    @Test
    void testPromptList() {
        var listPrompt = client.listPrompts();
        assertThat(listPrompt.size(), is(1));

        var prompt = listPrompt.getFirst();
        assertThat(prompt.name(), is(PROMPT_NAME));
        assertThat(prompt.description(), is(PROMPT_DESCRIPTION));

        var arguments = prompt.arguments();
        assertThat(arguments.size(), is(1));

        var argument = arguments.getFirst();
        assertThat(argument.name(), is(PROMPT_ARGUMENT_NAME));
        assertThat(argument.description(), is(PROMPT_ARGUMENT_DESCRIPTION));
    }

    @Test
    void testResourceList() {
        var result = client.listResources();
        assertThat(result.size(), is(1));

        var resource = result.getFirst();
        assertThat(resource.uri(), is(RESOURCE_URI));
        assertThat(resource.name(), is(RESOURCE_NAME));
        assertThat(resource.description(), is(RESOURCE_DESCRIPTION));
    }

    @Test
    void testToolCall() {
        var result = client.executeTool(ToolExecutionRequest.builder()
                                                .name(TOOL_NAME)
                                                .arguments("{\"town\":\"Praha\"}")
                                                .build());
        assertThat(result, is("There is a hurricane in Praha"));
    }

    @Test
    void testPromptCall() {
        var result = client.getPrompt(PROMPT_NAME, Map.of(PROMPT_ARGUMENT_NAME, "Praha"));
        assertThat(result.description(), is(PROMPT_DESCRIPTION));

        var messages = result.messages();
        assertThat(messages.size(), is(1));

        var message = messages.getFirst();
        assertThat(message.role(), is(McpRole.USER));

        var text = (McpTextContent) message.content();
        assertThat(text.text(), is("What is the weather like in Praha ?"));
        assertThat(text.type(), is(McpPromptContent.Type.TEXT));
    }

    @Test
    void testResourceCall() {
        var result = client.readResource(RESOURCE_URI);

        var contents = result.contents();
        assertThat(contents.size(), is(1));

        var content = contents.getFirst();
        assertThat(content.type(), is(McpResourceContents.Type.TEXT));
        assertThat(content, instanceOf(McpTextResourceContents.class));

        var text = (McpTextResourceContents) content;
        assertThat(text.uri(), is(RESOURCE_URI));
        assertThat(text.mimeType(), is(MediaTypes.TEXT_PLAIN_VALUE));
        assertThat(text.text(), is("There are severe weather alerts in Praha"));
    }

}
