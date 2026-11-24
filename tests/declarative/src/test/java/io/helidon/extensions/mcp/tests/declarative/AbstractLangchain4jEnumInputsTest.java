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

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.exception.ToolExecutionException;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonEnumSchema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

abstract class AbstractLangchain4jEnumInputsTest {
    private final McpClient client;

    AbstractLangchain4jEnumInputsTest(McpTransport transport) {
        client = new DefaultMcpClient.Builder().transport(transport).build();
    }

    @AfterEach
    void closeClient() throws Exception {
        client.close();
    }

    @Test
    void discoversEnumSchema() {
        JsonEnumSchema expected = JsonEnumSchema.builder()
                .description("Helidon flavor")
                .enumValues("SE", "MP")
                .build();

        ToolSpecification tool = tool("createProject");

        assertThat(tool.parameters().properties().get("flavor"), is(expected));
    }

    @Test
    void discoversRequiredEnumParameter() {
        ToolSpecification tool = tool("requiredFlavor");

        assertThat(tool.parameters().required(), contains("flavor"));
    }

    @Test
    void discoversEnumListSchema() {
        JsonArraySchema expected = JsonArraySchema.builder()
                .description("Helidon flavors")
                .items(JsonEnumSchema.builder().enumValues("SE", "MP").build())
                .build();

        ToolSpecification tool = tool("flavors");

        assertThat(tool.parameters().properties().get("flavors"), is(expected));
    }

    @Test
    void discoversEnumArraySchema() {
        JsonArraySchema expected = JsonArraySchema.builder()
                .description("Helidon flavors")
                .items(JsonEnumSchema.builder().enumValues("SE", "MP").build())
                .build();

        ToolSpecification tool = tool("flavorArray");

        assertThat(tool.parameters().properties().get("flavors"), is(expected));
    }

    @ParameterizedTest
    @ValueSource(strings = {"SE", "MP"})
    void bindsEnumByConstantName(String flavor) {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("createProject")
                .arguments("""
                        {"projectName":"example","flavor":"%s"}
                        """.formatted(flavor))
                .build();

        var result = client.executeTool(request);

        assertAll(() -> assertThat(result.isError(), is(false)),
                  () -> assertThat(result.resultText(), is("example:" + flavor)));
    }

    @Test
    void bindsDependencyEnum() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("day")
                .arguments("""
                        {"day":"MONDAY"}
                        """)
                .build();

        var result = client.executeTool(request);

        assertThat(result.resultText(), is("MONDAY"));
    }

    @Test
    void preservesMissingEnumFallback() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("createProject")
                .arguments("""
                        {"projectName":"example"}
                        """)
                .build();

        var result = client.executeTool(request);

        assertThat(result.resultText(), is("example:unspecified"));
    }

    @Test
    void preservesNullEnumFallback() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("createProject")
                .arguments("""
                        {"projectName":"example","flavor":null}
                        """)
                .build();

        var result = client.executeTool(request);

        assertThat(result.resultText(), is("example:unspecified"));
    }

    @Test
    void bindsEnumList() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("flavors")
                .arguments("""
                        {"flavors":["MP","SE"]}
                        """)
                .build();

        var result = client.executeTool(request);

        assertThat(result.resultText(), is("MP,SE"));
    }

    @Test
    void bindsEnumArray() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("flavorArray")
                .arguments("""
                        {"flavors":["MP","SE","MP"]}
                        """)
                .build();

        var result = client.executeTool(request);

        assertAll(() -> assertThat(result.isError(), is(false)),
                  () -> assertThat(result.resultText(), is("MP,SE,MP")));
    }

    @Test
    void bindsEmptyEnumArray() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("flavorArray")
                .arguments("""
                        {"flavors":[]}
                        """)
                .build();

        var result = client.executeTool(request);

        assertThat(result.resultText(), is(""));
    }

    @Test
    void preservesMissingEnumArrayFallback() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("flavorArray")
                .build();

        var result = client.executeTool(request);

        assertThat(result.resultText(), is("unspecified"));
    }

    @Test
    void preservesNullEnumArrayFallback() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("flavorArray")
                .arguments("""
                        {"flavors":null}
                        """)
                .build();

        var result = client.executeTool(request);

        assertThat(result.resultText(), is("unspecified"));
    }

    @Test
    void rejectsUnknownEnumName() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("createProject")
                .arguments("""
                        {"projectName":"example","flavor":"UNKNOWN"}
                        """)
                .build();

        ToolExecutionException exception = assertThrows(ToolExecutionException.class, () -> client.executeTool(request));

        assertThat(exception.errorCode(), notNullValue());
    }

    @Test
    void rejectsUnknownEnumArrayValue() {
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("flavorArray")
                .arguments("""
                        {"flavors":["SE","UNKNOWN"]}
                        """)
                .build();

        ToolExecutionException exception = assertThrows(ToolExecutionException.class, () -> client.executeTool(request));

        assertThat(exception.errorCode(), notNullValue());
    }

    private ToolSpecification tool(String name) {
        return client.listTools().stream()
                .filter(tool -> tool.name().equals(name))
                .findFirst()
                .orElseThrow();
    }
}
