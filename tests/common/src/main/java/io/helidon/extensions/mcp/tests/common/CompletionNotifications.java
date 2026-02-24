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

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import io.helidon.extensions.mcp.server.McpCompletion;
import io.helidon.extensions.mcp.server.McpCompletionContext;
import io.helidon.extensions.mcp.server.McpCompletionRequest;
import io.helidon.extensions.mcp.server.McpCompletionResult;
import io.helidon.extensions.mcp.server.McpCompletionType;
import io.helidon.extensions.mcp.server.McpServerFeature;
import io.helidon.webserver.http.HttpRouting;

/**
 * Completion notifications.
 */
public class CompletionNotifications {
    private CompletionNotifications() {
    }

    /**
     * Setup webserver routing.
     *
     * @param builder routing builder
     */
    public static void setUpRoute(HttpRouting.Builder builder) {
        builder.addFeature(McpServerFeature.builder()
                                   .path("/")
                                   .addCompletion(new CompletionHandler())
                                   .addCompletion(new CompletionContext()));
    }

    private static class CompletionHandler implements McpCompletion {

        @Override
        public String reference() {
            return "helidon";
        }

        @Override
        public McpCompletionType referenceType() {
            return McpCompletionType.PROMPT;
        }

        @Override
        public McpCompletionResult completion(McpCompletionRequest request) {
            if (Objects.equals(request.value(), "Hel")) {
                return McpCompletionResult.builder()
                        .addValue("Helidon")
                        .total(1)
                        .build();
            }
            return McpCompletionResult.create();
        }
    }

    private static class CompletionContext implements McpCompletion {

        @Override
        public String reference() {
            return "context";
        }

        @Override
        public McpCompletionResult completion(McpCompletionRequest request) {
            String content = request.context()
                    .map(McpCompletionContext::arguments)
                    .map(Map::entrySet)
                    .stream()
                    .flatMap(Set::stream)
                    .map(entry -> entry.getKey() + "," + entry.getValue())
                    .collect(Collectors.joining(","));
            return McpCompletionResult.builder()
                    .addValue(content)
                    .total(1)
                    .build();
        }
    }
}
