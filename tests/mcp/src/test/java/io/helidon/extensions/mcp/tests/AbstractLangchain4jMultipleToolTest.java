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

package io.helidon.extensions.mcp.tests;

import java.util.List;

import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.McpClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/*
 * With current langchain4j version, it supports only text as tool returned content type.
 */
abstract class AbstractLangchain4jMultipleToolTest {
    protected static McpClient client;

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        MultipleTool.setUpRoute(builder);
    }

    @AfterAll
    static void closeClient() throws Exception {
        client.close();
    }

    @Test
    void testListTools() {
        List<ToolSpecification> tools = client.listTools();
        assertThat(tools.size(), is(4));

        ToolSpecification tool1 = tools.getFirst();
        assertThat(tool1.name(), is("tool1"));
        assertThat(tool1.description(), is("Tool 1"));
        assertThat(tool1.parameters().properties().isEmpty(), is(false));

        ToolSpecification tool2 = tools.get(1);
        assertThat(tool2.name(), is("tool2"));
        assertThat(tool2.description(), is("Tool 2"));
        assertThat(tool2.parameters().properties().isEmpty(), is(false));

        ToolSpecification tool3 = tools.get(2);
        assertThat(tool3.name(), is("tool3"));
        assertThat(tool3.description(), is("Tool 3"));
        assertThat(tool3.parameters().properties().isEmpty(), is(false));

        ToolSpecification tool4 = tools.get(3);
        assertThat(tool4.name(), is("tool4"));
        assertThat(tool4.description(), is("Tool 4"));
        assertThat(tool4.parameters().properties().isEmpty(), is(false));
    }

    @Test
    void testTool4() {
        String result = client.executeTool(ToolExecutionRequest.builder()
                                                   .name("tool4")
                                                   .arguments("{\"name\":\"Praha\", \"population\":10}")
                                                   .build());
        assertThat(result, is("Praha has a population of 10 inhabitants"));
    }
}
