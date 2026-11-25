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

import io.helidon.json.JsonArray;
import io.helidon.json.JsonObject;
import io.helidon.json.JsonParser;
import io.helidon.json.JsonValue;
import io.helidon.webclient.jsonrpc.JsonRpcClient;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.testing.junit5.ServerTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;

@ServerTest
class McpEnumInputsTest {
    private final JsonRpcClient client;

    McpEnumInputsTest(WebServer server) {
        client = JsonRpcClient.create(config -> config.baseUri("http://localhost:" + server.port() + "/enum-inputs"));
    }

    @Test
    void generatesSchemaForUnannotatedEnum() {
        JsonObject expected = JsonParser.create("""
                {"type":"object","properties":{
                  "projectName":{"type":"string"},
                  "flavor":{"type":"string","description":"Choose \\"SE\\" or \\"MP\\"\\tby name","enum":["SE","MP"]}
                }}
                """).readJsonObject();

        JsonObject schema = schema("createProject");

        assertThat(schema, is(expected));
    }

    @Test
    void preservesRequiredEnumParameter() {
        JsonObject expected = JsonParser.create("""
                {"type":"object","properties":{"flavor":{"type":"string","enum":["SE","MP"]}},
                 "required":["flavor"]}
                """).readJsonObject();

        JsonObject schema = schema("requiredFlavor");

        assertThat(schema, is(expected));
    }

    @Test
    void generatesSchemaForDependencyEnum() {
        JsonObject expected = JsonParser.create("""
                {"type":"object","properties":{"day":{"type":"string",
                 "enum":["MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY","SUNDAY"]}}}
                """).readJsonObject();

        JsonObject schema = schema("day");

        assertThat(schema, is(expected));
    }

    @Test
    void generatesSchemaForEnumList() {
        JsonObject expected = JsonParser.create("""
                {"type":"object","properties":{"flavors":{"type":"array","description":"Helidon flavors",
                 "items":{"type":"string","enum":["SE","MP"]}}}}
                """).readJsonObject();

        JsonObject schema = schema("flavors");

        assertThat(schema, is(expected));
    }

    @Test
    void generatesSchemaForEnumArray() {
        JsonObject expected = JsonParser.create("""
                {"type":"object","properties":{"flavors":{"type":"array","description":"Helidon flavors",
                 "items":{"type":"string","enum":["SE","MP"]}}}}
                """).readJsonObject();

        JsonObject schema = schema("flavorArray");

        assertThat(schema, is(expected));
    }

    @Test
    void preservesExplicitEnumSchema() {
        JsonObject schema = schema("deployment");

        JsonObject deployment = schema.objectValue("properties").orElseThrow().objectValue("deployment").orElseThrow();

        assertAll(() -> assertThat(deployment.stringValue("title").orElseThrow(), is("Deployment environment")),
                  () -> assertThat(deployment.stringValue("type").orElseThrow(), is("string")),
                  () -> assertThat(deployment.arrayValue("enum").orElseThrow(),
                                   is(JsonArray.createStrings(List.of("DEVELOPMENT", "PRODUCTION")))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"deploymentList", "deploymentArray"})
    void preservesExplicitEnumItemSchema(String tool) {
        JsonObject expected = schema("deployment").objectValue("properties").orElseThrow()
                .objectValue("deployment").orElseThrow();

        JsonObject items = schema(tool).objectValue("properties").orElseThrow()
                .objectValue("deployments").orElseThrow().objectValue("items").orElseThrow();

        assertThat(items, is(expected));
    }

    @Test
    void bindsSecondEnumList() {
        JsonObject input = JsonParser.create("""
                {"deployments":["PRODUCTION","DEVELOPMENT"]}
                """).readJsonObject();

        JsonObject result = call("deploymentList", input);

        assertThat(text(result), is("PRODUCTION,DEVELOPMENT"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"SE", "MP"})
    void bindsEnumByConstantName(String flavor) {
        JsonObject input = JsonObject.builder().set("projectName", "example").set("flavor", flavor).build();

        JsonObject result = call("createProject", input);

        assertAll(() -> assertThat(result.booleanValue("isError").orElse(false), is(false)),
                  () -> assertThat(text(result), is("example:" + flavor)));
    }

    @Test
    void bindsDependencyEnum() {
        JsonObject input = JsonObject.builder().set("day", "MONDAY").build();

        JsonObject result = call("day", input);

        assertThat(text(result), is("MONDAY"));
    }

    @Test
    void bindsEnumList() {
        JsonObject input = JsonParser.create("""
                {"flavors":["MP","SE"]}
                """).readJsonObject();

        JsonObject result = call("flavors", input);

        assertThat(text(result), is("MP,SE"));
    }

    @Test
    void bindsEmptyEnumList() {
        JsonObject input = JsonParser.create("""
                {"flavors":[]}
                """).readJsonObject();

        JsonObject result = call("flavors", input);

        assertThat(text(result), is(""));
    }

    @Test
    void preservesMissingEnumListFallback() {
        JsonObject input = JsonObject.empty();

        JsonObject result = call("flavors", input);

        assertThat(text(result), is(""));
    }

    @Test
    void bindsEnumArray() {
        JsonObject input = JsonParser.create("""
                {"flavors":["MP","SE","MP"]}
                """).readJsonObject();

        JsonObject result = call("flavorArray", input);

        assertAll(() -> assertThat(result.booleanValue("isError").orElse(false), is(false)),
                  () -> assertThat(text(result), is("MP,SE,MP")));
    }

    @Test
    void bindsEmptyEnumArray() {
        JsonObject input = JsonParser.create("""
                {"flavors":[]}
                """).readJsonObject();

        JsonObject result = call("flavorArray", input);

        assertThat(text(result), is(""));
    }

    @Test
    void preservesMissingEnumArrayFallback() {
        JsonObject input = JsonObject.empty();

        JsonObject result = call("flavorArray", input);

        assertThat(text(result), is("unspecified"));
    }

    @Test
    void rejectsNullEnumArray() {
        JsonObject input = JsonParser.create("""
                {"flavors":null}
                """).readJsonObject();

        JsonObject result = call("flavorArray", input);

        assertToolError(result, "Invalid argument 'flavors': Expected an array of HelidonFlavor constant names");
    }

    @Test
    void rejectsUnknownEnumArrayValue() {
        JsonObject input = JsonParser.create("""
                {"flavors":["SE","UNKNOWN"]}
                """).readJsonObject();

        JsonObject result = call("flavorArray", input);

        assertToolError(result, "Invalid argument 'flavors': Unknown HelidonFlavor constant: UNKNOWN");
    }

    @Test
    void preservesMissingEnumFallback() {
        JsonObject input = JsonObject.builder().set("projectName", "example").build();

        JsonObject result = call("createProject", input);

        assertThat(text(result), is("example:unspecified"));
    }

    @Test
    void rejectsNullEnum() {
        JsonObject input = JsonParser.create("""
                {"projectName":"example","flavor":null}
                """).readJsonObject();

        JsonObject result = call("createProject", input);

        assertToolError(result, "Invalid argument 'flavor': Expected a string naming a HelidonFlavor constant");
    }

    @Test
    void rejectsUnknownEnumName() {
        JsonObject input = JsonObject.builder().set("projectName", "example").set("flavor", "UNKNOWN").build();

        JsonObject result = call("createProject", input);

        assertToolError(result, "Invalid argument 'flavor': Unknown HelidonFlavor constant: UNKNOWN");
    }

    @Test
    void rejectsNullEnumList() {
        JsonObject input = JsonParser.create("""
                {"flavors":null}
                """).readJsonObject();

        JsonObject result = call("flavors", input);

        assertToolError(result, "Invalid argument 'flavors': Expected an array of HelidonFlavor constant names");
    }

    @ParameterizedTest
    @ValueSource(strings = {"flavors", "flavorArray"})
    void rejectsNullEnumItem(String tool) {
        JsonObject input = JsonParser.create("""
                {"flavors":["SE",null]}
                """).readJsonObject();

        JsonObject result = call(tool, input);

        assertToolError(result, "Invalid argument 'flavors': Expected a string naming a HelidonFlavor constant");
    }

    @Test
    void rejectsMissingRequiredEnum() {
        JsonObject input = JsonObject.empty();

        JsonObject result = call("requiredFlavor", input);

        assertToolError(result, "Invalid argument 'flavor': Value is required");
    }

    private JsonObject call(String tool, JsonObject arguments) {
        try (var response = client.rpcMethod("tools/call")
                .rpcId(1)
                .param("name", tool)
                .param("arguments", arguments)
                .submit()) {
            return response.result().orElseThrow().asJsonObject();
        }
    }

    private JsonObject schema(String toolName) {
        try (var response = client.rpcMethod("tools/list").rpcId(1).submit()) {
            return response.result().orElseThrow().asJsonObject().arrayValue("tools").orElseThrow().values().stream()
                    .map(JsonValue::asObject)
                    .filter(tool -> tool.stringValue("name").orElseThrow().equals(toolName))
                    .findFirst().orElseThrow()
                    .objectValue("inputSchema").orElseThrow();
        }
    }

    private static void assertToolError(JsonObject result, String message) {
        assertAll(() -> assertThat(result.booleanValue("isError").orElseThrow(), is(true)),
                  () -> assertThat(text(result), is(message)));
    }

    private static String text(JsonObject result) {
        return result.arrayValue("content").orElseThrow().get(0).orElseThrow().asObject().stringValue("text").orElseThrow();
    }
}
