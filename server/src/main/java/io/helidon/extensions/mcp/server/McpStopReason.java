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

package io.helidon.extensions.mcp.server;

import java.util.Optional;

/**
 * Standard sampling response stop reasons recognized by this implementation.
 * <p>
 * The protocol also permits non-standard stop reasons. Their exact values are available through
 * {@link McpSamplingResponse#rawStopReason()}.
 */
public enum McpStopReason {
    /**
     * End turn.
     */
    END_TURN("endTurn"),
    /**
     * Stop sequence.
     */
    STOP_SEQUENCE("stopSequence"),
    /**
     * Max tokens.
     */
    MAX_TOKENS("maxTokens"),
    /**
     * Tool use.
     */
    TOOL_USE("toolUse");

    private final String text;

    McpStopReason(String text) {
        this.text = text;
    }

    String text() {
        return text;
    }

    static Optional<McpStopReason> from(String reason) {
        for (McpStopReason stopReason : McpStopReason.values()) {
            if (stopReason.text().equalsIgnoreCase(reason)) {
                return Optional.of(stopReason);
            }
        }
        return Optional.empty();
    }
}
