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

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import io.helidon.json.JsonObject;
import io.helidon.json.JsonValue;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class McpUrlElicitationRequiredExceptionTest {

    @Test
    void createsStructuredErrorData() {
        McpElicitationUrlRequest first = McpElicitationUrlRequest.builder()
                .message("Authorize storage access")
                .elicitationId("storage-authorization")
                .url(URI.create("https://example.com/storage/authorize"))
                .timeout(Duration.ofSeconds(10))
                .build();
        McpElicitationUrlRequest second = McpElicitationUrlRequest.builder()
                .message("Authorize payment access")
                .elicitationId("payment-authorization")
                .url(URI.create("https://example.com/payment/authorize"))
                .build();
        List<McpElicitationUrlRequest> source = new ArrayList<>(List.of(first, second));

        McpUrlElicitationRequiredException exception = new McpUrlElicitationRequiredException(source);
        source.clear();

        assertThat(McpUrlElicitationRequiredException.ERROR_CODE, is(-32042));
        assertThat(exception.getMessage(), is("URL elicitation required"));
        assertThat(exception.elicitations(), contains(first, second));

        JsonObject data = exception.errorData();
        assertThat(data.keysAsStrings(), contains("elicitations"));
        List<JsonValue> values = data.arrayValue("elicitations").orElseThrow().values();
        assertThat(values.size(), is(2));
        assertElicitation(values.get(0).asObject(),
                          "Authorize storage access",
                          "storage-authorization",
                          "https://example.com/storage/authorize");
        assertElicitation(values.get(1).asObject(),
                          "Authorize payment access",
                          "payment-authorization",
                          "https://example.com/payment/authorize");
    }

    @Test
    void createsErrorForOneElicitation() {
        McpElicitationUrlRequest elicitation = McpElicitationUrlRequest.builder()
                .message("Authorize access")
                .elicitationId("authorization")
                .url(URI.create("https://example.com/authorize"))
                .build();

        McpUrlElicitationRequiredException exception = new McpUrlElicitationRequiredException(elicitation);

        assertThat(exception.elicitations(), contains(elicitation));
    }

    @Test
    void rejectsEmptyElicitationCollection() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                                                           () -> new McpUrlElicitationRequiredException(List.of()));

        assertThat(exception.getMessage(), is("At least one URL elicitation is required"));
    }

    private static void assertElicitation(JsonObject elicitation,
                                          String message,
                                          String elicitationId,
                                          String url) {
        assertThat(elicitation.keysAsStrings(), containsInAnyOrder("mode", "message", "elicitationId", "url"));
        assertThat(elicitation.stringValue("mode").orElseThrow(), is("url"));
        assertThat(elicitation.stringValue("message").orElseThrow(), is(message));
        assertThat(elicitation.stringValue("elicitationId").orElseThrow(), is(elicitationId));
        assertThat(elicitation.stringValue("url").orElseThrow(), is(url));
        assertThat(elicitation.containsKey("timeout"), is(false));
    }
}
