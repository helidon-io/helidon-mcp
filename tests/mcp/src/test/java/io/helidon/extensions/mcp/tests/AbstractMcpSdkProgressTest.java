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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

abstract class AbstractMcpSdkProgressTest extends AbstractMcpSdkTest {

    private final List<McpSchema.ProgressNotification> messages = new ArrayList<>();

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        ProgressNotifications.setUpRoute(builder);
    }

    @Test
    void testMcpSdkProgress() {
        McpSchema.CallToolRequest request = McpSchema.CallToolRequest.builder()
                .name("progress")
                .arguments(Map.of())
                .progressToken("atoken")
                .build();
        client().callTool(request);
    }

    protected List<McpSchema.ProgressNotification> messages() {
        return messages;
    }
}
