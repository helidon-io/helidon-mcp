/*
 * Copyright (c) 2025, 2026 Oracle and/or its affiliates.
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
import io.helidon.extensions.mcp.server.McpToolAnnotations;
import io.helidon.extensions.mcp.server.McpToolContent;
import io.helidon.extensions.mcp.server.McpToolContents;
import io.helidon.webserver.http.HttpRouting;

class ToolAnnotationsServer {
    private ToolAnnotationsServer() {
    }

    static void setUpRoute(HttpRouting.Builder builder) {
        builder.addFeature(McpServerFeature.builder()
                                   .path("/toolAnnotations")
                                   .name("mcp-server")
                                   .addTool(new Tool1())
                                   .addTool(new Tool2()));
    }

    private static class Tool1 implements McpTool {

        @Override
        public String name() {
            return "tool1";
        }

        @Override
        public String description() {
            return "Tool description";
        }

        @Override
        public String schema() {
            return "";
        }

        @Override
        public Function<McpRequest, List<McpToolContent>> tool() {
            return request -> List.of(McpToolContents.textContent(""));
        }

        @Override
        public McpToolAnnotations annotations() {
            var builder = McpToolAnnotations.builder();
            builder.title("")
                    .readOnlyHint(false)
                    .destructiveHint(true)
                    .idempotentHint(false)
                    .openWorldHint(true);
            return builder.build();
        }

    }

    private static class Tool2 implements McpTool {

        @Override
        public String name() {
            return "tool2";
        }

        @Override
        public String description() {
            return "Tool description";
        }

        @Override
        public String schema() {
            return "";
        }

        @Override
        public Function<McpRequest, List<McpToolContent>> tool() {
            return request -> List.of(McpToolContents.textContent(""));
        }

        @Override
        public McpToolAnnotations annotations() {
            var builder = McpToolAnnotations.builder();
            builder.title("tool2 title")
                    .readOnlyHint(true)
                    .destructiveHint(false)
                    .idempotentHint(true)
                    .openWorldHint(false);
            return builder.build();
        }
    }
}
