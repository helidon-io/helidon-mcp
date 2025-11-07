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

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.function.Function;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.extensions.mcp.server.McpContent;
import io.helidon.extensions.mcp.server.McpException;
import io.helidon.extensions.mcp.server.McpRequest;
import io.helidon.extensions.mcp.server.McpSampling;
import io.helidon.extensions.mcp.server.McpSamplingException;
import io.helidon.extensions.mcp.server.McpSamplingMessage;
import io.helidon.extensions.mcp.server.McpSamplingMessages;
import io.helidon.extensions.mcp.server.McpSamplingResponse;
import io.helidon.extensions.mcp.server.McpServerFeature;
import io.helidon.extensions.mcp.server.McpTool;
import io.helidon.extensions.mcp.server.McpToolContent;
import io.helidon.extensions.mcp.server.McpToolErrorException;
import io.helidon.json.schema.Schema;
import io.helidon.webserver.http.HttpRouting;

import static io.helidon.extensions.mcp.server.McpRole.USER;
import static io.helidon.extensions.mcp.server.McpToolContents.textContent;

class SamplingServer {
    private SamplingServer() {
    }

    static void setUpRoute(HttpRouting.Builder builder) {
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
        public Function<McpRequest, List<McpToolContent>> tool() {
            return this::sampling;
        }

        List<McpToolContent> sampling(McpRequest request) {
            McpSampling sampling = request.features().sampling();
            McpContent.ContentType requestType = request.parameters()
                    .get("type")
                    .asString()
                    .map(String::toUpperCase)
                    .map(McpContent.ContentType::valueOf)
                    .orElseThrow(() -> new McpToolErrorException("Error while parsing content type"));

            McpSamplingMessage message = createMessage(requestType);
            McpSamplingResponse response = sampling.request(req -> req.addMessage(message));
            var type = response.message().type();
            return switch (type) {
                case TEXT -> List.of(textContent(response.asTextMessage().text()));
                case IMAGE -> List.of(textContent(new String(response.asImageMessage().data())));
                case AUDIO -> List.of(textContent(new String(response.asAudioMessage().data())));
            };
        }

        McpSamplingMessage createMessage(McpContent.ContentType type) {
            return switch (type) {
                case TEXT -> McpSamplingMessages.textMessage("samplingMessage", USER);
                case IMAGE -> McpSamplingMessages.imageMessage("samplingMessage".getBytes(StandardCharsets.UTF_8),
                                                               MediaTypes.TEXT_PLAIN,
                                                               USER);
                case AUDIO -> McpSamplingMessages.audioMessage("samplingMessage".getBytes(StandardCharsets.UTF_8),
                                                               MediaTypes.TEXT_PLAIN,
                                                               USER);
                default -> throw new McpToolErrorException(textContent("Unsupported sampling message type: " + type));
            };
        }
    }

    private static class EnabledTool extends SamplingTool {
        @Override
        public String name() {
            return "enabled-tool";
        }

        @Override
        public Function<McpRequest, List<McpToolContent>> tool() {
            return this::enabledSampling;
        }

        private List<McpToolContent> enabledSampling(McpRequest request) {
            McpSampling sampling = request.features().sampling();
            if (sampling.enabled()) {
                return sampling(request);
            }
            throw new McpToolErrorException(textContent("sampling is disabled"));
        }
    }

    private static class MultipleSamplingRequestTool extends SamplingTool {
        private final McpSamplingMessage message = McpSamplingMessages.textMessage("ignored", USER);

        @Override
        public String name() {
            return "multiple-sampling-tool";
        }

        @Override
        public Function<McpRequest, List<McpToolContent>> tool() {
            return request -> {
                McpSampling sampling = request.features().sampling();
                var response = sampling.request(req -> req.addMessage(message));
                return sampling(request);
            };
        }
    }

    private static class TimeoutSamplingTool extends SamplingTool {
        @Override
        public String name() {
            return "timeout-tool";
        }

        @Override
        public Function<McpRequest, List<McpToolContent>> tool() {
            return request -> {
                try {
                    request.features()
                            .sampling()
                            .request(req -> req.timeout(Duration.ofSeconds(2))
                                    .addMessage(McpSamplingMessages.textMessage("timeout", USER)));
                    throw new McpException("Timeout should have been triggered");
                } catch (McpSamplingException e) {
                    throw new McpToolErrorException(e.getMessage());
                }
            };
        }
    }

    private static class ErrorSamplingTool extends SamplingTool {
        @Override
        public String name() {
            return "error-tool";
        }

        @Override
        public Function<McpRequest, List<McpToolContent>> tool() {
            return request -> {
                try {
                    request.features()
                            .sampling()
                            .request(req -> req.addMessage(McpSamplingMessages.textMessage("error", USER)));
                    throw new McpException("MCP sampling exception should have been triggered");
                } catch (McpSamplingException e) {
                    throw new McpToolErrorException(e.getMessage());
                }
            };
        }
    }
}
