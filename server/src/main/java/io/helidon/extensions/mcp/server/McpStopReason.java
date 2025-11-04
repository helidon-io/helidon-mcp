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

package io.helidon.extensions.mcp.server;

/**
 * Sampling request stop reasons.
 */
public enum McpStopReason {
    /**
     * End turn.
     */
    END_TURN,
    /**
     * Stop sequence.
     */
    STOP_SEQUENCE,
    /**
     * Max tokens.
     */
    MAX_TOKENS;

    String text() {
        return this.name().toLowerCase().replace("_", "");
    }

    static McpStopReason map(String reason) {
        reason = reason.toLowerCase();
        for (McpStopReason stopReason : McpStopReason.values()) {
            if (stopReason.text().equals(reason)) {
                return stopReason;
            }
        }
        throw new IllegalArgumentException("Unknown stop reason: " + reason);
    }
}
