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
import java.util.Set;
import java.util.function.Function;

import io.helidon.extensions.mcp.server.McpException;
import io.helidon.extensions.mcp.server.McpLogger;
import io.helidon.extensions.mcp.server.McpParameters;
import io.helidon.extensions.mcp.server.McpPrompt;
import io.helidon.extensions.mcp.server.McpPromptArgument;
import io.helidon.extensions.mcp.server.McpPromptContent;
import io.helidon.extensions.mcp.server.McpPromptContents;
import io.helidon.extensions.mcp.server.McpRequest;
import io.helidon.extensions.mcp.server.McpRole;

/**
 * Prompt to create a new calendar event and add it to the calendar.
 */
final class CreateCalendarEventPrompt implements McpPrompt {

    @Override
    public String name() {
        return "create-event";
    }

    @Override
    public String description() {
        return "Create a new event and add it to the calendar";
    }

    @Override
    public Set<McpPromptArgument> arguments() {
        McpPromptArgument name = McpPromptArgument.builder()
                .name("name")
                .description("Event name")
                .required(true)
                .build();
        McpPromptArgument date = McpPromptArgument.builder()
                .name("date")
                .description("Event date in the following format YYYY-MM-DD")
                .required(true)
                .build();
        McpPromptArgument attendees = McpPromptArgument.builder()
                .name("attendees")
                .description("Event attendees names separated by commas")
                .required(true)
                .build();

        return Set.of(name, date, attendees);
    }

    @Override
    public Function<McpRequest, List<McpPromptContent>> prompt() {
        return this::createCalendarEvent;
    }

    private List<McpPromptContent> createCalendarEvent(McpRequest request) {
        McpLogger logger = request.features().logger();
        McpParameters mcpParameters = request.parameters();

        logger.debug("Creating calendar event prompt...");

        String name = mcpParameters.get("name")
                .asString()
                .orElseThrow(() -> requiredArgument("name"));
        String date = mcpParameters.get("date")
                .asString()
                .orElseThrow(() -> requiredArgument("date"));
        String attendees = mcpParameters.get("attendees")
                .asString()
                .orElseThrow(() -> requiredArgument("attendees"));

        logger.debug("Argument successfully parsed from client request");

        return List.of(McpPromptContents.textContent("""
                                                     Create a new calendar event with name %s, at date %s and attendees %s. Make
                                                     sure all attendees are free to attend the event.
                                                     """.formatted(name, date, attendees), McpRole.USER));
    }

    private RuntimeException requiredArgument(String argument) {
        return new McpException("Missing required argument: " + argument);
    }
}
