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

import io.helidon.json.JsonObject;
import io.helidon.json.JsonParser;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;

class McpJsonSerializerV4Test {
    private static final McpJsonSerializer SERIALIZER =
            McpJsonSerializer.create(McpProtocolVersion.VERSION_2025_11_25);
    private static final McpJsonSerializer LEGACY_SERIALIZER =
            McpJsonSerializer.create(McpProtocolVersion.VERSION_2025_06_18);

    @Test
    void parsesSingleSamplingContentBlock() {
        McpSamplingResponse response = SERIALIZER.createSamplingResponse(response("""
                {
                  "type": "text",
                  "text": "first"
                }
                """));

        assertThat(response.messages().size(), is(1));
        assertThat(response.asTextMessage().text(), is("first"));
    }

    @Test
    void parsesSamplingContentBlockArray() {
        McpSamplingResponse response = SERIALIZER.createSamplingResponse(response("""
                [
                  {
                    "type": "text",
                    "text": "first"
                  },
                  {
                    "type": "text",
                    "text": "second"
                  }
                ]
                """));

        assertThat(response.messages().size(), is(2));
        assertThat(((McpSamplingTextMessage) response.messages().get(0)).text(), is("first"));
        assertThat(((McpSamplingTextMessage) response.messages().get(1)).text(), is("second"));
        assertThat(response.messages().getFirst().role(), is(McpRole.ASSISTANT));
        assertThat(response.messages().get(1).role(), is(McpRole.ASSISTANT));
        assertThat(response.model(), is("test-model"));
        assertThat(response.stopReason().orElseThrow(), is(McpStopReason.END_TURN));
        assertThrows(UnsupportedOperationException.class, () -> response.messages().clear());
    }

    @Test
    void preservesSingularAccessForSamplingContentBlockArray() {
        McpSamplingResponse response = SERIALIZER.createSamplingResponse(response("""
                [
                  {
                    "type": "text",
                    "text": "first"
                  },
                  {
                    "type": "text",
                    "text": "second"
                  }
                ]
                """));

        assertThat(response.message(), sameInstance(response.messages().getFirst()));
        assertThat(response.asTextMessage().text(), is("first"));
    }

    @Test
    void supportsEmptySamplingContentBlockArray() {
        McpSamplingResponse response = SERIALIZER.createSamplingResponse(response("[]"));

        assertThat(response.messages().isEmpty(), is(true));
        assertThrows(McpSamplingException.class, response::message);
    }

    @Test
    void rejectsNonObjectSamplingContentBlock() {
        McpSamplingException exception = assertThrows(McpSamplingException.class,
                                                      () -> SERIALIZER.createSamplingResponse(response("[42]")));

        assertThat(exception.getMessage(), is("Wrong sampling response format"));
    }

    @Test
    void keepsLegacySamplingContentObjectOnly() {
        McpSamplingException exception = assertThrows(McpSamplingException.class,
                                                      () -> LEGACY_SERIALIZER.createSamplingResponse(response("""
                                                              [
                                                                {
                                                                  "type": "text",
                                                                  "text": "first"
                                                                }
                                                              ]
                                                              """)));

        assertThat(exception.getMessage(), is("Wrong sampling response format"));
    }

    private static JsonObject response(String content) {
        return JsonParser.create("""
                {
                  "jsonrpc": "2.0",
                  "id": 1,
                  "result": {
                    "model": "test-model",
                    "role": "assistant",
                    "content": %s,
                    "stopReason": "endTurn"
                  }
                }
                """.formatted(content)).readJsonObject();
    }
}
