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

/**
 * Configuration of an MCP Completion.
 */
public interface McpCompletion {
    /**
     * Completion reference must be a {@link McpPromptArgument} name or a {@link McpResource} uri template.
     *
     * @return completion reference
     */
    String reference();

    /**
     * The reference type of this completion.
     *
     * @return reference type
     */
    default McpCompletionType referenceType() {
        return McpCompletionType.PROMPT;
    }

    /**
     * Completion request handler.
     *
     * @param request completion request
     * @return completion suggestion
     */
    McpCompletionResult completion(McpCompletionRequest request);
}
