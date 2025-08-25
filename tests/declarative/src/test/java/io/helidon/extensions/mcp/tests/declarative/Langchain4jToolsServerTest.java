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

package io.helidon.extensions.mcp.tests.declarative;

import io.helidon.webserver.WebServer;
import io.helidon.webserver.testing.junit5.ServerTest;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static io.helidon.extensions.mcp.tests.declarative.McpToolsServer.TOOL_CONTENT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

@ServerTest
class Langchain4jToolsServerTest {
    private static McpClient client;

    Langchain4jToolsServerTest(WebServer server) {
        McpTransport transport = new HttpMcpTransport.Builder()
                .sseUrl("http://localhost:" + server.port() + "/tools")
                .logRequests(true)
                .logResponses(true)
                .build();
        client = new DefaultMcpClient.Builder()
                .transport(transport)
                .build();
    }

    @AfterAll
    static void afterAll() throws Exception {
        if (client != null) {
            client.close();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "tool", "tool2"
    })
    void runToolWithArgumentsTest(String name) {
        String arguments = """
                {
                    "value": "value1",
                    "foo": {
                        "foo": "foo1",
                        "bar": 1
                    }
                }
                """;
        var result = client.executeTool(ToolExecutionRequest.builder()
                                                .name(name)
                                                .arguments(arguments)
                                                .build());
        assertThat(result, containsString("value=value1"));
        assertThat(result, containsString("foo=foo1"));
        assertThat(result, containsString("bar=1"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "tool1", "tool3", "tool10", "tool11"
    })
    void runToolTest(String name) {
        var result = client.executeTool(ToolExecutionRequest.builder()
                                                .name(name)
                                                .build());
        assertThat(result, is(TOOL_CONTENT));
    }

    @Test
    void testToolByte() {
        var result = client.executeTool(ToolExecutionRequest.builder()
                                                .name("tool4")
                                                .arguments("{ \"aByte\": 0}")
                                                .build());
        assertThat(result, is("0"));
    }

    @Test
    void testToolShort() {
        var result = client.executeTool(ToolExecutionRequest.builder()
                                                .name("tool5")
                                                .arguments("{ \"aShort\": 0}")
                                                .build());
        assertThat(result, is("0"));
    }

    @Test
    void testToolInteger() {
        var result = client.executeTool(ToolExecutionRequest.builder()
                                                .name("tool6")
                                                .arguments("{ \"aInteger\": 0}")
                                                .build());
        assertThat(result, is("0"));
    }

    @Test
    void testToolLong() {
        var result = client.executeTool(ToolExecutionRequest.builder()
                                                .name("tool7")
                                                .arguments("{ \"aLong\": 0}")
                                                .build());
        assertThat(result, is("0"));
    }

    @Test
    void testToolDouble() {
        var result = client.executeTool(ToolExecutionRequest.builder()
                                                .name("tool8")
                                                .arguments("{ \"aDouble\": 0.0}")
                                                .build());
        assertThat(result, is("0.0"));
    }

    @Test
    void testToolFloat() {
        var result = client.executeTool(ToolExecutionRequest.builder()
                                                .name("tool9")
                                                .arguments("{ \"aFloat\": 0.0}")
                                                .build());
        assertThat(result, is("0.0"));
    }
}
