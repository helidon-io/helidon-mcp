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

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import io.helidon.common.context.Context;
import io.helidon.http.sse.SseEvent;
import io.helidon.json.JsonNull;
import io.helidon.json.JsonObject;
import io.helidon.json.JsonParser;
import io.helidon.jsonrpc.core.JsonRpcParams;
import io.helidon.webserver.jsonrpc.JsonRpcResponse;
import io.helidon.webserver.sse.SseSink;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class McpElicitationTest {

    @Test
    void sendsUrlElicitationAndIgnoresResponseContent() {
        McpSession session = session("""
                {"elicitation": {"url": {}}}
                """);
        JsonObject clientResponse = JsonParser.create("""
                {
                  "jsonrpc": "2.0",
                  "id": 0,
                  "result": {
                    "action": "accept",
                    "content": {
                      "marker": "ignored-content"
                    }
                  }
                }
                """).readJsonObject();
        JsonRpcResponse httpResponse = mock(JsonRpcResponse.class);
        SseSink sink = mock(SseSink.class);
        when(httpResponse.sink(SseSink.TYPE)).thenReturn(sink);
        doAnswer(invocation -> {
            session.acceptResponse(new McpResponseImpl(clientResponse, Context.create()));
            return null;
        }).when(sink).emit(any(SseEvent.class));
        McpElicitation elicitation = new McpFeatures(session, new McpStreamableHttpTransport(httpResponse)).elicitation();

        McpElicitationResponse response = elicitation.requestUrl(request -> request
                .message("Authorize access")
                .elicitationId("elicitation-id")
                .url(URI.create("https://example.com/authorize"))
                .timeout(Duration.ofSeconds(1)));

        assertThat(response.action(), is(McpElicitationAction.ACCEPT));
        assertThat(response.content().isEmpty(), is(true));
        ArgumentCaptor<SseEvent> eventCaptor = ArgumentCaptor.forClass(SseEvent.class);
        verify(sink).emit(eventCaptor.capture());
        JsonObject payload = JsonParser.create((String) eventCaptor.getValue().data()).readJsonObject();
        JsonObject params = payload.objectValue("params").orElseThrow();
        assertThat(params.stringValue("mode").orElseThrow(), is("url"));
        assertThat(params.stringValue("elicitationId").orElseThrow(), is("elicitation-id"));
    }

    @Test
    void correlatesConcurrentElicitationResponsesByRequestId() throws Exception {
        McpSession session = session("""
                {"elicitation": {"form": {}, "url": {}}}
                """);
        JsonRpcResponse httpResponse = mock(JsonRpcResponse.class);
        SseSink sink = mock(SseSink.class);
        when(httpResponse.sink(SseSink.TYPE)).thenReturn(sink);
        CountDownLatch formRequestSent = new CountDownLatch(1);
        CountDownLatch urlRequestSent = new CountDownLatch(1);
        AtomicLong formRequestId = new AtomicLong(-1);
        AtomicLong urlRequestId = new AtomicLong(-1);
        doAnswer(invocation -> {
            SseEvent event = invocation.getArgument(0);
            JsonObject payload = JsonParser.create((String) event.data()).readJsonObject();
            String mode = payload.objectValue("params")
                    .orElseThrow()
                    .stringValue("mode")
                    .orElseThrow();
            long requestId = payload.longValue("id").orElseThrow();
            if (mode.equals("form")) {
                formRequestId.set(requestId);
                formRequestSent.countDown();
            } else if (mode.equals("url")) {
                urlRequestId.set(requestId);
                urlRequestSent.countDown();
            }
            return null;
        }).when(sink).emit(any(SseEvent.class));
        McpElicitation elicitation = new McpFeatures(session, new McpStreamableHttpTransport(httpResponse)).elicitation();

        CompletableFuture<McpElicitationResponse> formResponse = CompletableFuture.supplyAsync(() -> elicitation.request(request ->
                request.message("Provide details")
                        .schema("""
                                {"type": "object"}
                                """)
                        .timeout(Duration.ofSeconds(5))));
        assertThat(formRequestSent.await(1, TimeUnit.SECONDS), is(true));
        CompletableFuture<McpElicitationResponse> urlResponse = CompletableFuture.supplyAsync(() -> elicitation.requestUrl(request ->
                request.message("Authorize access")
                        .elicitationId("elicitation-id")
                        .url(URI.create("https://example.com/authorize"))
                        .timeout(Duration.ofSeconds(5))));
        assertThat(urlRequestSent.await(1, TimeUnit.SECONDS), is(true));

        session.acceptResponse(response(urlRequestId.get(), "decline"));
        session.acceptResponse(response(formRequestId.get(), "accept"));

        assertThat(formResponse.get(1, TimeUnit.SECONDS).action(), is(McpElicitationAction.ACCEPT));
        assertThat(urlResponse.get(1, TimeUnit.SECONDS).action(), is(McpElicitationAction.DECLINE));
    }

    @Test
    void cancellationWakesActiveFormAndUrlElicitationWaits() throws InterruptedException {
        McpSession session = session("""
                {"elicitation": {"form": {}, "url": {}}}
                """);
        JsonRpcResponse httpResponse = mock(JsonRpcResponse.class);
        SseSink sink = mock(SseSink.class);
        CountDownLatch requestsSent = new CountDownLatch(2);
        when(httpResponse.sink(SseSink.TYPE)).thenReturn(sink);
        doAnswer(invocation -> {
            requestsSent.countDown();
            return null;
        }).when(sink).emit(any());
        McpFeatures features = new McpFeatures(session, new McpStreamableHttpTransport(httpResponse));
        McpElicitation elicitation = features.elicitation();
        AtomicReference<Throwable> formFailure = new AtomicReference<>();
        AtomicReference<Throwable> urlFailure = new AtomicReference<>();
        Thread formThread = Thread.ofVirtual().start(() -> {
            try {
                elicitation.request(request -> request
                        .message("Provide details")
                        .schema("""
                                {"type": "object"}
                                """)
                        .timeout(Duration.ofMinutes(1)));
            } catch (Throwable e) {
                formFailure.set(e);
            }
        });
        Thread urlThread = Thread.ofVirtual().start(() -> {
            try {
                elicitation.requestUrl(request -> request
                        .message("Authorize access")
                        .elicitationId("elicitation-id")
                        .url(URI.create("https://example.com/authorize"))
                        .timeout(Duration.ofMinutes(1)));
            } catch (Throwable e) {
                urlFailure.set(e);
            }
        });
        assertThat(requestsSent.await(1, TimeUnit.SECONDS), is(true));

        features.cancellation().cancel(JsonNull.instance());
        formThread.join(1000);
        urlThread.join(1000);

        assertThat(formThread.isAlive(), is(false));
        assertThat(urlThread.isAlive(), is(false));
        assertThat(formFailure.get(), instanceOf(McpElicitationException.class));
        assertThat(urlFailure.get(), instanceOf(McpElicitationException.class));
        assertThat(formFailure.get().getMessage(), is("Elicitation request cancelled"));
        assertThat(urlFailure.get().getMessage(), is("Elicitation request cancelled"));
    }

    @Test
    void rejectsUrlElicitationForFormOnlyClient() {
        McpSession session = session("""
                {"elicitation": {"form": {}}}
                """);
        JsonRpcResponse httpResponse = mock(JsonRpcResponse.class);
        McpElicitation elicitation = new McpFeatures(session, new McpStreamableHttpTransport(httpResponse)).elicitation();
        McpElicitationUrlRequest request = McpElicitationUrlRequest.builder()
                .message("Authorize access")
                .elicitationId("elicitation-id")
                .url(URI.create("https://example.com/authorize"))
                .build();

        McpElicitationException exception = assertThrows(McpElicitationException.class,
                                                         () -> elicitation.requestUrl(request));

        assertThat(exception.getMessage(), is("URL elicitation feature is not supported by client"));
        verifyNoInteractions(httpResponse);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"elicitation\": {\"url\": {}}}", "{\"elicitation\": {\"form\": {}}}"})
    void rejectsNullUrlElicitationRequestAtApiBoundary(String capabilities) {
        McpSession session = session(capabilities);
        JsonRpcResponse httpResponse = mock(JsonRpcResponse.class);
        McpElicitation elicitation = new McpFeatures(session, new McpStreamableHttpTransport(httpResponse)).elicitation();

        NullPointerException exception = assertThrows(NullPointerException.class,
                                                      () -> elicitation.requestUrl((McpElicitationUrlRequest) null));

        assertThat(exception.getMessage(), is("request is null"));
        verifyNoInteractions(httpResponse);
    }

    private static McpSession session(String capabilities) {
        McpServerConfig config = McpServerConfig.create();
        McpSessions sessions = new McpSessions(config.maxSessionCount());
        McpSession session = new McpSession(sessions,
                                            mock(McpTransportManager.class),
                                            config,
                                            "test-session");
        session.protocolVersion(McpProtocolVersion.VERSION_2025_11_25);
        JsonObject object = JsonParser.create(capabilities).readJsonObject();
        session.initializeClientCapabilities(new McpParameters(JsonRpcParams.create(object)));
        return session;
    }

    private static McpResponse response(long requestId, String action) {
        JsonObject response = JsonObject.builder()
                .set("jsonrpc", "2.0")
                .set("id", requestId)
                .set("result", JsonObject.builder()
                        .set("action", action)
                        .build())
                .build();
        return new McpResponseImpl(response, Context.create());
    }
}
