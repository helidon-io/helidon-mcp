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

package io.helidon.extensions.mcp.examples.calendar;

import java.util.List;
import java.util.function.Function;

import io.helidon.extensions.mcp.server.McpParameters;
import io.helidon.extensions.mcp.server.McpRequest;
import io.helidon.extensions.mcp.server.McpTool;
import io.helidon.extensions.mcp.server.McpToolContent;
import io.helidon.extensions.mcp.server.McpToolContents;

/**
 * MCP tool to list calendar events. Available as an alternative to using
 * resources.
 */
final class ListCalendarEventTool implements McpTool {
    private static final String SCHEMA = """
            {
                "type": "object",
                "description": "List calendar events",
                "properties": {
                    "date": {
                        "description": "Event date in the following format YYYY-MM-DD",
                        "type": "string"
                    }
                }
            }
            """;

    private final Calendar calendar;

    ListCalendarEventTool(Calendar calendar) {
        this.calendar = calendar;
    }

    @Override
    public String name() {
        return "list-calendar-event";
    }

    @Override
    public String description() {
        return "List calendar events.";
    }

    @Override
    public String schema() {
        return SCHEMA;
    }

    @Override
    public Function<McpRequest, List<McpToolContent>> tool() {
        return this::listCalendarEvents;
    }

    private List<McpToolContent> listCalendarEvents(McpRequest request) {
        McpParameters mcpParameters = request.parameters();

        String date = mcpParameters.get("date")
                .asString()
                .orElse(null);

        String entries = calendar.readContentMatchesLine(line -> date == null || line.contains(date));
        return List.of(McpToolContents.textContent(entries));
    }
}
