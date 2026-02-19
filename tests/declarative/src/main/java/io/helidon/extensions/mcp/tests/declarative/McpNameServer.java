/*
 * Copyright (c) 2025, 2026 Oracle and/or its affiliates.
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
package io.helidon.extensions.mcp.tests.declarative;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.extensions.mcp.server.Mcp;

@Mcp.Server
@Mcp.Path("/name")
class McpNameServer {

    @Mcp.Tool("Named tool")
    @Mcp.Name("my-tool")
    String tool() {
        return "My tool";
    }

    @Mcp.Resource(uri = "https://resource.com",
                  description = "My resource",
                  mediaType = MediaTypes.TEXT_PLAIN_VALUE)
    @Mcp.Name("my-resource")
    String resource() {
        return "My resource";
    }

    @Mcp.Prompt("Prompt description")
    @Mcp.Name("my-prompt")
    String prompt() {
        return "My prompt";
    }
}
