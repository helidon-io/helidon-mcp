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

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

final class McpCompletionContextImpl implements McpCompletionContext {
    private final Map<String, String> arguments;

    McpCompletionContextImpl(McpParameters parameters) {
        this.arguments = parameters.get("arguments").as(this::parse).orElseGet(HashMap::new);
    }

    @Override
    public Map<String, String> arguments() {
        return arguments;
    }

    private Map<String, String> parse(McpParameters parameters) {
        return parameters.asMap()
                .orElseGet(HashMap::new)
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                                          e -> e.getValue().asString().orElse("")));
    }
}
