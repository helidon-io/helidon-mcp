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

import java.util.Objects;

import io.helidon.common.context.Context;
import io.helidon.json.JsonObject;

final class McpResponseImpl implements McpResponse {
    private final JsonObject response;
    private final Context requestContext;

    McpResponseImpl(JsonObject response, Context requestContext) {
        this.response = Objects.requireNonNull(response, "response is null");
        this.requestContext = Objects.requireNonNull(requestContext, "request context is null");
    }

    @Override
    public JsonObject asJsonObject() {
        return response;
    }

    @Override
    public Context requestContext() {
        return requestContext;
    }
}
