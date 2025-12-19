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

import java.util.List;

import io.helidon.json.schema.JsonSchema;

/**
 * Calendar Event.
 */
@JsonSchema.Schema
@JsonSchema.Title("Calendar Event")
public class CalendarEvent {

    @JsonSchema.Required
    @JsonSchema.Description("Calendar event name")
    private String name;

    @JsonSchema.Required
    @JsonSchema.Description("Calendar event date")
    private String date;

    @JsonSchema.Required
    @JsonSchema.Description("Calendar event attendees")
    private List<String> attendees;

    /**
     * Create a new instance of {@code CalendarEvent}.
     */
    public CalendarEvent() {
    }

    /**
     * Get event name.
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Get event date.
     *
     * @return date
     */
    public String getDate() {
        return date;
    }

    /**
     * Get event attendees.
     *
     * @return attendees
     */
    public List<String> getAttendees() {
        return attendees;
    }

    /**
     * Set event name.
     *
     * @param name the event name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Set event date.
     *
     * @param date the event date
     */
    public void setDate(String date) {
        this.date = date;
    }

    /**
     * Set event attendees.
     *
     * @param attendees the event attendees
     */
    public void setAttendees(List<String> attendees) {
        this.attendees = attendees;
    }
}
