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

import java.util.Objects;

import io.helidon.builder.api.Prototype;

class McpSamplingSupport {
    private McpSamplingSupport() {
    }

    /**
     * Add a sampling text message to the sampling request from the provided text.
     *
     * @param builder sampling request builder
     * @param text    text message
     */
    @Prototype.BuilderMethod
    static void addTextMessage(McpSamplingRequest.BuilderBase<?, ?> builder, String text) {
        addTextMessage(builder, McpRole.ASSISTANT, text);
    }

    /**
     * Add a sampling text message to the sampling request from the provided role and text.
     *
     * @param builder sampling request builder
     * @param role    message role
     * @param text    text message
     */
    @Prototype.BuilderMethod
    static void addTextMessage(McpSamplingRequest.BuilderBase<?, ?> builder, McpRole role, String text) {
        Objects.requireNonNull(role, "role is null");
        Objects.requireNonNull(text, "text is null");
        builder.addMessage(McpSamplingMessage.builder()
                                   .role(role)
                                   .addContent(McpSamplingTextContent.create(text))
                                   .build());
    }

    /**
     * Whether this request uses sampling tools through tool names, a tool choice, or tool messages.
     *
     * @param request sampling request
     * @return {@code true} if this request uses sampling tools
     */
    @Prototype.PrototypeMethod
    static boolean usesTool(McpSamplingRequest request) {
        return !request.tools().isEmpty()
                || request.toolChoice().isPresent()
                || request.messages().stream()
                        .flatMap(message -> message.contents().stream())
                        .map(McpSamplingContent::type)
                        .anyMatch(type -> type == McpSamplingContentType.TOOL_USE
                                || type == McpSamplingContentType.TOOL_RESULT);
    }

    /**
     * Whether this request uses sampling context inclusion.
     *
     * @param request sampling request
     * @return {@code true} if this request includes context from this or other servers
     */
    @Prototype.PrototypeMethod
    static boolean usesContext(McpSamplingRequest request) {
        return request.includeContext()
                .filter(context -> context != McpIncludeContext.NONE)
                .isPresent();
    }
}
