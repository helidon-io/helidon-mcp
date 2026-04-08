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

import io.helidon.common.media.type.MediaTypes;
import io.helidon.extensions.mcp.server.McpCompletionResult;
import io.helidon.extensions.mcp.server.McpException;
import io.helidon.extensions.mcp.server.McpFeatures;
import io.helidon.extensions.mcp.server.McpPromptResult;
import io.helidon.extensions.mcp.server.McpRequest;
import io.helidon.extensions.mcp.server.McpResourceResult;
import io.helidon.extensions.mcp.server.McpServerFeature;
import io.helidon.extensions.mcp.server.McpToolResult;
import io.helidon.webserver.http.HttpRouting;

/**
 * MCP stateless server.
 */
public class StatelessServer {
    private StatelessServer() {
    }

    /**
     * Stateless server using every MCP components.
     *
     * @param builder routing builder
     */
    public static void setUpRoute(HttpRouting.Builder builder) {
        builder.addFeature(McpServerFeature.builder()
                                   .path("/")
                                   .stateless(true)
                                   .addTool(tool -> tool.name("stateless-tool")
                                           .description("Stateless tool")
                                           .schema("")
                                           .tool(request -> {
                                               testFeatures(request);
                                               return McpToolResult.create("Success");
                                           }))
                                   .addPrompt(prompt -> prompt.name("stateless-prompt")
                                           .description("Stateless prompt")
                                           .prompt(request -> {
                                               testFeatures(request);
                                               return McpPromptResult.create("Success");
                                           }))
                                   .addResource(resource -> resource.name("stateless-resource")
                                           .description("Stateless resource")
                                           .mediaType(MediaTypes.TEXT_PLAIN)
                                           .uri("https://foo")
                                           .resource(request -> {
                                               testFeatures(request);
                                               return McpResourceResult.create("Success");
                                           }))
                                   .addCompletion(completion -> completion.reference("stateless-completion")
                                           .completion(request -> {
                                               testFeatures(request);
                                               return McpCompletionResult.create("Success");
                                           }))
        );
    }

    private static void testFeatures(McpRequest request) {
        McpFeatures features = request.features();
        if (features.elicitation().enabled()) {
            throw new McpException("Elicitation is enabled");
        }
        if (features.sampling().enabled()) {
            throw new McpException("Sampling is enabled");
        }
        if (features.roots().enabled()) {
            throw new McpException("Roots is enabled");
        }
    }
}
