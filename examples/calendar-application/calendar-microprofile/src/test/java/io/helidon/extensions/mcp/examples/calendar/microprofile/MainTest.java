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
package io.helidon.extensions.mcp.examples.calendar.microprofile;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.microprofile.testing.Socket;
import io.helidon.microprofile.testing.junit5.HelidonTest;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.McpGetPromptResult;
import dev.langchain4j.mcp.client.McpPrompt;
import dev.langchain4j.mcp.client.McpPromptArgument;
import dev.langchain4j.mcp.client.McpPromptMessage;
import dev.langchain4j.mcp.client.McpResource;
import dev.langchain4j.mcp.client.McpResourceContents;
import dev.langchain4j.mcp.client.McpResourceTemplate;
import dev.langchain4j.mcp.client.McpRole;
import dev.langchain4j.mcp.client.McpTextContent;
import dev.langchain4j.mcp.client.McpTextResourceContents;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import jakarta.inject.Inject;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

@HelidonTest
@TestMethodOrder(OrderAnnotation.class)
public class MainTest {
    private final McpClient client;

    @Inject
    MainTest(@Socket("@default") URI uri) {
        McpTransport transport = new StreamableHttpMcpTransport.Builder()
                .url(uri.toASCIIString() + "/calendar")
                .logRequests(true)
                .logResponses(true)
                .timeout(Duration.ofSeconds(1))
                .build();
        client = new DefaultMcpClient.Builder()
                .transport(transport)
                .initializationTimeout(Duration.ofSeconds(1))
                .build();
    }

    @Test
    @Order(1)
    void testToolList() {
        List<ToolSpecification> tools = client.listTools();
        assertThat(tools.size(), is(2));

        ToolSpecification tool1 = tools.stream()
                .filter(it -> it.name().equals("listCalendarEvents"))
                .findFirst()
                .orElseThrow();
        assertThat(tool1.name(), is("listCalendarEvents"));
        assertThat(tool1.description(), is("List calendar events"));

        assertThat(tool1.parameters().properties().keySet(), hasItems("date"));

        ToolSpecification tool2 = tools.stream()
                .filter(it -> it.name().equals("addCalendarEvent"))
                .findFirst()
                .orElseThrow();
        assertThat(tool2.name(), is("addCalendarEvent"));
        assertThat(tool2.description(), is("Adds a new event to the calendar"));

        assertThat(tool2.parameters().properties().keySet(), hasItems("event"));
    }

    @Test
    @Order(2)
    void testAddToolCall() {
        var result = client.executeTool(ToolExecutionRequest.builder()
                .name("addCalendarEvent")
                .arguments("""
                        {
                          "event": {
                            "name": "Frank-birthday",
                            "date": "2021-04-20",
                            "attendees": ["Frank"]
                          }
                        }
                        """)
                .build());

        assertThat(result.resultText(), is("New event added to the calendar"));
    }

    @Test
    @Order(3)
    void testListToolCall() {
        var result = client.executeTool(ToolExecutionRequest.builder()
                .name("listCalendarEvents")
                .arguments("{\"date\":\"2021-04-20\"}")
                .build());

        assertThat(result.resultText(), containsString("Frank-birthday"));
    }

    @Test
    @Order(4)
    void testPromptList() {
        List<McpPrompt> prompts = client.listPrompts();
        assertThat(prompts.size(), is(1));

        McpPrompt prompt = prompts.getFirst();
        assertThat(prompt.name(), is("createEventPrompt"));
        assertThat(prompt.description(), is("Prompt to create a new event given a name, date and attendees"));

        List<McpPromptArgument> arguments = prompt.arguments();
        arguments.sort(this::sortArguments);
        assertThat(arguments.size(), is(3));

        McpPromptArgument attendees = arguments.getFirst();
        assertThat(attendees.name(), is("attendees"));
        assertThat(attendees.description(), is("event's attendees"));
        assertThat(attendees.required(), is(true));

        McpPromptArgument date = arguments.get(1);
        assertThat(date.name(), is("date"));
        assertThat(date.description(), is("event's date"));
        assertThat(date.required(), is(true));

        McpPromptArgument name = arguments.getLast();
        assertThat(name.name(), is("name"));
        assertThat(name.description(), is("event's name"));
        assertThat(name.required(), is(true));
    }

    @Test
    @Order(5)
    void testPromptCall() {
        Map<String, Object> arguments = Map.of("name", "Frank-birthday", "date", "2021-04-20", "attendees", "Frank");
        McpGetPromptResult promptResult = client.getPrompt("createEventPrompt", arguments);
        assertThat(promptResult.description(), is(nullValue()));

        List<McpPromptMessage> messages = promptResult.messages();
        assertThat(messages.size(), is(1));

        McpPromptMessage message = messages.getFirst();
        assertThat(message.role(), is(McpRole.USER));
        assertThat(message.content(), instanceOf(McpTextContent.class));

        McpTextContent textContent = (McpTextContent) message.content();
        assertThat(textContent.text(), containsString("Create a new calendar event with name Frank-birthday"));
        assertThat(textContent.text(), containsString("on 2021-04-20"));
        assertThat(textContent.text(), containsString("attendees Frank"));
    }

    @Test
    @Order(6)
    void testResourceList() {
        List<McpResource> resources = client.listResources();
        assertThat(resources.size(), is(1));

        McpResource resource = resources.getFirst();
        assertThat(resource.name(), is("eventsResource"));
        assertThat(resource.uri(), startsWith("file://"));
        assertThat(resource.mimeType(), is(MediaTypes.TEXT_PLAIN_VALUE));
        assertThat(resource.description(), is("List of calendar events created"));
    }

    @Test
    @Order(7)
    void testResourceCall() {
        String uri = client.listResources().getFirst().uri();
        var result = client.readResource(uri);

        List<McpResourceContents> contents = result.contents();
        assertThat(contents.size(), is(1));

        McpResourceContents content = contents.getFirst();
        assertThat(content, instanceOf(McpTextResourceContents.class));

        McpTextResourceContents textContent = (McpTextResourceContents) content;
        assertThat(textContent.uri(), is(uri));
        assertThat(textContent.mimeType(), is(MediaTypes.TEXT_PLAIN_VALUE));
        assertThat(textContent.text(), is("Event: { name: Frank-birthday, date: 2021-04-20, attendees: [Frank] }\n"));
    }

    @Test
    @Order(8)
    void testResourceTemplateList() {
        List<McpResourceTemplate> templates = client.listResourceTemplates();
        assertThat(templates.size(), is(1));

        McpResourceTemplate template = templates.getFirst();
        assertThat(template.uriTemplate(), containsString("{name}"));
        assertThat(template.mimeType(), is(MediaTypes.TEXT_PLAIN_VALUE));
        assertThat(template.name(), is("eventResourceTemplate"));
        assertThat(template.description(), is("List single calendar event by name"));
    }

    @Test
    @Order(9)
    void testResourceTemplateCall() {
        var result = client.readResource("file://events/Frank-birthday");
        var contents = result.contents();
        assertThat(contents.size(), is(1));

        McpResourceContents content = contents.getFirst();
        assertThat(content, instanceOf(McpTextResourceContents.class));

        McpTextResourceContents text = (McpTextResourceContents) content;
        assertThat(text.uri(), is("file://events/Frank-birthday"));
        assertThat(text.mimeType(), is(MediaTypes.TEXT_PLAIN_VALUE));
        assertThat(text.text(), is("Event: { name: Frank-birthday, date: 2021-04-20, attendees: [Frank] }"));
    }

    private int sortArguments(McpPromptArgument first, McpPromptArgument second) {
        return first.name().compareTo(second.name());
    }
}
