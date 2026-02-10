/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
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
package io.helidon.extensions.mcp.server;

import java.util.Optional;

import io.helidon.jsonrpc.core.JsonRpcParams;

import jakarta.json.JsonObject;

final class McpElicitationResponseImpl implements McpElicitationResponse {
    private final McpParameters content;
    private final McpElicitationAction action;

    McpElicitationResponseImpl(McpElicitationAction action, JsonObject content) {
        this.action = action;
        this.content = content != null
                ? new McpParameters(JsonRpcParams.create(content), content)
                : null;
    }

    @Override
    public McpElicitationAction action() {
        return action;
    }

    @Override
    public Optional<McpParameters> content() {
        return Optional.ofNullable(content);
    }
}
