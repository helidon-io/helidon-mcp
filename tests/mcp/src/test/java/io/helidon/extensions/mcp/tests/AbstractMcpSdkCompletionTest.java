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

import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import static io.helidon.jsonrpc.core.JsonRpcError.INVALID_PARAMS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

abstract class AbstractMcpSdkCompletionTest extends AbstractMcpSdkTest {

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        CompletionNotifications.setUpRoute(builder);
    }

    @Test
    void testMcpSdkCompletion() {
        McpSchema.CompleteRequest request = new McpSchema.CompleteRequest(
                new McpSchema.PromptReference("helidon"),
                new McpSchema.CompleteRequest.CompleteArgument("argument", "Hel"));
        McpSchema.CompleteResult.CompleteCompletion result = client().completeCompletion(request).completion();
        assertThat(result.total(), is(1));
        assertThat(result.hasMore(), is(false));

        var list = result.values();
        assertThat(list.size(), is(1));
        assertThat(list.getFirst(), is("Helidon"));
    }

    @Test
    void testMcpSdkMissingPrompt() {
        McpSchema.CompleteRequest request = new McpSchema.CompleteRequest(
                new McpSchema.PromptReference("unknown"),
                new McpSchema.CompleteRequest.CompleteArgument("foo", "bar"));
        McpError error = assertThrows(McpError.class,
                                      () -> client().completeCompletion(request).completion());
        assertThat(error.getJsonRpcError().code(), is(INVALID_PARAMS));
        assertThat(error.getJsonRpcError().message(), is("No prompt completion found"));
    }
}
