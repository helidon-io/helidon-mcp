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
import io.helidon.json.JsonArray;
import io.helidon.json.JsonObject;
import io.helidon.json.JsonValue;
import io.helidon.jsonrpc.core.JsonRpcResult;
import io.helidon.webclient.jsonrpc.JsonRpcClient;
import io.helidon.webclient.jsonrpc.JsonRpcClientResponse;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;

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
    private final JsonRpcClient client;

    StatelessClientTest(WebServer server) {
        this.client = JsonRpcClient.create(builder ->
                                                   builder.baseUri("http://localhost:"
                                                                           + server.port()
                                                                           + "/calendar"));
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
            JsonArray tools = result(response).arrayValue("tools").orElseThrow();
            assertThat(tools.size(), is(2));

            JsonObject addTool = tools.get(0)
                    .map(JsonValue::asObject)
                    .orElseThrow();
            assertThat(addTool.stringValue("name").orElseThrow(), is("add-calendar-event"));
            assertThat(addTool.stringValue("description").orElseThrow(), is("Adds a new event to the calendar"));
            JsonObject addSchema = addTool.objectValue("inputSchema").orElseThrow();
            assertThat(addSchema.stringValue("type").orElseThrow(), is("object"));
            var addProperties = addSchema.objectValue("properties")
                    .map(JsonObject::keysAsStrings)
                    .orElseThrow();
            assertThat(addProperties, hasItems("name", "date", "attendees"));

            JsonObject listTool = tools.get(1)
                    .map(JsonValue::asObject)
                    .orElseThrow();
            assertThat(listTool.stringValue("name").orElseThrow(), is("list-calendar-events"));
            assertThat(listTool.stringValue("description").orElseThrow(), is("List calendar events"));
            JsonObject listSchema = listTool.objectValue("inputSchema").orElseThrow();
            assertThat(listSchema.stringValue("type").orElseThrow(), is("object"));
            var listProperties = listSchema.objectValue("properties")
                    .map(JsonObject::keysAsStrings)
                    .orElseThrow();
            assertThat(listProperties, hasItems("date"));
        }
    }

    @Test
    @Order(2)
    void testAddToolCall() {
        JsonObject arguments = JsonObject.builder()
                .set("name", "Frank-birthday")
                .set("date", "2021-04-20")
                .setStrings("attendees", List.of("Frank"))
                .build();

        try (var response = client.rpcMethod("tools/call")
                .rpcId(2)
                .param("name", "add-calendar-event")
                .param("arguments", arguments)
                .submit()) {
            JsonObject result = result(response);
            assertThat(result.booleanValue("isError").orElseThrow(), is(false));
            JsonObject content = result.arrayValue("content")
                    .flatMap(array -> array.get(0))
                    .map(JsonValue::asObject)
                    .orElseThrow();
            assertThat(content.stringValue("type").orElseThrow(), is("text"));
            assertThat(content.stringValue("text").orElseThrow(), is("New event added to the calendar"));
        }
    }

    @Test
    @Order(3)
    void testListToolCall() {
        JsonObject arguments = JsonObject.builder()
                .set("date", "2021-04-20")
                .build();

        try (var response = client.rpcMethod("tools/call")
                .rpcId(3)
                .param("name", "list-calendar-events")
                .param("arguments", arguments)
                .submit()) {
            JsonObject result = result(response);
            assertThat(result.booleanValue("isError").orElseThrow(), is(false));
            JsonObject content = result.arrayValue("content")
                    .flatMap(array -> array.get(0))
                    .map(JsonValue::asObject)
                    .orElseThrow();
            assertThat(content.stringValue("type").orElseThrow(), is("text"));
            assertThat(content.stringValue("text").orElseThrow(), containsString("Frank-birthday"));
        }
    }

    @Test
    @Order(4)
    void testPromptList() {
        try (var response = client.rpcMethod("prompts/list")
                .rpcId(4)
                .submit()) {
            JsonArray prompts = result(response).arrayValue("prompts").orElseThrow();
            assertThat(prompts.size(), is(1));

            JsonObject prompt = prompts.get(0)
                    .map(JsonValue::asObject)
                    .orElseThrow();
            assertThat(prompt.stringValue("name").orElseThrow(), is("create-event"));
            assertThat(prompt.stringValue("description").orElseThrow(),
                       is("Create a new event and add it to the calendar"));

            JsonArray arguments = prompt.arrayValue("arguments").orElseThrow();
            assertThat(arguments.size(), is(3));
            JsonObject nameArgument = arguments.get(0)
                    .map(JsonValue::asObject)
                    .orElseThrow();
            assertThat(nameArgument.stringValue("name").orElseThrow(), is("name"));
            assertThat(nameArgument.stringValue("description").orElseThrow(), is("Event name"));
            assertThat(nameArgument.booleanValue("required").orElseThrow(), is(true));

            JsonObject dateArgument = arguments.get(1)
                    .map(JsonValue::asObject)
                    .orElseThrow();
            assertThat(dateArgument.stringValue("name").orElseThrow(), is("date"));
            assertThat(dateArgument.stringValue("description").orElseThrow(),
                       is("Event date in the following format YYYY-MM-DD"));
            assertThat(dateArgument.booleanValue("required").orElseThrow(), is(true));

            JsonObject attendeesArgument = arguments.get(2)
                    .map(JsonValue::asObject)
                    .orElseThrow();
            assertThat(attendeesArgument.stringValue("name").orElseThrow(), is("attendees"));
            assertThat(attendeesArgument.stringValue("description").orElseThrow(),
                       is("Event attendees names separated by commas"));
            assertThat(attendeesArgument.booleanValue("required").orElseThrow(), is(true));
        }
    }

    @Test
    @Order(5)
    void testPromptCall() {
        JsonObject arguments = JsonObject.builder()
                .set("name", "Frank-birthday")
                .set("date", "2021-04-20")
                .set("attendees", "Frank")
                .build();

        try (var response = client.rpcMethod("prompts/get")
                .rpcId(5)
                .param("name", "create-event")
                .param("arguments", arguments)
                .submit()) {
            JsonObject prompt = result(response);
            assertThat(prompt.stringValue("description").orElseThrow(), is("New event created"));
            JsonObject message = prompt.arrayValue("messages")
                    .flatMap(messages -> messages.get(0))
                    .map(JsonValue::asObject)
                    .orElseThrow();
            assertThat(message.stringValue("role").orElseThrow(), is("user"));
            String text = message.objectValue("content")
                    .flatMap(content -> content.stringValue("text"))
                    .orElseThrow();
            assertThat(text, is("""
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
            JsonArray resources = result(response).arrayValue("resources").orElseThrow();
            assertThat(resources.size(), is(1));

            JsonObject resource = resources.get(0)
                    .map(JsonValue::asObject)
                    .orElseThrow();
            assertThat(resource.stringValue("name").orElseThrow(), is("calendar-events"));
            assertThat(resource.stringValue("uri").orElseThrow(), startsWith("file://"));
            assertThat(resource.stringValue("mimeType").orElseThrow(), is(MediaTypes.TEXT_PLAIN_VALUE));
            assertThat(resource.stringValue("description").orElseThrow(), is("List of calendar events created"));
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
            JsonObject content = result(response).arrayValue("contents")
                    .flatMap(contents -> contents.get(0))
                    .map(JsonValue::asObject)
                    .orElseThrow();
            assertThat(content.stringValue("uri").orElseThrow(), is(uri));
            assertThat(content.stringValue("mimeType").orElseThrow(), is(MediaTypes.TEXT_PLAIN_VALUE));
            assertThat(content.stringValue("text").orElseThrow(),
                       is("Event: { name: Frank-birthday, date: 2021-04-20, attendees: [Frank] }\n"));
        }
    }

    @Test
    @Order(8)
    void testResourceTemplateList() {
        try (var response = client.rpcMethod("resources/templates/list")
                .rpcId(8)
                .submit()) {
            JsonArray templates = result(response).arrayValue("resourceTemplates").orElseThrow();
            assertThat(templates.size(), is(1));

            JsonObject template = templates.get(0)
                    .map(JsonValue::asObject)
                    .orElseThrow();
            assertThat(template.stringValue("uriTemplate").orElseThrow(), containsString("{name}"));
            assertThat(template.stringValue("mimeType").orElseThrow(), is(MediaTypes.TEXT_PLAIN_VALUE));
            assertThat(template.stringValue("name").orElseThrow(), is("calendar-events-resource-template"));
            assertThat(template.stringValue("description").orElseThrow(),
                       is("Resource Template to find calendar events with name"));
        }
    }

    @Test
    @Order(9)
    void testResourceTemplateCall() {
        try (var response = client.rpcMethod("resources/read")
                .rpcId(9)
                .param("uri", "file://events/Frank-birthday")
                .submit()) {
            JsonObject content = result(response).arrayValue("contents")
                    .flatMap(contents -> contents.get(0))
                    .map(JsonValue::asObject)
                    .orElseThrow();
            assertThat(content.stringValue("uri").orElseThrow(), is("file://events/Frank-birthday"));
            assertThat(content.stringValue("mimeType").orElseThrow(), is(MediaTypes.TEXT_PLAIN_VALUE));
            assertThat(content.stringValue("text").orElseThrow(),
                       is("Event: { name: Frank-birthday, date: 2021-04-20, attendees: [Frank] }"));
        }
    }

    @Test
    @Order(10)
    void testCalendarEventPromptCompletion() {
        JsonObject ref = JsonObject.builder()
                .set("type", "ref/prompt")
                .set("name", "create-event")
                .build();
        JsonObject nameCompletion = complete(10, ref, "name").objectValue("completion").orElseThrow();
        assertThat(nameCompletion.booleanValue("hasMore").orElseThrow(), is(false));
        assertThat(nameCompletion.intValue("total").orElseThrow(), is(1));
        assertThat(nameCompletion.arrayValue("values").map(JsonArray::size).orElseThrow(), is(1));

        JsonObject dateCompletion = complete(11, ref, "date").objectValue("completion").orElseThrow();
        assertThat(dateCompletion.booleanValue("hasMore").orElseThrow(), is(false));
        assertThat(dateCompletion.intValue("total").orElseThrow(), is(3));
        assertThat(dateCompletion.arrayValue("values").map(JsonArray::size).orElseThrow(), is(3));

        JsonObject attendeesCompletion = complete(12, ref, "attendees").objectValue("completion").orElseThrow();
        assertThat(attendeesCompletion.booleanValue("hasMore").orElseThrow(), is(false));
        assertThat(attendeesCompletion.intValue("total").orElseThrow(), is(3));
        assertThat(attendeesCompletion.arrayValue("values").map(JsonArray::size).orElseThrow(), is(3));
    }

    @Test
    @Order(11)
    void testCalendarEventResourceCompletion() {
        JsonObject ref = JsonObject.builder()
                .set("type", "ref/resource")
                .set("uri", Calendar.EVENTS_URI_TEMPLATE)
                .build();
        JsonObject completion = complete(13, ref, "name").objectValue("completion").orElseThrow();
        assertThat(completion.booleanValue("hasMore").orElseThrow(), is(false));
        assertThat(completion.intValue("total").orElseThrow(), is(1));
        String value = completion.arrayValue("values")
                .flatMap(values -> values.get(0))
                .map(JsonValue::asString)
                .map(jsonString -> jsonString.value())
                .orElseThrow();
        assertThat(value, is("Frank-birthday"));
    }

    private JsonObject complete(int rpcId, JsonObject ref, String argument) {
        JsonObject completionArgument = JsonObject.builder()
                .set("name", argument)
                .set("value", "")
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
            return result(response).arrayValue("resources")
                    .flatMap(resources -> resources.get(0))
                    .map(JsonValue::asObject)
                    .flatMap(resource -> resource.stringValue("uri"))
                    .orElseThrow();
        }
    }

    private JsonObject result(JsonRpcClientResponse response) {
        assertThat(response.error().isEmpty(), is(true));
        assertThat(response.result().isEmpty(), is(false));
        return response.result()
                .map(JsonRpcResult::asJsonObject)
                .orElseThrow();
    }
}
