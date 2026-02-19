/*
 * Copyright (c) 2025, 2026 Oracle and/or its affiliates.
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

import io.helidon.common.media.type.MediaTypes;
import io.helidon.extensions.mcp.tests.common.McpMedia;
import io.helidon.extensions.mcp.tests.common.MultipleTool;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@ServerTest
class McpSdkStreamableMultipleToolTest extends AbstractMcpSdkTest {

    private final McpSyncClient client;

    McpSdkStreamableMultipleToolTest(WebServer server) {
        client = McpClient.sync(streamable(server.port())).build();
        client.initialize();
    }

    @Override
    McpSyncClient client() {
        return client;
    }

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        MultipleTool.setUpRoute(builder);
    }

    @Test
    void testListTools() {
        McpSchema.ListToolsResult result = client().listTools();
        assertThat(result.tools().size(), is(5));
    }

    @Test
    void testTool1() {
        McpSchema.CallToolResult tool1 = client().callTool(new McpSchema.CallToolRequest("tool1", Map.of()));
        assertThat(tool1.content().size(), is(1));

        McpSchema.Content content = tool1.content().getFirst();
        assertThat(content.type(), is("image"));

        McpSchema.ImageContent image = (McpSchema.ImageContent) content;
        assertThat(image.data(), is(McpMedia.base64Media("helidon.png")));
        assertThat(image.mimeType(), is(McpMedia.IMAGE_PNG_VALUE));
    }

    @Test
    void testTool2() {
        McpSchema.CallToolResult tool2 = client().callTool(new McpSchema.CallToolRequest("tool2", Map.of()));
        assertThat(tool2.content().size(), is(1));

        McpSchema.Content first = tool2.content().getFirst();
        assertThat(first.type(), is("resource"));

        var resource = (McpSchema.EmbeddedResource) first;
        var text = (McpSchema.TextResourceContents) resource.resource();
        assertThat(text.text(), is("resource"));
        assertThat(text.uri(), is("http://resource"));
        assertThat(text.mimeType(), is(MediaTypes.TEXT_PLAIN_VALUE));
    }

    @Test
    void testTool3() {
        McpSchema.CallToolResult tool3 = client().callTool(new McpSchema.CallToolRequest("tool3", Map.of()));
        assertThat(tool3.content().size(), is(6));

        McpSchema.Content first = tool3.content().getFirst();
        McpSchema.Content second = tool3.content().get(1);
        McpSchema.Content third = tool3.content().get(2);
        McpSchema.Content fourth = tool3.content().get(3);
        McpSchema.Content fifth = tool3.content().get(4);
        McpSchema.Content sixth = tool3.content().get(5);
        assertThat(first.type(), is("image"));
        assertThat(second.type(), is("resource"));
        assertThat(third.type(), is("text"));
        assertThat(fourth.type(), is("audio"));
        assertThat(fifth.type(), is("resource_link"));
        assertThat(sixth.type(), is("resource_link"));

        McpSchema.ImageContent image = (McpSchema.ImageContent) first;
        assertThat(image.data(), is(McpMedia.base64Media("helidon.png")));
        assertThat(image.mimeType(), is(McpMedia.IMAGE_PNG_VALUE));

        McpSchema.EmbeddedResource resource = (McpSchema.EmbeddedResource) second;
        assertThat(resource.resource().uri(), is("http://resource"));
        assertThat(resource.resource().mimeType(), is(MediaTypes.TEXT_PLAIN_VALUE));

        McpSchema.TextContent text = (McpSchema.TextContent) third;
        assertThat(text.text(), is("text"));

        McpSchema.AudioContent audio = (McpSchema.AudioContent) fourth;
        assertThat(audio.data(), is(McpMedia.base64Media("helidon.wav")));
        assertThat(audio.mimeType(), is(McpMedia.AUDIO_WAV_VALUE));

        McpSchema.ResourceLink link = (McpSchema.ResourceLink) fifth;
        assertThat(link.uri(), is("https://foo"));
        assertThat(link.name(), is("resource-link-default"));

        McpSchema.ResourceLink link1 = (McpSchema.ResourceLink) sixth;
        assertThat(link1.size(), is(10L));
        assertThat(link1.title(), is("title"));
        assertThat(link1.uri(), is("https://foo"));
        assertThat(link1.description(), is("description"));
        assertThat(link1.name(), is("resource-link-custom"));
        assertThat(link1.mimeType(), is(MediaTypes.TEXT_PLAIN_VALUE));
    }

    @Test
    void testTool4() {
        McpSchema.CallToolResult tool4 = client().callTool(
                new McpSchema.CallToolRequest("tool4", Map.of("name", "Paris", "population", 10)));
        assertThat(tool4.content().size(), is(1));

        McpSchema.TextContent text = (McpSchema.TextContent) tool4.content().getFirst();
        assertThat(text.text(), is("Paris has a population of 10 inhabitants"));
    }

    @Test
    void testTool5() {
        McpSchema.CallToolRequest request = McpSchema.CallToolRequest.builder().name("tool5").build();
        McpSchema.CallToolResult tool5 = client().callTool(request);

        assertThat(tool5.content().size(), is(1));
        McpSchema.TextContent text = (McpSchema.TextContent) tool5.content().getFirst();

        assertThat(tool5.structuredContent(), is(notNullValue()));
        assertThat(tool5.structuredContent(), is(instanceOf(Map.class)));
        Map<String, String> content = (Map<String, String>) tool5.structuredContent();
        assertThat(content.get("foo"), is("foo"));
    }
}
