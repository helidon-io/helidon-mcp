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

package io.helidon.extensions.mcp.examples.calendar;

import java.util.List;
import java.util.Map;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

@ServerTest
class MainTest {
    private final McpSyncClient client;

    MainTest(WebServer server) {
        this.client = McpClient.sync(HttpClientSseClientTransport.builder("http://localhost:" + server.port())
                                             .sseEndpoint("/calendar")
                                             .build())
                .build();
        client.initialize();
    }

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        Main.setUpRoute(builder);
    }

    @Test
    @Order(1)
    void testToolList() {
        McpSchema.ListToolsResult listTool = client.listTools();
        List<McpSchema.Tool> tools = listTool.tools();
        assertThat(tools.size(), is(1));

        McpSchema.Tool tool = tools.getFirst();
        assertThat(tool.name(), is("add-calendar-event"));
        assertThat(tool.description(), is("Adds a new event to the calendar."));

        McpSchema.JsonSchema schema = tool.inputSchema();
        assertThat(schema.type(), is("object"));
        assertThat(schema.properties().keySet(), hasItems("name", "date", "attendees"));
    }

    @Test
    @Order(2)
    void testToolCall() {
        Map<String, Object> arguments = Map.of("name", "Franck-birthday", "date", "2021-04-20", "attendees", List.of("Franck"));
        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("add-calendar-event", arguments);
        McpSchema.CallToolResult result = client.callTool(request);
        assertThat(result.isError(), nullValue());

        List<McpSchema.Content> contents = result.content();
        assertThat(contents.size(), is(1));

        McpSchema.Content content = contents.getFirst();
        assertThat(content.type(), is("text"));
        assertThat(content, instanceOf(McpSchema.TextContent.class));

        McpSchema.TextContent textContent = (McpSchema.TextContent) content;
        assertThat(textContent.text(), is("New event added to the calendar."));
    }

    @Test
    @Order(3)
    void testPromptList() {
        McpSchema.ListPromptsResult listPrompt = client.listPrompts();
        List<McpSchema.Prompt> prompts = listPrompt.prompts();
        assertThat(prompts.size(), is(1));

        McpSchema.Prompt prompt = prompts.getFirst();
        assertThat(prompt.name(), is("create-event"));
        assertThat(prompt.description(), is("Create a new event and add it to the calendar"));

        List<McpSchema.PromptArgument> arguments = prompt.arguments();
        arguments.sort(this::sortArguments);
        assertThat(arguments.size(), is(3));

        McpSchema.PromptArgument attendees = arguments.getFirst();
        assertThat(attendees.name(), is("attendees"));
        assertThat(attendees.description(), is("Event attendees names separated by commas"));
        assertThat(attendees.required(), is(true));

        McpSchema.PromptArgument date = arguments.get(1);
        assertThat(date.name(), is("date"));
        assertThat(date.description(), is("Event date in the following format YYYY-MM-DD"));
        assertThat(date.required(), is(true));

        McpSchema.PromptArgument name = arguments.getLast();
        assertThat(name.name(), is("name"));
        assertThat(name.description(), is("Event name"));
        assertThat(name.required(), is(true));
    }

    @Test
    @Order(4)
    void testPromptCall() {
        Map<String, Object> arguments = Map.of("name", "Franck-birthday", "date", "2021-04-20", "attendees", "Franck");
        McpSchema.GetPromptRequest request = new McpSchema.GetPromptRequest("create-event", arguments);
        McpSchema.GetPromptResult promptResult = client.getPrompt(request);
        assertThat(promptResult.description(), is("Create a new event and add it to the calendar"));

        List<McpSchema.PromptMessage> messages = promptResult.messages();
        assertThat(messages.size(), is(1));

        McpSchema.PromptMessage message = messages.getFirst();
        assertThat(message.role(), is(McpSchema.Role.USER));
        assertThat(message.content(), instanceOf(McpSchema.TextContent.class));

        McpSchema.TextContent textContent = (McpSchema.TextContent) message.content();
        assertThat(textContent.text(), is("""
                                             Create a new calendar event with name Franck-birthday, at date 2021-04-20 and attendees Franck. Make
                                             sure all attendees are free to attend the event.
                                             """));
    }

    @Test
    @Order(5)
    void testResourceList() {
        McpSchema.ListResourcesResult result = client.listResources();
        List<McpSchema.Resource> resources = result.resources();
        assertThat(resources.size(), is(1));

        McpSchema.Resource resource = resources.getFirst();
        assertThat(resource.name(), is("calendar-events"));
        assertThat(resource.uri(), startsWith("file:///"));
        assertThat(resource.mimeType(), is(MediaTypes.TEXT_PLAIN_VALUE));
        assertThat(resource.description(), is("List of calendar events created"));
    }

    @Test
    @Order(6)
    void testResourceCall() {
        String uri = client.listResources().resources().getFirst().uri();
        McpSchema.ReadResourceRequest request = new McpSchema.ReadResourceRequest(uri);
        McpSchema.ReadResourceResult result = client.readResource(request);

        List<McpSchema.ResourceContents> contents = result.contents();
        assertThat(contents.size(), is(1));

        McpSchema.ResourceContents content = contents.getFirst();
        assertThat(content.uri(), is(uri));
        assertThat(content.mimeType(), is(MediaTypes.TEXT_PLAIN_VALUE));
        assertThat(content, instanceOf(McpSchema.TextResourceContents.class));

        McpSchema.TextResourceContents textContent = (McpSchema.TextResourceContents) content;
        assertThat(textContent.text(), is("Event: { name: Franck-birthday, date: 2021-04-20, attendees: [Franck] }\n"));
    }

    @Test
    @Order(7)
    void testResourceTemplateList() {
        McpSchema.ListResourceTemplatesResult result = client.listResourceTemplates();
        List<McpSchema.ResourceTemplate> templates = result.resourceTemplates();
        assertThat(templates.size(), is(1));

        McpSchema.ResourceTemplate template = templates.getFirst();
        assertThat(template.uriTemplate(), containsString("{path}"));
        assertThat(template.mimeType(), is(MediaTypes.TEXT_PLAIN_VALUE));
        assertThat(template.name(), is("calendar-events-resource-template"));
        assertThat(template.description(), is("Resource Template to find calendar events registry, path is \"calendar\""));
    }

    private int sortArguments(McpSchema.PromptArgument first, McpSchema.PromptArgument second) {
        return first.name().compareTo(second.name());
    }
}
