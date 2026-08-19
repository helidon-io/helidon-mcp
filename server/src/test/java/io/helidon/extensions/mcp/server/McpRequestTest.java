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

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;

class McpRequestTest {

    @Test
    @SuppressWarnings("removal")
    void exposesProtocolMetadata() {
        McpParameters parameters = new McpParameters(JsonObject.builder()
                                                            .set(McpMetadata.META, JsonObject.builder()
                                                                    .set("trace", "legacy")
                                                                    .build())
                                                            .build());
        McpParameters metadata = new McpParameters(JsonObject.builder()
                                                            .set("trace", "test")
                                                            .build());
        McpRequest request = McpRequest.builder()
                .parameters(parameters)
                .metadata(metadata)
                .features(mock(McpFeatures.class))
                .protocolVersion(McpProtocolVersion.VERSION_2025_11_25.text())
                .sessionContext(Context.create())
                .requestContext(Context.create())
                .build();

        McpMetadata requestMetadata = request;
        assertThat(requestMetadata.metadata().orElseThrow(), sameInstance(metadata));
        assertThat(request.meta(), sameInstance(metadata));

        McpRequest requestWithoutMetadata = McpRequest.builder(request)
                .clearMetadata()
                .build();
        assertThat(requestWithoutMetadata.metadata().isEmpty(), is(true));
        assertThat(requestWithoutMetadata.meta().get("trace").asString().orElseThrow(), is("legacy"));
    }
}
