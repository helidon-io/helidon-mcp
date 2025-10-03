package io.helidon.extensions.mcp.tests;

import io.helidon.webserver.WebServer;
import io.helidon.webserver.testing.junit5.ServerTest;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;

@ServerTest
class McpSdkStreamableErrorServerTest extends AbstractMcpSdkMcpExceptionTest {
    private final McpSyncClient client;

    McpSdkStreamableErrorServerTest(WebServer server) {
        this.client = McpClient.sync(streamable(server.port())).build();
        client.initialize();
    }

    @Override
    McpSyncClient client() {
        return client;
    }
}
