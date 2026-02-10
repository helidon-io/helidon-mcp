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

import org.junit.jupiter.api.Test;

import static io.helidon.extensions.mcp.server.McpJsonSerializerV1.EMPTY_OBJECT_SCHEMA;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class McpElicitationRequestTest {
    private static final String EMPTY_SCHEMA = EMPTY_OBJECT_SCHEMA.toString();

    @Test
    void testDefaultElicitationRequest() {
        McpElicitationRequest request = McpElicitationRequest.builder()
                .message("message")
                .schema(EMPTY_SCHEMA)
                .build();

        assertThat(request.message(), is("message"));
        assertThat(request.schema(), is(EMPTY_SCHEMA));
        assertThat(request.timeout(), is(Duration.ofSeconds(5)));
    }

    @Test
    void testCustomElicitationRequest() {
        McpElicitationRequest request = McpElicitationRequest.builder()
                .message("message")
                .schema(EMPTY_SCHEMA)
                .timeout(Duration.ofSeconds(10))
                .build();

        assertThat(request.message(), is("message"));
        assertThat(request.schema(), is(EMPTY_SCHEMA));
        assertThat(request.timeout(), is(Duration.ofSeconds(10)));
    }
}
