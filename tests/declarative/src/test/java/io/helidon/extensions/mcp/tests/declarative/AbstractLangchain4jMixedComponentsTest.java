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

import java.util.List;
import java.util.Map;

import io.helidon.common.media.type.MediaTypes;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.McpGetPromptResult;
import dev.langchain4j.mcp.client.McpPrompt;
import dev.langchain4j.mcp.client.McpPromptArgument;
import dev.langchain4j.mcp.client.McpPromptMessage;
import dev.langchain4j.mcp.client.McpReadResourceResult;
import dev.langchain4j.mcp.client.McpResource;
import dev.langchain4j.mcp.client.McpRole;
import dev.langchain4j.mcp.client.McpTextContent;
import dev.langchain4j.mcp.client.McpTextResourceContents;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

abstract class AbstractLangchain4jMixedComponentsTest {
    protected static McpClient client;

    @AfterAll
    static void afterAll() throws Exception {
        if (client != null) {
            client.close();
        }
    }

    @Test
    void toolList() {
        List<ToolSpecification> tools = client.listTools();
        assertThat(tools.size(), is(1));

        ToolSpecification tool = tools.getFirst();
        assertThat(tool.name(), is("weatherAlert"));
        assertThat(tool.description(), is("Tool description"));

        JsonObjectSchema schema = tool.parameters();
        JsonSchemaElement state = schema.properties().get("state");
        assertThat(state, instanceOf(JsonStringSchema.class));

        JsonSchemaElement location = schema.properties().get("alert");
        assertThat(location, instanceOf(JsonObjectSchema.class));
    }

    @Test
    void promptList() {
        List<McpPrompt> prompts = client.listPrompts();
        assertThat(prompts.size(), is(1));

        McpPrompt prompt = prompts.getFirst();
        assertThat(prompt.name(), is("weatherInTown"));
        assertThat(prompt.description(), is("Prompt description"));

        List<McpPromptArgument> arguments = prompt.arguments();
        assertThat(arguments.size(), is(1));

        McpPromptArgument argument = arguments.getFirst();
        assertThat(argument.name(), is("town"));
        assertThat(argument.required(), is(true));
        assertThat(argument.description(), is("town's name"));
    }

    @Test
    void testResource() {
        List<McpResource> list = client.listResources();
        assertThat(list.size(), is(1));

        McpResource resource = list.getFirst();
        assertThat(resource.name(), is("weatherAlerts"));
        assertThat(resource.uri(), is("resource:resource"));
        assertThat(resource.mimeType(), is(MediaTypes.TEXT_PLAIN_VALUE));
        assertThat(resource.description(), is("Resource description"));
    }

    @Test
    void testCallTool() {
        String state = "Arizona";
        String alertName = "storm-alert";
        String argument = """
                {
                    "state": "%s",
                    "alert": {
                        "name": "%s",
                        "priority": 10,
                        "location": {
                             "latitude": 10,
                             "longitude": 10
                        }
                    }
                }
                """.formatted(state, alertName);
        ToolExecutionRequest request = ToolExecutionRequest.builder()
                .name("weatherAlert")
                .arguments(argument)
                .build();
        String result = client.executeTool(request);
        String expected = String.format("state: %s, alert name: %s", state, alertName);
        assertThat(result, is(expected));
    }

    @Test
    void testCallPrompt() {
        McpGetPromptResult prompt = client.getPrompt("weatherInTown", Map.of("town", "Paris"));
        assertThat(prompt.messages().size(), is(1));

        McpPromptMessage message = prompt.messages().getFirst();
        assertThat(message.role(), is(McpRole.USER));

        McpTextContent content = (McpTextContent) message.content();
        assertThat(content.text(), is("Town: Paris"));
    }

    @Test
    void testReadResource() {
        McpReadResourceResult result = client.readResource("resource:resource");
        assertThat(result.contents().size(), is(1));

        McpTextResourceContents textResource = (McpTextResourceContents) result.contents().getFirst();
        assertThat(textResource.uri(), is("resource:resource"));
        assertThat(textResource.text(), is("Resource content"));
        assertThat(textResource.mimeType(), is(MediaTypes.TEXT_PLAIN_VALUE));
    }
}
