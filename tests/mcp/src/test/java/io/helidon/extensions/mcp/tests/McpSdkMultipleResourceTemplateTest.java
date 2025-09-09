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

import io.helidon.common.media.type.MediaTypes;
import io.helidon.jsonrpc.core.JsonRpcError;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static io.helidon.extensions.mcp.tests.MultipleResourceTemplate.RESOURCE1_URI;
import static io.helidon.extensions.mcp.tests.MultipleResourceTemplate.RESOURCE2_URI;
import static io.helidon.extensions.mcp.tests.MultipleResourceTemplate.RESOURCE3_URI;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;

@ServerTest
class McpSdkMultipleResourceTemplateTest {
    private static McpSyncClient client;

    McpSdkMultipleResourceTemplateTest(WebServer server) {
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
    void listResourceTemplates() {
        List<McpSchema.ResourceTemplate> list = client.listResourceTemplates().resourceTemplates();
        list = list.reversed();
        assertThat(list.size(), is(3));

        var resource3 = list.getFirst();
        assertThat(resource3.name(), is("resource3"));
        assertThat(resource3.description(), is("Resource 3"));
        assertThat(resource3.uriTemplate(), is(RESOURCE3_URI));
        assertThat(resource3.mimeType(), is(MediaTypes.APPLICATION_OCTET_STREAM_VALUE));

        var resource2 = list.get(1);
        assertThat(resource2.name(), is("resource2"));
        assertThat(resource2.description(), is("Resource 2"));
        assertThat(resource2.uriTemplate(), is(RESOURCE2_URI));
        assertThat(resource2.mimeType(), is(MediaTypes.APPLICATION_JSON_VALUE));

        var resource1 = list.get(2);
        assertThat(resource1.name(), is("resource1"));
        assertThat(resource1.description(), is("Resource 1"));
        assertThat(resource1.uriTemplate(), is(RESOURCE1_URI));
        assertThat(resource1.mimeType(), is(MediaTypes.TEXT_PLAIN_VALUE));
    }

    @Test
    void readResource1Template() {
        var result = client.readResource(new McpSchema.ReadResourceRequest("https://foo"));
        assertThat(result.contents().size(), is(1));

        var resource = result.contents().getFirst();
        assertThat(resource, instanceOf(McpSchema.TextResourceContents.class));

        var text = (McpSchema.TextResourceContents) resource;
        assertThat(text.text(), is("foo"));
        assertThat(text.uri(), is("https://foo"));
        assertThat(text.mimeType(), is(MediaTypes.TEXT_PLAIN_VALUE));
    }

    @Test
    void readResource2Template() {
        var result = client.readResource(new McpSchema.ReadResourceRequest("https://foo/foo1/foo2"));
        assertThat(result.contents().size(), is(1));

        var content = result.contents().getFirst();
        assertThat(content, instanceOf(McpSchema.TextResourceContents.class));

        var text = (McpSchema.TextResourceContents) content;
        assertThat(text.text(), is("foo2"));
        assertThat(text.uri(), is("https://foo/foo1/foo2"));
        assertThat(text.mimeType(), is(MediaTypes.TEXT_PLAIN_VALUE));
    }

    @Test
    void readResource3Template() {
        var result = client.readResource(new McpSchema.ReadResourceRequest("https://foo/bar"));
        assertThat(result.contents().size(), is(2));

        var content = result.contents().getFirst();
        assertThat(content, instanceOf(McpSchema.TextResourceContents.class));

        var text = (McpSchema.TextResourceContents) content;
        assertThat(text.text(), is("foo"));
        assertThat(text.uri(), is("https://foo/bar"));
        assertThat(text.mimeType(), is(MediaTypes.TEXT_PLAIN_VALUE));

        var content1 = result.contents().get(1);
        assertThat(content1, instanceOf(McpSchema.TextResourceContents.class));

        var text1 = (McpSchema.TextResourceContents) content1;
        assertThat(text1.text(), is("bar"));
        assertThat(text1.uri(), is("https://foo/bar"));
        assertThat(text1.mimeType(), is(MediaTypes.TEXT_PLAIN_VALUE));
    }
}
