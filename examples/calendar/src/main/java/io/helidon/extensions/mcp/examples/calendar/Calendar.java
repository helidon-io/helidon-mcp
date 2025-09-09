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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.helidon.extensions.mcp.server.McpException;

/**
 * The calendar is a temporary file containing a list of created events.
 */
final class Calendar {
    private final Path file;
    private final String uri;
    private final String uriTemplate;

    Calendar() {
        try {
            this.file = Files.createTempFile("calendar", "-calendar");
            this.uri = file.toUri().toString();
            this.uriTemplate = "file://events/{name}";
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    String uri() {
        return this.uri;
    }

    String uriTemplate() {
        return this.uriTemplate;
    }

    String readContent() {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    String readContentMatchesLine(Predicate<String> lineMatcher) {
        try {
            return Files.readAllLines(file)
                    .stream()
                    .filter(lineMatcher)
                    .collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    void createNewEvent(String name, String date, List<String> attendees) {
        try {
            String content = String.format("Event: { name: %s, date: %s, attendees: %s }\n", name, date, attendees);
            Files.writeString(file, content, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new McpException("Error happened when writing to event registry", e);
        }
    }
}
