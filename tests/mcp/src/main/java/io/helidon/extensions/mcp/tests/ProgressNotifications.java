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

import io.helidon.extensions.mcp.server.McpRequest;
import io.helidon.extensions.mcp.server.McpServerFeature;
import io.helidon.extensions.mcp.server.McpTool;
import io.helidon.extensions.mcp.server.McpToolContent;
import io.helidon.extensions.mcp.server.McpToolContents;
import io.helidon.webserver.http.HttpRouting;

class ProgressNotifications {

    private ProgressNotifications() {
    }

    static void setUpRoute(HttpRouting.Builder builder) {
        builder.addFeature(McpServerFeature.builder()
                                   .path("/")
                                   .addTool(new ProgressTool()));
    }

    static class ProgressTool implements McpTool {

        @Override
        public String name() {
            return "progress";
        }

        @Override
        public String description() {
            return "A tool that uses progress notifications.";
        }

        @Override
        public String schema() {
            return "";
        }

        @Override
        public Function<McpRequest, List<McpToolContent>> tool() {
            return this::process;
        }

        List<McpToolContent> process(McpRequest request) {
            var progress = request.features().progress();
            progress.total(100);
            try {
                for (int i = 1; i <= 10; i++) {
                    Thread.sleep(50);
                    progress.send(i * 10);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return List.of(McpToolContents.textContent("Dummy text"));
        }
    }
}
