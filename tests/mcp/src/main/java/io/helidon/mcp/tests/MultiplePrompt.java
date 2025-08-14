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

package io.helidon.mcp.tests;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.mcp.server.McpParameters;
import io.helidon.mcp.server.McpPrompt;
import io.helidon.mcp.server.McpPromptArgument;
import io.helidon.mcp.server.McpPromptContent;
import io.helidon.mcp.server.McpPromptContents;
import io.helidon.mcp.server.McpRequest;
import io.helidon.mcp.server.McpResourceContents;
import io.helidon.mcp.server.McpRole;
import io.helidon.mcp.server.McpServerFeature;
import io.helidon.webserver.http.HttpRouting;

class MultiplePrompt {
    private MultiplePrompt() {
    }

    static void setUpRoute(HttpRouting.Builder builder) {
        builder.addFeature(McpServerFeature.builder()
                                   .path("/")
                                   .addPrompt(prompt -> prompt.name("prompt1")
                                           .description("Prompt 1")
                                           .prompt(request ->
                                                           List.of(McpPromptContents.textContent("text", McpRole.USER))))

                                   .addPrompt(prompt -> prompt.name("prompt2")
                                           .description("Prompt 2")
                                           .prompt(request ->
                                                           List.of(McpPromptContents.imageContent(
                                                                   "binary",
                                                                   MediaTypes.APPLICATION_OCTET_STREAM,
                                                                   McpRole.ASSISTANT))))

                                   .addPrompt(prompt -> prompt.name("prompt3")
                                           .description("Prompt 3")
                                           .prompt(request ->
                                                           List.of(McpPromptContents.resourceContent(
                                                                   "http://resource",
                                                                   McpResourceContents.textContent("resource"),
                                                                   McpRole.ASSISTANT))))
                                   .addPrompt(new MyPrompt()));
    }

    private static class MyPrompt implements McpPrompt {

        @Override
        public String name() {
            return "prompt4";
        }

        @Override
        public String description() {
            return "Prompt 4";
        }

        @Override
        public Set<McpPromptArgument> arguments() {
            return Set.of(McpPromptArgument.builder()
                                  .name("argument1")
                                  .description("Argument 1")
                                  .required(true)
                                  .build());
        }

        @Override
        public Function<McpRequest, List<McpPromptContent>> prompt() {
            return this::prompts;
        }

        public List<McpPromptContent> prompts(McpRequest request) {
            McpParameters parameters = request.parameters();
            return List.of(
                    McpPromptContents.imageContent("binary", MediaTypes.APPLICATION_OCTET_STREAM, McpRole.USER),
                    McpPromptContents.textContent(parameters.get("argument1").asString().orElse("missing"), McpRole.USER),
                    McpPromptContents.resourceContent("http://resource",
                                                      McpResourceContents.textContent("resource"),
                                                      McpRole.USER));
        }
    }
}
