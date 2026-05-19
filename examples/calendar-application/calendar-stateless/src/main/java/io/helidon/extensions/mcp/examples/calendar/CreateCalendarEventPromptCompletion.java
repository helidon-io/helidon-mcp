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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import io.helidon.extensions.mcp.server.McpCompletion;
import io.helidon.extensions.mcp.server.McpCompletionRequest;
import io.helidon.extensions.mcp.server.McpCompletionResult;
import io.helidon.extensions.mcp.server.McpCompletionType;

/**
 * Auto-completion for {@link CreateCalendarEventPrompt}.
 */
final class CreateCalendarEventPromptCompletion implements McpCompletion {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final List<String> FRIENDS = List.of("Frank, Tweety", "Frank, Daffy", "Frank, Tweety, Daffy");

    @Override
    public String reference() {
        return CreateCalendarEventPrompt.PROMPT_NAME;
    }

    @Override
    public McpCompletionType referenceType() {
        return McpCompletionType.PROMPT;
    }

    @Override
    public McpCompletionResult completion(McpCompletionRequest request) {
        String promptName = request.name();
        if ("name".equals(promptName)) {
            return McpCompletionResult.create("Frank & Friends");
        }
        if ("date".equals(promptName)) {
            LocalDate today = LocalDate.now();
            List<String> dates = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                dates.add(today.plusDays(i).format(FORMATTER));
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
