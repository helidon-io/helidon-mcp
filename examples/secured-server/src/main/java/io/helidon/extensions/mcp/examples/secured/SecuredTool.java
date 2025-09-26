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

package io.helidon.extensions.mcp.examples.secured;

import java.util.List;
import java.util.function.Function;

import io.helidon.extensions.mcp.server.McpRequest;
import io.helidon.extensions.mcp.server.McpTool;
import io.helidon.extensions.mcp.server.McpToolContent;
import io.helidon.extensions.mcp.server.McpToolContents;
import io.helidon.security.SecurityContext;

/**
 * A tool returning the logged username.
 */
final class SecuredTool implements McpTool {

    @Override
    public String name() {
        return "secured-tool";
    }

    @Override
    public String description() {
        return "A tool secured by MCP OIDC.";
    }

    @Override
    public String schema() {
        return "";
    }

    @Override
    public Function<McpRequest, List<McpToolContent>> tool() {
        return request -> {
            String username = request.requestContext()
                    .get(SecurityContext.class)
                    .map(SecurityContext::userName)
                    .orElse("Unknown");
            return List.of(McpToolContents.textContent("Username: " + username));
        };
    }
}
