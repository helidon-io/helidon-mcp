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

import io.helidon.common.context.Context;

final class McpCompletionRequestImpl implements McpCompletionRequest {
    private final String name;
    private final String value;
    private final McpRequest request;
    private final McpCompletionContext context;

    McpCompletionRequestImpl(McpRequest request) {
        McpParameters parameters = request.parameters();
        this.request = request;
        this.name = parameters.get("argument").get("name").asString()
                .orElseThrow(() -> new McpInternalException("Completion request is missing parameter 'name'"));
        this.value = parameters.get("argument").get("value").asString()
                .orElseThrow(() -> new McpInternalException("Completion request is missing parameter 'value'"));
        this.context = parameters.get("context").as(McpCompletionContextImpl::new).orElse(null);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String value() {
        return value;
    }

    @Override
    public Optional<McpCompletionContext> context() {
        return Optional.ofNullable(context);
    }

    @Override
    public McpParameters parameters() {
        return request.parameters();
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
