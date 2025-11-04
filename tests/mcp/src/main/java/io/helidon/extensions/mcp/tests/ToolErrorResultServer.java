/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
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

import java.util.List;
import java.util.function.Function;

import io.helidon.extensions.mcp.server.McpRequest;
import io.helidon.extensions.mcp.server.McpServerFeature;
import io.helidon.extensions.mcp.server.McpTool;
import io.helidon.extensions.mcp.server.McpToolContent;
import io.helidon.extensions.mcp.server.McpToolErrorException;
import io.helidon.webserver.http.HttpRouting;

import static io.helidon.extensions.mcp.server.McpToolContents.textContent;

class ToolErrorResultServer {
    private ToolErrorResultServer() {
    }

    static void setUpRoute(HttpRouting.Builder builder) {
        builder.addFeature(McpServerFeature.builder()
                                   .path("/")
                                   .addTool(new FailingTool())
                                   .addTool(new FailingTool1())
                                   .addTool(new FailingTool2()));
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
            McpToolContent content = textContent("Tool error message");
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
            McpToolContent content = textContent("Tool error message");
            throw new McpToolErrorException(List.of(content));
        }
    }

    private static class FailingTool2 extends FailingTool {
        @Override
        public String name() {
            return "failing-tool-2";
        }

        @Override
        public Function<McpRequest, List<McpToolContent>> tool() {
            McpToolContent content = textContent("Tool error message");
            McpToolContent content1 = textContent("Second error message");
            throw new McpToolErrorException(content, content1);
        }
    }
}
