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
package io.helidon.extensions.mcp.tests;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import io.helidon.extensions.mcp.tests.common.SamplingServer;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.spec.McpClientSession;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

@ServerTest
class McpSdkStreamableSamplingToolsTest {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);
    private static final String PROTOCOL_VERSION = "2025-11-25";
    private static final String SAMPLING_TOOL = "sampling-with-tools-tool";
    private static final String SAMPLED_TOOL = "sampled-tool";
    private static final String TOOL_USE_ID = "sampled-call-1";
    private static final String TOOL_INPUT = "integration-value";
    private static final String TOOL_RESULT = "sampled " + TOOL_INPUT;
    private static final String FINAL_RESPONSE = "final sampling response";
    private static final TypeRef<Map<String, Object>> MAP_TYPE = new TypeRef<>() {};
    private static final TypeRef<McpSchema.InitializeResult> INITIALIZE_RESULT_TYPE = new TypeRef<>() {};
    private static final TypeRef<McpSchema.CallToolResult> CALL_TOOL_RESULT_TYPE = new TypeRef<>() {};
    private static final Map<String, Object> TOOL_USE_RESPONSE = Map.of(
            "role", "assistant",
            "content", List.of(Map.of(
                    "type", "tool_use",
                    "id", TOOL_USE_ID,
                    "name", SAMPLED_TOOL,
                    "input", Map.of("value", TOOL_INPUT))),
            "model", "test-model",
            "stopReason", "toolUse");
    private static final Map<String, Object> FINAL_SAMPLING_RESPONSE = Map.of(
            "role", "assistant",
            "content", Map.of(
                    "type", "text",
                    "text", FINAL_RESPONSE),
            "model", "test-model",
            "stopReason", "endTurn");

    private final int port;

    McpSdkStreamableSamplingToolsTest(WebServer server) {
        port = server.port();
    }

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        SamplingServer.setUpToolsRoute(builder);
    }

    @Test
    void invokesSamplingToolAndReturnsFinalResponse() {
        List<Map<String, Object>> samplingRequests = new CopyOnWriteArrayList<>();
        AtomicInteger samplingRound = new AtomicInteger();
        HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport
                .builder("http://localhost:" + port)
                .endpoint("/")
                .supportedProtocolVersions(List.of(PROTOCOL_VERSION))
                .build();
        McpClientSession.RequestHandler<Map<String, Object>> samplingHandler = params -> {
            samplingRequests.add(transport.unmarshalFrom(params, MAP_TYPE));
            return switch (samplingRound.getAndIncrement()) {
                case 0 -> Mono.just(TOOL_USE_RESPONSE);
                case 1 -> Mono.just(FINAL_SAMPLING_RESPONSE);
                default -> Mono.error(new IllegalStateException("Unexpected sampling request"));
            };
        };
        McpClientSession session = new McpClientSession(
                REQUEST_TIMEOUT,
                transport,
                Map.of(McpSchema.METHOD_SAMPLING_CREATE_MESSAGE, samplingHandler),
                Map.of(),
                connection -> connection);

        try {
            McpSchema.InitializeResult initializeResult = session.sendRequest(
                    McpSchema.METHOD_INITIALIZE,
                    Map.of("protocolVersion", PROTOCOL_VERSION,
                           "capabilities", Map.of("sampling", Map.of("tools", Map.of())),
                           "clientInfo", Map.of("name", "sampling-tools-test", "version", "1.0.0")),
                    INITIALIZE_RESULT_TYPE)
                    .block(REQUEST_TIMEOUT);
            assertThat(initializeResult.protocolVersion(), is(PROTOCOL_VERSION));
            session.sendNotification(McpSchema.METHOD_NOTIFICATION_INITIALIZED).block(REQUEST_TIMEOUT);

            McpSchema.CallToolResult result = session.sendRequest(
                    McpSchema.METHOD_TOOLS_CALL,
                    new McpSchema.CallToolRequest(SAMPLING_TOOL, Map.of()),
                    CALL_TOOL_RESULT_TYPE)
                    .block(REQUEST_TIMEOUT);

            assertThat(result.isError(), is(false));
            assertThat(result.content().size(), is(1));
            assertThat(result.content().getFirst(), instanceOf(McpSchema.TextContent.class));
            assertThat(((McpSchema.TextContent) result.content().getFirst()).text(), is(FINAL_RESPONSE));
        } finally {
            session.closeGracefully()
                    .then(transport.closeGracefully())
                    .block(REQUEST_TIMEOUT);
        }

        assertSamplingRequests(samplingRequests);
    }

    private static void assertSamplingRequests(List<Map<String, Object>> requests) {
        assertThat(requests.size(), is(2));

        Map<String, Object> firstRequest = requests.getFirst();
        List<?> tools = asList(firstRequest.get("tools"));
        assertThat(tools.size(), is(1));
        Map<?, ?> tool = asMap(tools.getFirst());
        assertThat(tool.get("name"), is(SAMPLED_TOOL));
        assertThat(tool.get("description"), is("Returns the supplied value."));
        Map<?, ?> inputSchema = asMap(tool.get("inputSchema"));
        assertThat(inputSchema.get("type"), is("object"));
        assertThat(asMap(inputSchema.get("properties")), hasKey("value"));
        assertThat(inputSchema.get("required"), is(List.of("value")));
        assertThat(asMap(firstRequest.get("toolChoice")).get("mode"), is("required"));

        List<?> firstMessages = asList(firstRequest.get("messages"));
        assertThat(firstMessages.size(), is(1));
        Map<?, ?> firstMessage = asMap(firstMessages.getFirst());
        assertThat(firstMessage.get("role"), is("user"));
        Map<?, ?> firstContent = asMap(firstMessage.get("content"));
        assertThat(firstContent.get("type"), is("text"));
        assertThat(firstContent.get("text"), is("Use the sampled tool"));

        Map<String, Object> secondRequest = requests.get(1);
        assertThat(secondRequest.get("tools"), is(firstRequest.get("tools")));
        assertThat(asMap(secondRequest.get("toolChoice")).get("mode"), is("auto"));
        List<?> messages = asList(secondRequest.get("messages"));
        assertThat(messages.size(), is(3));

        Map<?, ?> toolUseMessage = asMap(messages.get(1));
        assertThat(toolUseMessage.get("role"), is("assistant"));
        Map<?, ?> toolUse = asMap(toolUseMessage.get("content"));
        assertThat(toolUse.get("type"), is("tool_use"));
        assertThat(toolUse.get("id"), is(TOOL_USE_ID));
        assertThat(toolUse.get("name"), is(SAMPLED_TOOL));
        assertThat(asMap(toolUse.get("input")).get("value"), is(TOOL_INPUT));

        Map<?, ?> toolResultMessage = asMap(messages.get(2));
        assertThat(toolResultMessage.get("role"), is("user"));
        Map<?, ?> toolResult = asMap(toolResultMessage.get("content"));
        assertThat(toolResult.get("type"), is("tool_result"));
        assertThat(toolResult.get("toolUseId"), is(TOOL_USE_ID));
        assertThat(toolResult.get("isError"), is(false));
        List<?> resultContent = asList(toolResult.get("content"));
        assertThat(resultContent.size(), is(1));
        Map<?, ?> text = asMap(resultContent.getFirst());
        assertThat(text.get("type"), is("text"));
        assertThat(text.get("text"), is(TOOL_RESULT));
    }

    private static Map<?, ?> asMap(Object value) {
        assertThat(value, instanceOf(Map.class));
        return (Map<?, ?>) value;
    }

    private static List<?> asList(Object value) {
        assertThat(value, instanceOf(List.class));
        return (List<?>) value;
    }
}
