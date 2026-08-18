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

import io.helidon.common.context.Context;
import io.helidon.json.JsonObject;

/**
 * An MCP client response and the context of the request that delivered it.
 */
sealed interface McpResponse permits McpResponseImpl {
    /**
     * Identifier of the JSON-RPC request.
     *
     * @return request identifier
     */
    long id();

    /**
     * JSON-RPC response.
     *
     * @return response object
     */
    JsonObject asJsonObject();

    /**
     * Context of the request that delivered the response.
     *
     * @return request context
     */
    Context requestContext();
}
