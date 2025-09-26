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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.helidon.webserver.WebServer;
import io.helidon.webserver.testing.junit5.ServerTest;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import static io.helidon.extensions.mcp.tests.ResourceSubscriptions.context;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@ServerTest
class McpSdkStreamableSubUnsubTest extends AbstractMcpSdkSubUnsubTest {

    private final McpSyncClient client;

    McpSdkStreamableSubUnsubTest(WebServer server) {
        client = McpClient.sync(streamable(server.port()))
                .build();
        client.initialize();
    }

    @Override
    McpSyncClient client() {
        return client;
    }

    @Test
    void testSubUnsub() throws InterruptedException {
        CountDownLatch subscribeLatch = new CountDownLatch(1);
        async(() -> {
            // in async as this blocks thread with streamable HTTP
            client().subscribeResource(new McpSchema.SubscribeRequest("http://myresource"));
            subscribeLatch.countDown();
        });

        simulateSessionTraffic();

        // read resource, should trigger subscription update and a second read
        // possible race condition of it arrives before the subscription above
        CountDownLatch readLatch = context().get(CountDownLatch.class).orElseThrow();
        client().readResource(new McpSchema.ReadResourceRequest("http://myresource"));

        simulateSessionTraffic();

        // if pending reads then race condition, fix it
        while (readLatch.getCount() > 0) {
            client().readResource(new McpSchema.ReadResourceRequest("http://myresource"));
        }

        // unsubscribe, should unblock subscriber
        client().unsubscribeResource(new McpSchema.UnsubscribeRequest("http://myresource"));

        // check state after test execution
        assertThat(readLatch.await(10, TimeUnit.SECONDS), is(true));
        assertThat(subscribeLatch.await(10, TimeUnit.SECONDS), is(true));
    }
}
