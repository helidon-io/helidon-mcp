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

package io.helidon.extensions.mcp.tests.declarative;

import java.util.List;

import io.helidon.extensions.mcp.server.Mcp;
import io.helidon.extensions.mcp.server.McpCompletionContent;
import io.helidon.extensions.mcp.server.McpCompletionContents;
import io.helidon.extensions.mcp.server.McpFeatures;
import io.helidon.extensions.mcp.server.McpParameters;

@Mcp.Server
@Mcp.Path("/completions")
class McpCompletionsServer {

    @Mcp.Completion("prompt1")
    McpCompletionContent completionPrompt(McpParameters parameters) {
        String argument = parameters.get("value").asString().orElse(null);
        return McpCompletionContents.completion(argument);
    }

    @Mcp.Completion("prompt2")
    McpCompletionContent completionPromptFeatures(McpFeatures features) {
        return McpCompletionContents.completion("prompt2");
    }

    @Mcp.Completion("prompt3")
    McpCompletionContent completionPromptParametersFeatures(McpParameters parameters, McpFeatures features) {
        String argument = parameters.get("value").asString().orElse(null);
        return McpCompletionContents.completion(argument);
    }

    @Mcp.Completion("prompt4")
    McpCompletionContent completionPromptFeaturesParameters(McpFeatures features, McpParameters parameters) {
        String argument = parameters.get("value").asString().orElse(null);
        return McpCompletionContents.completion(argument);
    }

    @Mcp.Completion("prompt5")
    McpCompletionContent completionPromptArgument(String argument) {
        return McpCompletionContents.completion(argument);
    }

    @Mcp.Completion("prompt6")
    McpCompletionContent completionPromptArgumentFeatures(String argument, McpFeatures features) {
        return McpCompletionContents.completion(argument);
    }

    @Mcp.Completion("prompt7")
    McpCompletionContent completionPromptFeaturesArgument(McpFeatures features, String argument) {
        return McpCompletionContents.completion(argument);
    }

    @Mcp.Completion("resource/{path1}")
    McpCompletionContent completionResource(McpParameters parameters) {
        String argument = parameters.get("value").asString().orElse(null);
        return McpCompletionContents.completion(argument);
    }

    @Mcp.Completion("resource/{path2}")
    McpCompletionContent completionResourceArgument(String path2) {
        return McpCompletionContents.completion(path2);
    }

    @Mcp.Completion("prompt8")
    List<String> completionPromptList(McpParameters parameters) {
        String argument = parameters.get("value").asString().orElse(null);
        return List.of(argument);
    }

    @Mcp.Completion("prompt9")
    List<String> completionPromptListFeatures(McpFeatures features) {
        return List.of("prompt9");
    }

    @Mcp.Completion("prompt10")
    List<String> completionPromptListParametersFeatures(McpParameters parameters, McpFeatures features) {
        String argument = parameters.get("value").asString().orElse(null);
        return List.of(argument);
    }

    @Mcp.Completion("prompt11")
    List<String> completionPromptListFeaturesParameters(McpFeatures features, McpParameters parameters) {
        String argument = parameters.get("value").asString().orElse(null);
        return List.of(argument);
    }

    @Mcp.Completion("prompt12")
    List<String> completionPromptListArgument(String argument) {
        return List.of(argument);
    }

    @Mcp.Completion("prompt13")
    List<String> completionPromptListArgumentFeatures(String argument, McpFeatures features) {
        return List.of(argument);
    }

    @Mcp.Completion("prompt14")
    List<String> completionPromptListFeaturesArgument(McpFeatures features, String argument) {
        return List.of(argument);
    }

    @Mcp.Completion("resource/{path3}")
    List<String> completionResourceListArgument(String path3) {
        return List.of(path3);
    }

    @Mcp.Completion("resource/{path4}")
    List<String> completionResourceListArgumentFeatures(String path4, McpFeatures features) {
        return List.of(path4);
    }

    @Mcp.Completion("resource/{path5}")
    List<String> completionResourceListParametersFeatures(McpParameters parameters, McpFeatures features) {
        String argument = parameters.get("value").asString().orElse(null);
        return List.of(argument);
    }

    @Mcp.Completion("resource/{path6}")
    List<String> completionResourceListParameters(McpParameters parameters) {
        String argument = parameters.get("value").asString().orElse(null);
        return List.of(argument);
    }

    @Mcp.Completion("resource/{path7}")
    List<String> completionResourceListFeatures(McpFeatures features) {
        return List.of("path7");
    }
}
