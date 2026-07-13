/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
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
package io.helidon.extensions.mcp.examples.calendar.microprofile;

import java.net.URI;
import java.util.List;
import java.util.Map;

import io.helidon.microprofile.testing.Socket;
import io.helidon.microprofile.testing.junit5.HelidonTest;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@HelidonTest
class CompletionTest {
    private final URI uri;

    @Inject
    CompletionTest(@Socket("@default") URI uri) {
        this.uri = uri;
    }

    @Test
    void testPromptCompletion() {
        try (McpSyncClient client = McpClient.sync(
                HttpClientStreamableHttpTransport.builder(uri.toASCIIString())
                        .endpoint("/calendar")
                        .build()).build()) {
            client.initialize();
            McpSchema.CompleteRequest request = new McpSchema.CompleteRequest(
                    new McpSchema.PromptReference("createEventPrompt"),
                    new McpSchema.CompleteRequest.CompleteArgument("name", "Frank"));
            McpSchema.CompleteResult.CompleteCompletion completion = client.completeCompletion(request).completion();

            assertThat(completion.hasMore(), is(false));
            assertThat(completion.total(), is(1));
            assertThat(completion.values(), is(List.of("Frank & Friends")));
        }
    }

    @Test
    void testResourceCompletion() {
        try (McpSyncClient client = McpClient.sync(
                HttpClientStreamableHttpTransport.builder(uri.toASCIIString())
                        .endpoint("/calendar")
                        .build()).build()) {
            client.initialize();
            Map<String, Object> arguments = Map.of(
                    "event", Map.of(
                            "name", "Frank-birthday",
                            "date", "2021-04-20",
                            "attendees", List.of("Frank")));
            McpSchema.CallToolRequest callToolRequest = new McpSchema.CallToolRequest("addCalendarEvent", arguments);
            McpSchema.CallToolResult callToolResult = client.callTool(callToolRequest);
            assertThat(callToolResult.isError(), is(false));

            McpSchema.CompleteRequest request = new McpSchema.CompleteRequest(
                    new McpSchema.ResourceReference(McpCalendarServer.EVENTS_URI_TEMPLATE),
                    new McpSchema.CompleteRequest.CompleteArgument("name", "Frank"));
            McpSchema.CompleteResult.CompleteCompletion completion = client.completeCompletion(request).completion();

            assertThat(completion.hasMore(), is(false));
            assertThat(completion.total(), is(1));
            assertThat(completion.values(), is(List.of("Frank-birthday")));
        }
    }
}
