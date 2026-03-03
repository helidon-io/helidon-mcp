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

import java.util.List;
import java.util.Optional;

final class McpPromptImpl implements McpPrompt {
    private final McpPromptConfig config;

    McpPromptImpl(McpPromptConfig config) {
        this.config = config;
    }

    @Override
    public String name() {
        return config.name();
    }

    @Override
    public String description() {
        return config.description();
    }

    @Override
    public List<McpPromptArgument> arguments() {
        return config.arguments();
    }

    @Override
    public McpPromptResult prompt(McpPromptRequest request) {
        return config.prompt().apply(request);
    }

    @Override
    public Optional<String> title() {
        return config.title();
    }

    @Override
    public McpPromptConfig prototype() {
        return config;
    }
}
