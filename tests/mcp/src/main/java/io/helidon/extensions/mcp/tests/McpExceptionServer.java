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

import io.helidon.common.media.type.MediaType;
import io.helidon.common.media.type.MediaTypes;
import io.helidon.extensions.mcp.server.McpCompletion;
import io.helidon.extensions.mcp.server.McpCompletionContent;
import io.helidon.extensions.mcp.server.McpException;
import io.helidon.extensions.mcp.server.McpPrompt;
import io.helidon.extensions.mcp.server.McpPromptArgument;
import io.helidon.extensions.mcp.server.McpPromptContent;
import io.helidon.extensions.mcp.server.McpRequest;
import io.helidon.extensions.mcp.server.McpResource;
import io.helidon.extensions.mcp.server.McpResourceContent;
import io.helidon.extensions.mcp.server.McpServerFeature;
import io.helidon.extensions.mcp.server.McpTool;
import io.helidon.extensions.mcp.server.McpToolContent;
import io.helidon.webserver.http.HttpRouting;

import static io.helidon.jsonrpc.core.JsonRpcError.INTERNAL_ERROR;

class McpExceptionServer {
    private McpExceptionServer() {
    }

    static void setUpRoute(HttpRouting.Builder builder) {
        builder.addFeature(McpServerFeature.builder()
                                   .path("/")
                                   .addTool(new ErrorTool())
                                   .addPrompt(new ErrorPrompt())
                                   .addResource(new ErrorResource())
                                   .addCompletion(new ErrorCompletion())
                                   .addTool(new ErrorToolSwitchTransport())
                                   .addPrompt(new ErrorPromptSwitchTransport())
                                   .addResource(new ErrorResourceSwitchTransport())
                                   .addCompletion(new ErrorCompletionSwitchTransport()));
    }

    private static class ErrorTool implements McpTool {
        protected static final String MESSAGE = "Tool error message";

        @Override
        public String name() {
            return "error-tool";
        }

        @Override
        public String description() {
            return "Tool returns an error";
        }

        @Override
        public String schema() {
            return "";
        }

        @Override
        public Function<McpRequest, List<McpToolContent>> tool() {
            return request -> {
                throw new McpException(INTERNAL_ERROR, MESSAGE);
            };
        }
    }

    private static class ErrorToolSwitchTransport extends ErrorTool {
        @Override
        public String name() {
            return "error-tool-switch-transport";
        }

        @Override
        public String schema() {
            return "";
        }

        @Override
        public Function<McpRequest, List<McpToolContent>> tool() {
            return request -> {
                request.features().logger().info("Switching to the SSE channel");
                throw new McpException(INTERNAL_ERROR, MESSAGE);
            };
        }
    }

    private static class ErrorResource implements McpResource {
        protected static final String MESSAGE = "Resource error message";

        @Override
        public String uri() {
            return "error-resource";
        }

        @Override
        public String name() {
            return "Error Resource";
        }

        @Override
        public String description() {
            return "Resource returns an error";
        }

        @Override
        public MediaType mediaType() {
            return MediaTypes.TEXT_PLAIN;
        }

        @Override
        public Function<McpRequest, List<McpResourceContent>> resource() {
            return request -> {
                throw new McpException(INTERNAL_ERROR, MESSAGE);
            };
        }
    }

    private static class ErrorResourceSwitchTransport extends ErrorResource {
        @Override
        public String uri() {
            return "error-resource-switch-transport";
        }

        @Override
        public Function<McpRequest, List<McpResourceContent>> resource() {
            return request -> {
                request.features().logger().info("Switching to the SSE channel");
                throw new McpException(INTERNAL_ERROR, MESSAGE);
            };
        }
    }

    private static class ErrorPrompt implements McpPrompt {
        protected static final String MESSAGE = "Prompt error message";

        @Override
        public String name() {
            return "error-prompt";
        }

        @Override
        public String description() {
            return "Error prompt";
        }

        @Override
        public List<McpPromptArgument> arguments() {
            return List.of();
        }

        @Override
        public Function<McpRequest, List<McpPromptContent>> prompt() {
            return request -> {
                throw new McpException(INTERNAL_ERROR, MESSAGE);
            };
        }
    }

    private static class ErrorPromptSwitchTransport extends ErrorPrompt {
        @Override
        public String name() {
            return "error-prompt-switch-transport";
        }

        @Override
        public Function<McpRequest, List<McpPromptContent>> prompt() {
            return request -> {
                request.features().logger().info("Switching to the SSE channel");
                throw new McpException(INTERNAL_ERROR, MESSAGE);
            };
        }
    }

    private static class ErrorCompletion implements McpCompletion {
        protected static final String MESSAGE = "Completion error message";

        @Override
        public String reference() {
            return "error-completion";
        }

        @Override
        public Function<McpRequest, McpCompletionContent> completion() {
            return request -> {
                throw new McpException(INTERNAL_ERROR, MESSAGE);
            };
        }
    }

    private static class ErrorCompletionSwitchTransport extends ErrorCompletion {
        @Override
        public String reference() {
            return "error-completion-switch-transport";
        }

        @Override
        public Function<McpRequest, McpCompletionContent> completion() {
            return request -> {
                request.features().logger().info("Switching to the SSE channel");
                throw new McpException(INTERNAL_ERROR, MESSAGE);
            };
        }
    }
}
