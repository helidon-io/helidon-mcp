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
package io.helidon.extensions.mcp.server;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.json.JsonArray;
import io.helidon.json.JsonObject;
import io.helidon.json.JsonValue;
import io.helidon.json.JsonValueType;
import io.helidon.json.binding.Json;
import io.helidon.json.schema.Schema;
import io.helidon.json.schema.SchemaString;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

class McpJsonSerializerV3Test {
    private static final McpJsonSerializer MJS = McpJsonSerializer.create(McpProtocolVersion.VERSION_2025_06_18);

    @Test
    void testSerializeTool() {
        McpToolConfig config = McpToolConfig.builder()
                .name("name")
                .title("title")
                .schema("")
                .description("description")
                .outputSchema("")
                .tool(request -> null)
                .build();
        McpTool tool = new McpToolImpl(config);

        JsonObject payload = MJS.toJson(tool).build();
        assertThat(payload.stringValue("name").orElseThrow(), is("name"));
        assertThat(payload.stringValue("title").orElseThrow(), is("title"));
        assertThat(payload.stringValue("description").orElseThrow(), is("description"));
        assertThat(payload.objectValue("inputSchema").orElseThrow(), notNullValue());

        JsonObject outputSchema = payload.objectValue("outputSchema").orElseThrow();
        assertThat(outputSchema, notNullValue());
        assertThat(outputSchema.stringValue("type").orElseThrow(), is("object"));
        assertThat(outputSchema.objectValue("properties").orElseThrow(), is(JsonObject.empty()));
    }

    @Test
    void testSerializeToolOutputSchema() {
        McpToolConfig config = McpToolConfig.builder()
                .name("name")
                .title("title")
                .schema("")
                .description("description")
                .outputSchema(Schema.builder()
                                      .rootObject(root -> root.addStringProperty("foo", SchemaString.create()))
                                      .build()
                                      .generate())
                .tool(request -> null)
                .build();
        McpTool tool = new McpToolImpl(config);

        JsonObject payload = MJS.toJson(tool).build();
        assertThat(payload.stringValue("name").orElseThrow(), is("name"));
        assertThat(payload.stringValue("title").orElseThrow(), is("title"));
        assertThat(payload.stringValue("description").orElseThrow(), is("description"));
        assertThat(payload.objectValue("inputSchema").orElseThrow(), notNullValue());

        JsonObject outputSchema = payload.objectValue("outputSchema").orElseThrow();
        assertThat(outputSchema, notNullValue());
        assertThat(outputSchema.stringValue("type").orElseThrow(), is("object"));

        String foo = outputSchema.objectValue("properties")
                .flatMap(properties -> properties.objectValue("foo"))
                .flatMap(property -> property.stringValue("type"))
                .orElseThrow();
        assertThat(foo, is("string"));
    }

    @Test
    void testSerializeResource() {
        McpResourceConfig config = McpResourceConfig.builder()
                .uri("https://foo")
                .name("name")
                .title("title")
                .description("description")
                .mediaType(MediaTypes.APPLICATION_JSON)
                .resource(request -> null)
                .build();
        McpResource resource = new McpResourceImpl(config);

        JsonObject payload = MJS.toJson(resource).build();
        assertThat(payload.stringValue("name").orElseThrow(), is("name"));
        assertThat(payload.stringValue("title").orElseThrow(), is("title"));
        assertThat(payload.stringValue("uri").orElseThrow(), is("https://foo"));
        assertThat(payload.stringValue("description").orElseThrow(), is("description"));
        assertThat(payload.stringValue("mimeType").orElseThrow(), is(MediaTypes.APPLICATION_JSON.text()));
    }

    @Test
    void testSerializePrompt() {
        McpPromptConfig config = McpPromptConfig.builder()
                .name("name")
                .title("title")
                .description("description")
                .addArgument(argument -> argument.name("name")
                        .title("title")
                        .description("description")
                        .required(true))
                .prompt(request -> null)
                .build();
        McpPrompt prompt = new McpPromptImpl(config);
        JsonObject payload = MJS.toJson(prompt).build();
        assertThat(payload.stringValue("name").orElseThrow(), is("name"));
        assertThat(payload.stringValue("title").orElseThrow(), is("title"));
        assertThat(payload.stringValue("description").orElseThrow(), is("description"));

        JsonObject argument = payload.arrayValue("arguments")
                .flatMap(arguments -> arguments.get(0))
                .map(JsonValue::asObject)
                .orElseThrow();
        assertThat(argument.stringValue("name").orElseThrow(), is("name"));
        assertThat(argument.stringValue("title").orElseThrow(), is("title"));
        assertThat(argument.booleanValue("required").orElseThrow(), is(true));
        assertThat(argument.stringValue("description").orElseThrow(), is("description"));
    }

    @Test
    void testSerializePromptArgument() {
        McpPromptArgument argument = McpPromptArgument.builder()
                .name("name")
                .title("title")
                .description("description")
                .required(true)
                .build();

        JsonObject payload = MJS.toJson(argument).build();
        assertThat(payload.stringValue("name").orElseThrow(), is("name"));
        assertThat(payload.stringValue("title").orElseThrow(), is("title"));
        assertThat(payload.booleanValue("required").orElseThrow(), is(true));
        assertThat(payload.stringValue("description").orElseThrow(), is("description"));
    }

    @Test
    void testSerializeElicitationRequest() {
        long id = 1;
        McpElicitationRequest elicitationRequest = McpElicitationRequest.builder()
                .message("message")
                .schema(Schema.builder()
                                .rootObject(root -> root.addStringProperty("foo", SchemaString.create()))
                                .build()
                                .generate())
                .build();

        JsonObject request = MJS.createElicitationRequest(id, elicitationRequest);
        assertThat(request.intValue("id").orElseThrow(), is((int) id));
        assertThat(request.containsKey("params"), is(true));

        JsonObject params = request.objectValue("params").orElseThrow();
        assertThat(params.stringValue("message").orElseThrow(), is("message"));

        JsonValue schema = params.value("requestedSchema").orElseThrow();
        assertThat(schema.type(), is(JsonValueType.OBJECT));
    }

    @Test
    void testSerializeElicitationResponse() {
        JsonObject params = JsonObject.builder()
                .set("action", "accept")
                .set("content", JsonObject.empty())
                .build();
        JsonObject elicitation = MJS.createJsonRpcResultResponse(1, params);

        McpElicitationResponse response = MJS.createElicitationResponse(elicitation);
        assertThat(response.action(), is(McpElicitationAction.ACCEPT));
        assertThat(response.content().isEmpty(), is(false));
        assertThat(response.content().map(McpParameters::isPresent).orElse(false), is(true));
    }

    @Test
    void testStructuredContent() {
        McpToolResult result = McpToolResult.builder()
                .structuredContent(new StructuredContent("bar"))
                .build();
        McpToolConfig config = McpToolConfig.builder()
                .schema("")
                .name("name")
                .description("description")
                .tool((request) -> null)
                .build();
        McpTool tool = new McpToolImpl(config);

        JsonObject object = MJS.toolCall(tool, result);
        assertThat(object, is(notNullValue()));
        assertThat(object.value("content").orElse(null), is(notNullValue()));
        assertThat(object.value("structuredContent").orElse(null), is(notNullValue()));

        JsonArray array = object.arrayValue("content").orElseThrow();
        assertThat(array, is(notNullValue()));
        assertThat(array.size(), is(1));

        String content = array.get(0)
                .map(JsonValue::asObject)
                .flatMap(value -> value.stringValue("text"))
                .orElseThrow();
        assertThat(content, is("{\"foo\":\"bar\"}"));

        JsonObject structuredContent = object.objectValue("structuredContent").orElseThrow();
        assertThat(structuredContent.stringValue("foo").orElseThrow(), is("bar"));
    }

    @Test
    void testStructuredContentWithContent() {
        McpToolResult result = McpToolResult.builder()
                .addTextContent("foo")
                .structuredContent(new StructuredContent("bar"))
                .build();
        McpToolConfig config = McpToolConfig.builder()
                .schema("")
                .name("name")
                .description("description")
                .tool((request) -> null)
                .build();
        McpTool tool = new McpToolImpl(config);

        JsonObject object = MJS.toolCall(tool, result);
        assertThat(object, is(notNullValue()));
        assertThat(object.value("content").orElse(null), is(notNullValue()));
        assertThat(object.value("structuredContent").orElse(null), is(notNullValue()));

        JsonArray array = object.arrayValue("content").orElseThrow();
        assertThat(array, is(notNullValue()));
        assertThat(array.size(), is(1));

        String content = array.get(0)
                .map(JsonValue::asObject)
                .flatMap(value -> value.stringValue("text"))
                .orElseThrow();
        assertThat(content, is("foo"));

        JsonObject structuredContent = object.objectValue("structuredContent").orElseThrow();
        assertThat(structuredContent.stringValue("foo").orElseThrow(), is("bar"));
    }

    @Test
    void testSerializeResourceLinkDefault() {
        McpToolContent link = McpToolResourceLinkContent.builder()
                .name("name")
                .uri("https://foo").build();

        JsonObject payload = MJS.toJson(link).orElseGet(JsonObject::builder).build();
        assertThat(payload.stringValue("type").orElseThrow(), is(McpContentType.RESOURCE_LINK.text()));
        assertThat(payload.stringValue("uri").orElseThrow(), is("https://foo"));
        assertThat(payload.stringValue("name").orElseThrow(), is("name"));
    }

    @Test
    void testSerializeResourceLinkCustom() {
        McpToolContent link = McpToolResourceLinkContent.builder()
                .size(10)
                .name("name")
                .title("title")
                .uri("https://foo")
                .description("description")
                .mediaType(MediaTypes.APPLICATION_JSON)
                .build();

        JsonObject payload = MJS.toJson(link).orElseGet(JsonObject::builder).build();
        assertThat(payload.longValue("size").orElseThrow(), is(10L));
        assertThat(payload.stringValue("type").orElseThrow(), is(McpContentType.RESOURCE_LINK.text()));
        assertThat(payload.stringValue("name").orElseThrow(), is("name"));
        assertThat(payload.stringValue("title").orElseThrow(), is("title"));
        assertThat(payload.stringValue("uri").orElseThrow(), is("https://foo"));
        assertThat(payload.stringValue("description").orElseThrow(), is("description"));
        assertThat(payload.stringValue("mimeType").orElseThrow(), is(MediaTypes.APPLICATION_JSON_VALUE));
    }

    @Test
    void testSerializeToolCallDefault() {
        McpToolResult result = McpToolResult.builder()
                .addResourceLinkContent("name", "https://foo")
                .build();
        McpToolConfig config = McpToolConfig.builder()
                .schema("")
                .name("name")
                .description("description")
                .tool((request) -> null)
                .build();
        McpTool tool = new McpToolImpl(config);
        JsonObject payload = MJS.toolCall(tool, result);

        JsonArray content = payload.arrayValue("content").orElseThrow();
        assertThat(content.size(), is(1));

        JsonObject resourceLink = content.get(0)
                .map(JsonValue::asObject)
                .orElseThrow();
        assertThat(resourceLink.stringValue("name").orElseThrow(), is("name"));
        assertThat(resourceLink.stringValue("uri").orElseThrow(), is("https://foo"));
        assertThat(resourceLink.stringValue("type").orElseThrow(), is("resource_link"));
    }

    @Json.Entity
    public record StructuredContent(String foo) {
    }
}
