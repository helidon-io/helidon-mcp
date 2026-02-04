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
import java.util.function.Function;

import io.helidon.common.mapper.OptionalValue;
import io.helidon.extensions.mcp.server.McpElicitation;
import io.helidon.extensions.mcp.server.McpElicitationException;
import io.helidon.extensions.mcp.server.McpElicitationResponse;
import io.helidon.extensions.mcp.server.McpException;
import io.helidon.extensions.mcp.server.McpParameters;
import io.helidon.extensions.mcp.server.McpRequest;
import io.helidon.extensions.mcp.server.McpServerFeature;
import io.helidon.extensions.mcp.server.McpTool;
import io.helidon.extensions.mcp.server.McpToolContents;
import io.helidon.extensions.mcp.server.McpToolResult;
import io.helidon.json.schema.Schema;
import io.helidon.json.schema.SchemaString;
import io.helidon.webserver.http.HttpRouting;

class ElicitationServer {
    private static final String FOO_SCHEMA = Schema.builder()
            .rootObject(root -> root.addStringProperty("foo", SchemaString.create()))
            .build()
            .generate();
    private ElicitationServer() {
    }

    static void setUpRoute(HttpRouting.Builder builder) {
        builder.addFeature(McpServerFeature.builder()
                                   .path("/")
                                   .addTool(tool -> tool.name("elicitation-exception")
                                           .description("Elicitation exception")
                                           .schema("")
                                           .tool(request -> {
                                               throw new McpElicitationException("Elicitation exception");
                                           }))
                                   .addTool(new ElicitationTool())
                                   .addTool(new ElicitationTimeoutTool()));
    }

    /**
     * Elicitation tool returns elicitation response as tool result.
     */
    private static class ElicitationTool implements McpTool {

        @Override
        public String name() {
            return "elicitation-tool";
        }

        @Override
        public String description() {
            return "Elicitation tool";
        }

        @Override
        public String schema() {
            return "";
        }

        @Override
        public Function<McpRequest, McpToolResult> tool() {
            return request -> {
                McpElicitation elicitation = request.features().elicitation();
                if (elicitation.enabled()) {
                    McpElicitationResponse response = elicitation.request(req -> req.message("foo").schema(FOO_SCHEMA));
                    String content = response.content()
                            .map(p -> p.get("foo"))
                            .map(McpParameters::asString)
                            .filter(OptionalValue::isPresent)
                            .map(OptionalValue::get)
                            .orElse("None");
                    return McpToolResult.builder()
                            .addContent(McpToolContents.textContent("Elicitation action: " + response.action().name()))
                            .addContent(McpToolContents.textContent("Elicitation content " + content))
                            .build();
                }
                throw new McpException("Elicitation is not enabled");
            };
        }
    }

    private static class ElicitationTimeoutTool extends ElicitationTool {
        @Override
        public String name() {
            return "elicitation-timeout";
        }

        @Override
        public Function<McpRequest, McpToolResult> tool() {
            return request -> {
                try {
                    request.features()
                            .elicitation()
                            .request(req -> req.message("timeout")
                                    .timeout(Duration.ofSeconds(1))
                                    .schema(FOO_SCHEMA));
                    throw new McpException("Timeout should have been triggered");
                } catch (McpElicitationException e) {
                    return McpToolResult.builder()
                            .error(true)
                            .addContent(McpToolContents.textContent(e.getMessage()))
                            .build();
                }
            };
        }
    }
}
