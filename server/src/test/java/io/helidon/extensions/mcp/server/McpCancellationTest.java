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

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.json.JsonValue;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class McpCancellationTest {

    @Test
    void testCancellationDefault() {
        McpCancellation cancellation = new McpCancellation();

        assertThat(cancellation.result().isRequested(), is(false));
        assertThat(cancellation.result().reason(), is("No cancellation requested"));
    }

    @Test
    void testCancellationRequested() {
        String reason = "Process is taking too long";
        McpCancellation cancellation = new McpCancellation();
        cancellation.cancel(reason, JsonValue.NULL);

        assertThat(cancellation.result().isRequested(), is(true));
        assertThat(cancellation.result().reason(), is(reason));
    }

    @Test
    void testCancellationHook() {
        AtomicInteger counter = new AtomicInteger();
        String reason = "Process is taking too long";
        McpCancellation cancellation = new McpCancellation();

        cancellation.registerCancellationHook(counter::getAndIncrement);
        cancellation.cancel(reason, JsonValue.NULL);
        cancellation.cancel(reason, JsonValue.NULL);

        McpCancellationResult result = cancellation.result();
        assertThat(result.isRequested(), is(true));
        assertThat(result.reason(), is(reason));
        assertThat(counter.get(), is(1));
    }
}
