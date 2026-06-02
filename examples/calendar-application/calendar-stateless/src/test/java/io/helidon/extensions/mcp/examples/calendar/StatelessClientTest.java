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

package io.helidon.extensions.mcp.examples.calendar;

import java.util.List;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.webclient.jsonrpc.JsonRpcClient;
import io.helidon.webclient.jsonrpc.JsonRpcClientResponse;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.spi.JsonProvider;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

@ServerTest
@TestMethodOrder(OrderAnnotation.class)
class StatelessClientTest {
    private static final JsonProvider JSON_PROVIDER = JsonProvider.provider();
    private final JsonRpcClient client;

    StatelessClientTest(WebServer server) {
        this.client = JsonRpcClient.create(builder -> builder.baseUri("http://localhost:" + server.port() + "/calendar"));
    }

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        Main.setUpRoute(builder);
    }

    @Test
    @Order(1)
    void testToolList() {
        try (var response = client.rpcMethod("tools/list")
                .rpcId(1)
                .submit()) {
            JsonArray tools = result(response).getJsonArray("tools");
            assertThat(tools.size(), is(2));

            JsonObject addTool = tools.getJsonObject(0);
            assertThat(addTool.getString("name"), is("add-calendar-event"));
            assertThat(addTool.getString("description"), is("Adds a new event to the calendar"));
            JsonObject addSchema = addTool.getJsonObject("inputSchema");
            assertThat(addSchema.getString("type"), is("object"));
            assertThat(addSchema.getJsonObject("properties").keySet(), hasItems("name", "date", "attendees"));

            JsonObject listTool = tools.getJsonObject(1);
            assertThat(listTool.getString("name"), is("list-calendar-events"));
            assertThat(listTool.getString("description"), is("List calendar events"));
            JsonObject listSchema = listTool.getJsonObject("inputSchema");
            assertThat(listSchema.getString("type"), is("object"));
            assertThat(listSchema.getJsonObject("properties").keySet(), hasItems("date"));
        }
    }

    @Test
    @Order(2)
    void testAddToolCall() {
        JsonObject arguments = JSON_PROVIDER.createObjectBuilder()
                .add("name", "Frank-birthday")
                .add("date", "2021-04-20")
                .add("attendees", JSON_PROVIDER.createArrayBuilder(List.of("Frank")))
                .build();

        try (var response = client.rpcMethod("tools/call")
                .rpcId(2)
                .param("name", "add-calendar-event")
                .param("arguments", arguments)
                .submit()) {
            JsonObject result = result(response);
            assertThat(result.getBoolean("isError"), is(false));
            JsonObject content = result.getJsonArray("content").getJsonObject(0);
            assertThat(content.getString("type"), is("text"));
            assertThat(content.getString("text"), is("New event added to the calendar"));
        }
    }

    @Test
    @Order(3)
    void testListToolCall() {
        JsonObject arguments = JSON_PROVIDER.createObjectBuilder()
                .add("date", "2021-04-20")
                .build();

        try (var response = client.rpcMethod("tools/call")
                .rpcId(3)
                .param("name", "list-calendar-events")
                .param("arguments", arguments)
                .submit()) {
            JsonObject result = result(response);
            assertThat(result.getBoolean("isError"), is(false));
            JsonObject content = result.getJsonArray("content").getJsonObject(0);
            assertThat(content.getString("type"), is("text"));
            assertThat(content.getString("text"), containsString("Frank-birthday"));
        }
    }

    @Test
    @Order(4)
    void testPromptList() {
        try (var response = client.rpcMethod("prompts/list")
                .rpcId(4)
                .submit()) {
            JsonArray prompts = result(response).getJsonArray("prompts");
            assertThat(prompts.size(), is(1));

            JsonObject prompt = prompts.getJsonObject(0);
            assertThat(prompt.getString("name"), is("create-event"));
            assertThat(prompt.getString("description"), is("Create a new event and add it to the calendar"));

            JsonArray arguments = prompt.getJsonArray("arguments");
            assertThat(arguments.size(), is(3));
            assertPromptArgument(arguments.getJsonObject(0), "name", "Event name");
            assertPromptArgument(arguments.getJsonObject(1), "date", "Event date in the following format YYYY-MM-DD");
            assertPromptArgument(arguments.getJsonObject(2), "attendees", "Event attendees names separated by commas");
        }
    }

    @Test
    @Order(5)
    void testPromptCall() {
        JsonObject arguments = JSON_PROVIDER.createObjectBuilder()
                .add("name", "Frank-birthday")
                .add("date", "2021-04-20")
                .add("attendees", "Frank")
                .build();

        try (var response = client.rpcMethod("prompts/get")
                .rpcId(5)
                .param("name", "create-event")
                .param("arguments", arguments)
                .submit()) {
            JsonObject prompt = result(response);
            assertThat(prompt.getString("description"), is("New event created"));
            JsonObject message = prompt.getJsonArray("messages").getJsonObject(0);
            assertThat(message.getString("role"), is("user"));
            assertThat(message.getJsonObject("content").getString("text"), is("""
                    Create a new calendar event with name Frank-birthday, on 2021-04-20 with attendees Frank. Make
                    sure all attendees are free to attend the event.
                    """));
        }
    }

    @Test
    @Order(6)
    void testResourceList() {
        try (var response = client.rpcMethod("resources/list")
                .rpcId(6)
                .submit()) {
            JsonArray resources = result(response).getJsonArray("resources");
            assertThat(resources.size(), is(1));

            JsonObject resource = resources.getJsonObject(0);
            assertThat(resource.getString("name"), is("calendar-events"));
            assertThat(resource.getString("uri"), startsWith("file://"));
            assertThat(resource.getString("mimeType"), is(MediaTypes.TEXT_PLAIN_VALUE));
            assertThat(resource.getString("description"), is("List of calendar events created"));
        }
    }

    @Test
    @Order(7)
    void testResourceCall() {
        String uri = listResourceUri();
        try (var response = client.rpcMethod("resources/read")
                .rpcId(7)
                .param("uri", uri)
                .submit()) {
            JsonObject content = result(response).getJsonArray("contents").getJsonObject(0);
            assertThat(content.getString("uri"), is(uri));
            assertThat(content.getString("mimeType"), is(MediaTypes.TEXT_PLAIN_VALUE));
            assertThat(content.getString("text"), is("Event: { name: Frank-birthday, date: 2021-04-20, attendees: [Frank] }\n"));
        }
    }

    @Test
    @Order(8)
    void testResourceTemplateList() {
        try (var response = client.rpcMethod("resources/templates/list")
                .rpcId(8)
                .submit()) {
            JsonArray templates = result(response).getJsonArray("resourceTemplates");
            assertThat(templates.size(), is(1));

            JsonObject template = templates.getJsonObject(0);
            assertThat(template.getString("uriTemplate"), containsString("{name}"));
            assertThat(template.getString("mimeType"), is(MediaTypes.TEXT_PLAIN_VALUE));
            assertThat(template.getString("name"), is("calendar-events-resource-template"));
            assertThat(template.getString("description"), is("Resource Template to find calendar events with name"));
        }
    }

    @Test
    @Order(9)
    void testResourceTemplateCall() {
        try (var response = client.rpcMethod("resources/read")
                .rpcId(9)
                .param("uri", "file://events/Frank-birthday")
                .submit()) {
            JsonObject content = result(response).getJsonArray("contents").getJsonObject(0);
            assertThat(content.getString("uri"), is("file://events/Frank-birthday"));
            assertThat(content.getString("mimeType"), is(MediaTypes.TEXT_PLAIN_VALUE));
            assertThat(content.getString("text"), is("Event: { name: Frank-birthday, date: 2021-04-20, attendees: [Frank] }"));
        }
    }

    @Test
    @Order(10)
    void testCalendarEventPromptCompletion() {
        JsonObject ref = JSON_PROVIDER.createObjectBuilder()
                .add("type", "ref/prompt")
                .add("name", "create-event")
                .build();
        assertCompletion(10, ref, "name", 1);
        assertCompletion(11, ref, "date", 3);
        assertCompletion(12, ref, "attendees", 3);
    }

    @Test
    @Order(11)
    void testCalendarEventResourceCompletion() {
        JsonObject ref = JSON_PROVIDER.createObjectBuilder()
                .add("type", "ref/resource")
                .add("uri", Calendar.EVENTS_URI_TEMPLATE)
                .build();
        JsonObject completion = complete(13, ref, "name").getJsonObject("completion");
        assertThat(completion.getBoolean("hasMore"), is(false));
        assertThat(completion.getInt("total"), is(1));
        assertThat(completion.getJsonArray("values").getString(0), is("Frank-birthday"));
    }

    private void assertCompletion(int rpcId, JsonObject ref, String argument, int total) {
        JsonObject completion = complete(rpcId, ref, argument).getJsonObject("completion");
        assertThat(completion.getBoolean("hasMore"), is(false));
        assertThat(completion.getInt("total"), is(total));
        assertThat(completion.getJsonArray("values").size(), is(total));
    }

    private JsonObject complete(int rpcId, JsonObject ref, String argument) {
        JsonObject completionArgument = JSON_PROVIDER.createObjectBuilder()
                .add("name", argument)
                .add("value", "")
                .build();

        try (var response = client.rpcMethod("completion/complete")
                .rpcId(rpcId)
                .param("ref", ref)
                .param("argument", completionArgument)
                .submit()) {
            return result(response);
        }
    }

    private String listResourceUri() {
        try (var response = client.rpcMethod("resources/list")
                .rpcId(20)
                .submit()) {
            return result(response).getJsonArray("resources").getJsonObject(0).getString("uri");
        }
    }

    private void assertPromptArgument(JsonObject argument, String name, String description) {
        assertThat(argument.getString("name"), is(name));
        assertThat(argument.getString("description"), is(description));
        assertThat(argument.getBoolean("required"), is(true));
    }

    private JsonObject result(JsonRpcClientResponse response) {
        assertThat(response.error().isEmpty(), is(true));
        assertThat(response.result().isEmpty(), is(false));
        return response.result().orElseThrow().asJsonObject();
    }
}
