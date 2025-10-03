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

import java.util.Map;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import static io.helidon.extensions.mcp.tests.McpWeather.PROMPT_ARGUMENT_DESCRIPTION;
import static io.helidon.extensions.mcp.tests.McpWeather.PROMPT_ARGUMENT_NAME;
import static io.helidon.extensions.mcp.tests.McpWeather.PROMPT_DESCRIPTION;
import static io.helidon.extensions.mcp.tests.McpWeather.PROMPT_NAME;
import static io.helidon.extensions.mcp.tests.McpWeather.PROTOCOL_VERSION;
import static io.helidon.extensions.mcp.tests.McpWeather.RESOURCE_DESCRIPTION;
import static io.helidon.extensions.mcp.tests.McpWeather.RESOURCE_NAME;
import static io.helidon.extensions.mcp.tests.McpWeather.RESOURCE_URI;
import static io.helidon.extensions.mcp.tests.McpWeather.SERVER_NAME;
import static io.helidon.extensions.mcp.tests.McpWeather.SERVER_VERSION;
import static io.helidon.extensions.mcp.tests.McpWeather.TOOL_DESCRIPTION;
import static io.helidon.extensions.mcp.tests.McpWeather.TOOL_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isOneOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * {@link McpWeather} test using McpSdk client.
 */
abstract class AbstractMcpSdkClientTest extends AbstractMcpSdkTest {

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        McpWeather.setUpRoute(builder);
    }

    @Test
    void testClientInitialize() {
        McpSchema.InitializeResult result = client().initialize();

        var implementation = result.serverInfo();
        assertThat(implementation.name(), is(SERVER_NAME));
        assertThat(implementation.version(), is(SERVER_VERSION));

        var capabilities = result.capabilities();
        assertThat(capabilities.experimental(), is(nullValue()));
        assertThat(capabilities.logging(), notNullValue(McpSchema.ServerCapabilities.LoggingCapabilities.class));
        assertThat(capabilities.prompts().listChanged(), is(true));

        var resources = capabilities.resources();
        assertThat(resources.listChanged(), is(true));
        assertThat(resources.subscribe(), is(true));

        var tools = capabilities.tools();
        assertThat(tools.listChanged(), is(true));
        assertThat(result.instructions(), is(""));
        assertThat(result.protocolVersion(), isOneOf(PROTOCOL_VERSION));
    }

    @Test
    void testPing() {
        var ping = client().ping();
        assertThat(ping.toString(), is("{}"));
    }

    @Test
    void testToolList() {
        McpSchema.ListToolsResult result = client().listTools();
        assertThat(result.nextCursor(), is(nullValue()));

        var tools = result.tools();
        assertThat(tools.size(), is(1));

        var tool = tools.getFirst();
        assertThat(tool.name(), is(TOOL_NAME));
        assertThat(tool.description(), is(TOOL_DESCRIPTION));

        var schema = tool.inputSchema();
        assertThat(schema.type(), is("object"));
    }

    @Test
    void testPromptList() {
        McpSchema.ListPromptsResult listPrompt = client().listPrompts();
        assertThat(listPrompt.nextCursor(), is(nullValue()));

        var list = listPrompt.prompts();
        assertThat(list.size(), is(1));

        var prompt = list.getFirst();
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
        McpSchema.ListResourcesResult result = client().listResources();
        assertThat(result.nextCursor(), is(nullValue()));

        var list = result.resources();
        assertThat(list.size(), is(1));

        var resource = list.getFirst();
        assertThat(resource.uri(), is(RESOURCE_URI));
        assertThat(resource.name(), is(RESOURCE_NAME));
        assertThat(resource.description(), is(RESOURCE_DESCRIPTION));
    }

    @Test
    void testToolCall() {
        McpSchema.CallToolResult result = client().callTool(
                new McpSchema.CallToolRequest(TOOL_NAME, Map.of("town", "Praha")));

        assertThat(result.isError(), is(false));

        var contents = result.content();
        assertThat(contents.size(), is(1));

        var content = contents.getFirst();
        assertThat(content.type(), is("text"));
        assertThat(content, instanceOf(McpSchema.TextContent.class));

        var text = (McpSchema.TextContent) content;
        assertThat(text.text(), is("There is a hurricane in Praha"));
        assertThat(text.priority(), is(nullValue()));
    }

    @Test
    void testPromptCall() {
        var result = client().getPrompt(
                new McpSchema.GetPromptRequest(PROMPT_NAME, Map.of(PROMPT_ARGUMENT_NAME, "Praha")));
        assertThat(result.description(), is(PROMPT_DESCRIPTION));

        var messages = result.messages();
        assertThat(messages.size(), is(1));

        var message = messages.getFirst();
        assertThat(message.role(), is(McpSchema.Role.USER));
        assertThat(message.content(), instanceOf(McpSchema.TextContent.class));

        var text = (McpSchema.TextContent) message.content();
        assertThat(text.text(), is("What is the weather like in Praha ?"));
        assertThat(text.priority(), nullValue());
        assertThat(text.audience(), nullValue());
    }

    @Test
    void testResourceCall() {
        var result = client().readResource(new McpSchema.ReadResourceRequest(RESOURCE_URI));

        var contents = result.contents();
        assertThat(contents.size(), is(1));

        var content = contents.getFirst();
        assertThat(content.uri(), is(RESOURCE_URI));
        assertThat(content.mimeType(), is(MediaTypes.TEXT_PLAIN_VALUE));
        assertThat(content, instanceOf(McpSchema.TextResourceContents.class));

        var text = (McpSchema.TextResourceContents) content;
        assertThat(text.text(), is("There are severe weather alerts in Praha"));
    }

    @Test
    void testCompletion() {
        var result = client().completeCompletion(new McpSchema.CompleteRequest(
                new McpSchema.PromptReference("ref/prompt", PROMPT_NAME),
                new McpSchema.CompleteRequest.CompleteArgument(PROMPT_ARGUMENT_NAME, "f")));

        var completion = result.completion();
        assertThat(completion.hasMore(), is(false));
        assertThat(completion.total(), is(1));
        assertThat(completion.values().size(), is(1));
        assertThat(completion.values().getFirst(), is("foo"));
    }
}
