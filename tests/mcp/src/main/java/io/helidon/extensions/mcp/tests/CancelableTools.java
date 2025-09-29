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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import io.helidon.extensions.mcp.server.McpCancellation;
import io.helidon.extensions.mcp.server.McpCancellationResult;
import io.helidon.extensions.mcp.server.McpRequest;
import io.helidon.extensions.mcp.server.McpServerFeature;
import io.helidon.extensions.mcp.server.McpTool;
import io.helidon.extensions.mcp.server.McpToolContent;
import io.helidon.extensions.mcp.server.McpToolContents;
import io.helidon.webserver.http.HttpRouting;

class CancelableTools {
    private CancelableTools() {
    }

    static void setUpRoute(HttpRouting.Builder builder,
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
        public Function<McpRequest, List<McpToolContent>> tool() {
            return this::process;
        }

        private List<McpToolContent> process(McpRequest request) {
            long now = System.currentTimeMillis();
            long timeout = now + TimeUnit.SECONDS.toMillis(5);
            McpToolContent content = McpToolContents.textContent("Failed");
            McpCancellation cancellation = request.features().cancellation();
            cancellation.registerCancellationHook(this::cancellationHook);

            while (now < timeout) {
                try {
                    McpCancellationResult result = cancellation.result();
                    if (result.isRequested()) {
                        content = McpToolContents.textContent(result.reason());
                        latch.countDown();
                        break;
                    }
                    TimeUnit.MILLISECONDS.sleep(500);
                    now = System.currentTimeMillis();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            return List.of(content);
        }

        private void cancellationHook() {
            latch.countDown();
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
        public Function<McpRequest, List<McpToolContent>> tool() {
            return this::process;
        }

        private List<McpToolContent> process(McpRequest request) {
            AtomicReference<McpToolContent> content = new AtomicReference<>(McpToolContents.textContent("Failed"));
            McpCancellation cancellation = request.features().cancellation();
            cancellation.registerCancellationHook(() -> cancellationHook(content));
            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return List.of(content.get());
        }

        private void cancellationHook(AtomicReference<McpToolContent> content) {
            latch.countDown();
        }
    }
}
