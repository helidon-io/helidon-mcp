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

package io.helidon.extensions.mcp.examples.calendar.declarative;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.extensions.mcp.server.Mcp;
import io.helidon.extensions.mcp.server.McpCompletionContent;
import io.helidon.extensions.mcp.server.McpCompletionContents;
import io.helidon.extensions.mcp.server.McpCompletionType;
import io.helidon.extensions.mcp.server.McpException;
import io.helidon.extensions.mcp.server.McpFeatures;
import io.helidon.extensions.mcp.server.McpLogger;
import io.helidon.extensions.mcp.server.McpParameters;
import io.helidon.extensions.mcp.server.McpProgress;
import io.helidon.extensions.mcp.server.McpPromptContent;
import io.helidon.extensions.mcp.server.McpPromptContents;
import io.helidon.extensions.mcp.server.McpResourceContent;
import io.helidon.extensions.mcp.server.McpResourceContents;
import io.helidon.extensions.mcp.server.McpRole;
import io.helidon.extensions.mcp.server.McpToolContent;
import io.helidon.extensions.mcp.server.McpToolContents;
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
    List<McpToolContent> listCalendarEvent(String date) {
        String entries = calendar.readContentMatchesLine(
                line -> date.isEmpty() || line.contains("date: " + date));
        return List.of(McpToolContents.textContent(entries));
    }

    /**
     * Tool that adds a new calendar event with a name, date and list of
     * attendees.
     *
     * @param features  the MCP features
     * @param name      the event's name
     * @param date      the event's date
     * @param attendees the list of attendees
     * @return text confirming event being created
     */
    @Mcp.Tool("Adds a new event to the calendar")
    List<McpToolContent> addCalendarEvent(McpFeatures features,
                                          String name,
                                          String date,
                                          List<String> attendees) {
        if (name.isEmpty() || date.isEmpty() || attendees.isEmpty()) {
            throw new McpException("Missing required arguments name, date or attendees");
        }

        McpLogger logger = features.logger();
        McpProgress progress = features.progress();
        progress.total(100);
        logger.info("Request to add new event");
        progress.send(0);
        calendar.createNewEvent(name, date, attendees);
        progress.send(50);
        features.subscriptions().sendUpdate(calendar.uri());
        progress.send(100);

        return List.of(McpToolContents.textContent("New event added to the calendar"));
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
    List<McpResourceContent> eventsResource(McpLogger logger) {
        logger.debug("Reading calendar events from registry...");
        String content = calendar.readContent();
        return List.of(McpResourceContents.textContent(content));
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
    List<McpResourceContent> eventResourceTemplate(McpLogger logger, String name) {
        logger.debug("Reading calendar events from registry...");
        String content = calendar.readContentMatchesLine(line -> line.contains("name: " + name));
        return List.of(McpResourceContents.textContent(content));
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
    McpCompletionContent eventResourceTemplateCompletion(String nameValue) {
        List<String> values = calendar.readEventNames()
                .stream()
                .filter(name -> name.contains(nameValue))
                .toList();
        return McpCompletionContents.completion(values.toArray(new String[0]));
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
    List<McpPromptContent> createEventPrompt(McpLogger logger,
                                             @Mcp.Description("event's name") String name,
                                             @Mcp.Description("event's date") String date,
                                             @Mcp.Description("event's attendees") String attendees) {
        logger.debug("Creating calendar event prompt...");
        return List.of(McpPromptContents.textContent(
                """
                        Create a new calendar event with name %s, on %s with attendees %s. Make
                        sure all attendees are free to attend the event.
                        """.formatted(name, date, attendees), McpRole.USER));
    }

    /**
     * Completion for prompt.
     *
     * @param parameters the MCP parameters
     * @return list of possible values or empty
     */
    @Mcp.Completion(value = "createEventPrompt",
                    type = McpCompletionType.PROMPT)
    McpCompletionContent createEventPromptCompletion(McpParameters parameters) {
        String promptName = parameters.get("name").asString().orElse(null);
        if ("name".equals(promptName)) {
            return McpCompletionContents.completion("Frank & Friends");
        }
        if ("date".equals(promptName)) {
            LocalDate today = LocalDate.now();
            String[] dates = new String[3];
            for (int i = 0; i < dates.length; i++) {
                dates[i] = today.plusDays(i).format(FORMATTER);
            }
            return McpCompletionContents.completion(dates);
        }
        if ("attendees".equals(promptName)) {
            return McpCompletionContents.completion(FRIENDS);
        }
        // no completion
        return McpCompletionContents.completion();
    }
}
