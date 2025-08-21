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

package io.helidon.extensions.mcp.tests.declarative;

import java.util.List;

import io.helidon.extensions.mcp.server.Mcp;
import io.helidon.extensions.mcp.server.McpFeatures;
import io.helidon.extensions.mcp.server.McpToolContent;
import io.helidon.extensions.mcp.server.McpToolContents;

@Mcp.Server
@Mcp.Path("/tools")
class McpToolsServer {
    public static final String TOOL_CONTENT = "Tool Content";
    public static final String TOOL_DESCRIPTION = "Tool description";

    @Mcp.Tool(TOOL_DESCRIPTION)
    String tool(String value, Foo foo) {
        return """
                value=%s
                foo=%s
                bar=%d
                """.formatted(value, foo.foo, foo.bar);
    }

    @Mcp.Tool(TOOL_DESCRIPTION)
    String tool1(McpFeatures features) {
        return TOOL_CONTENT;
    }

    @Mcp.Tool(TOOL_DESCRIPTION)
    List<McpToolContent> tool2(String value, Foo foo) {
        return List.of(McpToolContents.textContent("""
                value=%s
                foo=%s
                bar=%d
                """.formatted(value, foo.foo, foo.bar)));
    }

    @Mcp.Tool(TOOL_DESCRIPTION)
    List<McpToolContent> tool3(McpFeatures features) {
        return List.of(McpToolContents.textContent(TOOL_CONTENT));
    }

    @Mcp.JsonSchema("""
            {
                "type": "object",
                "properties": {
                    "foo": {
                        "type": "string"
                    },
                    "bar": {
                        "type": "integer"
                    }
                }
            }
            """)
    public static class Foo {
        public String foo;
        public int bar;
    }
}
