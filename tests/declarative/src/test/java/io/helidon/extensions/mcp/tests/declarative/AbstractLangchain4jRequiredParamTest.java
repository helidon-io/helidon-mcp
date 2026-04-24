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
import dev.langchain4j.mcp.client.McpClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    }

    @Test
    void testMissingSingleRequiredParam() {
        ToolExecutionException exception = assertThrows(ToolExecutionException.class,
                () -> client.executeTool(ToolExecutionRequest.builder()
                                                 .name("tool21")
                                                 .arguments("{}")
                                                 .build()));
        assertThat(exception.getMessage(), containsString("Missing required parameter: mandatory"));
    }

    @Test
    void testMissingMultipleRequiredParams() {
        ToolExecutionException exception = assertThrows(ToolExecutionException.class,
                () -> client.executeTool(ToolExecutionRequest.builder()
                                                 .name("tool22")
                                                 .arguments("{}")
                                                 .build()));
        assertThat(exception.getMessage(), containsString("Missing required parameters: a, b"));
    }

    @Test
    void testRequiredParamProvided() {
        var result = client.executeTool(ToolExecutionRequest.builder()
                                                .name("tool21")
                                                .arguments("{\"mandatory\": \"x\"}")
                                                .build());
        assertThat(result.resultText(), is("mandatory=x|optional="));
    }
}
