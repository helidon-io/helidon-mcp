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

import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

abstract class AbstractMcpSdkMcpExceptionTest extends AbstractMcpSdkTest {

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
            var request = McpSchema.CallToolRequest.builder()
                    .name(name)
                    .arguments(Map.of())
                    .build();
            var result = client().callTool(request);
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Tool error message"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "error-prompt",
            "error-prompt-switch-transport"
    })
    void testErrorPrompt(String name) {
        try {
            var request = new McpSchema.GetPromptRequest(name, Map.of());
            var result = client().getPrompt(request);
            assertThat("Prompt execution must throw an exception", true, is(false));
        } catch (McpError e) {
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
            var request = new McpSchema.ReadResourceRequest(uri);
            client().readResource(request);
        } catch (McpError e) {
            assertThat(e.getMessage(), containsString("Resource error message"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "error-completion",
            "error-completion-switch-transport"
    })
    void testErrorCompletion(String name) {
        try {
            var reference = new McpSchema.PromptReference(name);
            var request = new McpSchema.CompleteRequest(reference, null);
            client().completeCompletion(request);
        } catch (McpError e) {
            assertThat(e.getMessage(), containsString("Completion error message"));
        }
    }
}
