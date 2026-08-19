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

import io.helidon.common.media.type.MediaTypes;
import io.helidon.json.JsonObject;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;

class McpSamplingToolsTest {

    @Test
    void constructsToolContent() {
        McpParameters input = new McpParameters(JsonObject.builder()
                                                        .set("city", "Prague")
                                                        .build());
        McpSamplingToolUseContent toolUse = McpSamplingToolUseContent.builder()
                .id("call-1")
                .name("weather")
                .input(input)
                .build();
        McpSamplingToolResultContent toolResult = McpSamplingToolResultContent.builder()
                .toolUseId(toolUse.id())
                .result(McpToolResult.create("18 C"))
                .metadata(new McpParameters(JsonObject.builder()
                                                .set("cached", true)
                                                .build()))
                .build();
        McpSamplingMessage message = McpSamplingMessage.builder()
                .role(McpRole.ASSISTANT)
                .addContent(McpSamplingTextContent.builder()
                                    .text("Checking the weather")
                                    .annotations(McpAnnotations.builder()
                                                         .addAudience(McpRole.USER)
                                                         .priority(0.8)
                                                         .build())
                                    .build())
                .addContent(toolUse)
                .metadata(new McpParameters(JsonObject.builder()
                                                .set("traceId", "trace-1")
                                                .build()))
                .build();

        assertThat(toolUse.input().get("city").asString().orElseThrow(), is("Prague"));
        assertThat(message.contents().size(), is(2));
        assertThat(message.contents().get(1), sameInstance(toolUse));
        assertThat(((McpSamplingTextContent) message.contents().getFirst()).annotations().orElseThrow()
                           .audience().getFirst(),
                   is(McpRole.USER));
        assertThat(message.metadata().orElseThrow().get("traceId").asString().orElseThrow(), is("trace-1"));
        assertThat(toolResult.metadata().orElseThrow().get("cached").asBoolean().orElseThrow(), is(true));
        assertThat(McpSamplingRequest.builder().addMessage(message).build().usesTool(), is(true));
        assertThrows(UnsupportedOperationException.class, () -> message.contents().clear());

        McpToolTextContent textContent = McpToolTextContent.builder()
                .text("text")
                .metadata(new McpParameters(JsonObject.builder().set("source", "tool").build()))
                .build();
        McpToolResult result = McpToolResult.builder()
                .addTextContent(textContent)
                .build();
        assertThat(result.textContents().getFirst().type(), is(McpContentType.TEXT));
        assertThat(textContent.metadata().orElseThrow().get("source").asString().orElseThrow(), is("tool"));
        assertThat(McpToolTextContent.builder(textContent).clearMetadata().build().metadata().isEmpty(), is(true));

        McpToolTextResourceContent resourceContent = McpToolTextResourceContent.builder()
                .uri(URI.create("memory://forecast"))
                .mediaType(MediaTypes.TEXT_PLAIN)
                .text("sunny")
                .metadata(new McpParameters(JsonObject.builder().set("source", "resource").build()))
                .build();
        assertThat(resourceContent.metadata().orElseThrow().get("source").asString().orElseThrow(), is("resource"));
    }

    @Test
    void addsToolNameToSamplingRequest() {
        McpSamplingRequest request = McpSamplingRequest.builder()
                .addTool("weather")
                .build();

        assertThat(request.tools().getFirst(), is("weather"));
        assertThat(request.usesTool(), is(true));
    }

    @Test
    void reportsSamplingContextUse() {
        McpSamplingRequest noContext = McpSamplingRequest.builder()
                .includeContext(McpIncludeContext.NONE)
                .build();
        McpSamplingRequest withContext = McpSamplingRequest.builder()
                .includeContext(McpIncludeContext.THIS_SERVER)
                .build();

        assertThat(noContext.usesContext(), is(false));
        assertThat(withContext.usesContext(), is(true));
    }

    @Test
    void rejectsNullParameterObject() {
        NullPointerException exception = assertThrows(NullPointerException.class,
                                                      () -> new McpParameters((JsonObject) null));

        assertThat(exception.getMessage(), is("value is null"));
    }
}
