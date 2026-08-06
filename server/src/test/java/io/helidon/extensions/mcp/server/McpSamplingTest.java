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

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import io.helidon.common.context.Context;
import io.helidon.http.sse.SseEvent;
import io.helidon.json.JsonObject;
import io.helidon.json.JsonParser;
import io.helidon.jsonrpc.core.JsonRpcParams;
import io.helidon.webserver.jsonrpc.JsonRpcResponse;
import io.helidon.webserver.sse.SseSink;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class McpSamplingTest {

    @Test
    void samplingRequestBuilderDefaultsToolOptions() {
        McpSamplingRequest request = McpSamplingRequest.builder().build();

        assertThat(request.messages().isEmpty(), is(true));
        assertThat(request.tools().isEmpty(), is(true));
        assertThat(request.toolChoice().isEmpty(), is(true));
        assertThat(request.usesTool(), is(false));
        assertThat(request.usesContext(), is(false));
    }

    @Test
    void samplingRequestBuilderKeepsCustomToolOptions() {
        McpSamplingMessage message = message(McpRole.USER,
                                             McpSamplingTextContent.create("What is the weather?"));

        McpSamplingRequest request = McpSamplingRequest.builder()
                .addMessage(message)
                .addTool("weather")
                .toolChoice(McpToolChoice.REQUIRED)
                .build();

        assertThat(request.messages().size(), is(1));
        assertThat(request.messages().getFirst(), sameInstance(message));
        assertThat(request.tools().size(), is(1));
        assertThat(request.tools().getFirst(), is("weather"));
        assertThat(request.toolChoice().orElseThrow(), is(McpToolChoice.REQUIRED));
        assertThat(request.usesTool(), is(true));
    }

    @Test
    void samplingRequestDetectsToolChoiceAndToolMessages() {
        McpSamplingRequest ordinary = McpSamplingRequest.builder()
                .addMessage(message(McpRole.USER, McpSamplingTextContent.create("Hello")))
                .build();
        McpSamplingRequest withChoice = McpSamplingRequest.builder()
                .toolChoice(McpToolChoice.NONE)
                .build();
        McpSamplingRequest withToolMessage = McpSamplingRequest.builder()
                .addMessage(message(McpRole.ASSISTANT, McpSamplingToolUseContent.builder()
                        .id("call-1")
                        .name("weather")
                        .input(new McpParameters(JsonObject.empty()))
                        .build()))
                .build();
        McpSamplingRequest withToolResult = McpSamplingRequest.builder()
                .addMessage(message(McpRole.USER, McpSamplingToolResultContent.builder()
                        .toolUseId("call-1")
                        .result(McpToolResult.create("18 C"))
                        .build()))
                .build();

        assertThat(ordinary.usesTool(), is(false));
        assertThat(withChoice.usesTool(), is(true));
        assertThat(withToolMessage.usesTool(), is(true));
        assertThat(withToolResult.usesTool(), is(true));
    }

    @ParameterizedTest
    @EnumSource(McpIncludeContext.class)
    void samplingRequestDetectsContextUse(McpIncludeContext includeContext) {
        McpSamplingRequest request = McpSamplingRequest.builder()
                .includeContext(includeContext)
                .build();

        assertThat(request.usesContext(), is(includeContext != McpIncludeContext.NONE));
    }

    @Test
    void reportsSamplingToolsCapability() {
        McpSampling basic = sampling(session(McpProtocolVersion.VERSION_2025_11_25, """
                {"sampling": {}}
                """), new McpStreamableHttpTransport(mock(JsonRpcResponse.class)));
        McpSampling withTools = sampling(session(McpProtocolVersion.VERSION_2025_11_25, """
                {"sampling": {"tools": {}}}
                """), new McpStreamableHttpTransport(mock(JsonRpcResponse.class)));

        assertThat(basic.enabledTools(), is(false));
        assertThat(withTools.enabledTools(), is(true));
    }

    @Test
    void reportsSamplingContextCapability() {
        McpSampling basic = sampling(session(McpProtocolVersion.VERSION_2025_11_25, """
                {"sampling": {}}
                """), new McpStreamableHttpTransport(mock(JsonRpcResponse.class)));
        McpSampling withContext = sampling(session(McpProtocolVersion.VERSION_2025_11_25, """
                {"sampling": {"context": {}}}
                """), new McpStreamableHttpTransport(mock(JsonRpcResponse.class)));

        assertThat(basic.enabledContext(), is(false));
        assertThat(withContext.enabledContext(), is(true));
    }

    @Test
    void sendsDeclaredSamplingToolsAndToolChoice() {
        McpTool weather = new TestTool("weather", "Looks up the weather", "{\"type\":\"object\"}");
        McpTool time = new TestTool("time", "Looks up the time", "{\"type\":\"object\"}");
        McpSession session = session(McpProtocolVersion.VERSION_2025_11_25, """
                {"sampling": {"tools": {}}}
                """, time, weather);
        session.prepareResponse(0);
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
        McpSampling sampling = sampling(session, new McpStreamableHttpTransport(response));
        McpSamplingRequest request = McpSamplingRequest.builder()
                .addTool(weather.name())
                .addTool(time.name())
                .toolChoice(McpToolChoice.AUTO)
                .build();

        McpSamplingResponse samplingResponse = sampling.request(request);

        ArgumentCaptor<SseEvent> eventCaptor = ArgumentCaptor.forClass(SseEvent.class);
        verify(sink).emit(eventCaptor.capture());
        JsonObject params = JsonParser.create((String) eventCaptor.getValue().data())
                .readJsonObject()
                .objectValue("params")
                .orElseThrow();
        var serializedTools = params.arrayValue("tools").orElseThrow().values();
        JsonObject serializedWeather = serializedTools.getFirst().asObject();
        JsonObject serializedTime = serializedTools.getLast().asObject();
        assertThat(samplingResponse.model(), is("test-model"));
        assertThat(serializedTools.size(), is(2));
        assertThat(serializedWeather.stringValue("name").orElseThrow(), is("weather"));
        assertThat(serializedWeather.stringValue("description").orElseThrow(), is("Looks up the weather"));
        assertThat(serializedTime.stringValue("name").orElseThrow(), is("time"));
        assertThat(serializedTime.stringValue("description").orElseThrow(), is("Looks up the time"));
        assertThat(params.objectValue("toolChoice").orElseThrow()
                           .stringValue("mode").orElseThrow(),
                   is("auto"));
    }

    @Test
    void executesSamplingToolAndReturnsFinalResponse() {
        AtomicReference<McpToolRequest> toolRequest = new AtomicReference<>();
        McpTool tool = new TestTool("weather", "Looks up the weather", "{\"type\":\"object\"}", request -> {
            toolRequest.set(request);
            return McpToolResult.create("18 C");
        });
        McpSession session = session(McpProtocolVersion.VERSION_2025_11_25, """
                {"sampling": {"tools": {}}}
                """, tool);
        acceptSamplingResponse(session, 0, """
                [
                  {"type": "text", "text": "Checking the weather"},
                  {
                    "type": "tool_use",
                    "id": "call-1",
                    "name": "weather",
                    "input": {"city": "Prague"},
                    "_meta": {"trace": "sample-1"}
                  }
                ]
                """, "toolUse");
        acceptSamplingResponse(session, 1, """
                {"type": "text", "text": "It is 18 C in Prague"}
                """, "endTurn");
        JsonRpcResponse response = mock(JsonRpcResponse.class);
        SseSink sink = mock(SseSink.class);
        when(response.sink(SseSink.TYPE)).thenReturn(sink);
        McpTransport transport = new McpStreamableHttpTransport(response);
        Context requestContext = Context.create();
        McpFeatures features = new McpFeatures(session, transport, requestContext);
        McpSampling sampling = features.sampling();
        McpSamplingRequest request = McpSamplingRequest.builder()
                .addTextMessage(McpRole.USER, "What is the weather in Prague?")
                .addTool(tool.name())
                .toolChoice(McpToolChoice.REQUIRED)
                .build();

        McpSamplingResponse samplingResponse = sampling.request(request);

        assertThat(samplingResponse.asTextContent().text(), is("It is 18 C in Prague"));
        assertThat(samplingResponse.stopReason().orElseThrow(), is(McpStopReason.END_TURN));
        assertThat(toolRequest.get().name(), is("weather"));
        assertThat(toolRequest.get().arguments().get("city").asString().get(), is("Prague"));
        assertThat(toolRequest.get().meta().get("trace").asString().get(), is("sample-1"));
        assertThat(toolRequest.get().features(), sameInstance(features));
        assertThat(toolRequest.get().sessionContext(), sameInstance(session.context()));
        assertThat(toolRequest.get().requestContext(), sameInstance(requestContext));
        assertThat(request.messages().size(), is(1));
        assertThat(request.toolChoice().orElseThrow(), is(McpToolChoice.REQUIRED));

        List<JsonObject> sent = sentSamplingRequests(sink, 2);
        assertThat(sent.get(0).longValue("id").orElseThrow(), is(0L));
        assertThat(sent.get(1).longValue("id").orElseThrow(), is(1L));
        JsonObject secondParams = sent.get(1).objectValue("params").orElseThrow();
        assertThat(secondParams.objectValue("toolChoice").orElseThrow()
                           .stringValue("mode").orElseThrow(),
                   is("auto"));
        var messages = secondParams.arrayValue("messages").orElseThrow().values();
        assertThat(messages.size(), is(3));
        JsonObject assistantMessage = messages.get(1).asObject();
        assertThat(assistantMessage.stringValue("role").orElseThrow(), is("assistant"));
        assertThat(assistantMessage.arrayValue("content").orElseThrow().size(), is(2));
        JsonObject resultMessage = messages.get(2).asObject();
        assertThat(resultMessage.stringValue("role").orElseThrow(), is("user"));
        JsonObject result = resultMessage.objectValue("content").orElseThrow();
        assertThat(result.stringValue("toolUseId").orElseThrow(), is("call-1"));
        assertThat(result.booleanValue("isError").orElseThrow(), is(false));
        assertThat(result.arrayValue("content").orElseThrow().values().get(0).asObject()
                           .stringValue("text").orElseThrow(),
                   is("18 C"));
    }

    @Test
    void preservesSamplingRequestOptionsAcrossToolRounds() {
        McpTool tool = new TestTool("weather", "Looks up the weather", "");
        McpSession session = session(McpProtocolVersion.VERSION_2025_11_25, """
                {"sampling": {"tools": {}, "context": {}}}
                """, tool);
        acceptSamplingResponse(session, 0, """
                {"type": "tool_use", "id": "call-1", "name": "weather", "input": {}}
                """, "toolUse");
        acceptSamplingResponse(session, 1, """
                {"type": "text", "text": "Final response"}
                """, "endTurn");
        JsonRpcResponse response = mock(JsonRpcResponse.class);
        SseSink sink = mock(SseSink.class);
        when(response.sink(SseSink.TYPE)).thenReturn(sink);
        McpSampling sampling = sampling(session, new McpStreamableHttpTransport(response));
        McpSamplingRequest request = McpSamplingRequest.builder()
                .addTextMessage(McpRole.USER, "Use every request option")
                .addTool(tool.name())
                .toolChoice(McpToolChoice.AUTO)
                .hints(List.of("test-model"))
                .costPriority(0.1)
                .speedPriority(0.2)
                .intelligencePriority(0.3)
                .systemPrompt("Test system prompt")
                .temperature(0.4)
                .maxTokens(42)
                .stopSequences(List.of("stop"))
                .includeContext(McpIncludeContext.THIS_SERVER)
                .metadata(Map.of("requestId", "test-request"))
                .build();

        McpSamplingResponse samplingResponse = sampling.request(request);

        assertThat(samplingResponse.asTextContent().text(), is("Final response"));
        List<JsonObject> sent = sentSamplingRequests(sink, 2);
        JsonObject first = sent.get(0).objectValue("params").orElseThrow();
        JsonObject second = sent.get(1).objectValue("params").orElseThrow();
        for (String key : List.of("modelPreference",
                                  "maxTokens",
                                  "systemPrompt",
                                  "temperature",
                                  "includeContext",
                                  "stopSequences",
                                  "metadata",
                                  "tools",
                                  "toolChoice")) {
            assertThat(second.value(key).orElseThrow().toString(),
                       is(first.value(key).orElseThrow().toString()));
        }
        assertThat(first.arrayValue("messages").orElseThrow().size(), is(1));
        assertThat(second.arrayValue("messages").orElseThrow().size(), is(3));
        assertThat(second.arrayValue("messages").orElseThrow().values().getFirst().toString(),
                   is(first.arrayValue("messages").orElseThrow().values().getFirst().toString()));
        assertThat(request.messages().size(), is(1));
    }

    @Test
    void executesParallelToolUsesAndContinuesAfterToolErrors() {
        AtomicInteger workingInvocations = new AtomicInteger();
        McpTool broken = new TestTool("broken", "Fails", "", request -> {
            throw new IllegalStateException("provider unavailable");
        });
        McpTool working = new TestTool("working", "Works", "", request -> {
            workingInvocations.incrementAndGet();
            return McpToolResult.create("success");
        });
        McpTool empty = new TestTool("empty", "Returns null", "", request -> null);
        McpTool unselected = new TestTool("missing", "Not selected", "");
        McpSession session = session(McpProtocolVersion.VERSION_2025_11_25, """
                {"sampling": {"tools": {}}}
                """, broken, working, empty, unselected);
        acceptSamplingResponse(session, 0, """
                [
                  {"type": "tool_use", "id": "call-1", "name": "broken", "input": {}},
                  {"type": "tool_use", "id": "call-2", "name": "working", "input": {}},
                  {"type": "tool_use", "id": "call-3", "name": "missing", "input": {}},
                  {"type": "tool_use", "id": "call-4", "name": "empty", "input": {}}
                ]
                """, "toolUse");
        acceptSamplingResponse(session, 1, """
                {"type": "text", "text": "Recovered from the tool errors"}
                """, "endTurn");
        JsonRpcResponse response = mock(JsonRpcResponse.class);
        SseSink sink = mock(SseSink.class);
        when(response.sink(SseSink.TYPE)).thenReturn(sink);
        McpSampling sampling = sampling(session, new McpStreamableHttpTransport(response));
        McpSamplingRequest request = McpSamplingRequest.builder()
                .addTextMessage(McpRole.USER, "Use the tools")
                .addTool(broken.name())
                .addTool(working.name())
                .addTool(empty.name())
                .build();

        McpSamplingResponse samplingResponse = sampling.request(request);

        assertThat(samplingResponse.asTextContent().text(), is("Recovered from the tool errors"));
        assertThat(workingInvocations.get(), is(1));
        List<JsonObject> sent = sentSamplingRequests(sink, 2);
        var messages = sent.get(1).objectValue("params").orElseThrow()
                .arrayValue("messages").orElseThrow().values();
        var results = messages.get(2).asObject().arrayValue("content").orElseThrow().values();
        assertThat(results.size(), is(4));
        assertToolResult(results.get(0).asObject(), "call-1", true, "failed");
        assertToolResult(results.get(1).asObject(), "call-2", false, "success");
        assertToolResult(results.get(2).asObject(), "call-3", true, "is not available");
        assertToolResult(results.get(3).asObject(), "call-4", true, "returned no result");
    }

    @Test
    void executesMultipleSamplingToolRounds() {
        AtomicInteger invocations = new AtomicInteger();
        McpTool tool = new TestTool("counter", "Counts", "", request ->
                McpToolResult.create(Integer.toString(invocations.incrementAndGet())));
        McpSession session = session(McpProtocolVersion.VERSION_2025_11_25, """
                {"sampling": {"tools": {}}}
                """, tool);
        acceptSamplingResponse(session, 0, """
                {"type": "tool_use", "id": "call-1", "name": "counter", "input": {}}
                """, "toolUse");
        acceptSamplingResponse(session, 1, """
                {"type": "tool_use", "id": "call-2", "name": "counter", "input": {}}
                """, "toolUse");
        acceptSamplingResponse(session, 2, """
                {"type": "text", "text": "All tools completed"}
                """, "endTurn");
        JsonRpcResponse response = mock(JsonRpcResponse.class);
        SseSink sink = mock(SseSink.class);
        when(response.sink(SseSink.TYPE)).thenReturn(sink);
        McpSampling sampling = sampling(session, new McpStreamableHttpTransport(response));
        McpSamplingRequest request = McpSamplingRequest.builder()
                .addTextMessage(McpRole.USER, "Count twice")
                .addTool(tool.name())
                .build();

        McpSamplingResponse samplingResponse = sampling.request(request);

        assertThat(samplingResponse.asTextContent().text(), is("All tools completed"));
        assertThat(invocations.get(), is(2));
        List<JsonObject> sent = sentSamplingRequests(sink, 3);
        var messages = sent.get(2).objectValue("params").orElseThrow()
                .arrayValue("messages").orElseThrow().values();
        assertThat(messages.size(), is(5));
        assertThat(messages.get(0).asObject().stringValue("role").orElseThrow(), is("user"));
        assertThat(messages.get(1).asObject().stringValue("role").orElseThrow(), is("assistant"));
        assertThat(messages.get(2).asObject().stringValue("role").orElseThrow(), is("user"));
        assertThat(messages.get(3).asObject().stringValue("role").orElseThrow(), is("assistant"));
        assertThat(messages.get(4).asObject().stringValue("role").orElseThrow(), is("user"));
        assertThat(request.messages().size(), is(1));
    }

    @Test
    void rejectsReplayedSamplingToolUseIdentifier() {
        AtomicInteger invocations = new AtomicInteger();
        McpTool tool = new TestTool("counter", "Counts", "", request ->
                McpToolResult.create(Integer.toString(invocations.incrementAndGet())));
        McpSession session = session(McpProtocolVersion.VERSION_2025_11_25, """
                {"sampling": {"tools": {}}}
                """, tool);
        acceptSamplingResponse(session, 0, """
                {"type": "tool_use", "id": "call-1", "name": "counter", "input": {}}
                """, "toolUse");
        acceptSamplingResponse(session, 1, """
                {"type": "tool_use", "id": "call-1", "name": "counter", "input": {}}
                """, "toolUse");
        JsonRpcResponse response = mock(JsonRpcResponse.class);
        SseSink sink = mock(SseSink.class);
        when(response.sink(SseSink.TYPE)).thenReturn(sink);
        McpSampling sampling = sampling(session, new McpStreamableHttpTransport(response));
        McpSamplingRequest request = McpSamplingRequest.builder()
                .addTool(tool.name())
                .build();

        McpSamplingException exception = assertThrows(McpSamplingException.class, () -> sampling.request(request));

        assertThat(exception.getMessage(), is("Sampling tool use identifier was reused: call-1"));
        assertThat(invocations.get(), is(1));
        sentSamplingRequests(sink, 2);
    }

    @Test
    void rejectsSamplingToolUseIdentifierReplayedFromRequestHistory() {
        AtomicInteger invocations = new AtomicInteger();
        McpTool tool = new TestTool("counter", "Counts", "", request ->
                McpToolResult.create(Integer.toString(invocations.incrementAndGet())));
        McpSession session = session(McpProtocolVersion.VERSION_2025_11_25, """
                {"sampling": {"tools": {}}}
                """, tool);
        acceptSamplingResponse(session, 0, """
                {"type": "tool_use", "id": "call-1", "name": "counter", "input": {}}
                """, "toolUse");
        JsonRpcResponse response = mock(JsonRpcResponse.class);
        SseSink sink = mock(SseSink.class);
        when(response.sink(SseSink.TYPE)).thenReturn(sink);
        McpSampling sampling = sampling(session, new McpStreamableHttpTransport(response));
        McpSamplingRequest request = McpSamplingRequest.builder()
                .addTextMessage(McpRole.USER, "Continue the conversation")
                .addMessage(message(McpRole.ASSISTANT, McpSamplingToolUseContent.builder()
                        .id("call-1")
                        .name("counter")
                        .input(new McpParameters(JsonObject.empty()))
                        .build()))
                .addMessage(message(McpRole.USER, McpSamplingToolResultContent.builder()
                        .toolUseId("call-1")
                        .result(McpToolResult.create("previous result"))
                        .build()))
                .addTool(tool.name())
                .build();

        McpSamplingException exception = assertThrows(McpSamplingException.class, () -> sampling.request(request));

        assertThat(exception.getMessage(), is("Sampling tool use identifier was reused: call-1"));
        assertThat(invocations.get(), is(0));
        sentSamplingRequests(sink, 1);
    }

    @Test
    void rejectsDuplicateSamplingToolUseIdentifiersBeforeInvocation() {
        AtomicInteger invocations = new AtomicInteger();
        McpTool tool = new TestTool("counter", "Counts", "", request ->
                McpToolResult.create(Integer.toString(invocations.incrementAndGet())));
        McpSession session = session(McpProtocolVersion.VERSION_2025_11_25, """
                {"sampling": {"tools": {}}}
                """, tool);
        acceptSamplingResponse(session, 0, """
                [
                  {"type": "tool_use", "id": "call-1", "name": "counter", "input": {}},
                  {"type": "tool_use", "id": "call-1", "name": "counter", "input": {}}
                ]
                """, "toolUse");
        JsonRpcResponse response = mock(JsonRpcResponse.class);
        SseSink sink = mock(SseSink.class);
        when(response.sink(SseSink.TYPE)).thenReturn(sink);
        McpSampling sampling = sampling(session, new McpStreamableHttpTransport(response));

        McpSamplingException exception = assertThrows(McpSamplingException.class,
                                                      () -> sampling.request(req -> req.addTool(tool.name())));

        assertThat(exception.getMessage(), is("Sampling tool use identifiers must be unique within a message"));
        assertThat(invocations.get(), is(0));
        sentSamplingRequests(sink, 1);
    }

    @Test
    void rejectsDuplicateSamplingToolNamesBeforeSending() {
        McpTool tool = new TestTool("duplicate", "Duplicate", "");
        JsonRpcResponse response = mock(JsonRpcResponse.class);
        McpSampling sampling = sampling(session(McpProtocolVersion.VERSION_2025_11_25, """
                {"sampling": {"tools": {}}}
                """, tool), new McpStreamableHttpTransport(response));
        McpSamplingRequest request = McpSamplingRequest.builder()
                .addTool(tool.name())
                .addTool(tool.name())
                .build();

        McpSamplingException exception = assertThrows(McpSamplingException.class, () -> sampling.request(request));

        assertThat(exception.getMessage(), is("Sampling tool names must be unique: duplicate"));
        verifyNoInteractions(response);
    }

    @Test
    void rejectsUnregisteredSamplingToolNameBeforeSending() {
        McpTool tool = new TestTool("weather", "Looks up the weather", "");
        JsonRpcResponse response = mock(JsonRpcResponse.class);
        McpSampling sampling = sampling(session(McpProtocolVersion.VERSION_2025_11_25, """
                {"sampling": {"tools": {}}}
                """, tool), new McpStreamableHttpTransport(response));
        McpSamplingRequest request = McpSamplingRequest.builder()
                .addTool("missing")
                .build();

        McpSamplingException exception = assertThrows(McpSamplingException.class, () -> sampling.request(request));

        assertThat(exception.getMessage(), is("Sampling tool is not registered: missing"));
        verifyNoInteractions(response);
    }

    @Test
    void rejectsAmbiguousRegisteredSamplingToolNameBeforeSending() {
        McpTool first = new TestTool("duplicate", "First", "");
        McpTool second = new TestTool("duplicate", "Second", "");
        JsonRpcResponse response = mock(JsonRpcResponse.class);
        McpSampling sampling = sampling(session(McpProtocolVersion.VERSION_2025_11_25, """
                {"sampling": {"tools": {}}}
                """, first, second), new McpStreamableHttpTransport(response));
        McpSamplingRequest request = McpSamplingRequest.builder()
                .addTool("duplicate")
                .build();

        McpSamplingException exception = assertThrows(McpSamplingException.class, () -> sampling.request(request));

        assertThat(exception.getMessage(), is("Registered sampling tool names must be unique: duplicate"));
        verifyNoInteractions(response);
    }

    @Test
    void doesNotOfferRegisteredToolsForEmptySelection() {
        McpTool tool = new TestTool("weather", "Looks up the weather", "");
        McpSession session = session(McpProtocolVersion.VERSION_2025_11_25, """
                {"sampling": {"tools": {}}}
                """, tool);
        acceptSamplingResponse(session, 0, """
                {"type": "text", "text": "No tools needed"}
                """, "endTurn");
        JsonRpcResponse response = mock(JsonRpcResponse.class);
        SseSink sink = mock(SseSink.class);
        when(response.sink(SseSink.TYPE)).thenReturn(sink);
        McpSampling sampling = sampling(session, new McpStreamableHttpTransport(response));
        McpSamplingRequest request = McpSamplingRequest.builder()
                .tools(List.of())
                .build();

        McpSamplingResponse samplingResponse = sampling.request(request);

        assertThat(samplingResponse.asTextContent().text(), is("No tools needed"));
        assertThat(request.usesTool(), is(false));
        JsonObject params = sentSamplingRequests(sink, 1).getFirst().objectValue("params").orElseThrow();
        assertThat(params.value("tools").isEmpty(), is(true));
    }

    @Test
    void rejectsToolUseWhenToolChoiceIsNone() {
        AtomicInteger invocations = new AtomicInteger();
        McpTool tool = new TestTool("weather", "Looks up the weather", "", request -> {
            invocations.incrementAndGet();
            return McpToolResult.create("unused");
        });
        McpSession session = session(McpProtocolVersion.VERSION_2025_11_25, """
                {"sampling": {"tools": {}}}
                """, tool);
        acceptSamplingResponse(session, 0, """
                {"type": "tool_use", "id": "call-1", "name": "weather", "input": {}}
                """, "toolUse");
        JsonRpcResponse response = mock(JsonRpcResponse.class);
        SseSink sink = mock(SseSink.class);
        when(response.sink(SseSink.TYPE)).thenReturn(sink);
        McpSampling sampling = sampling(session, new McpStreamableHttpTransport(response));
        McpSamplingRequest request = McpSamplingRequest.builder()
                .addTool(tool.name())
                .toolChoice(McpToolChoice.NONE)
                .build();

        McpSamplingException exception = assertThrows(McpSamplingException.class, () -> sampling.request(request));

        assertThat(exception.getMessage(), is("Sampling client returned a tool use when tool choice is none"));
        assertThat(invocations.get(), is(0));
        sentSamplingRequests(sink, 1);
    }

    @Test
    void limitsSamplingToolIterationsAndRequestsFinalResponse() {
        AtomicInteger invocations = new AtomicInteger();
        McpTool tool = new TestTool("counter", "Counts", "", request ->
                McpToolResult.create(Integer.toString(invocations.incrementAndGet())));
        McpServerConfig config = McpServerFeature.builder()
                .maxSamplingToolIterations(1)
                .addTool(tool)
                .buildPrototype();
        McpSession session = session(McpProtocolVersion.VERSION_2025_11_25,
                                     """
                                             {"sampling": {"tools": {}}}
                                             """,
                                     config);
        for (int id = 0; id <= 1; id++) {
            acceptSamplingResponse(session, id, """
                    {"type": "tool_use", "id": "call-%d", "name": "counter", "input": {}}
                    """.formatted(id), "toolUse");
        }
        JsonRpcResponse response = mock(JsonRpcResponse.class);
        SseSink sink = mock(SseSink.class);
        when(response.sink(SseSink.TYPE)).thenReturn(sink);
        McpSampling sampling = sampling(session, new McpStreamableHttpTransport(response));
        McpSamplingRequest request = McpSamplingRequest.builder()
                .addTool(tool.name())
                .build();

        McpSamplingException exception = assertThrows(McpSamplingException.class, () -> sampling.request(request));

        assertThat(exception.getMessage(), is("Sampling tool iteration limit reached"));
        assertThat(invocations.get(), is(1));
        List<JsonObject> sent = sentSamplingRequests(sink, 2);
        assertThat(sent.getLast().objectValue("params").orElseThrow()
                           .objectValue("toolChoice").orElseThrow()
                           .stringValue("mode").orElseThrow(),
                   is("none"));
    }

    @Test
    void returnsFinalResponseAfterMaximumSamplingToolIterations() {
        AtomicInteger invocations = new AtomicInteger();
        McpTool tool = new TestTool("counter", "Counts", "", request ->
                McpToolResult.create(Integer.toString(invocations.incrementAndGet())));
        McpSession session = session(McpProtocolVersion.VERSION_2025_11_25, """
                {"sampling": {"tools": {}}}
                """, tool);
        for (int id = 0; id < 10; id++) {
            acceptSamplingResponse(session, id, """
                    {"type": "tool_use", "id": "call-%d", "name": "counter", "input": {}}
                    """.formatted(id), "toolUse");
        }
        acceptSamplingResponse(session, 10, """
                {"type": "text", "text": "Reached the final response"}
                """, "endTurn");
        JsonRpcResponse response = mock(JsonRpcResponse.class);
        SseSink sink = mock(SseSink.class);
        when(response.sink(SseSink.TYPE)).thenReturn(sink);
        McpSampling sampling = sampling(session, new McpStreamableHttpTransport(response));
        McpSamplingRequest request = McpSamplingRequest.builder()
                .addTool(tool.name())
                .build();

        McpSamplingResponse samplingResponse = sampling.request(request);

        assertThat(samplingResponse.asTextContent().text(), is("Reached the final response"));
        assertThat(invocations.get(), is(10));
        List<JsonObject> sent = sentSamplingRequests(sink, 11);
        assertThat(sent.getLast().objectValue("params").orElseThrow()
                           .objectValue("toolChoice").orElseThrow()
                           .stringValue("mode").orElseThrow(),
                   is("none"));
    }

    @Test
    void rejectsToolUseResponseWithoutSamplingToolsCapability() {
        McpSession session = session(McpProtocolVersion.VERSION_2025_11_25, """
                {"sampling": {}}
                """);
        acceptSamplingResponse(session, 0, """
                {"type": "tool_use", "id": "call-1", "name": "weather", "input": {}}
                """, "toolUse");
        JsonRpcResponse response = mock(JsonRpcResponse.class);
        SseSink sink = mock(SseSink.class);
        when(response.sink(SseSink.TYPE)).thenReturn(sink);
        McpSampling sampling = sampling(session, new McpStreamableHttpTransport(response));
        McpSamplingRequest request = McpSamplingRequest.builder()
                .addTextMessage(McpRole.USER, "Hello")
                .build();

        McpSamplingException exception = assertThrows(McpSamplingException.class, () -> sampling.request(request));

        assertThat(exception.getMessage(), is("Sampling tools are not supported by client"));
        sentSamplingRequests(sink, 1);
    }

    @Test
    void rejectsResponseThatOmitsRequiredToolUse() {
        McpTool tool = new TestTool("weather", "Looks up the weather", "");
        McpSession session = session(McpProtocolVersion.VERSION_2025_11_25, """
                {"sampling": {"tools": {}}}
                """, tool);
        acceptSamplingResponse(session, 0, """
                {"type": "text", "text": "No tool was used"}
                """, "endTurn");
        JsonRpcResponse response = mock(JsonRpcResponse.class);
        SseSink sink = mock(SseSink.class);
        when(response.sink(SseSink.TYPE)).thenReturn(sink);
        McpSampling sampling = sampling(session, new McpStreamableHttpTransport(response));
        McpSamplingRequest request = McpSamplingRequest.builder()
                .addTool(tool.name())
                .toolChoice(McpToolChoice.REQUIRED)
                .build();

        McpSamplingException exception = assertThrows(McpSamplingException.class, () -> sampling.request(request));

        assertThat(exception.getMessage(), is("Sampling response did not use a required tool"));
        sentSamplingRequests(sink, 1);
    }

    @Test
    void rejectsToolUseStopReasonWithoutToolUseContent() {
        McpTool tool = new TestTool("weather", "Looks up the weather", "");
        McpSession session = session(McpProtocolVersion.VERSION_2025_11_25, """
                {"sampling": {"tools": {}}}
                """, tool);
        acceptSamplingResponse(session, 0, """
                {"type": "text", "text": "Malformed response"}
                """, "toolUse");
        JsonRpcResponse response = mock(JsonRpcResponse.class);
        SseSink sink = mock(SseSink.class);
        when(response.sink(SseSink.TYPE)).thenReturn(sink);
        McpSampling sampling = sampling(session, new McpStreamableHttpTransport(response));
        McpSamplingRequest request = McpSamplingRequest.builder()
                .addTool(tool.name())
                .build();

        McpSamplingException exception = assertThrows(McpSamplingException.class, () -> sampling.request(request));

        assertThat(exception.getMessage(), is("Sampling response stopped for tool use without tool use content"));
        sentSamplingRequests(sink, 1);
    }

    @Test
    void rejectsToolNamesWithoutSamplingToolsCapability() {
        JsonRpcResponse response = mock(JsonRpcResponse.class);
        McpSampling sampling = sampling(session(McpProtocolVersion.VERSION_2025_11_25, """
                {"sampling": {}}
                """), new McpStreamableHttpTransport(response));
        McpSamplingRequest request = McpSamplingRequest.builder()
                .addTool("weather")
                .build();

        McpSamplingException exception = assertThrows(McpSamplingException.class, () -> sampling.request(request));

        assertThat(exception.getMessage(), is("Sampling tools are not supported by client"));
        verifyNoInteractions(response);
    }

    @Test
    void rejectsRequestWithoutSamplingCapability() {
        JsonRpcResponse response = mock(JsonRpcResponse.class);
        McpSampling sampling = sampling(session(McpProtocolVersion.VERSION_2025_11_25, "{}"),
                                        new McpStreamableHttpTransport(response));

        McpSamplingException exception = assertThrows(McpSamplingException.class,
                                                       () -> sampling.request(McpSamplingRequest.builder().build()));

        assertThat(sampling.enabled(), is(false));
        assertThat(exception.getMessage(), is("Sampling feature is not supported by client"));
        verifyNoInteractions(response);
    }

    @Test
    void rejectsToolChoiceWithoutSamplingToolsCapability() {
        JsonRpcResponse response = mock(JsonRpcResponse.class);
        McpSampling sampling = sampling(session(McpProtocolVersion.VERSION_2025_11_25, """
                {"sampling": {}}
                """), new McpStreamableHttpTransport(response));
        McpSamplingRequest request = McpSamplingRequest.builder()
                .toolChoice(McpToolChoice.NONE)
                .build();

        McpSamplingException exception = assertThrows(McpSamplingException.class, () -> sampling.request(request));

        assertThat(exception.getMessage(), is("Sampling tools are not supported by client"));
        verifyNoInteractions(response);
    }

    @Test
    void rejectsToolMessagesWithoutSamplingToolsCapability() {
        JsonRpcResponse response = mock(JsonRpcResponse.class);
        McpSampling sampling = sampling(session(McpProtocolVersion.VERSION_2025_11_25, """
                {"sampling": {}}
                """), new McpStreamableHttpTransport(response));
        McpSamplingRequest request = McpSamplingRequest.builder()
                .addMessage(message(McpRole.ASSISTANT, McpSamplingToolUseContent.builder()
                        .id("call-1")
                        .name("weather")
                        .input(new McpParameters(JsonParser.create("{}").readJsonObject()))
                        .build()))
                .build();

        McpSamplingException exception = assertThrows(McpSamplingException.class, () -> sampling.request(request));

        assertThat(exception.getMessage(), is("Sampling tools are not supported by client"));
        verifyNoInteractions(response);
    }

    @Test
    void rejectsToolResponseWithoutSamplingToolsCapability() {
        JsonRpcResponse response = mock(JsonRpcResponse.class);
        McpSampling sampling = sampling(session(McpProtocolVersion.VERSION_2025_11_25, """
                {"sampling": {}}
                """), new McpStreamableHttpTransport(response));
        McpSamplingMessage responseMessage = McpSamplingMessage.builder()
                .role(McpRole.ASSISTANT)
                .addContent(McpSamplingTextContent.create("Calling a tool"))
                .addContent(McpSamplingToolUseContent.builder()
                                    .id("call-1")
                                    .name("weather")
                                    .input(new McpParameters(JsonObject.empty()))
                                    .build())
                .build();
        McpSamplingResponse samplingResponse = new McpSamplingResponseImpl(responseMessage, "test-model", "toolUse");
        McpSamplingRequest request = McpSamplingRequest.builder()
                .addMessage(samplingResponse.message())
                .build();

        var contents = request.messages().getFirst().contents();
        assertThat(contents.size(), is(2));
        assertThat(contents.get(0), instanceOf(McpSamplingTextContent.class));
        assertThat(contents.get(1), instanceOf(McpSamplingToolUseContent.class));
        assertThat(request.usesTool(), is(true));
        assertThrows(UnsupportedOperationException.class,
                     () -> request.messages().getFirst().contents().clear());

        McpSamplingException exception = assertThrows(McpSamplingException.class, () -> sampling.request(request));

        assertThat(exception.getMessage(), is("Sampling tools are not supported by client"));
        verifyNoInteractions(response);
    }

    @ParameterizedTest
    @EnumSource(value = McpProtocolVersion.class,
                names = {"VERSION_2024_11_05", "VERSION_2025_03_26", "VERSION_2025_06_18"})
    void rejectsSamplingToolsForLegacyProtocolVersions(McpProtocolVersion protocolVersion) {
        JsonRpcResponse response = mock(JsonRpcResponse.class);
        McpSampling sampling = sampling(session(protocolVersion, """
                {"sampling": {"tools": {}}}
                """), new McpStreamableHttpTransport(response));
        McpSamplingRequest request = McpSamplingRequest.builder()
                .addTool("weather")
                .build();

        McpSamplingException exception = assertThrows(McpSamplingException.class, () -> sampling.request(request));

        assertThat(sampling.enabledTools(), is(false));
        assertThat(exception.getMessage(), is("Sampling tools are not supported by client"));
        verifyNoInteractions(response);
    }

    @ParameterizedTest
    @EnumSource(value = McpIncludeContext.class, names = {"THIS_SERVER", "ALL_SERVERS"})
    void rejectsUndeclaredSamplingContext(McpIncludeContext includeContext) {
        McpSession session = session(McpProtocolVersion.VERSION_2025_11_25, """
                {"sampling": {}}
                """);
        JsonRpcResponse response = mock(JsonRpcResponse.class);
        McpSampling sampling = sampling(session, new McpStreamableHttpTransport(response));
        McpSamplingRequest request = McpSamplingRequest.builder()
                .includeContext(includeContext)
                .build();

        assertThat(request.usesContext(), is(true));

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
        String capabilities = contextSupported
                ? """
                        {"sampling": {"context": {}}}
                        """
                : """
                        {"sampling": {}}
                        """;
        McpSession session = session(protocolVersion, capabilities);
        session.prepareResponse(0);
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
        McpSampling sampling = sampling(session, new McpStreamableHttpTransport(response));
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

    private static McpSession session(McpProtocolVersion protocolVersion, String capabilities) {
        McpServerConfig config = McpServerConfig.create();
        return session(protocolVersion, capabilities, config);
    }

    private static McpSession session(McpProtocolVersion protocolVersion,
                                      String capabilities,
                                      McpTool... tools) {
        McpServerConfig config = McpServerFeature.builder()
                .addTools(List.of(tools))
                .buildPrototype();
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
        session.initializeClientCapabilities(new McpParameters(
                JsonRpcParams.create(JsonParser.create(capabilities).readJsonObject())));
        return session;
    }

    private static McpSampling sampling(McpSession session, McpTransport transport) {
        return new McpFeatures(session, transport).sampling();
    }

    private static void acceptSamplingResponse(McpSession session, long id, String content, String stopReason) {
        session.prepareResponse(id);
        session.acceptResponse(JsonParser.create("""
                {
                  "jsonrpc": "2.0",
                  "id": %d,
                  "result": {
                    "model": "test-model-%d",
                    "role": "assistant",
                    "content": %s,
                    "stopReason": "%s"
                  }
                }
                """.formatted(id, id, content, stopReason)).readJsonObject());
    }

    private static List<JsonObject> sentSamplingRequests(SseSink sink, int count) {
        ArgumentCaptor<SseEvent> eventCaptor = ArgumentCaptor.forClass(SseEvent.class);
        verify(sink, times(count)).emit(eventCaptor.capture());
        return eventCaptor.getAllValues().stream()
                .map(SseEvent::data)
                .map(String.class::cast)
                .map(JsonParser::create)
                .map(JsonParser::readJsonObject)
                .toList();
    }

    private static void assertToolResult(JsonObject result,
                                         String toolUseId,
                                         boolean error,
                                         String messageFragment) {
        assertThat(result.stringValue("toolUseId").orElseThrow(), is(toolUseId));
        assertThat(result.booleanValue("isError").orElseThrow(), is(error));
        String text = result.arrayValue("content").orElseThrow().values().get(0).asObject()
                .stringValue("text").orElseThrow();
        assertThat(text.contains(messageFragment), is(true));
    }

    private static McpSamplingMessage message(McpRole role, McpSamplingContent content) {
        return McpSamplingMessage.builder()
                .role(role)
                .addContent(content)
                .build();
    }

    private record TestTool(String name,
                            String description,
                            String schema,
                            Function<McpToolRequest, McpToolResult> invocation) implements McpTool {
        private TestTool(String name, String description, String schema) {
            this(name, description, schema, request -> McpToolResult.create());
        }

        @Override
        public McpToolResult tool(McpToolRequest request) {
            return invocation.apply(request);
        }
    }
}
