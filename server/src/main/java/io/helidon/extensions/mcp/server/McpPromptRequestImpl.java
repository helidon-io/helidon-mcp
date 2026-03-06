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

import io.helidon.common.context.Context;

final class McpPromptRequestImpl implements McpPromptRequest {
    private final McpRequest request;

    McpPromptRequestImpl(McpRequest request) {
        this.request = request;
    }

    @Override
    public McpParameters arguments() {
        return request.parameters().get("arguments");
    }

    @Override
    public String name() {
        return request.parameters()
                .get("name")
                .asString()
                .orElseThrow(() -> new IllegalStateException("Prompt request name is missing"));
    }

    @Override
    public McpParameters parameters() {
        return request.parameters();
    }

    @Override
    public McpParameters meta() {
        return request.meta();
    }

    @Override
    public McpFeatures features() {
        return request.features();
    }

    @Override
    public String protocolVersion() {
        return request.protocolVersion();
    }

    @Override
    public Context sessionContext() {
        return request.sessionContext();
    }

    @Override
    public Context requestContext() {
        return request.requestContext();
    }
}
