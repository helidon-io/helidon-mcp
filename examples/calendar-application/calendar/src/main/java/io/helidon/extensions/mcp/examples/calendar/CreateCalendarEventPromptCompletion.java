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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

import io.helidon.extensions.mcp.server.McpCompletion;
import io.helidon.extensions.mcp.server.McpCompletionContent;
import io.helidon.extensions.mcp.server.McpCompletionContents;
import io.helidon.extensions.mcp.server.McpCompletionType;
import io.helidon.extensions.mcp.server.McpRequest;

/**
 * Auto-completion for {@link CreateCalendarEventPrompt}.
 */
final class CreateCalendarEventPromptCompletion implements McpCompletion {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final String[] FRIENDS = new String[] {
            "Frank, Tweety", "Frank, Daffy", "Frank, Tweety, Daffy"
    };

    @Override
    public String reference() {
        return CreateCalendarEventPrompt.PROMPT_NAME;
    }

    @Override
    public McpCompletionType referenceType() {
        return McpCompletionType.PROMPT;
    }

    @Override
    public Function<McpRequest, McpCompletionContent> completion() {
        return this::complete;
    }

    private McpCompletionContent complete(McpRequest request) {
        String name = request.parameters()
                .get("name")
                .asString()
                .orElse(null);
        if ("name".equals(name)) {
            return McpCompletionContents.completion("Frank & Friends");
        }

        String date = request.parameters()
                .get("name")
                .asString()
                .orElse(null);
        if ("date".equals(date)) {
            LocalDate today = LocalDate.now();
            String[] dates = new String[3];
            for (int i = 0; i < dates.length; i++) {
                dates[i] = today.plusDays(i).format(FORMATTER);
            }
            return McpCompletionContents.completion(dates);
        }

        String attendees = request.parameters()
                .get("name")
                .asString()
                .orElse(null);
        if ("attendees".equals(attendees)) {
            return McpCompletionContents.completion(FRIENDS);
        }

        // no completion
        return McpCompletionContents.completion();
    }
}
