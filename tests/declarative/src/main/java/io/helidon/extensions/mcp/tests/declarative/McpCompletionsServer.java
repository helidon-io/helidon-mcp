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

package io.helidon.extensions.mcp.tests.declarative;

import java.util.List;

import io.helidon.extensions.mcp.server.Mcp;
import io.helidon.extensions.mcp.server.McpCompletionRequest;
import io.helidon.extensions.mcp.server.McpCompletionResult;
import io.helidon.extensions.mcp.server.McpCompletionType;
import io.helidon.extensions.mcp.server.McpFeatures;
import io.helidon.extensions.mcp.server.McpParameters;
import io.helidon.extensions.mcp.server.McpRequest;

@Mcp.Server
@Mcp.Path("/completions")
class McpCompletionsServer {
    @Mcp.Completion("prompt1")
    McpCompletionResult completionPrompt(McpParameters parameters) {
        String argument = parameters.get("argument").get("value").asString().orElse(null);
        return McpCompletionResult.create(argument);
    }

    @Mcp.Completion("prompt2")
    McpCompletionResult completionPromptFeatures(McpFeatures features) {
        return McpCompletionResult.create("prompt2");
    }

    @Mcp.Completion("prompt3")
    McpCompletionResult completionPromptParametersFeatures(McpParameters parameters, McpFeatures features) {
        String argument = parameters.get("argument").get("value").asString().orElse(null);
        return McpCompletionResult.create(argument);
    }

    @Mcp.Completion("prompt4")
    McpCompletionResult completionPromptFeaturesParameters(McpFeatures features, McpParameters parameters) {
        String argument = parameters.get("argument").get("value").asString().orElse(null);
        return McpCompletionResult.create(argument);
    }

    @Mcp.Completion("prompt5")
    McpCompletionResult completionPromptArgument(String argument) {
        return McpCompletionResult.create(argument);
    }

    @Mcp.Completion("prompt6")
    McpCompletionResult completionPromptArgumentFeatures(String argument, McpFeatures features) {
        return McpCompletionResult.create(argument);
    }

    @Mcp.Completion("prompt7")
    McpCompletionResult completionPromptFeaturesArgument(McpFeatures features, String argument) {
        return McpCompletionResult.create(argument);
    }

    @Mcp.Completion(value = "resource/{path1}", type = McpCompletionType.RESOURCE)
    McpCompletionResult completionResource(McpParameters parameters) {
        String argument = parameters.get("argument").get("value").asString().orElse(null);
        return McpCompletionResult.create(argument);
    }

    @Mcp.Completion(value = "resource/{path2}", type = McpCompletionType.RESOURCE)
    McpCompletionResult completionResourceArgument(String path2) {
        return McpCompletionResult.create(path2);
    }

    @Mcp.Completion(value = "resource/{path8}", type = McpCompletionType.RESOURCE)
    McpCompletionResult completionMcpRequest(McpRequest request) {
        String argument = request.parameters().get("value").asString().orElse(null);
        return McpCompletionResult.create(argument);
    }

    @Mcp.Completion("prompt15")
    McpCompletionResult completionPromptResult(McpCompletionRequest request) {
        return McpCompletionResult.create(request.value());
    }

    @Mcp.Completion(value = "resource/{path10}", type = McpCompletionType.RESOURCE)
    McpCompletionResult completion1StringMcpRequest(String argument, McpRequest request) {
        return McpCompletionResult.create(argument);
    }

    @Mcp.Completion("prompt8")
    List<String> completionPromptList(McpParameters parameters) {
        String argument = parameters.get("argument").get("value").asString().orElse(null);
        return List.of(argument);
    }

    @Mcp.Completion("prompt9")
    List<String> completionPromptListFeatures(McpFeatures features) {
        return List.of("prompt9");
    }

    @Mcp.Completion("prompt10")
    List<String> completionPromptListParametersFeatures(McpParameters parameters, McpFeatures features) {
        String argument = parameters.get("argument").get("value").asString().orElse(null);
        return List.of(argument);
    }

    @Mcp.Completion("prompt11")
    List<String> completionPromptListFeaturesParameters(McpFeatures features, McpParameters parameters) {
        String argument = parameters.get("argument").get("value").asString().orElse(null);
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

    @Mcp.Completion(value = "resource/{path3}", type = McpCompletionType.RESOURCE)
    List<String> completionResourceListArgument(String path3) {
        return List.of(path3);
    }

    @Mcp.Completion(value = "resource/{path4}", type = McpCompletionType.RESOURCE)
    List<String> completionResourceListArgumentFeatures(String path4, McpFeatures features) {
        return List.of(path4);
    }

    @Mcp.Completion(value = "resource/{path5}", type = McpCompletionType.RESOURCE)
    List<String> completionResourceListParametersFeatures(McpParameters parameters, McpFeatures features) {
        String argument = parameters.get("argument").get("value").asString().orElse(null);
        return List.of(argument);
    }

    @Mcp.Completion(value = "resource/{path6}", type = McpCompletionType.RESOURCE)
    List<String> completionResourceListParameters(McpParameters parameters) {
        String argument = parameters.get("argument").get("value").asString().orElse(null);
        return List.of(argument);
    }

    @Mcp.Completion(value = "resource/{path7}", type = McpCompletionType.RESOURCE)
    List<String> completionResourceListFeatures(McpFeatures features) {
        return List.of("path7");
    }

    @Mcp.Completion(value = "resource/{path8}", type = McpCompletionType.RESOURCE)
    List<String> completion1McpRequest(McpRequest request) {
        String argument = request.parameters().get("argument").get("value").asString().orElse(null);
        return List.of(argument);
    }

    @Mcp.Completion(value = "resource/{path9}", type = McpCompletionType.RESOURCE)
    List<String> completionStringMcpRequest(String argument, McpRequest request) {
        return List.of(argument);
    }
}
