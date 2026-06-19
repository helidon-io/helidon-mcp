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

package io.helidon.extensions.mcp.server;

import java.time.Duration;

import io.helidon.webserver.http.ServerResponse;
import io.helidon.webserver.jsonrpc.JsonRpcRequest;
import io.helidon.webserver.jsonrpc.JsonRpcResponse;

import jakarta.json.Json;
import jakarta.json.JsonValue;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

class McpSubscriptionsTest {
    private static final String RESOURCE_URI = "test://resource";

    @Test
    void testStreamableSubscriptionIsRemovedOnTimeout() {
        JsonValue requestId = Json.createValue(1);
        McpStreamableHttpTransport transport = new McpStreamableHttpTransport(mock(JsonRpcResponse.class));
        McpSession session = session(transport);
        McpSubscriptions subscriptions = session.features().subscriptions();

        session.onRequest(requestId, mock(JsonRpcRequest.class), mock(JsonRpcResponse.class));
        subscriptions.subscribe(requestId, RESOURCE_URI);
        subscriptions.blockSubscribe(RESOURCE_URI);

        assertDoesNotThrow(() -> subscriptions.sendSessionUpdate(RESOURCE_URI));
    }

    @Test
    void testStreamableBlockReportsTimeout() {
        McpStreamableHttpTransport transport = new McpStreamableHttpTransport(mock(JsonRpcResponse.class));

        assertThat(transport.block(Duration.ZERO), is(false));
    }

    @Test
    void testStreamableBlockReportsUnblock() {
        McpStreamableHttpTransport transport = new McpStreamableHttpTransport(mock(JsonRpcResponse.class));

        transport.unblock();

        assertThat(transport.block(Duration.ofSeconds(1)), is(true));
    }

    private static McpSession session(McpTransport transport) {
        McpServerConfig config = McpServerConfig.builder()
                .subscriptionTimeout(Duration.ZERO)
                .buildPrototype();
        McpSessions sessions = new McpSessions(config.maxSessionCount());
        McpSession session = new McpSession(sessions, new TestTransportManager(transport), config, "test-session");
        session.protocolVersion(McpProtocolVersion.VERSION_2025_06_18);
        sessions.put("test-session", session);
        return session;
    }

    private static final class TestTransportManager implements McpTransportManager {
        private final McpTransport transport;

        private TestTransportManager(McpTransport transport) {
            this.transport = transport;
        }

        @Override
        public McpTransport create(JsonRpcRequest request, JsonRpcResponse response) {
            return transport;
        }

        @Override
        public void onConnect(ServerResponse response) {
        }

        @Override
        public void onDisconnect(ServerResponse response) {
        }

        @Override
        public void onRequest(JsonRpcRequest request, JsonRpcResponse response) {
        }

        @Override
        public void onNotification(JsonRpcRequest request, JsonRpcResponse response) {
        }
    }
}
