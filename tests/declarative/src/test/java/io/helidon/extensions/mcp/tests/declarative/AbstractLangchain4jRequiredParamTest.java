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
import dev.langchain4j.mcp.client.McpClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

abstract class AbstractLangchain4jRequiredParamTest {
    protected static McpClient client;

    @AfterAll
    static void afterAll() throws Exception {
        if (client != null) {
            client.close();
        }
    }

    @Test
    void testRequiredArrayInSchema() {
        var tools = client.listTools();
        ToolSpecification tool21 = tools.stream()
                .filter(tool -> "tool21".equals(tool.name()))
                .findFirst()
                .orElseThrow();
        assertThat(tool21.parameters().required(), contains("mandatory"));

        ToolSpecification tool22 = tools.stream()
                .filter(tool -> "tool22".equals(tool.name()))
                .findFirst()
                .orElseThrow();
        assertThat(tool22.parameters().required(), contains("a", "b"));

        ToolSpecification tool23 = tools.stream()
                .filter(tool -> "tool23".equals(tool.name()))
                .findFirst()
                .orElseThrow();
        assertThat(tool23.parameters().required(), contains("foo"));

        ToolSpecification tool24 = tools.stream()
                .filter(tool -> "tool24".equals(tool.name()))
                .findFirst()
                .orElseThrow();
        assertThat(tool24.parameters().required(), contains("count"));

        ToolSpecification tool25 = tools.stream()
                .filter(tool -> "tool25".equals(tool.name()))
                .findFirst()
                .orElseThrow();
        assertThat(tool25.parameters().required(), contains("values"));
    }

    @Test
    void testRequiredStringParamProvided() {
        var result = client.executeTool(ToolExecutionRequest.builder()
                                                .name("tool21")
                                                .arguments("{\"mandatory\": \"x\"}")
                                                .build());
        assertThat(result.resultText(), is("mandatory=x|optional="));
    }

    @Test
    void testRequiredStringAndIntegerParamsProvided() {
        var result = client.executeTool(ToolExecutionRequest.builder()
                                                .name("tool22")
                                                .arguments("{\"a\": \"x\", \"b\": 42}")
                                                .build());
        assertThat(result.resultText(), is("a=x|b=42"));
    }

    @Test
    void testRequiredClassParamProvided() {
        var result = client.executeTool(ToolExecutionRequest.builder()
                                                .name("tool23")
                                                .arguments("{\"foo\": {\"foo\": \"hello\", \"bar\": 42}}")
                                                .build());
        assertThat(result.resultText(), is("foo=hello|bar=42"));
    }

    @Test
    void testRequiredIntParamProvided() {
        var result = client.executeTool(ToolExecutionRequest.builder()
                                                .name("tool24")
                                                .arguments("{\"count\": 42}")
                                                .build());
        assertThat(result.resultText(), is("count=42"));
    }

    @Test
    void testRequiredListParamProvided() {
        var result = client.executeTool(ToolExecutionRequest.builder()
                                                .name("tool25")
                                                .arguments("{\"values\": [\"a\", \"b\", \"c\"]}")
                                                .build());
        assertThat(result.resultText(), is("values=a,b,c"));
    }
}
