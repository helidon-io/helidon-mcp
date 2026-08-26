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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class McpElicitationUrlRequestTest {
    private static final URI URL = URI.create("https://example.com/authorize");

    @Test
    void buildsDefaultUrlElicitationRequest() {
        McpElicitationUrlRequest request = McpElicitationUrlRequest.builder()
                .message("Authorize access")
                .elicitationId("elicitation-id")
                .url(URL)
                .build();

        assertThat(request.message(), is("Authorize access"));
        assertThat(request.elicitationId(), is("elicitation-id"));
        assertThat(request.url(), is(URL));
        assertThat(request.timeout(), is(Duration.ofMinutes(5)));
    }

    @Test
    void buildsUrlElicitationRequestWithCustomTimeout() {
        McpElicitationUrlRequest request = McpElicitationUrlRequest.builder()
                .message("Authorize access")
                .elicitationId("elicitation-id")
                .url(URL)
                .timeout(Duration.ofSeconds(10))
                .build();

        assertThat(request.timeout(), is(Duration.ofSeconds(10)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"relative/path", "//example.com/authorize"})
    void rejectsRelativeUrl(String url) {
        McpElicitationException exception = assertThrows(McpElicitationException.class,
                                                         () -> McpElicitationUrlRequest.builder()
                                                                 .message("Authorize access")
                                                                 .elicitationId("elicitation-id")
                                                                 .url(URI.create(url))
                                                                 .build());

        assertThat(exception.getMessage(), is("Elicitation URL must be an absolute URI"));
    }

    @Test
    void rejectsBlankElicitationId() {
        McpElicitationException exception = assertThrows(McpElicitationException.class,
                                                         () -> McpElicitationUrlRequest.builder()
                                                                 .message("Authorize access")
                                                                 .elicitationId(" ")
                                                                 .url(URL)
                                                                 .build());

        assertThat(exception.getMessage(), is("Elicitation ID must not be blank"));
    }
}
