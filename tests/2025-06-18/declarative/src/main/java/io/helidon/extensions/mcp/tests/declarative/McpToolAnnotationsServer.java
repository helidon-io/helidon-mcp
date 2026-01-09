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

import io.helidon.extensions.mcp.server.Mcp;

@Mcp.Server
@Mcp.Path("/toolAnnotations")
class McpToolAnnotationsServer {
    public static final String TOOL_CONTENT = "Tool Content";
    public static final String TOOL_DESCRIPTION = "Tool description";

    @Mcp.Tool(value = TOOL_DESCRIPTION)     // default annotations
    String tool1() {
        return TOOL_CONTENT;
    }

    @Mcp.Tool(value = TOOL_DESCRIPTION,
              title = "tool2 title",        // non-default annotations
              readOnlyHint = true,
              destructiveHint = false,
              idempotentHint = true,
              openWorldHint = false)
    String tool2() {
        return TOOL_CONTENT;
    }
}
