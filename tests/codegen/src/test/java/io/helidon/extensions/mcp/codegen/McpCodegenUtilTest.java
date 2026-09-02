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
package io.helidon.extensions.mcp.codegen;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import io.helidon.codegen.classmodel.ClassModel;
import io.helidon.common.types.Annotated;
import io.helidon.common.types.Annotation;
import io.helidon.extensions.mcp.server.McpIcons;

import org.junit.jupiter.api.Test;

import static io.helidon.extensions.mcp.codegen.McpCodegenUtil.addIcons;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_ICON_ANNOTATION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;

class McpCodegenUtilTest {
    @Test
    void cachesGeneratedIcons() throws IOException {
        String source = render(Annotation.create(MCP_ICON_ANNOTATION, "https://example.com/icon.svg"));

        assertThat(source, containsString("private static final List<McpIcon> ICONS = List.of("));
        assertThat(source, containsString("return ICONS;"));
        assertThat(source.lastIndexOf("McpIcon.builder()"), lessThan(source.indexOf("public List<McpIcon> icons()")));
    }

    @Test
    void generatesSharedEmptyListWithoutCacheField() throws IOException {
        String source = render();

        assertThat(source, not(containsString("static final List<McpIcon> ICONS")));
        assertThat(source, containsString("return List.of();"));
    }

    private static String render(Annotation... annotations) throws IOException {
        Annotated component = new Annotated() {
            @Override
            public List<Annotation> annotations() {
                return List.of(annotations);
            }

            @Override
            public List<Annotation> inheritedAnnotations() {
                return List.of();
            }
        };
        StringWriter writer = new StringWriter();
        ClassModel.builder()
                .packageName("example")
                .name("Generated")
                .addInnerClass(innerClass -> {
                    innerClass.name("Component")
                            .addInterface(McpIcons.class);
                    addIcons(innerClass, component);
                })
                .build()
                .write(writer);
        return writer.toString();
    }
}
