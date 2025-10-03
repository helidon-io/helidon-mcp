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

import java.util.Map;

import io.helidon.jsonrpc.core.JsonRpcError;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.exception.LangChain4jException;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.protocol.McpGetPromptRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

abstract class AbstractLangchain4jMcpExceptionTest {
    protected McpClient client;

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        McpExceptionServer.setUpRoute(builder);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "error-tool",
            "error-tool-switch-transport"
    })
    void testErrorTool(String name) {
        try {
            var request = ToolExecutionRequest.builder()
                    .name("error-tool")
                    .arguments("")
                    .build();
            var result = client.executeTool(request);
            assertThat("Tool execution must throw an exception", true, is(false));
        } catch (LangChain4jException e) {
            assertThat(e.getMessage(), containsString("Tool error message"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "error-prompt",
            "error-prompt-switch-transport"
    })
    void testErrorPrompt(String name) {
        try {
            var result = client.getPrompt(name, Map.of());
            assertThat("Prompt execution must throw an exception", true, is(false));
        } catch (LangChain4jException e) {
            assertThat(e.getMessage(), containsString("Prompt error message"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "error-resource",
            "error-resource-switch-transport"
    })
    void testErrorResource(String uri) {
        try {
            client.readResource(uri);
        } catch (LangChain4jException e) {
            assertThat(e.getMessage(), containsString("Resource error message"));
        }
    }
}
