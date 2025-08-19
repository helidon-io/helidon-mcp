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

import io.helidon.webserver.WebServer;
import io.helidon.webserver.testing.junit5.ServerTest;

import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.McpResourceTemplate;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static io.helidon.extensions.mcp.tests.declarative.McpResourceTemplatesServer.RESOURCE_DESCRIPTION;
import static io.helidon.extensions.mcp.tests.declarative.McpResourceTemplatesServer.RESOURCE_MEDIA_TYPE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ServerTest
class Langchain4jResourceTemplatesServerTest {
    private static McpClient client;

    Langchain4jResourceTemplatesServerTest(WebServer server) {
        McpTransport transport = new HttpMcpTransport.Builder()
                .sseUrl("http://localhost:" + server.port() + "/resource/templates")
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

    @Test
    void readResource() {
        List<McpResourceTemplate> result = client.listResourceTemplates();
        assertThat(result.size(), is(4));

        McpResourceTemplate first = result.getFirst();
        assertThat(first.name(), is("resource1"));
        assertThat(first.uriTemplate(), is("https://{path}"));
        assertThat(first.mimeType(), is(RESOURCE_MEDIA_TYPE));
        assertThat(first.description(), is(RESOURCE_DESCRIPTION));

        McpResourceTemplate second = result.get(1);
        assertThat(second.name(), is("resource3"));
        assertThat(second.uriTemplate(), is("git://{path}"));
        assertThat(second.mimeType(), is(RESOURCE_MEDIA_TYPE));
        assertThat(second.description(), is(RESOURCE_DESCRIPTION));

        McpResourceTemplate third = result.get(2);
        assertThat(third.name(), is("resource"));
        assertThat(third.uriTemplate(), is("resource::{path}"));
        assertThat(third.mimeType(), is(RESOURCE_MEDIA_TYPE));
        assertThat(third.description(), is(RESOURCE_DESCRIPTION));

        McpResourceTemplate fourth = result.get(3);
        assertThat(fourth.name(), is("resource2"));
        assertThat(fourth.uriTemplate(), is("file://{path}"));
        assertThat(fourth.mimeType(), is(RESOURCE_MEDIA_TYPE));
        assertThat(fourth.description(), is(RESOURCE_DESCRIPTION));
    }
}
