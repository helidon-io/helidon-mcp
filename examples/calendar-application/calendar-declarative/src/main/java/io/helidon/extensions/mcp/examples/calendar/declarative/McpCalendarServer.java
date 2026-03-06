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

package io.helidon.extensions.mcp.examples.calendar.declarative;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.extensions.mcp.server.Mcp;
import io.helidon.extensions.mcp.server.McpCompletionResult;
import io.helidon.extensions.mcp.server.McpCompletionType;
import io.helidon.extensions.mcp.server.McpException;
import io.helidon.extensions.mcp.server.McpFeatures;
import io.helidon.extensions.mcp.server.McpLogger;
import io.helidon.extensions.mcp.server.McpParameters;
import io.helidon.extensions.mcp.server.McpProgress;
import io.helidon.extensions.mcp.server.McpPromptResult;
import io.helidon.extensions.mcp.server.McpResourceResult;
import io.helidon.extensions.mcp.server.McpRole;
import io.helidon.extensions.mcp.server.McpToolResult;
import io.helidon.service.registry.Service;

@Mcp.Path("/calendar")
@Mcp.Server("helidon-mcp-calendar-manager")
class McpCalendarServer {
    static final String[] FRIENDS = new String[] {
            "Frank, Tweety", "Frank, Daffy", "Frank, Tweety, Daffy"
    };
    static final String EVENTS_URI = "file://events";
    static final String EVENTS_URI_TEMPLATE = EVENTS_URI + "/{name}";
    static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Service.Inject
    Calendar calendar;

    // -- Tools ---------------------------------------------------------------

    /**
     * Tool that returns all calendar events on a certain date or all events
     * if no date is provided.
     *
     * @param date the date or {@code ""} if not provided
     * @return list of calendar events
     */
    @Mcp.Tool("List calendar events")
    McpToolResult listCalendarEvents(String date) {
        String entries = calendar.readContentMatchesLine(
                line -> date.isEmpty() || line.contains("date: " + date));
        return McpToolResult.create(entries);
    }

    /**
     * Tool that adds a new calendar event with a name, date and list of
     * attendees.
     *
     * @param features the MCP features
     * @param event    the event
     * @return text confirming event being created
     */
    @Mcp.Tool("Adds a new event to the calendar")
    McpToolResult addCalendarEvent(McpFeatures features, CalendarEvent event) {
        if (event.getName().isEmpty() || event.getDate().isEmpty() || event.getAttendees().isEmpty()) {
            throw new McpException("Missing required arguments name, date or attendees");
        }

        McpLogger logger = features.logger();
        McpProgress progress = features.progress();
        progress.total(100);
        logger.info("Request to add new event");
        progress.send(0);
        calendar.createNewEvent(event.getName(), event.getDate(), event.getAttendees());
        progress.send(50);
        features.subscriptions().sendUpdate(EVENTS_URI);
        progress.send(100);

        return McpToolResult.create("New event added to the calendar");
    }

    // -- Resources -----------------------------------------------------------

    /**
     * Resource whose representation is a list of all calendar events created.
     *
     * @param logger the MCP logger
     * @return text with a list of calendar events
     */
    @Mcp.Resource(uri = EVENTS_URI,
                  mediaType = MediaTypes.TEXT_PLAIN_VALUE,
                  description = "List of calendar events created")
    McpResourceResult eventsResource(McpLogger logger) {
        logger.debug("Reading calendar events from registry...");
        String content = calendar.readContent();
        return McpResourceResult.create(content);
    }

    /**
     * Resource whose representation is a single calendar event given its name.
     *
     * @param logger the MCP logger
     * @param name   the event's name
     * @return text with calendar event lines or empty line if not found
     */
    @Mcp.Resource(uri = EVENTS_URI_TEMPLATE,
                  mediaType = MediaTypes.TEXT_PLAIN_VALUE,
                  description = "List single calendar event by name")
    McpResourceResult eventResourceTemplate(McpLogger logger, String name) {
        logger.debug("Reading calendar events from registry...");
        String content = calendar.readContentMatchesLine(line -> line.contains("name: " + name));
        return McpResourceResult.builder().addTextContent(content).build();
    }

    /**
     * Completion for event resource template that returns possible event names
     * containing {@code nameValue} as a substring.
     *
     * @param nameValue the value for the name template argument
     * @return list of possible values or empty
     */
    @Mcp.Completion(value = EVENTS_URI_TEMPLATE,
                    type = McpCompletionType.RESOURCE)
    McpCompletionResult eventResourceTemplateCompletion(String nameValue) {
        List<String> values = calendar.readEventNames()
                .stream()
                .filter(name -> name.contains(nameValue))
                .toList();
        return McpCompletionResult.create(values);
    }

    // -- Prompts -------------------------------------------------------------

    /**
     * Prompt to create a new event given a name, date and attendees.
     *
     * @param logger    the MCP logger
     * @param name      the event's name
     * @param date      the event's date
     * @param attendees the list of attendees
     * @return text with prompt
     */
    @Mcp.Prompt("Prompt to create a new event given a name, date and attendees")
    McpPromptResult createEventPrompt(McpLogger logger,
                                      @Mcp.Description("event's name") String name,
                                      @Mcp.Description("event's date") String date,
                                      @Mcp.Description("event's attendees") String attendees) {
        logger.debug("Creating calendar event prompt...");
        return McpPromptResult.builder()
                .addTextContent(t -> t
                        .text("""
                                  Create a new calendar event with name %s, on %s with attendees %s. Make
                                  sure all attendees are free to attend the event.
                                  """.formatted(name, date, attendees))
                        .role(McpRole.USER))
                .build();
    }

    /**
     * Completion for prompt.
     *
     * @param parameters the MCP parameters
     * @return list of possible values or empty
     */
    @Mcp.Completion(value = "createEventPrompt",
                    type = McpCompletionType.PROMPT)
    McpCompletionResult createEventPromptCompletion(McpParameters parameters) {
        String promptName = parameters.get("argument").get("name").asString().orElse(null);
        if ("name".equals(promptName)) {
            return McpCompletionResult.create("Frank & Friends");
        }
        if ("date".equals(promptName)) {
            LocalDate today = LocalDate.now();
            String[] dates = new String[3];
            for (int i = 0; i < dates.length; i++) {
                dates[i] = today.plusDays(i).format(FORMATTER);
            }
            return McpCompletionResult.create(dates);
        }
        if ("attendees".equals(promptName)) {
            return McpCompletionResult.create(FRIENDS);
        }
        // no completion
        return McpCompletionResult.create();
    }
}
