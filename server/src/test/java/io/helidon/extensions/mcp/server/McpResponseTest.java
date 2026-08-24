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

import java.math.BigDecimal;

import io.helidon.common.context.Context;
import io.helidon.json.JsonException;
import io.helidon.json.JsonObject;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;

class McpResponseTest {

    @Test
    void containsResponseAndRequestContext() {
        JsonObject json = JsonObject.builder().set("id", 1).build();
        Context requestContext = Context.create();

        McpResponse response = new McpResponseImpl(json, requestContext);

        assertThat(response.id(), is(1L));
        assertThat(response.asJsonObject(), sameInstance(json));
        assertThat(response.requestContext(), sameInstance(requestContext));
    }

    @Test
    void rejectsMissingValues() {
        JsonObject json = JsonObject.builder().set("id", 1).build();
        Context requestContext = Context.create();

        assertThrows(NullPointerException.class, () -> new McpResponseImpl(null, requestContext));
        assertThrows(NullPointerException.class, () -> new McpResponseImpl(json, null));
    }

    @Test
    void rejectsInvalidIdentifiers() {
        Context requestContext = Context.create();
        JsonObject missing = JsonObject.empty();
        JsonObject string = JsonObject.builder().set("id", "1").build();
        JsonObject fractional = JsonObject.builder().set("id", 1.5).build();
        JsonObject outOfRange = JsonObject.builder().set("id", new BigDecimal("9223372036854775808")).build();

        assertThrows(JsonException.class, () -> new McpResponseImpl(missing, requestContext));
        assertThrows(JsonException.class, () -> new McpResponseImpl(string, requestContext));
        assertThrows(JsonException.class, () -> new McpResponseImpl(fractional, requestContext));
        assertThrows(JsonException.class, () -> new McpResponseImpl(outOfRange, requestContext));
    }
}
