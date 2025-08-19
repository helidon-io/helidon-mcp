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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;

@ServerTest
class AnthropicMultipleResourceTest {
    private static McpSyncClient client;

    AnthropicMultipleResourceTest(WebServer server) {
        client = McpClient.sync(HttpClientSseClientTransport.builder("http://localhost:" + server.port())
                                        .sseEndpoint("/")
                                        .build())
                .build();
        client.initialize();
    }

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        MultipleResource.setUpRoute(builder);
    }

    @AfterAll
    static void closeClient() {
        client.close();
    }

    @Test
    void listResources() {
        McpSchema.ListResourcesResult list = client.listResources();
        assertThat(list.resources().size(), is(3));

        List<String> names = list.resources().stream().map(McpSchema.Resource::name).toList();
        assertThat(names, hasItems("resource1", "resource2", "resource3"));
    }

    @Test
    void testReadResource1() {
        McpSchema.ReadResourceResult resource = client.readResource(new McpSchema.ReadResourceRequest("http://resource1"));
        assertThat(resource.contents().size(), is(1));

        McpSchema.TextResourceContents first = (McpSchema.TextResourceContents) resource.contents().getFirst();
        assertThat(first.text(), is("text"));
        assertThat(first.uri(), is("http://resource1"));
        assertThat(first.mimeType(), is(MediaTypes.TEXT_PLAIN_VALUE));
    }

    @Test
    void testReadResource2() {
        McpSchema.ReadResourceResult resource = client.readResource(new McpSchema.ReadResourceRequest("http://resource2"));
        assertThat(resource.contents().size(), is(1));

        McpSchema.BlobResourceContents first = (McpSchema.BlobResourceContents) resource.contents().getFirst();
        assertThat(first.blob(), is(Base64.getEncoder().encodeToString("binary".getBytes(StandardCharsets.UTF_8))));
        assertThat(first.uri(), is("http://resource2"));
        assertThat(first.mimeType(), is(MediaTypes.APPLICATION_JSON_VALUE));
    }

    @Test
    void testReadResource3() {
        McpSchema.ReadResourceResult resource = client.readResource(new McpSchema.ReadResourceRequest("http://resource3"));
        assertThat(resource.contents().size(), is(2));

        McpSchema.TextResourceContents first = (McpSchema.TextResourceContents) resource.contents().getFirst();
        assertThat(first.text(), is("text"));
        assertThat(first.uri(), is("http://resource3"));
        assertThat(first.mimeType(), is(MediaTypes.TEXT_PLAIN_VALUE));

        McpSchema.BlobResourceContents second = (McpSchema.BlobResourceContents) resource.contents().get(1);
        assertThat(second.blob(), is(Base64.getEncoder().encodeToString("binary".getBytes(StandardCharsets.UTF_8))));
        assertThat(second.uri(), is("http://resource3"));
        assertThat(second.mimeType(), is(MediaTypes.APPLICATION_JSON_VALUE));
    }
}
