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

package io.helidon.extensions.mcp.examples.calendar.declarative;

import java.util.List;
import java.util.Map;

import io.helidon.common.media.type.MediaTypes;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

@TestMethodOrder(OrderAnnotation.class)
abstract class BaseTest {

    abstract McpSyncClient client();

    @Test
    @Order(1)
    void testToolList() {
        McpSchema.ListToolsResult listTool = client().listTools();
        List<McpSchema.Tool> tools = listTool.tools();
        assertThat(tools.size(), is(2));

        McpSchema.Tool tool1 = tools.getFirst();
        assertThat(tool1.name(), is("listCalendarEvents"));
        assertThat(tool1.description(), is("List calendar events"));

        McpSchema.JsonSchema schema1 = tool1.inputSchema();
        assertThat(schema1.type(), is("object"));
        assertThat(schema1.properties().keySet(), hasItems("date"));

        McpSchema.Tool tool2 = tools.getLast();
        assertThat(tool2.name(), is("addCalendarEvent"));
        assertThat(tool2.description(), is("Adds a new event to the calendar"));

        McpSchema.JsonSchema schema2 = tool2.inputSchema();
        assertThat(schema2.type(), is("object"));
        assertThat(schema2.properties().keySet(), hasItems("event"));
    }

    @Test
    @Order(2)
    void testAddToolCall() {
        Map<String, Object> arguments = Map.of("event", Map.of("name", "Frank-birthday",
                                                               "date", "2021-04-20",
                                                               "attendees", List.of("Frank")));
        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("addCalendarEvent", arguments);
        McpSchema.CallToolResult result = client().callTool(request);
        assertThat(result.isError(), is(false));

        List<McpSchema.Content> contents = result.content();
        assertThat(contents.size(), is(1));

        McpSchema.Content content = contents.getFirst();
        assertThat(content.type(), is("text"));
        assertThat(content, instanceOf(McpSchema.TextContent.class));

        McpSchema.TextContent textContent = (McpSchema.TextContent) content;
        assertThat(textContent.text(), is("New event added to the calendar"));
    }

    @Test
    @Order(3)
    void testListToolCall() {
        Map<String, Object> arguments = Map.of("date", "2021-04-20");
        McpSchema.CallToolRequest request = new McpSchema.CallToolRequest("listCalendarEvents", arguments);
        McpSchema.CallToolResult result = client().callTool(request);
        assertThat(result.isError(), is(false));

        List<McpSchema.Content> contents = result.content();
        assertThat(contents.size(), is(1));

        McpSchema.Content content = contents.getFirst();
        assertThat(content.type(), is("text"));
        assertThat(content, instanceOf(McpSchema.TextContent.class));

        McpSchema.TextContent textContent = (McpSchema.TextContent) content;
        assertThat(textContent.text(), containsString("Frank-birthday"));
    }

    @Test
    @Order(4)
    void testPromptList() {
        McpSchema.ListPromptsResult listPrompt = client().listPrompts();
        List<McpSchema.Prompt> prompts = listPrompt.prompts();
        assertThat(prompts.size(), is(1));

        McpSchema.Prompt prompt = prompts.getFirst();
        assertThat(prompt.name(), is("createEventPrompt"));
        assertThat(prompt.description(), is("Prompt to create a new event given a name, date and attendees"));

        List<McpSchema.PromptArgument> arguments = prompt.arguments();
        arguments.sort(this::sortArguments);
        assertThat(arguments.size(), is(3));

        McpSchema.PromptArgument attendees = arguments.getFirst();
        assertThat(attendees.name(), is("attendees"));
        assertThat(attendees.description(), is("event's attendees"));
        assertThat(attendees.required(), is(true));

        McpSchema.PromptArgument date = arguments.get(1);
        assertThat(date.name(), is("date"));
        assertThat(date.description(), is("event's date"));
        assertThat(date.required(), is(true));

        McpSchema.PromptArgument name = arguments.getLast();
        assertThat(name.name(), is("name"));
        assertThat(name.description(), is("event's name"));
        assertThat(name.required(), is(true));
    }

    @Test
    @Order(5)
    void testPromptCall() {
        Map<String, Object> arguments = Map.of("name", "Frank-birthday", "date", "2021-04-20", "attendees", "Frank");
        McpSchema.GetPromptRequest request = new McpSchema.GetPromptRequest("createEventPrompt", arguments);
        McpSchema.GetPromptResult promptResult = client().getPrompt(request);
        assertThat(promptResult.description(), is("Prompt to create a new event given a name, date and attendees"));

        List<McpSchema.PromptMessage> messages = promptResult.messages();
        assertThat(messages.size(), is(1));

        McpSchema.PromptMessage message = messages.getFirst();
        assertThat(message.role(), is(McpSchema.Role.USER));
        assertThat(message.content(), instanceOf(McpSchema.TextContent.class));

        McpSchema.TextContent textContent = (McpSchema.TextContent) message.content();
        assertThat(textContent.text(), is(
                """
                        Create a new calendar event with name Frank-birthday, on 2021-04-20 with attendees Frank. Make
                        sure all attendees are free to attend the event.
                        """));
    }

    @Test
    @Order(6)
    void testResourceList() {
        McpSchema.ListResourcesResult result = client().listResources();
        List<McpSchema.Resource> resources = result.resources();
        assertThat(resources.size(), is(1));

        McpSchema.Resource resource = resources.getFirst();
        assertThat(resource.name(), is("eventsResource"));
        assertThat(resource.uri(), startsWith("file://"));
        assertThat(resource.mimeType(), is(MediaTypes.TEXT_PLAIN_VALUE));
        assertThat(resource.description(), is("List of calendar events created"));
    }

    @Test
    @Order(7)
    void testResourceCall() {
        String uri = client().listResources().resources().getFirst().uri();
        McpSchema.ReadResourceRequest request = new McpSchema.ReadResourceRequest(uri);
        McpSchema.ReadResourceResult result = client().readResource(request);

        List<McpSchema.ResourceContents> contents = result.contents();
        assertThat(contents.size(), is(1));

        McpSchema.ResourceContents content = contents.getFirst();
        assertThat(content.uri(), is(uri));
        assertThat(content.mimeType(), is(MediaTypes.TEXT_PLAIN_VALUE));
        assertThat(content, instanceOf(McpSchema.TextResourceContents.class));

        McpSchema.TextResourceContents textContent = (McpSchema.TextResourceContents) content;
        assertThat(textContent.text(), is("Event: { name: Frank-birthday, date: 2021-04-20, attendees: [Frank] }\n"));
    }

    @Test
    @Order(8)
    void testResourceTemplateList() {
        McpSchema.ListResourceTemplatesResult result = client().listResourceTemplates();
        List<McpSchema.ResourceTemplate> templates = result.resourceTemplates();
        assertThat(templates.size(), is(1));

        McpSchema.ResourceTemplate template = templates.getFirst();
        assertThat(template.uriTemplate(), containsString("{name}"));
        assertThat(template.mimeType(), is(MediaTypes.TEXT_PLAIN_VALUE));
        assertThat(template.name(), is("eventResourceTemplate"));
        assertThat(template.description(), is("List single calendar event by name"));
    }

    @Test
    @Order(9)
    void testResourceTemplateCall() {
        McpSchema.ReadResourceRequest request = new McpSchema.ReadResourceRequest("file://events/Frank-birthday");
        McpSchema.ReadResourceResult result = client().readResource(request);
        var contents = result.contents();
        assertThat(contents.size(), is(1));

        McpSchema.ResourceContents content = contents.getFirst();
        assertThat(content, instanceOf(McpSchema.TextResourceContents.class));

        McpSchema.TextResourceContents text = (McpSchema.TextResourceContents) content;
        assertThat(content.uri(), is("file://events/Frank-birthday"));
        assertThat(content.mimeType(), is(MediaTypes.TEXT_PLAIN_VALUE));
        assertThat(text.text(), is("Event: { name: Frank-birthday, date: 2021-04-20, attendees: [Frank] }"));
    }

    @Test
    @Order(10)
    void testCalendarEventPromptCompletion() {
        McpSchema.CompleteRequest request1 = new McpSchema.CompleteRequest(
                new McpSchema.PromptReference("createEventPrompt"),
                new McpSchema.CompleteRequest.CompleteArgument("name", ""));
        McpSchema.CompleteResult.CompleteCompletion completion1 = client().completeCompletion(request1).completion();
        assertThat(completion1.hasMore(), is(false));
        assertThat(completion1.total(), is(1));
        assertThat(completion1.values().size(), is(1));
        assertThat(completion1.values().getFirst(), is("Frank & Friends"));

        McpSchema.CompleteRequest request2 = new McpSchema.CompleteRequest(
                new McpSchema.PromptReference("createEventPrompt"),
                new McpSchema.CompleteRequest.CompleteArgument("date", ""));
        McpSchema.CompleteResult.CompleteCompletion completion2 = client().completeCompletion(request2).completion();
        assertThat(completion2.hasMore(), is(false));
        assertThat(completion2.total(), is(3));
        assertThat(completion2.values().size(), is(3));

        McpSchema.CompleteRequest request3 = new McpSchema.CompleteRequest(
                new McpSchema.PromptReference("createEventPrompt"),
                new McpSchema.CompleteRequest.CompleteArgument("attendees", ""));
        McpSchema.CompleteResult.CompleteCompletion completion3 = client().completeCompletion(request3).completion();
        assertThat(completion3.hasMore(), is(false));
        assertThat(completion3.total(), is(3));
        assertThat(completion3.values().size(), is(3));
    }

    @Test
    @Order(11)
    void testCalendarEventResourceCompletion() {
        McpSchema.CompleteRequest request = new McpSchema.CompleteRequest(
                new McpSchema.ResourceReference(McpCalendarServer.EVENTS_URI_TEMPLATE),
                new McpSchema.CompleteRequest.CompleteArgument("name", ""));
        McpSchema.CompleteResult.CompleteCompletion completion = client().completeCompletion(request).completion();
        assertThat(completion.hasMore(), is(false));
        assertThat(completion.total(), is(1));
        assertThat(completion.values().size(), is(1));
        assertThat(completion.values().getFirst(), is("Frank-birthday"));
    }

    private int sortArguments(McpSchema.PromptArgument first, McpSchema.PromptArgument second) {
        return first.name().compareTo(second.name());
    }
}
