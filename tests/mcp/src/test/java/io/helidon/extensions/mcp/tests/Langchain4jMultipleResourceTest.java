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

import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpBlobResourceContents;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.McpReadResourceResult;
import dev.langchain4j.mcp.client.McpResource;
import dev.langchain4j.mcp.client.McpResourceContents;
import dev.langchain4j.mcp.client.McpTextResourceContents;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;

@ServerTest
class Langchain4jMultipleResourceTest {
    private static McpClient client;

    Langchain4jMultipleResourceTest(WebServer server) {
        McpTransport transport = new HttpMcpTransport.Builder()
                .sseUrl("http://localhost:" + server.port())
                .logRequests(true)
                .logResponses(true)
                .build();
        client = new DefaultMcpClient.Builder()
                .transport(transport)
                .build();
    }

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        MultipleResource.setUpRoute(builder);
    }

    @AfterAll
    static void closeClient() throws Exception {
        client.close();
    }

    @Test
    void listResources() {
        List<McpResource> list = client.listResources();
        assertThat(list.size(), is(3));

        List<String> names = list.stream().map(McpResource::name).toList();
        assertThat(names, hasItems("resource1", "resource2", "resource3"));
    }

    @Test
    void readResource1() {
        McpReadResourceResult resource = client.readResource("http://resource1");

        List<McpResourceContents> contents = resource.contents();
        assertThat(contents.size(), is(1));

        McpTextResourceContents first = (McpTextResourceContents) contents.getFirst();
        assertThat(first.uri(), is("http://resource1"));
        assertThat(first.text(), is("text"));
        assertThat(first.mimeType(), is(MediaTypes.TEXT_PLAIN_VALUE));
    }

    @Test
    void readResource2() {
        McpReadResourceResult resource = client.readResource("http://resource2");

        List<McpResourceContents> contents = resource.contents();
        assertThat(contents.size(), is(1));

        McpBlobResourceContents first = (McpBlobResourceContents) contents.getFirst();
        assertThat(first.uri(), is("http://resource2"));
        assertThat(first.blob(), is(Base64.getEncoder().encodeToString("binary".getBytes(StandardCharsets.UTF_8))));
        assertThat(first.mimeType(), is(MediaTypes.APPLICATION_JSON_VALUE));
    }

    @Test
    void readResource3() {
        McpReadResourceResult resource = client.readResource("http://resource3");

        List<McpResourceContents> contents = resource.contents();
        assertThat(contents.size(), is(2));

        McpTextResourceContents first = (McpTextResourceContents) contents.getFirst();
        assertThat(first.uri(), is("http://resource3"));
        assertThat(first.text(), is("text"));
        assertThat(first.mimeType(), is(MediaTypes.TEXT_PLAIN_VALUE));

        McpBlobResourceContents second = (McpBlobResourceContents) contents.get(1);
        assertThat(second.uri(), is("http://resource3"));
        assertThat(second.blob(), is(Base64.getEncoder().encodeToString("binary".getBytes(StandardCharsets.UTF_8))));
        assertThat(second.mimeType(), is(MediaTypes.APPLICATION_JSON_VALUE));
    }
}
