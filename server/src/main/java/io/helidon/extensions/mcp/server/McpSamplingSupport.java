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

import java.util.ArrayList;
import java.util.List;
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
        Objects.requireNonNull(text, "text is null");
        builder.addTextMessage(b -> b.text(text).role(McpRole.ASSISTANT));
    }

    static List<McpSamplingMessage> aggregate(McpSamplingRequest request) {
        List<McpSamplingMessage> messages = new ArrayList<>();
        messages.addAll(request.textMessages());
        messages.addAll(request.imageMessages());
        messages.addAll(request.audioMessages());
        return messages;
    }
}
