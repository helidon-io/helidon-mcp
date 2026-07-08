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
package io.helidon.extensions.mcp.tests.common;

import io.helidon.extensions.mcp.server.McpServerFeature;
import io.helidon.extensions.mcp.server.McpTool;
import io.helidon.extensions.mcp.server.McpToolRequest;
import io.helidon.extensions.mcp.server.McpToolResult;
import io.helidon.webserver.http.HttpRouting;

/**
 * Server with tools declaring required parameters in their input schema.
 */
public class RequiredParamServer {
    private RequiredParamServer() {
    }

    /**
     * Setup webserver routing.
     *
     * @param builder routing builder
     */
    public static void setUpRoute(HttpRouting.Builder builder) {
        builder.addFeature(McpServerFeature.builder()
                                   .path("/")
                                   .addTool(new SingleRequiredParamTool())
                                   .addTool(new MultipleRequiredParamsTool()));
    }

    private static class SingleRequiredParamTool implements McpTool {
        @Override
        public String name() {
            return "single-required-param-tool";
        }

        @Override
        public String description() {
            return "Tool with one required parameter";
        }

        @Override
        public String schema() {
            return """
                    {
                      "type": "object",
                      "properties": {
                        "mandatory": {"type": "string"},
                        "optional": {"type": "string"}
                      },
                      "required": ["mandatory"]
                    }
                    """;
        }

        @Override
        public McpToolResult tool(McpToolRequest request) {
            String mandatory = request.arguments().get("mandatory").asString().orElse("");
            return McpToolResult.create("mandatory=" + mandatory);
        }
    }

    private static class MultipleRequiredParamsTool implements McpTool {
        @Override
        public String name() {
            return "multiple-required-params-tool";
        }

        @Override
        public String description() {
            return "Tool with multiple required parameters";
        }

        @Override
        public String schema() {
            return """
                    {
                      "type": "object",
                      "properties": {
                        "a": {"type": "string"},
                        "b": {"type": "integer"}
                      },
                      "required": ["a", "b"]
                    }
                    """;
        }

        @Override
        public McpToolResult tool(McpToolRequest request) {
            String a = request.arguments().get("a").asString().orElse("");
            int b = request.arguments().get("b").asInteger().orElse(0);
            return McpToolResult.create("a=" + a + "|b=" + b);
        }
    }
}
