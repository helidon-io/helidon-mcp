/*
 * Copyright (c) 2025, 2026 Oracle and/or its affiliates.
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

import io.helidon.common.types.Annotation;
import io.helidon.common.types.ElementKind;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypedElementInfo;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class McpCodegenUtilTest {

    private static final TypeName NULLABLE = TypeName.create("javax.annotation.Nullable");

    @Test
    void whenNoAnnotationsThenNotNullable() {
        TypedElementInfo myParam = TypedElementInfo.builder()
                .elementName("param")
                .kind(ElementKind.PARAMETER)
                .typeName(TypeName.create(String.class))
                .build();

        assertThat(McpCodegenUtil.isNullable(myParam), is(false));
    }

    @Test
    void whenNullableInParameterAnnotationsThenNullable() {
        TypedElementInfo myParam = TypedElementInfo.builder()
                .elementName("param")
                .kind(ElementKind.PARAMETER)
                .typeName(TypeName.create(String.class))
                .addAnnotation(Annotation.create(NULLABLE))
                .build();

        assertThat(McpCodegenUtil.isNullable(myParam), is(true));
    }
}
