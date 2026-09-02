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

import java.util.List;

import io.helidon.common.media.type.MediaTypes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

class McpIconTest {
    @Test
    void defaultsComponentIconsToEmpty() {
        McpIcons component = new McpIcons() {
        };

        assertThat(component.icons(), is(List.of()));
    }

    @Test
    void buildsFullIconMetadata() {
        McpIcon icon = McpIcon.builder()
                .source("https://example.com/icon.svg")
                .mediaType(MediaTypes.create("image/svg+xml"))
                .addSize("48x48")
                .addSize("96x96")
                .theme(McpIconTheme.DARK)
                .build();

        String metadata = String.join("|",
                                      icon.source(),
                                      icon.mediaType().orElseThrow().text(),
                                      String.join(",", icon.sizes()),
                                      icon.theme().orElseThrow().text());
        assertThat(metadata, is("https://example.com/icon.svg|image/svg+xml|48x48,96x96|dark"));
    }

    @Test
    void buildsMinimalIconMetadata() {
        McpIcon icon = McpIcon.builder()
                .source("data:image/png;base64,iVBORw0KGgo=")
                .build();

        String metadata = String.join("|",
                                      icon.source(),
                                      Boolean.toString(icon.mediaType().isEmpty()),
                                      Boolean.toString(icon.sizes().isEmpty()),
                                      Boolean.toString(icon.theme().isEmpty()));
        assertThat(metadata, is("data:image/png;base64,iVBORw0KGgo=|true|true|true"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "data:;base64,AA==",
            "data:;base64,+w==",
            "data:;charset=utf-8;base64,AA==",
            "data:image/png;base64,AQ%3D%3D",
            "data:image/png;charset=utf-8;BASE64,iVBORw0KGgo=",
            "data:image/svg+xml;profile=a%2Fb;base64,PHN2Zy8+",
            "data:application/vnd.example.icon+png;name=small%20icon;base64,AAAA",
            "data:image/png;base64=1;base64,AAAA"
    })
    void acceptsValidDataSources(String source) {
        assertThat(McpIcon.builder().source(source).build().source(), is(source));
    }

    @Test
    void keepsSizesImmutable() {
        McpIcon icon = McpIcon.builder()
                .source("https://example.com/icon.svg")
                .addSize("any")
                .build();

        assertThrows(UnsupportedOperationException.class, () -> icon.sizes().add("48x48"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            " ",
            "icon.svg",
            "not a uri",
            "https://",
            "https:/icon.svg",
            "data:image/png;base64,",
            "data:image/png,not-base64",
            "data:image/png;base64,@@@@",
            "data:image/png;base64,A",
            "data:image/png;base64,AA=A",
            "data:image/png;base64=1,AAAA",
            "data:image/png;base64;charset=utf-8,AAAA",
            "data:image;base64,AAAA",
            "data:/png;base64,AAAA",
            "data:image/;base64,AAAA",
            "data:image/png/extra;base64,AAAA",
            "data:image/png;;base64,AAAA",
            "data:image/png;broken;base64,AAAA",
            "data:image/png;=value;base64,AAAA",
            "data:image/png;name=;base64,AAAA",
            "data:image/png;name=a=b;base64,AAAA",
            "data:image/png;name=a/b;base64,AAAA",
            "data:image/png;name=%0A;base64,AAAA",
            "data:image/png;name=%80;base64,AAAA",
            "data:image/png;base64;base64,AAAA",
            "data:image/png;base64,AQ%3",
            "data://example.com/icon,abc",
            "data:/image/png,abc",
            "file:///tmp/icon.svg",
            "javascript:alert(1)"
    })
    void rejectsInvalidSources(String source) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                                                           () -> McpIcon.builder().source(source));

        assertThat(exception.getMessage(), is("Icon source must be a valid HTTP(S) URL or data URI"));
    }
}
