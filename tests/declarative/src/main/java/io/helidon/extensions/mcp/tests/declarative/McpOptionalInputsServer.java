/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
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
import java.util.Optional;

import io.helidon.extensions.mcp.server.Mcp;
import io.helidon.extensions.mcp.server.McpToolRequest;
import io.helidon.extensions.mcp.tests.declarative.model.Note;

@Mcp.Server
@Mcp.Path("/optional-inputs")
@Mcp.Stateless
class McpOptionalInputsServer {
    @Mcp.Tool("Update a note with optional values")
    String update(@Mcp.Required String id,
                  @Mcp.Description("Use \"auto\" for C:\\notes.\nOtherwise supply a title.") Optional<String> title,
                  Optional<Integer> limit,
                  Optional<Boolean> enabled) {
        return id + "|" + title.map(value -> "set[" + value + "]").orElse("keep")
                + "|" + limit.orElse(20) + "|" + enabled.orElse(true);
    }

    @Mcp.Tool("Read an optional collection")
    String collection(Optional<List<String>> values) {
        return values.map(Object::toString).orElse("missing");
    }

    @Mcp.Tool("Read an optional object")
    String object(Optional<Note> note) {
        return note.map(Note::title).orElse("missing");
    }

    @Mcp.Tool("Read an optional collection of notes")
    String notes(Optional<List<Note>> notes) {
        return notes.map(values -> String.join(",", values.stream().map(Note::title).toList())).orElse("missing");
    }

    @Mcp.Tool("Preserve client argument names that match generated identifiers")
    String reservedNames(Optional<String> request, Optional<String> delegate, String argument0, McpToolRequest toolRequest) {
        return request.orElse("missing") + "|" + delegate.orElse("missing") + "|" + argument0 + "|" + toolRequest.name();
    }

    @Mcp.Tool("Require a nullable title argument")
    String requiredOptional(@Mcp.Required Optional<String> title) {
        return title.orElse("missing");
    }

    @Mcp.Tool("Read optional numeric values")
    String numbers(Optional<Byte> small,
                   Optional<Short> medium,
                   Optional<Long> large,
                   Optional<Float> decimal,
                   Optional<Double> precise) {
        return small + "|" + medium + "|" + large + "|" + decimal + "|" + precise;
    }

    @Mcp.Tool("Preserve existing argument fallbacks")
    String legacy(String title, Boolean enabled, Integer limit, List<String> values, Note note) {
        return "[" + title + "]|" + enabled + "|" + limit + "|" + values + "|" + note;
    }
}
