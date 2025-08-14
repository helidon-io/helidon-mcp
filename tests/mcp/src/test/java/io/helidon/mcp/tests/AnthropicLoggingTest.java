package io.helidon.mcp.tests;

import java.util.Map;

import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ServerTest
class AnthropicLoggingTest {
    private static McpSyncClient client;

    AnthropicLoggingTest(WebServer server) {
        client = McpClient.sync(HttpClientSseClientTransport.builder("http://localhost:" + server.port())
                                        .sseEndpoint("/")
                                        .build())
                .loggingConsumer(notification -> {
                    assertThat(notification.level(), is(McpSchema.LoggingLevel.INFO));
                    assertThat(notification.logger(), is("helidon-logger"));
                    assertThat(notification.data(), is("Logging data"));
                })
                .build();
        client.initialize();
    }

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        LoggingNotifications.setUpRoute(builder);
    }

    @AfterAll
    static void closeClient() {
        client.close();
    }

    @Test
    void testAnthropicProgress() {
        client.callTool(new McpSchema.CallToolRequest("logging", Map.of("question", "")));
    }
}
