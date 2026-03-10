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

import java.util.Optional;
import java.util.Set;

import io.helidon.codegen.CodegenContext;
import io.helidon.codegen.CodegenOptions;
import io.helidon.codegen.spi.TypeMapper;
import io.helidon.codegen.spi.TypeMapperProvider;
import io.helidon.common.types.Annotation;
import io.helidon.common.types.ElementKind;
import io.helidon.common.types.TypeInfo;
import io.helidon.common.types.TypeName;

import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_SERVER;
import static io.helidon.extensions.mcp.codegen.McpTypes.SCOPE_ANNOTATION;
import static io.helidon.extensions.mcp.codegen.McpTypes.SERVICE_SINGLETON;

/**
 * Type mapper provider to map class annotated with {@code @Mcp.Server}.
 */
public final class McpServerTypeMapperProvider implements TypeMapperProvider {
    @Override
    public TypeMapper create(CodegenOptions codegenOptions) {
        return new McpServerTypeMapper();
    }

    @Override
    public Set<TypeName> supportedAnnotations() {
        return Set.of(MCP_SERVER);
    }

    /**
     * Type mapper that adds a service scope annotation if not present.
     */
    static class McpServerTypeMapper implements TypeMapper {
        private static final Annotation SERVICE_SINGLETON_ANNOTATION = Annotation.create(SERVICE_SINGLETON);

        @Override
        public boolean supportsType(TypeInfo typeInfo) {
            return typeInfo.kind() == ElementKind.CLASS && typeInfo.hasAnnotation(MCP_SERVER);
        }

        @Override
        public Optional<TypeInfo> map(CodegenContext context, TypeInfo typeInfo) {
            if (hasNoScopeAnnotation(typeInfo)) {
                context.logger().log(System.Logger.Level.DEBUG, "Class " + typeInfo.declaredType().className()
                        + " has no scope annotation. Making this server a singleton.");
                return Optional.of(TypeInfo.builder(typeInfo)
                                           .addAnnotation(SERVICE_SINGLETON_ANNOTATION)
                                           .build());
            }
            return Optional.of(typeInfo);
        }

        private boolean hasNoScopeAnnotation(TypeInfo typeInfo) {
            return typeInfo.annotations()
                    .stream()
                    .noneMatch(it -> it.hasMetaAnnotation(SCOPE_ANNOTATION));
        }
    }
}
