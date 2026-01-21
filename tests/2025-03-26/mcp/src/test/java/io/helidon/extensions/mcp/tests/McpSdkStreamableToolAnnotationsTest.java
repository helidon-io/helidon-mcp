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

import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ServerTest
class McpSdkStreamableToolAnnotationsTest extends AbstractMcpSdkTest {
    private final McpSyncClient client;

    McpSdkStreamableToolAnnotationsTest(WebServer server) {
        client = McpClient.sync(streamable(server.port(), "/toolAnnotations"))
                .build();
        client.initialize();
    }

    @Override
    McpSyncClient client() {
        return client;
    }

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        ToolAnnotationsServer.setUpRoute(builder);
    }

    @Test
    void testListToolsWithAnnotations() {
        var result = client.listTools();
        var tools = result.tools();
        assertThat(tools.size(), is(3));

        var tool1 = tools.getFirst();
        assertThat(tool1.name(), is("tool1"));
        assertThat(tool1.description(), is("Tool description"));
        McpSchema.ToolAnnotations annotations1 = tool1.annotations();
        assertThat(annotations1.title(), is(""));
        assertThat(annotations1.readOnlyHint(), is(false));
        assertThat(annotations1.destructiveHint(), is(true));
        assertThat(annotations1.idempotentHint(), is(false));
        assertThat(annotations1.openWorldHint(), is(true));

        var tool2 = tools.get(1);
        assertThat(tool2.name(), is("tool2"));
        assertThat(tool2.description(), is("Tool description"));
        McpSchema.ToolAnnotations annotations2 = tool2.annotations();
        assertThat(annotations2.title(), is("tool2 title"));
        assertThat(annotations2.readOnlyHint(), is(true));
        assertThat(annotations2.destructiveHint(), is(false));
        assertThat(annotations2.idempotentHint(), is(true));
        assertThat(annotations2.openWorldHint(), is(false));

        var tool3 = tools.getLast();
        assertThat(tool3.name(), is("tool3"));
        assertThat(tool3.description(), is("Tool 3 description"));
        McpSchema.ToolAnnotations annotations3 = tool3.annotations();
        assertThat(annotations3.title(), is(""));
        assertThat(annotations3.readOnlyHint(), is(false));
        assertThat(annotations3.destructiveHint(), is(true));
        assertThat(annotations3.idempotentHint(), is(false));
        assertThat(annotations3.openWorldHint(), is(true));
    }
}
