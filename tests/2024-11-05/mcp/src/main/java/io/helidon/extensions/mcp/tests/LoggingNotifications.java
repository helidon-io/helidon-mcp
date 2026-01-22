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

package io.helidon.extensions.mcp.tests;

import io.helidon.extensions.mcp.server.McpServerFeature;
import io.helidon.extensions.mcp.server.McpTool;
import io.helidon.extensions.mcp.server.McpToolContents;
import io.helidon.extensions.mcp.server.McpToolResult;
import io.helidon.webserver.http.HttpRouting;

class LoggingNotifications {

    private LoggingNotifications() {
    }

    static void setUpRoute(HttpRouting.Builder builder) {
        builder.addFeature(McpServerFeature.builder()
                                   .path("")
                                   .addTool(McpTool.builder()
                                                    .tool(request -> {
                                                        request.features().logger().info("Logging data");
                                                        request.features().logger().debug("Logging data");
                                                        return McpToolResult.builder()
                                                                .addContent(McpToolContents.textContent("Dummy text"))
                                                                .build();
                                                    })
                                                    .description("A tool that uses logging")
                                                    .name("logging")
                                                    .schema("")
                                                    .build()));
    }
}
