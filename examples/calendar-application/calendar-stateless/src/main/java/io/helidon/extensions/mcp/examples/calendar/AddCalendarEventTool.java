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

package io.helidon.extensions.mcp.examples.calendar;

import java.util.List;

import io.helidon.common.mapper.OptionalValue;
import io.helidon.extensions.mcp.server.McpException;
import io.helidon.extensions.mcp.server.McpParameters;
import io.helidon.extensions.mcp.server.McpTool;
import io.helidon.extensions.mcp.server.McpToolRequest;
import io.helidon.extensions.mcp.server.McpToolResult;

/**
 * MCP tool to add a new event to the calendar.
 * <p>
 * This stateless variant avoids logging, progress, and resource subscription notifications because clients that skip
 * {@code initialize} do not have a durable MCP session or negotiated notification capabilities.
 */
final class AddCalendarEventTool implements McpTool {
    private static final String SCHEMA = """
            {
                "type": "object",
                "description": "Description of a new Event",
                "properties": {
                    "name": {
                        "description": "Event name",
                        "type": "string"
                    },
                    "date": {
                        "description": "Event date in the following format YYYY-MM-DD",
                        "type": "string"
                    },
                    "attendees": {
                        "description": "Event attendees",
                        "type": "array",
                        "items": {
                            "type": "string"
                        },
                        "minItems": 1
                    }
                },
                "required": [ "name", "date", "attendees" ]
            }
            """;
    private final Calendar calendar;

    AddCalendarEventTool(Calendar calendar) {
        this.calendar = calendar;
    }

    @Override
    public String name() {
        return "add-calendar-event";
    }

    @Override
    public String description() {
        return "Adds a new event to the calendar";
    }

    @Override
    public String schema() {
        return SCHEMA;
    }

    @Override
    public McpToolResult tool(McpToolRequest request) {
        McpParameters mcpParameters = request.arguments();
        String name = mcpParameters.get("name")
                .asString()
                .orElseThrow(() -> requiredArgument("name"));
        String date = mcpParameters.get("date")
                .asString()
                .orElseThrow(() -> requiredArgument("date"));
        List<String> attendees = mcpParameters.get("attendees")
                .asList()
                .map(values -> values.stream()
                        .map(McpParameters::asString)
                        .map(OptionalValue::get)
                        .toList())
                .orElseThrow(() -> requiredArgument("attendees"));

        calendar.createNewEvent(name, date, attendees);

        return McpToolResult.create("New event added to the calendar");
    }

    private RuntimeException requiredArgument(String argument) {
        return new McpException("Missing required argument: " + argument);
    }
}
