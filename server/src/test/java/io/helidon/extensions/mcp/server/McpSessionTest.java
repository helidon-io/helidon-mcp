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

import io.helidon.json.JsonObject;
import io.helidon.json.JsonParser;
import io.helidon.jsonrpc.core.JsonRpcParams;
import io.helidon.webserver.jsonrpc.JsonRpcResponse;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

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

        assertThat(sampling.capabilities().contains(McpCapability.SAMPLING), is(true));
        assertThat(sampling.capabilities().contains(McpCapability.SAMPLING_CONTEXT), is(false));
        assertThat(samplingContext.capabilities().contains(McpCapability.SAMPLING), is(true));
        assertThat(samplingContext.capabilities().contains(McpCapability.SAMPLING_CONTEXT), is(true));
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
        JsonObject malformedResponse = JsonObject.builder()
                .set("id", "not-a-number")
                .build();
        JsonObject expectedResponse = JsonObject.builder()
                .set("id", 42)
                .build();

        session.acceptResponse(malformedResponse);
        session.acceptResponse(expectedResponse);

        JsonObject response = session.pollResponse(42, Duration.ofSeconds(1));
        assertThat(response, is(expectedResponse));
    }

    private static McpSession session(McpProtocolVersion protocolVersion, String capabilities) {
        McpServerConfig config = McpServerConfig.create();
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
