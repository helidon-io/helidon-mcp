/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
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

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.helidon.codegen.classmodel.ClassModel;
import io.helidon.codegen.classmodel.Method;
import io.helidon.codegen.classmodel.Parameter;
import io.helidon.common.types.AccessModifier;
import io.helidon.common.types.Annotated;
import io.helidon.common.types.Annotation;
import io.helidon.common.types.ResolvedType;
import io.helidon.common.types.TypeInfo;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypeNames;
import io.helidon.common.types.TypedElementInfo;

import static io.helidon.common.types.TypeNames.LIST;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_CANCELLATION;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_DESCRIPTION;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_FEATURES;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_LOGGER;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_PARAMETERS;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_PROGRESS;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_REQUEST;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_ROOTS;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_SAMPLING;

/**
 * Utility class for methods used by several MCP code generator.
 */
class McpCodegenUtil {
    private static final Pattern PATTERN = Pattern.compile("^.");

    static final List<String> MCP_TYPES = List.of(MCP_REQUEST.classNameWithEnclosingNames(),
                                                  MCP_FEATURES.classNameWithEnclosingNames(),
                                                  MCP_LOGGER.classNameWithEnclosingNames(),
                                                  MCP_PROGRESS.classNameWithEnclosingNames(),
                                                  MCP_CANCELLATION.classNameWithEnclosingNames(),
                                                  MCP_SAMPLING.classNameWithEnclosingNames(),
                                                  MCP_PARAMETERS.classNameWithEnclosingNames());

    private McpCodegenUtil() {
    }

    static boolean isBoolean(TypeName type) {
        return TypeNames.PRIMITIVE_BOOLEAN.equals(type)
                || TypeNames.BOXED_BOOLEAN.equals(type);
    }

    static boolean isNumber(TypeName type) {
        return TypeNames.BOXED_INT.equals(type)
                || TypeNames.BOXED_BYTE.equals(type)
                || TypeNames.BOXED_LONG.equals(type)
                || TypeNames.BOXED_FLOAT.equals(type)
                || TypeNames.BOXED_SHORT.equals(type)
                || TypeNames.BOXED_DOUBLE.equals(type)
                || TypeNames.PRIMITIVE_INT.equals(type)
                || TypeNames.PRIMITIVE_BYTE.equals(type)
                || TypeNames.PRIMITIVE_LONG.equals(type)
                || TypeNames.PRIMITIVE_FLOAT.equals(type)
                || TypeNames.PRIMITIVE_SHORT.equals(type)
                || TypeNames.PRIMITIVE_DOUBLE.equals(type);
    }

    static boolean isList(TypeName type) {
        return type.equals(TypeNames.LIST) && type.typeArguments().size() == 1;
    }

    /**
     * Create a class name from the provided element and suffix.
     * The first character is change to upper case.
     *
     * @param element name as prefix
     * @param suffix the suffix
     * @return class name as TypeName
     */
    static TypeName createClassName(TypedElementInfo element, String suffix) {
        String uppercaseElement = PATTERN.matcher(element.elementName()).replaceFirst(m -> m.group().toUpperCase());
        return TypeName.builder()
                .className(uppercaseElement + suffix)
                .build();
    }

    static List<TypedElementInfo> getElementsWithAnnotation(TypeInfo type, TypeName target) {
        return type.elementInfo().stream()
                .filter(element -> element.hasAnnotation(target))
                .collect(Collectors.toList());
    }

    static TypeName generatedTypeName(TypeName factoryTypeName, String suffix) {
        return TypeName.builder()
                .packageName(factoryTypeName.packageName())
                .className(factoryTypeName.classNameWithEnclosingNames().replace('.', '_') + "__" + suffix)
                .build();
    }

    static boolean isIgnoredSchemaElement(TypeName typeName) {
        return MCP_REQUEST.equals(typeName)
                || MCP_ROOTS.equals(typeName)
                || MCP_LOGGER.equals(typeName)
                || MCP_FEATURES.equals(typeName)
                || MCP_PROGRESS.equals(typeName)
                || MCP_SAMPLING.equals(typeName)
                || MCP_CANCELLATION.equals(typeName);
    }

    static boolean isResourceTemplate(String uri) {
        return uri.contains("{") || uri.contains("}");
    }

    /**
     * Add a new method to the generated class that convert a {@code List<McpParameters>} to
     * a list of the provided type.
     *
     * @param classModel generated class
     * @param type generic type
     */
    static void addToListMethod(ClassModel.Builder classModel, TypeName type) {
        TypeName typeList = ResolvedType.create(TypeName.builder(LIST)
                                                        .addTypeArgument(type)
                                                        .build()).type();
        TypeName parameterList = ResolvedType.create(TypeName.builder(LIST)
                                                             .addTypeArgument(MCP_PARAMETERS)
                                                             .build()).type();
        Method.Builder method = Method.builder()
                .name("toList")
                .isStatic(true)
                .accessModifier(AccessModifier.PRIVATE)
                .returnType(typeList)
                .addParameter(Parameter.builder().name("list").type(parameterList).build())
                .addContentLine("return list == null ? List.of()")
                .increaseContentPadding()
                .addContentLine(": list.stream().map(p -> p.as(" + type + ".class))")
                .increaseContentPadding()
                .addContentLine(".map(p -> p.get()).toList();");
        classModel.addMethod(method.build());
    }

    /**
     * Returns {@code true} if the provided type is an MCP type and create request getter for that type,
     * otherwise nothing is created and return {@code false}.
     *
     * @param parameters list of parameters
     * @param type the tested type
     * @return {@code true} if an MCP type, {@code false} otherwise.
     */
    static boolean isMcpType(List<String> parameters, TypedElementInfo type) {
        if (MCP_REQUEST.equals(type.typeName())) {
            parameters.add("request");
            return true;
        }
        if (MCP_FEATURES.equals(type.typeName())) {
            parameters.add("request.features()");
            return true;
        }
        if (MCP_LOGGER.equals(type.typeName())) {
            parameters.add("request.features().logger()");
            return true;
        }
        if (MCP_PROGRESS.equals(type.typeName())) {
            parameters.add("request.features().progress()");
            return true;
        }
        if (MCP_CANCELLATION.equals(type.typeName())) {
            parameters.add("request.features().cancellation()");
            return true;
        }
        if (MCP_SAMPLING.equals(type.typeName())) {
            parameters.add("request.features().sampling()");
            return true;
        }
        if (MCP_ROOTS.equals(type.typeName())) {
            parameters.add("request.features().roots()");
            return true;
        }
        if (MCP_PARAMETERS.equals(type.typeName())) {
            parameters.add("request.parameters()");
            return true;
        }
        return false;
    }

    static Optional<String> getDescription(Annotated element) {
        if (element.hasAnnotation(MCP_DESCRIPTION)) {
            Annotation description = element.annotation(MCP_DESCRIPTION);
            return description.stringValue();
        }
        return Optional.empty();
    }
}
