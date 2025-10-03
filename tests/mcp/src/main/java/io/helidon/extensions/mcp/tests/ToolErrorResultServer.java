package io.helidon.extensions.mcp.tests;

import java.util.List;
import java.util.function.Function;

import io.helidon.extensions.mcp.server.McpRequest;
import io.helidon.extensions.mcp.server.McpServerFeature;
import io.helidon.extensions.mcp.server.McpTool;
import io.helidon.extensions.mcp.server.McpToolContent;
import io.helidon.extensions.mcp.server.McpToolContents;
import io.helidon.extensions.mcp.server.McpToolErrorException;
import io.helidon.webserver.http.HttpRouting;

class ToolErrorResultServer {
    private ToolErrorResultServer() {
    }

    static void setUpRoute(HttpRouting.Builder builder) {
        builder.addFeature(McpServerFeature.builder()
                                   .path("/")
                                   .addTool(new FailingTool())
                                   .addTool(new FailingTool1()));
    }

    private static class FailingTool implements McpTool {

        @Override
        public String name() {
            return "failing-tool";
        }

        @Override
        public String description() {
            return "Tool returning an error";
        }

        @Override
        public String schema() {
            return "";
        }

        @Override
        public Function<McpRequest, List<McpToolContent>> tool() {
            McpToolContent content = McpToolContents.textContent("Tool error message");
            throw new McpToolErrorException(content);
        }
    }

    private static class FailingTool1 extends FailingTool {

        @Override
        public String name() {
            return "failing-tool-1";
        }

        @Override
        public Function<McpRequest, List<McpToolContent>> tool() {
            McpToolContent content = McpToolContents.textContent("Tool error message");
            throw new McpToolErrorException(List.of(content));
        }
    }
}
