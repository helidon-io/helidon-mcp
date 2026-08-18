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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import io.helidon.common.context.Context;
import io.helidon.json.JsonObject;
import io.helidon.json.JsonParser;
import io.helidon.json.JsonValue;
import io.helidon.jsonrpc.core.JsonRpcParams;
import io.helidon.webserver.http.ServerResponse;
import io.helidon.webserver.jsonrpc.JsonRpcRequest;
import io.helidon.webserver.jsonrpc.JsonRpcResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class McpSessionTest {

    @Test
    void doesNotNegotiateMissingElicitationCapability() {
        McpSession session = session(McpProtocolVersion.VERSION_2025_11_25, """
                {}
                """);

        assertThat(session.capabilities().contains(McpCapability.ELICITATION), is(false));
    }

    @Test
    void negotiatesEmptyElicitationCapability() {
        McpSession session = session(McpProtocolVersion.VERSION_2025_11_25, """
                {"elicitation": {}}
                """);

        assertThat(session.capabilities().contains(McpCapability.ELICITATION_FORM), is(true));
    }

    @Test
    void negotiatesFormElicitationCapability() {
        McpSession session = session(McpProtocolVersion.VERSION_2025_11_25, """
                {"elicitation": {"form": {}}}
                """);

        assertThat(session.capabilities().contains(McpCapability.ELICITATION_FORM), is(true));
    }

    @Test
    void negotiatesUrlElicitationCapability() {
        McpSession session = session(McpProtocolVersion.VERSION_2025_11_25, """
                {"elicitation": {"url": {}}}
                """);

        assertThat(session.capabilities(),
                   containsInAnyOrder(McpCapability.ELICITATION,
                                      McpCapability.ELICITATION_URL));
    }

    @Test
    void disablesFormElicitationForUrlOnlyCapability() {
        McpSession session = session(McpProtocolVersion.VERSION_2025_11_25, """
                {"elicitation": {"url": {}}}
                """);
        McpElicitation elicitation = new McpElicitation(session,
                                                        new McpStreamableHttpTransport(mock(JsonRpcResponse.class)));

        assertThat(elicitation.enabled(), is(false));
    }

    @Test
    void negotiatesFormAndUrlElicitationCapabilities() {
        McpSession session = session(McpProtocolVersion.VERSION_2025_11_25, """
                {"elicitation": {"form": {}, "url": {}}}
                """);

        assertThat(session.capabilities(),
                   containsInAnyOrder(McpCapability.ELICITATION,
                                      McpCapability.ELICITATION_FORM,
                                      McpCapability.ELICITATION_URL));
    }

    @Test
    void negotiatesLatestSamplingSubcapabilities() {
        McpSession sampling = session(McpProtocolVersion.VERSION_2025_11_25, """
                {"sampling": {}}
                """);
        McpSession samplingContext = session(McpProtocolVersion.VERSION_2025_11_25, """
                {"sampling": {"context": {}}}
                """);
        McpSession samplingTools = session(McpProtocolVersion.VERSION_2025_11_25, """
                {"sampling": {"tools": {}}}
                """);

        assertThat(sampling.capabilities().contains(McpCapability.SAMPLING), is(true));
        assertThat(sampling.capabilities().contains(McpCapability.SAMPLING_CONTEXT), is(false));
        assertThat(sampling.capabilities().contains(McpCapability.SAMPLING_TOOLS), is(false));
        assertThat(samplingContext.capabilities().contains(McpCapability.SAMPLING), is(true));
        assertThat(samplingContext.capabilities().contains(McpCapability.SAMPLING_CONTEXT), is(true));
        assertThat(samplingTools.capabilities().contains(McpCapability.SAMPLING), is(true));
        assertThat(samplingTools.capabilities().contains(McpCapability.SAMPLING_TOOLS), is(true));
    }

    @ParameterizedTest
    @EnumSource(value = McpProtocolVersion.class,
                names = {"VERSION_2024_11_05", "VERSION_2025_03_26", "VERSION_2025_06_18"})
    void ignoresSamplingToolsCapabilityForLegacyProtocolVersions(McpProtocolVersion protocolVersion) {
        McpSession session = session(protocolVersion, """
                {"sampling": {"tools": {}}}
                """);

        assertThat(session.capabilities().contains(McpCapability.SAMPLING), is(true));
        assertThat(session.capabilities().contains(McpCapability.SAMPLING_TOOLS), is(false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"listChanged\": false}", "{\"listChanged\": true}"})
    void negotiatesRootsCapabilityRegardlessOfListChanged(String rootsCapability) {
        McpSession session = session(McpProtocolVersion.VERSION_2025_11_25, """
                {"roots": %s}
                """.formatted(rootsCapability));

        assertThat(session.capabilities().contains(McpCapability.ROOTS), is(true));
    }

    @Test
    void doesNotNegotiateMissingRootsCapability() {
        McpSession session = session(McpProtocolVersion.VERSION_2025_11_25, "{}");

        assertThat(session.capabilities().contains(McpCapability.ROOTS), is(false));
    }

    @Test
    void preservesLegacyElicitationCapability() {
        McpSession session = session(McpProtocolVersion.VERSION_2025_06_18, """
                {"elicitation": {}}
                """);

        assertThat(session.capabilities().contains(McpCapability.ELICITATION_FORM), is(true));
    }

    @Test
    void ignoresResponseWithNonNumericId() {
        McpServerConfig config = McpServerConfig.create();
        McpSessions sessions = new McpSessions(config.maxSessionCount());
        McpSession session = new McpSession(sessions,
                                            mock(McpTransportManager.class),
                                            config,
                                            "test-session");
        McpResponse malformedResponse = new McpResponseImpl(JsonObject.builder()
                .set("id", "not-a-number")
                .build(), Context.create());
        McpResponse expectedResponse = new McpResponseImpl(JsonObject.builder()
                .set("id", 42)
                .build(), Context.create());

        session.prepareResponse(42);
        session.acceptResponse(malformedResponse);
        session.acceptResponse(expectedResponse);

        McpResponse response = pollResponse(session, 42, Duration.ofSeconds(1)).orElseThrow();
        assertThat(response, sameInstance(expectedResponse));
    }

    @Test
    void correlatesOutOfOrderResponsesAndContextsByRequestId() {
        McpServerConfig config = McpServerFeature.builder()
                .maxRequestsPerSession(2)
                .buildPrototype();
        McpSession session = session(McpProtocolVersion.VERSION_2025_11_25, "{}", config);
        long firstId = session.jsonRpcId();
        long secondId = session.jsonRpcId();
        session.prepareResponse(firstId);
        session.prepareResponse(secondId);
        JsonObject firstJson = JsonObject.builder().set("id", firstId).build();
        JsonObject secondJson = JsonObject.builder().set("id", secondId).build();
        Context firstContext = Context.create();
        Context secondContext = Context.create();
        McpResponse firstResponse = new McpResponseImpl(firstJson, firstContext);
        McpResponse secondResponse = new McpResponseImpl(secondJson, secondContext);

        session.acceptResponse(secondResponse);
        session.acceptResponse(firstResponse);

        McpResponse polledFirst = pollResponse(session, firstId, Duration.ofSeconds(1)).orElseThrow();
        McpResponse polledSecond = pollResponse(session, secondId, Duration.ofSeconds(1)).orElseThrow();
        assertThat(polledFirst.asJsonObject(), sameInstance(firstJson));
        assertThat(polledFirst.requestContext(), sameInstance(firstContext));
        assertThat(polledSecond.asJsonObject(), sameInstance(secondJson));
        assertThat(polledSecond.requestContext(), sameInstance(secondContext));
    }

    @Test
    void rejectsPendingResponseBeyondCapacityWithoutEviction() {
        McpServerConfig config = McpServerFeature.builder()
                .maxRequestsPerSession(2)
                .buildPrototype();
        McpSession session = session(McpProtocolVersion.VERSION_2025_11_25, "{}", config);
        long firstId = session.jsonRpcId();
        long secondId = session.jsonRpcId();
        long thirdId = session.jsonRpcId();
        session.prepareResponse(firstId);
        session.prepareResponse(secondId);

        McpInternalException exception = assertThrows(McpInternalException.class,
                                                       () -> session.prepareResponse(thirdId));

        assertThat(exception.getMessage(), is("Maximum pending response count reached"));
        JsonObject firstJson = JsonObject.builder().set("id", firstId).build();
        JsonObject secondJson = JsonObject.builder().set("id", secondId).build();
        JsonObject thirdJson = JsonObject.builder().set("id", thirdId).build();
        McpResponse firstResponse = new McpResponseImpl(firstJson, Context.create());
        McpResponse secondResponse = new McpResponseImpl(secondJson, Context.create());
        McpResponse thirdResponse = new McpResponseImpl(thirdJson, Context.create());
        session.acceptResponse(secondResponse);
        session.acceptResponse(firstResponse);
        assertThat(pollResponse(session, firstId, Duration.ofSeconds(1)).orElseThrow(), sameInstance(firstResponse));

        session.prepareResponse(thirdId);
        session.acceptResponse(thirdResponse);

        assertThat(pollResponse(session, secondId, Duration.ofSeconds(1)).orElseThrow(), sameInstance(secondResponse));
        assertThat(pollResponse(session, thirdId, Duration.ofSeconds(1)).orElseThrow(), sameInstance(thirdResponse));
    }

    @Test
    void disconnectUnblocksPendingResponse() throws InterruptedException {
        McpSession session = session(McpProtocolVersion.VERSION_2025_11_25, "{}");
        long requestId = session.jsonRpcId();
        session.prepareResponse(requestId);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread poller = Thread.ofVirtual().start(() -> {
            try {
                pollResponse(session, requestId, Duration.ofSeconds(5));
            } catch (Throwable e) {
                failure.set(e);
            }
        });
        assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
            while (poller.getState() != Thread.State.WAITING
                    && poller.getState() != Thread.State.TIMED_WAITING) {
                Thread.onSpinWait();
            }
        });

        session.onDisconnect(mock(ServerResponse.class));
        poller.join(1000);

        assertThat(poller.isAlive(), is(false));
        assertThat(failure.get(), instanceOf(McpInternalException.class));
        assertThat(failure.get().getMessage(), is("Session disconnected"));
    }

    @Test
    void ignoresResponseReceivedBeforeRequestIsPrepared() {
        McpSession session = session(McpProtocolVersion.VERSION_2025_11_25, "{}");
        JsonObject unsolicited = JsonObject.builder().set("id", 0).build();
        session.acceptResponse(new McpResponseImpl(unsolicited, Context.create()));
        long requestId = session.jsonRpcId();
        session.prepareResponse(requestId);

        Optional<McpResponse> response = pollResponse(session, requestId, Duration.ZERO);

        assertThat(response.isEmpty(), is(true));
    }

    @Test
    void preservesFeaturesUntilResponseIsSent() {
        McpServerConfig config = McpServerConfig.create();
        McpTransportManager manager = mock(McpTransportManager.class);
        McpSession session = new McpSession(new McpSessions(config.maxSessionCount()),
                                            manager,
                                            config,
                                            "test-session");
        JsonRpcRequest request = mock(JsonRpcRequest.class);
        JsonRpcResponse response = mock(JsonRpcResponse.class);
        McpTransport transport = mock(McpStreamableHttpTransport.class);
        JsonValue requestId = JsonObject.builder().set("id", 1).build().value("id").orElseThrow();
        when(manager.create(request, response)).thenReturn(transport);
        session.createTransport(requestId, request, response);

        McpFeatures features = session.createFeatures(requestId);

        assertThat(session.findFeatures(requestId).orElseThrow(), sameInstance(features));
        session.send(requestId, response);
        assertThat(session.findFeatures(requestId).isEmpty(), is(true));
    }

    private static Optional<McpResponse> pollResponse(McpSession session, long requestId, Duration timeout) {
        try {
            return session.pollResponse(requestId, timeout);
        } finally {
            session.discardResponse(requestId);
        }
    }

    private static McpSession session(McpProtocolVersion protocolVersion, String capabilities) {
        McpServerConfig config = McpServerConfig.create();
        return session(protocolVersion, capabilities, config);
    }

    private static McpSession session(McpProtocolVersion protocolVersion,
                                      String capabilities,
                                      McpServerConfig config) {
        McpSessions sessions = new McpSessions(config.maxSessionCount());
        McpSession session = new McpSession(sessions,
                                            mock(McpTransportManager.class),
                                            config,
                                            "test-session");
        session.protocolVersion(protocolVersion);
        JsonObject object = JsonParser.create(capabilities).readJsonObject();
        session.initializeClientCapabilities(new McpParameters(JsonRpcParams.create(object)));
        return session;
    }
}
