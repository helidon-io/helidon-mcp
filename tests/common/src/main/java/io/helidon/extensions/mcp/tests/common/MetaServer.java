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
import io.helidon.extensions.mcp.server.McpPromptResult;
import io.helidon.extensions.mcp.server.McpResourceResult;
import io.helidon.extensions.mcp.server.McpServerFeature;
import io.helidon.extensions.mcp.server.McpToolResult;
import io.helidon.webserver.http.HttpRouting;

/**
 * Metadata server tests.
 */
public class MetaServer {
    private MetaServer() {
    }

    // Returns the "foo" key from "_meta" object sent by client
    /**
     * Setup webserver routing.
     *
     * @param builder routing builder
     */
    public static void setUpRoute(HttpRouting.Builder builder) {
        builder.addFeature(McpServerFeature.builder()
                                   .path("/")
                                   .addTool(tool -> tool.name("meta-tool")
                                           .description("Meta Tool")
                                           .schema("")
                                           .tool(request -> {
                                               String meta = request.meta().get("foo").asString().orElse("Not found");
                                               return McpToolResult.builder()
                                                       .addTextContent(meta)
                                                       .build();
                                           }))

                                   .addPrompt(prompt -> prompt.name("meta-prompt")
                                           .description("Meta Prompt")
                                           .prompt(request -> {
                                               String meta = request.meta().get("foo").asString().orElse("Not found");
                                               return McpPromptResult.builder()
                                                       .addTextContent(meta)
                                                       .build();
                                           }))

                                   .addResource(resource -> resource.uri("https://foo")
                                           .description("Meta Resource")
                                           .name("meta-resource")
                                           .mediaType(MediaTypes.TEXT_PLAIN)
                                           .resource(request -> {
                                               String meta = request.meta().get("foo").asString().orElse("Not found");
                                               return McpResourceResult.builder()
                                                       .addTextContent(meta)
                                                       .build();
                                           }))

                                   .addCompletion(completion -> completion.reference("meta-completion")
                                           .completion(request -> {
                                               String meta = request.meta().get("foo").asString().orElse("Not found");
                                               return McpCompletionResult.builder()
                                                       .addValue(meta)
                                                       .build();
                                           })));
    }
}
