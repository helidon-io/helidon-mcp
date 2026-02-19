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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.helidon.extensions.mcp.tests.common.MultipleResource;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.helidon.extensions.mcp.tests.common.MultipleResource.context;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@ServerTest
class McpSdkStreamableUnsubscriptionTest extends AbstractMcpSdkTest {

    private final McpSyncClient client;

    McpSdkStreamableUnsubscriptionTest(WebServer server) {
        client = McpClient.sync(streamable(server.port())).build();
        client.initialize();
    }

    @Override
    McpSyncClient client() {
        return client;
    }

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        MultipleResource.setUpRoute(builder);
    }

    @BeforeEach
    void setState() {
        context().register(new MultipleResource.State(new CountDownLatch(1),
                                                      new CountDownLatch(10),
                                                      new AtomicBoolean(false)));
    }

    @Test
    void testUnSubscription() throws InterruptedException {
        MultipleResource.State state = context().get(MultipleResource.State.class).orElseThrow();
        CountDownLatch subscribeLatch = new CountDownLatch(1);
        async(() -> {
            client().subscribeResource(new McpSchema.SubscribeRequest("http://resource3"));
            subscribeLatch.countDown();
        });
        assertThat(state.readLatch().await(10, TimeUnit.SECONDS), is(true));
        client().unsubscribeResource(new McpSchema.UnsubscribeRequest("http://resource3"));
        assertThat(subscribeLatch.await(10, TimeUnit.SECONDS), is(true));
    }
}
