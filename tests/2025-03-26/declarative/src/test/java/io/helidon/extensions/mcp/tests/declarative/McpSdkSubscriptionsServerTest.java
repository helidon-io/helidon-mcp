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

package io.helidon.extensions.mcp.tests.declarative;

import io.helidon.webserver.WebServer;
import io.helidon.webserver.testing.junit5.ServerTest;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ServerTest
class McpSdkSubscriptionsServerTest {
    private static McpSyncClient client;

    McpSdkSubscriptionsServerTest(WebServer server) {
        client = McpClient.sync(HttpClientStreamableHttpTransport.builder("http://localhost:" + server.port())
                                        .endpoint("/subscribers")
                                        .build())
                .build();
        client.initialize();
    }

    @AfterAll
    static void closeClient() {
        client.close();
    }

    @Test
    void testSubscriptions() {
        client.subscribeResource(new McpSchema.SubscribeRequest("http://myresource"));
        McpSchema.ReadResourceResult result = client.readResource(
                new McpSchema.ReadResourceRequest("http://myresource"));
        assertThat(result.contents().size(), is(1));
        client.unsubscribeResource(new McpSchema.UnsubscribeRequest("http://myresource"));
    }
}
