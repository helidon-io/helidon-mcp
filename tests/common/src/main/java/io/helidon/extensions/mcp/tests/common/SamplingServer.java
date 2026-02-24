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
package io.helidon.extensions.mcp.tests.common;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.extensions.mcp.server.McpContentType;
import io.helidon.extensions.mcp.server.McpException;
import io.helidon.extensions.mcp.server.McpSampling;
import io.helidon.extensions.mcp.server.McpSamplingException;
import io.helidon.extensions.mcp.server.McpSamplingMessage;
import io.helidon.extensions.mcp.server.McpSamplingMessages;
import io.helidon.extensions.mcp.server.McpSamplingResponse;
import io.helidon.extensions.mcp.server.McpServerFeature;
import io.helidon.extensions.mcp.server.McpTool;
import io.helidon.extensions.mcp.server.McpToolRequest;
import io.helidon.extensions.mcp.server.McpToolResult;
import io.helidon.json.schema.Schema;
import io.helidon.webserver.http.HttpRouting;

import static io.helidon.extensions.mcp.server.McpRole.USER;

/**
 * Sampling server.
 */
public class SamplingServer {
    private SamplingServer() {
    }

    /**
     * Setup webserver routing.
     *
     * @param builder routing builder
     */
    public static void setUpRoute(HttpRouting.Builder builder) {
        builder.addFeature(McpServerFeature.builder()
                                   .path("/")
                                   .addTool(new EnabledTool())
                                   .addTool(new SamplingTool())
                                   .addTool(new ErrorSamplingTool())
                                   .addTool(new TimeoutSamplingTool())
                                   .addTool(new MultipleSamplingRequestTool())
        );
    }

    private static class SamplingTool implements McpTool {
        @Override
        public String name() {
            return "sampling-tool";
        }

        @Override
        public String description() {
            return "A tool that returns sampling response as tool content.";
        }

        @Override
        public String schema() {
            return Schema.builder().build().generate();
        }

        @Override
        public McpToolResult tool(McpToolRequest request) {
            return sampling(request);
        }

        McpToolResult sampling(McpToolRequest request) {
            McpSampling sampling = request.features().sampling();
            Optional<McpContentType> requestType = request.arguments()
                    .get("type")
                    .asString()
                    .map(String::toUpperCase)
                    .map(McpContentType::valueOf);

            if (requestType.isEmpty()) {
                return McpToolResult.builder()
                        .addTextContent("Error while parsing content type")
                        .error(true)
                        .build();
            }

            McpSamplingMessage message = createMessage(requestType.get());
            McpSamplingResponse response = sampling.request(req -> req.addMessage(message));
            var type = response.message().type();
            var result = McpToolResult.builder();
            return switch (type) {
                case TEXT -> result.addTextContent(response.asTextMessage().text()).build();
                case IMAGE -> result.addTextContent(new String(response.asImageMessage().data())).build();
                case AUDIO -> result.addTextContent(new String(response.asAudioMessage().data())).build();
            };
        }

        McpSamplingMessage createMessage(McpContentType type) {
            return switch (type) {
                case TEXT -> McpSamplingMessages.textMessage("samplingMessage", USER);
                case IMAGE -> McpSamplingMessages.imageMessage("samplingMessage".getBytes(StandardCharsets.UTF_8),
                                                               MediaTypes.TEXT_PLAIN,
                                                               USER);
                case AUDIO -> McpSamplingMessages.audioMessage("samplingMessage".getBytes(StandardCharsets.UTF_8),
                                                               MediaTypes.TEXT_PLAIN,
                                                               USER);
                default -> throw new McpException("Unsupported sampling message type: " + type);
            };
        }
    }

    private static class EnabledTool extends SamplingTool {
        @Override
        public String name() {
            return "enabled-tool";
        }

        @Override
        public McpToolResult tool(McpToolRequest request) {
            McpSampling sampling = request.features().sampling();
            if (sampling.enabled()) {
                return sampling(request);
            }
            return McpToolResult.builder()
                    .addTextContent("sampling is disabled")
                    .error(true)
                    .build();
        }
    }

    private static class MultipleSamplingRequestTool extends SamplingTool {
        private final McpSamplingMessage message = McpSamplingMessages.textMessage("ignored", USER);

        @Override
        public String name() {
            return "multiple-sampling-tool";
        }

        @Override
        public McpToolResult tool(McpToolRequest request) {
            McpSampling sampling = request.features().sampling();
            var response = sampling.request(req -> req.addMessage(message));
            return sampling(request);
        }
    }

    private static class TimeoutSamplingTool extends SamplingTool {
        @Override
        public String name() {
            return "timeout-tool";
        }

        @Override
        public McpToolResult tool(McpToolRequest request) {
            try {
                request.features()
                        .sampling()
                        .request(req -> req.timeout(Duration.ofSeconds(2))
                                .addMessage(McpSamplingMessages.textMessage("timeout", USER)));
                throw new McpException("Timeout should have been triggered");
            } catch (McpSamplingException e) {
                return McpToolResult.builder()
                        .addTextContent(e.getMessage())
                        .error(true)
                        .build();
            }
        }
    }

    private static class ErrorSamplingTool extends SamplingTool {
        @Override
        public String name() {
            return "error-tool";
        }

        @Override
        public McpToolResult tool(McpToolRequest request) {
            try {
                request.features()
                        .sampling()
                        .request(req -> req.addMessage(McpSamplingMessages.textMessage("error", USER)));
                throw new McpException("MCP sampling exception should have been triggered");
            } catch (McpSamplingException e) {
                return McpToolResult.builder()
                        .addTextContent(e.getMessage())
                        .error(true)
                        .build();
            }
        }
    }
}
