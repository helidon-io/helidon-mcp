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

package io.helidon.mcp.tests;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static io.helidon.mcp.tests.MultipleResourceTemplate.RESOURCE1_URI;
import static io.helidon.mcp.tests.MultipleResourceTemplate.RESOURCE2_URI;
import static io.helidon.mcp.tests.MultipleResourceTemplate.RESOURCE3_URI;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;

@ServerTest
class AnthropicMultipleResourceTemplateTest {
    private static McpSyncClient client;

    AnthropicMultipleResourceTemplateTest(WebServer server) {
        client = McpClient.sync(HttpClientSseClientTransport.builder("http://localhost:" + server.port())
                                        .sseEndpoint("/")
                                        .build())
                .build();
        client.initialize();
    }

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        MultipleResourceTemplate.setUpRoute(builder);
    }

    @AfterAll
    static void closeClient() {
        client.close();
    }

    @Test
    void listResources() {
        McpSchema.ListResourceTemplatesResult list = client.listResourceTemplates();
        assertThat(list.resourceTemplates().size(), is(3));

        List<String> names = list.resourceTemplates().stream().map(McpSchema.ResourceTemplate::name).toList();
        assertThat(names, hasItems("resource1", "resource2", "resource3"));
    }

    @Test
    void testReadResource1() {
        McpSchema.ReadResourceResult resource = client.readResource(new McpSchema.ReadResourceRequest(RESOURCE1_URI));
        assertThat(resource.contents().size(), is(1));

        McpSchema.TextResourceContents first = (McpSchema.TextResourceContents) resource.contents().getFirst();
        assertThat(first.text(), is("text"));
        assertThat(first.uri(), is(RESOURCE1_URI));
        assertThat(first.mimeType(), is(MediaTypes.TEXT_PLAIN_VALUE));
    }

    @Test
    void testReadResource2() {
        McpSchema.ReadResourceResult resource = client.readResource(new McpSchema.ReadResourceRequest(RESOURCE2_URI));
        assertThat(resource.contents().size(), is(1));

        McpSchema.BlobResourceContents first = (McpSchema.BlobResourceContents) resource.contents().getFirst();
        assertThat(first.blob(), is(Base64.getEncoder().encodeToString("binary".getBytes(StandardCharsets.UTF_8))));
        assertThat(first.uri(), is(RESOURCE2_URI));
        assertThat(first.mimeType(), is(MediaTypes.APPLICATION_JSON_VALUE));
    }

    @Test
    void testReadResource3() {
        McpSchema.ReadResourceResult resource = client.readResource(new McpSchema.ReadResourceRequest(RESOURCE3_URI));
        assertThat(resource.contents().size(), is(2));

        McpSchema.TextResourceContents second = (McpSchema.TextResourceContents) resource.contents().getFirst();
        assertThat(second.text(), is("text"));
        assertThat(second.uri(), is(RESOURCE3_URI));
        assertThat(second.mimeType(), is(MediaTypes.TEXT_PLAIN_VALUE));

        McpSchema.BlobResourceContents first = (McpSchema.BlobResourceContents) resource.contents().get(1);
        assertThat(first.blob(), is(Base64.getEncoder().encodeToString("binary".getBytes(StandardCharsets.UTF_8))));
        assertThat(first.uri(), is(RESOURCE3_URI));
        assertThat(first.mimeType(), is(MediaTypes.APPLICATION_JSON_VALUE));
    }
}
