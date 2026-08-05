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
package io.helidon.extensions.mcp.server;

import java.util.Optional;

/**
 * The client's response to an elicitation request.
 * Form elicitation responses contain submitted data only when {@link #action()} is
 * {@link McpElicitationAction#ACCEPT}; URL elicitation responses never contain submitted data.
 * For URL elicitation, {@code ACCEPT} means only that the user consented to open and interact at the URL; it does not
 * indicate that the out-of-band interaction has completed.
 */
public sealed interface McpElicitationResponse permits McpElicitationResponseImpl {
    /**
     * The elicitation result action from the client.
     *
     * @return action
     */
    McpElicitationAction action();

    /**
     * The submitted form data. Content is present only when {@link #action()} is
     * {@link McpElicitationAction#ACCEPT} and the response belongs to a form elicitation request.
     * URL elicitation responses and responses with any other action have no content.
     * Form values can be strings, numbers, booleans, or, starting with MCP protocol version
     * {@code 2025-11-25}, lists of strings for multi-select enum fields. Use
     * {@link McpParameters#asList(Class)} to read a multi-select value.
     *
     * @return values matching the requested schema, if present
     */
    Optional<McpParameters> content();
}
