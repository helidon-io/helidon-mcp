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
import java.util.function.Function;

import io.helidon.extensions.mcp.server.McpCompletion;
import io.helidon.extensions.mcp.server.McpCompletionContent;
import io.helidon.extensions.mcp.server.McpCompletionContents;
import io.helidon.extensions.mcp.server.McpCompletionRequest;
import io.helidon.extensions.mcp.server.McpCompletionType;

/**
 * Auto-completion for {@link CalendarEventResourceTemplate}.
 */
final class CalendarEventResourceCompletion implements McpCompletion {
    private final Calendar calendar;

    CalendarEventResourceCompletion(Calendar calendar) {
        this.calendar = calendar;
    }

    @Override
    public String reference() {
        return Calendar.EVENTS_URI_TEMPLATE;
    }

    @Override
    public McpCompletionType referenceType() {
        return McpCompletionType.RESOURCE;
    }

    @Override
    public Function<McpCompletionRequest, McpCompletionContent> completion() {
        return this::complete;
    }

    private McpCompletionContent complete(McpCompletionRequest request) {
        List<String> values = calendar.readEventNames()
                .stream()
                .filter(name -> name.contains(request.value()))
                .toList();
        return McpCompletionContents.completion(values);
    }
}
