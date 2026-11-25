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

import java.util.List;
import java.util.Map;

import io.helidon.common.GenericType;
import io.helidon.extensions.mcp.server.McpServerFeature;
import io.helidon.extensions.mcp.server.McpToolResult;
import io.helidon.json.JsonObject;
import io.helidon.json.JsonParser;
import io.helidon.json.JsonValue;
import io.helidon.webclient.jsonrpc.JsonRpcClient;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import io.modelcontextprotocol.json.schema.JsonSchemaValidator;
import io.modelcontextprotocol.json.schema.jackson3.DefaultJsonSchemaValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;

@ServerTest
class McpOptionalInputsTest {
    private static final JsonSchemaValidator SCHEMA_VALIDATOR = new DefaultJsonSchemaValidator();

    private final int port;

    McpOptionalInputsTest(WebServer server) {
        port = server.port();
    }

    @SetUpRoute
    static void routing(HttpRouting.Builder routing) {
        McpOptionalInputsServer delegate = new McpOptionalInputsServer();
        routing.addFeature(McpServerFeature.builder()
                                   .path("/imperative-optional-inputs")
                                   .stateless(true)
                                   .addTool(tool -> tool.name("update")
                                           .description("Update a note with optional values")
                                           .schema("""
                                                   {"type":"object","properties":{"id":{"type":"string"},
                                                   "title":{"anyOf":[{"type":"string"},{"type":"null"}]},
                                                   "limit":{"anyOf":[{"type":"integer"},{"type":"null"}]},
                                                   "enabled":{"anyOf":[{"type":"boolean"},{"type":"null"}]}},
                                                   "required":["id"]}
                                                   """)
                                           .tool(request -> McpToolResult.create(delegate.update(
                                                   request.arguments().get("id").asString().orElseThrow(),
                                                   request.arguments().get("title").asString().asOptional(),
                                                   request.arguments().get("limit").asInteger().asOptional(),
                                                   request.arguments().get("enabled").asBoolean().asOptional()))))
                                   .addTool(tool -> tool.name("collection")
                                           .description("Read an optional collection")
                                           .schema("""
                                                   {"type":"object","properties":{"values":{
                                                   "anyOf":[{"type":"array","items":{"type":"string"}},
                                                   {"type":"null"}]}}}
                                                   """)
                                           .tool(request -> McpToolResult.create(delegate.collection(
                                                   request.arguments().get("values")
                                                           .as(new GenericType<List<String>>() { }).asOptional()))))
                                   .addTool(tool -> tool.name("object")
                                           .description("Read an optional object")
                                           .schema("""
                                                   {"type":"object","properties":{"note":{"anyOf":[{"type":"object",
                                                   "properties":{"title":{"type":"string"}}},{"type":"null"}]}}}
                                                   """)
                                           .tool(request -> McpToolResult.create(delegate.object(
                                                   request.arguments().get("note")
                                                           .as(McpOptionalInputsServer.Note.class).asOptional())))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/optional-inputs", "/imperative-optional-inputs"})
    void usesDefaultsForMissingOptionalArguments(String path) {
        JsonObject input = JsonObject.builder().set("id", "1").build();

        JsonObject result = call(path, "update", input);

        assertAll(() -> assertThat(result.booleanValue("isError").orElse(false), is(false)),
                  () -> assertThat(text(result), is("1|keep|20|true")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/optional-inputs", "/imperative-optional-inputs"})
    void preservesExplicitEmptyZeroAndFalse(String path) {
        JsonObject input = JsonParser.create("""
                {"id":"1","title":"","limit":0,"enabled":false}
                """).readJsonObject();

        JsonObject result = call(path, "update", input);

        assertAll(() -> assertThat(result.booleanValue("isError").orElse(false), is(false)),
                  () -> assertThat(text(result), is("1|set[]|0|false")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/optional-inputs", "/imperative-optional-inputs"})
    void bindsSuppliedOptionalArguments(String path) {
        JsonObject input = JsonParser.create("""
                {"id":"1","title":"new","limit":5,"enabled":true}
                """).readJsonObject();

        JsonObject result = call(path, "update", input);

        assertAll(() -> assertThat(result.booleanValue("isError").orElse(false), is(false)),
                  () -> assertThat(text(result), is("1|set[new]|5|true")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/optional-inputs", "/imperative-optional-inputs"})
    void usesDefaultsForNullOptionalArguments(String path) {
        JsonObject input = JsonParser.create("""
                {"id":"1","title":null,"limit":null,"enabled":null}
                """).readJsonObject();

        JsonObject result = call(path, "update", input);

        assertAll(() -> assertThat(result.booleanValue("isError").orElse(false), is(false)),
                  () -> assertThat(text(result), is("1|keep|20|true")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/optional-inputs", "/imperative-optional-inputs"})
    void bindsMissingOptionalCollection(String path) {
        JsonObject input = JsonObject.empty();

        JsonObject result = call(path, "collection", input);

        assertAll(() -> assertThat(result.booleanValue("isError").orElse(false), is(false)),
                  () -> assertThat(text(result), is("missing")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/optional-inputs", "/imperative-optional-inputs"})
    void preservesEmptyOptionalCollection(String path) {
        JsonObject input = JsonParser.create("""
                {"values":[]}
                """).readJsonObject();

        JsonObject result = call(path, "collection", input);

        assertAll(() -> assertThat(result.booleanValue("isError").orElse(false), is(false)),
                  () -> assertThat(text(result), is("[]")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/optional-inputs", "/imperative-optional-inputs"})
    void bindsSuppliedOptionalCollection(String path) {
        JsonObject input = JsonParser.create("""
                {"values":["a","b"]}
                """).readJsonObject();

        JsonObject result = call(path, "collection", input);

        assertAll(() -> assertThat(result.booleanValue("isError").orElse(false), is(false)),
                  () -> assertThat(text(result), is("[a, b]")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/optional-inputs", "/imperative-optional-inputs"})
    void bindsNullOptionalCollection(String path) {
        JsonObject input = JsonParser.create("""
                {"values":null}
                """).readJsonObject();

        JsonObject result = call(path, "collection", input);

        assertAll(() -> assertThat(result.booleanValue("isError").orElse(false), is(false)),
                  () -> assertThat(text(result), is("missing")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/optional-inputs", "/imperative-optional-inputs"})
    void bindsMissingOptionalObject(String path) {
        JsonObject input = JsonObject.empty();

        JsonObject result = call(path, "object", input);

        assertAll(() -> assertThat(result.booleanValue("isError").orElse(false), is(false)),
                  () -> assertThat(text(result), is("missing")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/optional-inputs", "/imperative-optional-inputs"})
    void bindsSuppliedOptionalObject(String path) {
        JsonObject input = JsonParser.create("""
                {"note":{"title":"new"}}
                """).readJsonObject();

        JsonObject result = call(path, "object", input);

        assertAll(() -> assertThat(result.booleanValue("isError").orElse(false), is(false)),
                  () -> assertThat(text(result), is("new")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/optional-inputs", "/imperative-optional-inputs"})
    void bindsNullOptionalObject(String path) {
        JsonObject input = JsonParser.create("""
                {"note":null}
                """).readJsonObject();

        JsonObject result = call(path, "object", input);

        assertAll(() -> assertThat(result.booleanValue("isError").orElse(false), is(false)),
                  () -> assertThat(text(result), is("missing")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/optional-inputs", "/imperative-optional-inputs"})
    void handlesOmittedArgumentsObject(String path) {
        JsonRpcClient client = client(path);

        try (var response = client.rpcMethod("tools/call").rpcId(1).param("name", "collection").submit()) {
            JsonObject result = response.result().orElseThrow().asJsonObject();

            assertThat(text(result), is("missing"));
        }
    }

    @Test
    void preservesLegacyFallbacks() {
        JsonObject result = call("/optional-inputs", "legacy", JsonObject.empty());

        assertThat(text(result), is("[]|false|null|[]|null"));
    }

    @Test
    void bindsMissingOptionalNumbers() {
        JsonObject result = call("/optional-inputs", "numbers", JsonObject.empty());

        assertThat(text(result), is("Optional.empty|Optional.empty|Optional.empty|Optional.empty|Optional.empty"));
    }

    @Test
    void bindsSuppliedOptionalNumbers() {
        JsonObject input = JsonParser.create("""
                {"small":1,"medium":2,"large":3,"decimal":4.5,"precise":6.5}
                """).readJsonObject();

        JsonObject result = call("/optional-inputs", "numbers", input);

        assertThat(text(result), is("Optional[1]|Optional[2]|Optional[3]|Optional[4.5]|Optional[6.5]"));
    }

    @Test
    void generatesSchemasForWrappedTypes() {
        JsonObject expected = JsonParser.create("""
                {"type":"object","properties":{"id":{"type":"string"},
                "title":{"description":"New title","anyOf":[{"type":"string"},{"type":"null"}]},
                "limit":{"anyOf":[{"type":"integer"},{"type":"null"}]},
                "enabled":{"anyOf":[{"type":"boolean"},{"type":"null"}]}},"required":["id"]}
                """).readJsonObject();

        JsonObject schema = schema("update");

        assertThat(schema, is(expected));
    }

    @Test
    void generatesSchemaForOptionalCollection() {
        JsonObject expected = JsonParser.create("""
                {"type":"object","properties":{"values":{"anyOf":[
                {"type":"array","items":{"type":"string"}},{"type":"null"}]}}}
                """).readJsonObject();

        JsonObject schema = schema("collection");

        assertThat(schema, is(expected));
    }

    @Test
    void generatesSchemaForOptionalObject() {
        JsonObject schema = schema("object");

        JsonObject note = schema.objectValue("properties").orElseThrow().objectValue("note").orElseThrow()
                .arrayValue("anyOf").orElseThrow().get(0).orElseThrow().asObject();

        assertAll(() -> assertThat(note.stringValue("type").orElseThrow(), is("object")),
                  () -> assertThat(note.objectValue("properties").orElseThrow().objectValue("title").orElseThrow()
                                           .stringValue("type").orElseThrow(), is("string")),
                  () -> assertThat(schema.containsKey("required"), is(false)));
    }

    @Test
    void preservesReservedArgumentNames() {
        JsonObject input = JsonParser.create("""
                {"request":"client request","delegate":"client delegate","argument0":"client argument"}
                """).readJsonObject();

        JsonObject result = call("/optional-inputs", "reservedNames", input);

        assertThat(text(result), is("client request|client delegate|client argument|reservedNames"));
    }

    @Test
    void bindsMissingArgumentsWithReservedNames() {
        JsonObject input = JsonObject.empty();

        JsonObject result = call("/optional-inputs", "reservedNames", input);

        assertThat(text(result), is("missing|missing||reservedNames"));
    }

    @Test
    void preservesReservedNamesInSchema() {
        JsonObject schema = schema("reservedNames");

        var names = schema.objectValue("properties").orElseThrow().keysAsStrings();

        assertThat(names, containsInAnyOrder("request", "delegate", "argument0"));
    }

    @Test
    void bindsOptionalObjectList() {
        JsonObject input = JsonParser.create("""
                {"notes":[{"title":"first"},{"title":"second"}]}
                """).readJsonObject();

        JsonObject result = call("/optional-inputs", "notes", input);

        assertThat(text(result), is("first,second"));
    }

    @Test
    void bindsEmptyOptionalObjectList() {
        JsonObject input = JsonParser.create("""
                {"notes":[]}
                """).readJsonObject();

        JsonObject result = call("/optional-inputs", "notes", input);

        assertThat(text(result), is(""));
    }

    @Test
    void bindsMissingOptionalObjectList() {
        JsonObject input = JsonObject.empty();

        JsonObject result = call("/optional-inputs", "notes", input);

        assertThat(text(result), is("missing"));
    }

    @Test
    void bindsNullOptionalObjectList() {
        JsonObject input = JsonParser.create("""
                {"notes":null}
                """).readJsonObject();

        JsonObject result = call("/optional-inputs", "notes", input);

        assertThat(text(result), is("missing"));
    }

    @Test
    void acceptsOmittedOptionalArgumentsInSchema() {
        String arguments = """
                {"id":"1"}
                """;

        var validation = validate("update", arguments);

        assertThat(validation.errorMessage(), validation.valid(), is(true));
    }

    @Test
    void acceptsNullOptionalArgumentsInSchema() {
        String arguments = """
                {"id":"1","title":null,"limit":null,"enabled":null}
                """;

        var validation = validate("update", arguments);

        assertThat(validation.errorMessage(), validation.valid(), is(true));
    }

    @Test
    void rejectsWrongOptionalArgumentTypeInSchema() {
        String arguments = """
                {"id":"1","title":42}
                """;

        var validation = validate("update", arguments);

        assertThat(validation.valid(), is(false));
    }

    @Test
    void acceptsNullOptionalCollectionInSchema() {
        String arguments = """
                {"values":null}
                """;

        var validation = validate("collection", arguments);

        assertThat(validation.errorMessage(), validation.valid(), is(true));
    }

    @Test
    void acceptsNullOptionalObjectInSchema() {
        String arguments = """
                {"note":null}
                """;

        var validation = validate("object", arguments);

        assertThat(validation.errorMessage(), validation.valid(), is(true));
    }

    @Test
    void acceptsOptionalObjectListInSchema() {
        String arguments = """
                {"notes":[{"title":"first"},{"title":"second"}]}
                """;

        var validation = validate("notes", arguments);

        assertThat(validation.errorMessage(), validation.valid(), is(true));
    }

    @Test
    void acceptsNullOptionalObjectListInSchema() {
        String arguments = """
                {"notes":null}
                """;

        var validation = validate("notes", arguments);

        assertThat(validation.errorMessage(), validation.valid(), is(true));
    }

    @Test
    void rejectsWrongOptionalObjectListItemInSchema() {
        String arguments = """
                {"notes":["not an object"]}
                """;

        var validation = validate("notes", arguments);

        assertThat(validation.valid(), is(false));
    }

    @Test
    void acceptsNullRequiredOptionalArgumentInSchema() {
        String arguments = """
                {"title":null}
                """;

        var validation = validate("requiredOptional", arguments);

        assertThat(validation.errorMessage(), validation.valid(), is(true));
    }

    @Test
    void rejectsMissingRequiredOptionalArgumentInSchema() {
        String arguments = "{}";

        var validation = validate("requiredOptional", arguments);

        assertThat(validation.valid(), is(false));
    }

    @Test
    void preservesNonOptionalSchemaTypes() {
        String arguments = """
                {"title":null}
                """;

        var validation = validate("legacy", arguments);

        assertThat(validation.valid(), is(false));
    }

    @Test
    void preservesSpecialCharactersInOptionalParameterDescription() {
        JsonObject properties = schema("describedTitle").objectValue("properties").orElseThrow();

        String description = properties.objectValue("title").orElseThrow().stringValue("description").orElseThrow();

        assertThat(description, is("Use \"auto\" for C:\\notes.\nOtherwise supply a title."));
    }

    @Test
    void generatesSchemasForExternalNoteAndListItems() {
        JsonObject properties = schema("externalNotes").objectValue("properties").orElseThrow();

        JsonObject note = properties.objectValue("note").orElseThrow()
                .arrayValue("anyOf").orElseThrow().get(0).orElseThrow().asObject();
        JsonObject list = properties.objectValue("notes").orElseThrow()
                .arrayValue("anyOf").orElseThrow().get(0).orElseThrow().asObject();

        assertAll(() -> assertThat(note.objectValue("properties").orElseThrow().objectValue("title").orElseThrow()
                                           .stringValue("type").orElseThrow(), is("string")),
                  () -> assertThat(list.stringValue("type").orElseThrow(), is("array")),
                  () -> assertThat(list.objectValue("items").orElseThrow(), is(note)));
    }

    @Test
    void bindsOptionalExternalNoteAndListItems() {
        JsonObject input = JsonParser.create("""
                {"note":{"title":"first"},"notes":[{"title":"second"},{"title":"third"}]}
                """).readJsonObject();

        JsonObject result = call("/optional-inputs", "externalNotes", input);

        assertThat(text(result), is("first|second,third"));
    }

    private JsonSchemaValidator.ValidationResponse validate(String tool, String arguments) {
        Map<String, Object> inputSchema = JsonMapper.shared()
                .readValue(schema(tool).toString(), new TypeReference<Map<String, Object>>() { });
        return SCHEMA_VALIDATOR.validate(inputSchema, arguments);
    }

    private JsonObject call(String path, String tool, JsonObject arguments) {
        try (var response = client(path).rpcMethod("tools/call")
                .rpcId(1)
                .param("name", tool)
                .param("arguments", arguments)
                .submit()) {
            return response.result().orElseThrow().asJsonObject();
        }
    }

    private JsonObject schema(String toolName) {
        try (var response = client("/optional-inputs").rpcMethod("tools/list").rpcId(1).submit()) {
            return response.result().orElseThrow().asJsonObject().arrayValue("tools").orElseThrow().values().stream()
                    .map(JsonValue::asObject)
                    .filter(tool -> tool.stringValue("name").orElseThrow().equals(toolName))
                    .findFirst().orElseThrow()
                    .objectValue("inputSchema").orElseThrow();
        }
    }

    private JsonRpcClient client(String path) {
        return JsonRpcClient.create(config -> config.baseUri("http://localhost:" + port + path));
    }

    private static String text(JsonObject result) {
        return result.arrayValue("content").orElseThrow().get(0).orElseThrow().asObject().stringValue("text").orElseThrow();
    }
}
