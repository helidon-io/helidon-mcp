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

import io.helidon.http.sse.SseEvent;
import io.helidon.json.JsonParser;
import io.helidon.jsonrpc.core.JsonRpcParams;
import io.helidon.webserver.jsonrpc.JsonRpcResponse;
import io.helidon.webserver.sse.SseSink;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class McpSamplingTest {

    @ParameterizedTest
    @EnumSource(value = McpIncludeContext.class, names = {"THIS_SERVER", "ALL_SERVERS"})
    void rejectsUndeclaredSamplingContext(McpIncludeContext includeContext) {
        McpSession session = session(McpProtocolVersion.VERSION_2025_11_25, false);
        JsonRpcResponse response = mock(JsonRpcResponse.class);
        McpSampling sampling = new McpSampling(session, new McpStreamableHttpTransport(response));
        McpSamplingRequest request = McpSamplingRequest.builder()
                .includeContext(includeContext)
                .build();

        McpSamplingException exception = assertThrows(McpSamplingException.class, () -> sampling.request(request));

        assertThat(exception.getMessage(), is("Sampling context is not supported by client"));
        verifyNoInteractions(response);
    }

    @Test
    void sendsDeclaredSamplingContext() {
        McpSamplingResponse response = requestContext(McpProtocolVersion.VERSION_2025_11_25,
                                                      true,
                                                      McpIncludeContext.THIS_SERVER);

        assertThat(response.model(), is("test-model"));
    }

    @Test
    void permitsNoneWithoutSamplingContext() {
        McpSamplingResponse response = requestContext(McpProtocolVersion.VERSION_2025_11_25,
                                                      false,
                                                      McpIncludeContext.NONE);

        assertThat(response.model(), is("test-model"));
    }

    @Test
    void preservesLegacySamplingContext() {
        McpSamplingResponse response = requestContext(McpProtocolVersion.VERSION_2025_06_18,
                                                      false,
                                                      McpIncludeContext.ALL_SERVERS);

        assertThat(response.model(), is("test-model"));
    }

    private static McpSamplingResponse requestContext(McpProtocolVersion protocolVersion,
                                                      boolean contextSupported,
                                                      McpIncludeContext includeContext) {
        McpSession session = session(protocolVersion, contextSupported);
        session.acceptResponse(JsonParser.create("""
                {
                  "jsonrpc": "2.0",
                  "id": 0,
                  "result": {
                    "model": "test-model",
                    "role": "assistant",
                    "content": {
                      "type": "text",
                      "text": "response"
                    },
                    "stopReason": "endTurn"
                  }
                }
                """).readJsonObject());
        JsonRpcResponse response = mock(JsonRpcResponse.class);
        SseSink sink = mock(SseSink.class);
        when(response.sink(SseSink.TYPE)).thenReturn(sink);
        McpSampling sampling = new McpSampling(session, new McpStreamableHttpTransport(response));
        McpSamplingRequest request = McpSamplingRequest.builder()
                .includeContext(includeContext)
                .build();

        McpSamplingResponse samplingResponse = sampling.request(request);
        ArgumentCaptor<SseEvent> eventCaptor = ArgumentCaptor.forClass(SseEvent.class);
        verify(sink).emit(eventCaptor.capture());
        JsonParser parser = JsonParser.create((String) eventCaptor.getValue().data());
        String serializedContext = parser.readJsonObject()
                .objectValue("params")
                .orElseThrow()
                .stringValue("includeContext")
                .orElseThrow();
        assertThat(serializedContext, is(includeContext.text()));
        return samplingResponse;
    }

    private static McpSession session(McpProtocolVersion protocolVersion, boolean contextSupported) {
        McpServerConfig config = McpServerConfig.create();
        McpSessions sessions = new McpSessions(config.maxSessionCount());
        McpSession session = new McpSession(sessions,
                                            mock(McpTransportManager.class),
                                            config,
                                            "test-session");
        session.protocolVersion(protocolVersion);
        String capabilities = contextSupported
                ? """
                        {"sampling": {"context": {}}}
                        """
                : """
                        {"sampling": {}}
                        """;
        session.initializeClientCapabilities(new McpParameters(
                JsonRpcParams.create(JsonParser.create(capabilities).readJsonObject())));
        return session;
    }
}
