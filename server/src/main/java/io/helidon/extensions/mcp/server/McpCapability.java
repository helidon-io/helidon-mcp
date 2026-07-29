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

enum McpCapability {
    TOOL_LIST_CHANGED,
    RESOURCE_LIST_CHANGED,
    RESOURCE_SUBSCRIBE,
    PROMPT_LIST_CHANGED,
    LOGGING,
    ELICITATION,
    ELICITATION_FORM,
    ELICITATION_URL,
    COMPLETION,
    PAGINATION,
    SAMPLING,
    SAMPLING_CONTEXT,
    SAMPLING_TOOLS,
    ROOTS,
    PROGRESS;

    String text() {
        return this.name().toLowerCase();
    }
}
