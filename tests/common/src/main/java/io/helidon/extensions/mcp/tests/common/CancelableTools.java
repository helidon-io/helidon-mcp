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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.helidon.extensions.mcp.server.McpCancellation;
import io.helidon.extensions.mcp.server.McpCancellationResult;
import io.helidon.extensions.mcp.server.McpServerFeature;
import io.helidon.extensions.mcp.server.McpTool;
import io.helidon.extensions.mcp.server.McpToolRequest;
import io.helidon.extensions.mcp.server.McpToolResult;
import io.helidon.webserver.http.HttpRouting;

/**
 * Tools using cancelable feature.
 */
public class CancelableTools {
    private CancelableTools() {
    }

    /**
     * Setup webserver routing.
     *
     * @param builder               routing builder
     * @param cancellationLatch     latch
     * @param cancellationHookLatch latch
     */
    public static void setUpRoute(HttpRouting.Builder builder,
                                  CountDownLatch cancellationLatch,
                                  CountDownLatch cancellationHookLatch) {
        builder.addFeature(McpServerFeature.builder()
                                   .path("/")
                                   .addTool(new CancellationHookTool(cancellationHookLatch))
                                   .addTool(new CancellationTool(cancellationLatch)));
    }

    private static class CancellationTool implements McpTool {
        private final CountDownLatch latch;

        CancellationTool(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public String name() {
            return "cancellation-tool";
        }

        @Override
        public String description() {
            return "Tool running a long process";
        }

        @Override
        public String schema() {
            return "";
        }

        @Override
        public McpToolResult tool(McpToolRequest request) {
            long now = System.currentTimeMillis();
            long timeout = now + TimeUnit.SECONDS.toMillis(5);
            String content = "Failed";
            McpCancellation cancellation = request.features().cancellation();
            cancellation.registerCancellationHook(latch::countDown);

            while (now < timeout) {
                try {
                    McpCancellationResult result = cancellation.result();
                    if (result.isRequested()) {
                        content = result.reason();
                        latch.countDown();
                        break;
                    }
                    TimeUnit.MILLISECONDS.sleep(500);
                    now = System.currentTimeMillis();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            return McpToolResult.builder()
                    .addTextContent(content)
                    .build();
        }
    }

    private static class CancellationHookTool implements McpTool {
        private final CountDownLatch latch;

        CancellationHookTool(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public String name() {
            return "cancellation-hook-tool";
        }

        @Override
        public String description() {
            return "Tool running a long process";
        }

        @Override
        public String schema() {
            return "";
        }

        @Override
        public McpToolResult tool(McpToolRequest request) {
            McpCancellation cancellation = request.features().cancellation();
            cancellation.registerCancellationHook(latch::countDown);
            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return McpToolResult.builder()
                    .addTextContent("Failed")
                    .build();
        }
    }
}
