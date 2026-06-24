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
import io.helidon.extensions.mcp.server.McpFeatures;
import io.helidon.extensions.mcp.server.McpRequest;
import io.helidon.extensions.mcp.server.McpToolRequest;
import io.helidon.extensions.mcp.server.McpToolResult;
import io.helidon.json.schema.JsonSchema;

@Mcp.Server
@Mcp.Path("/tools")
class McpToolsServer {
    public static final String TOOL_CONTENT = "Tool Content";
    public static final String TOOL_DESCRIPTION = "Tool description";
    public static final String OUTPUT_SCHEMA = "{\"type\":\"object\",\"properties\": {}}";
    public static final String OUTPUT_SCHEMA_MULTI_LINE = """
    {
        "type":"object",
        "properties": {
        }
    }""";

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
    McpToolResult tool2(String value, Foo foo) {
        return McpToolResult.builder()
                .addTextContent("value=" + value)
                .addTextContent("foo=" + foo.foo)
                .addTextContent("bar=" + foo.bar)
                .build();
    }

    @Mcp.Tool(TOOL_DESCRIPTION)
    McpToolResult tool3(McpFeatures features) {
        return McpToolResult.builder().addTextContent(TOOL_CONTENT).build();
    }

    @Mcp.Tool(TOOL_DESCRIPTION)
    McpToolResult tool4(Byte aByte) {
        return McpToolResult.builder().addTextContent(aByte.toString()).build();
    }

    @Mcp.Tool(TOOL_DESCRIPTION)
    McpToolResult tool5(Short aShort) {
        return McpToolResult.builder().addTextContent(aShort.toString()).build();
    }

    @Mcp.Tool(TOOL_DESCRIPTION)
    McpToolResult tool6(Integer aInteger) {
        return McpToolResult.builder().addTextContent(aInteger.toString()).build();
    }

    @Mcp.Tool(TOOL_DESCRIPTION)
    McpToolResult tool7(Long aLong) {
        return McpToolResult.builder().addTextContent(aLong.toString()).build();
    }

    @Mcp.Tool(TOOL_DESCRIPTION)
    McpToolResult tool8(Double aDouble) {
        return McpToolResult.builder().addTextContent(aDouble.toString()).build();
    }

    @Mcp.Tool(TOOL_DESCRIPTION)
    McpToolResult tool9(Float aFloat) {
        return McpToolResult.builder().addTextContent(aFloat.toString()).build();
    }

    @Mcp.Tool(TOOL_DESCRIPTION)
    String tool10(McpRequest request) {
        return TOOL_CONTENT;
    }

    @Mcp.Tool(TOOL_DESCRIPTION)
    McpToolResult tool11(McpRequest request) {
        return McpToolResult.builder()
                .addTextContent(TOOL_CONTENT)
                .build();
    }

    @Mcp.Tool(TOOL_DESCRIPTION)
    McpToolResult tool12(McpRequest request) {
        return McpToolResult.builder()
                .addTextContent(TOOL_CONTENT)
                .build();
    }

    @Mcp.Tool(value = TOOL_DESCRIPTION)
    @Mcp.ToolOutputSchemaText(OUTPUT_SCHEMA)
    McpToolResult tool13(McpRequest request) {
        return McpToolResult.builder()
                .addTextContent(TOOL_CONTENT)
                .build();
    }

    @Mcp.Tool(value = TOOL_DESCRIPTION)
    McpToolResult tool14(McpToolRequest request) {
        return McpToolResult.builder()
                .addTextContent(TOOL_CONTENT)
                .build();
    }

    @Mcp.Tool(value = TOOL_DESCRIPTION)
    @Mcp.ToolOutputSchema(OutputSchema.class)
    McpToolResult tool15(McpToolRequest request) {
        return McpToolResult.builder()
                .addTextContent(TOOL_CONTENT)
                .build();
    }

    @Mcp.Tool(value = TOOL_DESCRIPTION)
    @Mcp.ToolOutputSchemaText(OUTPUT_SCHEMA_MULTI_LINE)
    McpToolResult tool16(McpToolRequest request) {
        return McpToolResult.builder()
                .addTextContent(TOOL_CONTENT)
                .build();
    }

    @Mcp.Tool(value = TOOL_DESCRIPTION)
    @Mcp.ToolOutputSchema(OutputSchema.class)
    @Mcp.ToolOutputSchemaText(OUTPUT_SCHEMA)
    McpToolResult tool17(McpToolRequest request) {
        return McpToolResult.builder()
                .addTextContent(TOOL_CONTENT)
                .build();
    }

    @Mcp.Tool(value = TOOL_DESCRIPTION)
    @Mcp.ToolOutputSchema(Bar.class)
    McpToolResult tool18(McpToolRequest request) {
        return McpToolResult.builder()
                .addTextContent(TOOL_CONTENT)
                .build();
    }

    @Mcp.Tool("""
            Tool description block
            """)
    McpToolResult tool19(McpRequest request) {
        return McpToolResult.create(TOOL_CONTENT);
    }

    @Mcp.Tool("first line\n" + "second line\n")
    McpToolResult tool20(McpRequest request) {
        return McpToolResult.create(TOOL_CONTENT);
    }

    @Mcp.Tool(TOOL_DESCRIPTION)
    String tool21(@Mcp.Required String mandatory, String optional) {
        return "mandatory=" + mandatory + "|optional=" + optional;
    }

    @Mcp.Tool(TOOL_DESCRIPTION)
    String tool22(@Mcp.Required String a, @Mcp.Required Integer b) {
        return "a=" + a + "|b=" + b;
    }

    @JsonSchema.Schema
    public static class Foo {
        public String foo;
        public int bar;
    }

    @JsonSchema.Schema
    public static class OutputSchema {
    }

    @JsonSchema.Schema
    public record Bar(String bar) {
    }
}
