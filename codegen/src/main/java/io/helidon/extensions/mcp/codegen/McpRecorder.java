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
package io.helidon.extensions.mcp.codegen;

import java.util.ArrayList;
import java.util.List;

import io.helidon.common.types.TypeName;

/**
 * MCP recorder tracks the generated classes in order to register them to the server.
 */
class McpRecorder {
    private final List<TypeName> tools;
    private final List<TypeName> prompts;
    private final List<TypeName> resources;
    private final List<TypeName> completions;
    private final List<TypeName> subscribers;
    private final List<TypeName> unsubscribers;

    McpRecorder() {
        tools = new ArrayList<>();
        prompts = new ArrayList<>();
        resources = new ArrayList<>();
        completions = new ArrayList<>();
        subscribers = new ArrayList<>();
        unsubscribers = new ArrayList<>();
    }

    void tool(TypeName type) {
        tools.add(type);
    }

    void prompt(TypeName type) {
        prompts.add(type);
    }

    void resource(TypeName type) {
        resources.add(type);
    }

    void completion(TypeName type) {
        completions.add(type);
    }

    void subscriber(TypeName type) {
        subscribers.add(type);
    }

    void unsubscriber(TypeName type) {
        unsubscribers.add(type);
    }

    List<TypeName> tools() {
        return tools;
    }

    List<TypeName> prompts() {
        return prompts;
    }

    List<TypeName> resources() {
        return resources;
    }

    List<TypeName> completions() {
        return completions;
    }

    List<TypeName> subscribers() {
        return subscribers;
    }

    List<TypeName> unsubscribers() {
        return unsubscribers;
    }

    void clear() {
        tools.clear();
        prompts.clear();
        resources.clear();
        completions.clear();
        subscribers.clear();
        unsubscribers.clear();
    }
}
