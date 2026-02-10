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

import java.time.Duration;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * A request from the server to elicit additional information from the user via the client.
 */
@Prototype.Blueprint
interface McpElicitationRequestBlueprint {
    /**
     * The message to present to the user.
     *
     * @return the message
     */
    String message();

    /**
     * A JSON schema to format client response.
     *
     * @return the schema
     */
    String schema();

    /**
     * Elicitation request timeout. Default value is 5 seconds.
     *
     * @return the timeout
     */
    @Option.Default("PT5S")
    Duration timeout();
}
