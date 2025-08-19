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

import java.util.Objects;
import java.util.function.Function;

import io.helidon.extensions.mcp.server.McpCompletion;
import io.helidon.extensions.mcp.server.McpCompletionContent;
import io.helidon.extensions.mcp.server.McpCompletionContents;
import io.helidon.extensions.mcp.server.McpRequest;
import io.helidon.extensions.mcp.server.McpServerFeature;
import io.helidon.webserver.http.HttpRouting;

class CompletionNotifications {
    private CompletionNotifications() {
    }

    static void setUpRoute(HttpRouting.Builder builder) {
        builder.addFeature(McpServerFeature.builder()
                                   .path("/")
                                   .addCompletion(new CompletionHandler()));
    }

    private static class CompletionHandler implements McpCompletion {

        @Override
        public String reference() {
            return "prompt";
        }

        @Override
        public Function<McpRequest, McpCompletionContent> completion() {
            return this::complete;
        }

        McpCompletionContent complete(McpRequest request) {
            String argument = request.parameters().get("value").asString().get();
            if (Objects.equals(argument, "Hel")) {
                return McpCompletionContents.completion("Helidon");
            }
            return McpCompletionContents.completion();
        }
    }
}
