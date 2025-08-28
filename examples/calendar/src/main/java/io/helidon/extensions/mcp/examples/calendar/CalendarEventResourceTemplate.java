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

import io.helidon.common.media.type.MediaType;
import io.helidon.common.media.type.MediaTypes;
import io.helidon.extensions.mcp.server.McpException;
import io.helidon.extensions.mcp.server.McpRequest;
import io.helidon.extensions.mcp.server.McpResource;
import io.helidon.extensions.mcp.server.McpResourceContent;

/**
 * Resource template to help accessing to the event registry.
 */
final class CalendarEventResourceTemplate implements McpResource {
    private final Calendar calendar;

    CalendarEventResourceTemplate(Calendar calendar) {
        this.calendar = calendar;
    }

    @Override
    public String uri() {
        return calendar.uriTemplate();
    }

    @Override
    public String name() {
        return "calendar-events-resource-template";
    }

    @Override
    public String description() {
        return "Resource Template to find calendar events registry, path is \"calendar\"";
    }

    @Override
    public MediaType mediaType() {
        return MediaTypes.TEXT_PLAIN;
    }

    @Override
    public Function<McpRequest, List<McpResourceContent>> resource() {
        throw new McpException("Resource template cannot be read.");
    }
}
