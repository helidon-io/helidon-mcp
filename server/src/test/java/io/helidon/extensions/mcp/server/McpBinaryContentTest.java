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
import java.util.function.Supplier;
import java.util.stream.Stream;

import io.helidon.common.media.type.MediaTypes;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

class McpBinaryContentTest {

    static Stream<Supplier<?>> binaryContentBuilders() {
        byte[] data = {1, 2, 3};
        URI uri = URI.create("file:///resource");
        return Stream.of(
                McpPromptAudioContent.builder()
                        .role(McpRole.ASSISTANT)
                        .data(data)
                        .mediaType(MediaTypes.APPLICATION_OCTET_STREAM),
                McpPromptImageContent.builder()
                        .role(McpRole.ASSISTANT)
                        .data(data)
                        .mediaType(MediaTypes.APPLICATION_OCTET_STREAM),
                McpPromptBinaryResourceContent.builder()
                        .role(McpRole.ASSISTANT)
                        .data(data)
                        .mediaType(MediaTypes.APPLICATION_OCTET_STREAM)
                        .uri(uri),
                McpResourceBinaryContent.builder().data(data).mediaType(MediaTypes.APPLICATION_OCTET_STREAM),
                McpSamplingAudioContent.builder().data(data).mediaType(MediaTypes.APPLICATION_OCTET_STREAM),
                McpSamplingImageContent.builder().data(data).mediaType(MediaTypes.APPLICATION_OCTET_STREAM),
                McpToolAudioContent.builder().data(data).mediaType(MediaTypes.APPLICATION_OCTET_STREAM),
                McpToolImageContent.builder().data(data).mediaType(MediaTypes.APPLICATION_OCTET_STREAM),
                McpToolBinaryResourceContent.builder().data(data).mediaType(MediaTypes.APPLICATION_OCTET_STREAM).uri(uri));
    }

    @ParameterizedTest
    @MethodSource("binaryContentBuilders")
    void formatsBinaryPayloadInDiagnosticStrings(Supplier<?> builder) {
        assertThat(builder.toString(), containsString("data=[1, 2, 3]"));
        assertThat(builder.get().toString(), containsString("data=[1, 2, 3]"));
    }
}
