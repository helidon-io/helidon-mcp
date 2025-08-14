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

package io.helidon.mcp.server;

import java.util.List;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

@Prototype.Blueprint
@Prototype.Configured("mcp.server")
interface McpServerConfigBlueprint extends Prototype.Factory<McpServerFeature> {

    @Option.Configured
    @Option.Default("/mcp")
    String path();

    @Option.Configured
    @Option.Default("helidon-mcp-server")
    String name();

    @Option.Configured
    @Option.Default("0.0.1")
    String version();

    @Option.Configured
    @Option.DefaultBoolean(true)
    boolean logging();

    @Option.Singular
    List<McpTool> tools();

    @Option.Singular
    List<McpPrompt> prompts();

    @Option.Singular
    List<McpResource> resources();

    @Option.Singular
    List<McpCompletion> completions();
}
