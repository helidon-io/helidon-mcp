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

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import io.helidon.codegen.CodegenContext;
import io.helidon.codegen.classmodel.Method;
import io.helidon.common.types.ElementKind;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypeNames;
import io.helidon.common.types.TypedElementInfo;
import io.helidon.json.JsonObject;
import io.helidon.json.JsonParser;
import io.helidon.json.JsonString;

import static io.helidon.extensions.mcp.codegen.McpCodegenUtil.getDescription;
import static io.helidon.extensions.mcp.codegen.McpTypes.JSON_SCHEMA;
import static io.helidon.extensions.mcp.codegen.McpTypes.JSON_SCHEMA_PROVIDER;
import static io.helidon.extensions.mcp.codegen.McpTypes.QUALIFIER;
import static io.helidon.extensions.mcp.codegen.McpTypes.SERVICES;

/**
 * For each class annotated with {@code io.helidon.extensions.mcp.server.Mcp.JsonSchema},
 * generate a class with single public static method that returns a JSON
 * schema representation of that class as a string.
 */
class McpJsonSchemaCodegen {
    private static final TypeName GENERATOR = TypeName.create(McpCodegen.class);
    private static final TypeName COLLECTION = TypeName.create(Collection.class);

    private McpJsonSchemaCodegen() {
    }

    static void addSchemaMethodBody(Method.Builder method, List<TypedElementInfo> fields, CodegenContext context) {
        addSchemaMethodBody(method, fields, List.of(), context);
    }

    static void addSchemaMethodBody(Method.Builder method,
                                    List<TypedElementInfo> fields,
                                    List<String> required,
                                    CodegenContext context) {
        method.addContentLine("var builder = new StringBuilder();");
        method.addContentLine("builder.append(\"{\");");
        method.addContentLine("builder.append(\"\\\"type\\\": \\\"object\\\", \\\"properties\\\": {\");");

        int n = fields.size();
        for (int i = 0; i < n; i++) {
            TypedElementInfo field = fields.get(i);
            addPropertySchema(method, field, context);
            if (i < n - 1) {
                method.addContentLine("builder.append(\", \");");
            }
        }
        if (required.isEmpty()) {
            method.addContentLine("builder.append(\"}}\");");
        } else {
            method.addContentLine("builder.append(\"}, \\\"required\\\": [\");");
            int m = required.size();
            for (int i = 0; i < m; i++) {
                method.addContent("builder.append(\"\\\"")
                        .addContent(required.get(i))
                        .addContentLine("\\\"\");");
                if (i < m - 1) {
                    method.addContentLine("builder.append(\", \");");
                }
            }
            method.addContentLine("builder.append(\"]}\");");
        }
        method.addContentLine("return builder.toString();");
    }

    private static void addPropertySchema(Method.Builder method, TypedElementInfo element, CodegenContext context) {
        TypeName typeName = element.typeName();
        String description = getDescription(element)
                .map(text -> "\"description\": " + JsonString.create(text) + ", ")
                .orElse("");
        method.addContent("builder.append(")
                .addContentLiteral("\"" + element.elementName() + "\": ")
                .addContentLine(");");
        if (typeName.array() || isCollection(typeName)) {
            TypeName itemType = typeName.array()
                    ? typeName.componentType().orElseThrow()
                    : typeName.typeArguments().getFirst();
            method.addContent("builder.append(")
                    .addContentLiteral("{" + description + "\"type\": \"array\", \"items\": ")
                    .addContentLine(");");
            addValueSchema(method, itemType, context, "");
            method.addContentLine("builder.append(\"}\");");
        } else {
            addValueSchema(method, typeName, context, description);
        }
    }

    private static void addValueSchema(Method.Builder method, TypeName typeName, CodegenContext context, String description) {
        if (isPrimitiveJsonSchemaType(typeName)) {
            method.addContent("builder.append(")
                    .addContentLiteral("{" + description + "\"type\": \"" + mapTypeName(typeName) + "\"}")
                    .addContentLine(");");
            return;
        }
        var enumType = context.typeInfo(typeName).filter(type -> type.kind() == ElementKind.ENUM);
        method.addContent("builder.append(");
        if (enumType.isEmpty()) {
            addDeclaredSchema(method, typeName);
        } else {
            String constants = enumType.get().elementInfo().stream()
                    .filter(item -> item.kind() == ElementKind.ENUM_CONSTANT)
                    .map(item -> "\"" + item.elementName() + "\"")
                    .collect(Collectors.joining(", "));
            if (enumType.get().hasAnnotation(JSON_SCHEMA)) {
                // Helidon generates an object schema for an annotated enum. Keep its metadata on the string input.
                method.addContent(JsonObject.class)
                        .addContent(".builder().setStrings(\"enum\", ")
                        .addContent(List.class)
                        .addContent(".of(").addContent(constants).addContent(")).from(")
                        .addContent(JsonParser.class).addContent(".create(");
                addDeclaredSchema(method, typeName);
                method.addContent(").readJsonObject()).set(\"type\", \"string\").build()");
            } else {
                method.addContentLiteral("{" + description + "\"type\": \"string\", \"enum\": [" + constants + "]}");
            }
        }
        method.addContentLine(");");
    }

    private static void addDeclaredSchema(Method.Builder method, TypeName typeName) {
        method.addContent(SERVICES)
                .addContent(".get(")
                .addContent(JSON_SCHEMA_PROVIDER)
                .addContent(".class, ")
                .addContent(QUALIFIER)
                .addContent(".createNamed(")
                .addContent(typeName.genericTypeName())
                .addContent(".class.getCanonicalName())).jsonSchema()");
    }

    private static boolean isCollection(TypeName typeName) {
        return TypeNames.LIST.equals(typeName)
                || COLLECTION.equals(typeName);
    }

    private static boolean isPrimitiveJsonSchemaType(TypeName typeName) {
        return TypeNames.STRING.equals(typeName)
                || isBoolean(typeName)
                || isIntegerNumber(typeName)
                || isRealNumber(typeName);
    }

    static String mapTypeName(TypeName typeName) {
        if (TypeNames.STRING.equals(typeName)) {
            return "string";
        }
        if (isBoolean(typeName)) {
            return "boolean";
        }
        if (isIntegerNumber(typeName)) {
            return "integer";
        }
        if (isRealNumber(typeName)) {
            return "number";
        }
        throw new IllegalArgumentException("Unsupported type: " + typeName);
    }

    static boolean isBoolean(TypeName type) {
        return TypeNames.PRIMITIVE_BOOLEAN.equals(type) || TypeNames.BOXED_BOOLEAN.equals(type);
    }

    static boolean isRealNumber(TypeName type) {
        return TypeNames.BOXED_FLOAT.equals(type)
                || TypeNames.BOXED_DOUBLE.equals(type)
                || TypeNames.PRIMITIVE_FLOAT.equals(type)
                || TypeNames.PRIMITIVE_DOUBLE.equals(type);
    }

    static boolean isIntegerNumber(TypeName type) {
        return TypeNames.BOXED_INT.equals(type)
                || TypeNames.BOXED_BYTE.equals(type)
                || TypeNames.BOXED_LONG.equals(type)
                || TypeNames.BOXED_SHORT.equals(type)
                || TypeNames.PRIMITIVE_INT.equals(type)
                || TypeNames.PRIMITIVE_BYTE.equals(type)
                || TypeNames.PRIMITIVE_LONG.equals(type)
                || TypeNames.PRIMITIVE_SHORT.equals(type);
    }
}
