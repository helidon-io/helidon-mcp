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

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.helidon.codegen.CodegenContext;
import io.helidon.codegen.CodegenException;
import io.helidon.codegen.CodegenLogger;
import io.helidon.codegen.CodegenUtil;
import io.helidon.codegen.RoundContext;
import io.helidon.codegen.classmodel.ClassModel;
import io.helidon.codegen.classmodel.Method;
import io.helidon.codegen.spi.CodegenExtension;
import io.helidon.common.types.AccessModifier;
import io.helidon.common.types.Annotated;
import io.helidon.common.types.Annotation;
import io.helidon.common.types.ElementKind;
import io.helidon.common.types.TypeInfo;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypeNames;
import io.helidon.common.types.TypedElementInfo;

import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_DESCRIPTION;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_JSON_SCHEMA;
import static io.helidon.service.codegen.ServiceCodegenTypes.SERVICE_ANNOTATION_SINGLETON;

/**
 * For each class annotated with {@link io.helidon.extensions.mcp.server.Mcp.JsonSchema},
 * generate a class with single public static method that returns a JSON
 * schema representation of that class as a string.
 */
class McpJsonSchemaCodegen implements CodegenExtension {
    private static final TypeName GENERATOR = TypeName.create(McpCodegen.class);
    private static final TypeName COLLECTION = TypeName.create(Collection.class);
    private final CodegenLogger logger;
    static final String OBJECT_PROPERTY_SCHEMA = """
            builder.append("\\"%s\\": " + %s);
            """;

    McpJsonSchemaCodegen(CodegenContext context) {
        logger = context.logger();
    }

    @Override
    public void process(RoundContext roundContext) {
        //noinspection DuplicatedCode
        logger.log(System.Logger.Level.TRACE, "Processing mcp codegen extension with context "
                + roundContext.types().stream().map(Object::toString).collect(Collectors.joining()));
        Collection<TypeInfo> types = roundContext.annotatedTypes(MCP_JSON_SCHEMA);
        for (TypeInfo type : types) {
            process(roundContext, type);
        }
    }

    private void process(RoundContext roundContext, TypeInfo type) {
        if (type.kind() != ElementKind.CLASS) {
            throw new CodegenException("Type annotated with " + MCP_JSON_SCHEMA.fqName() + " must be a class.",
                                       type.originatingElementValue());
        }

        TypeName mcpFactoryType = type.typeName();
        TypeName generatedType = TypeName.builder()
                .packageName(mcpFactoryType.packageName())
                .className(mcpFactoryType.classNameWithEnclosingNames()
                                   .replace('.', '_') + "__JsonSchema")
                .build();

        var classModel = ClassModel.builder()
                .type(generatedType)
                .copyright(CodegenUtil.copyright(GENERATOR,
                                                 mcpFactoryType,
                                                 generatedType))
                .addAnnotation(CodegenUtil.generatedAnnotation(GENERATOR,
                                                               mcpFactoryType,
                                                               generatedType,
                                                               "1",
                                                               ""))
                .accessModifier(AccessModifier.PACKAGE_PRIVATE)
                .addAnnotation(Annotation.create(SERVICE_ANNOTATION_SINGLETON));

        String schema = type.annotation(MCP_JSON_SCHEMA)
                .stringValue()
                .orElseThrow(() -> new CodegenException(String.format(
                        "No %s annotation found for %s.",
                        MCP_JSON_SCHEMA.fqName(),
                        type.typeName().fqName())));

        Method.Builder method = Method.builder()
                .name("schema")
                .isStatic(true)
                .returnType(TypeNames.STRING)
                .accessModifier(AccessModifier.PACKAGE_PRIVATE)
                .addContentLine("return \"\"\"")
                .addContent(schema)
                .addContentLine("\"\"\";");
        classModel.addMethod(method.build());
        roundContext.addGeneratedType(generatedType, classModel, mcpFactoryType, type.originatingElementValue());
    }

    static void addSchemaMethodBody(Method.Builder method, List<TypedElementInfo> fields) {
        method.addContentLine("var builder = new StringBuilder();");
        method.addContentLine("builder.append(\"{\");");
        method.addContent("""
                                  builder.append("\\"type\\": \\"object\\", \\"properties\\": {");
                                  """);
        int n = fields.size();
        for (int i = 0; i < n; i++) {
            TypedElementInfo field = fields.get(i);
            addPropertySchema(method, field);
            if (i < n - 1) {
                method.addContentLine("builder.append(\", \");");
            }
        }
        method.addContentLine("builder.append(\"}}\");");
        method.addContentLine("return builder.toString();");
    }

    static Optional<String> getDescription(Annotated element) {
        if (element.hasAnnotation(MCP_DESCRIPTION)) {
            Annotation description = element.annotation(MCP_DESCRIPTION);
            return description.stringValue();
        }
        return Optional.empty();
    }

    private static void addPropertySchema(Method.Builder method, TypedElementInfo element) {
        TypeName typeName = element.typeName();
        Optional<String> description = getDescription(element);
        if (isPrimitiveJsonSchemaType(typeName)) {
            method.addContent("""
                                      builder.append("\\"%s\\": {");
                                      """.formatted(element.elementName()));
            description.ifPresent(desc -> addDescription(method, desc));
            method.addContent("""
                                      builder.append("\\"type\\": \\"%s\\"");
                                      """.formatted(mapTypeName(typeName)));
            method.addContent("""
                                      builder.append("}");
                                      """);
            return;
        }
        if (isCollection(typeName)) {
            TypeName argument = typeName.boxed().typeArguments().getFirst();
            method.addContent("""
                                      builder.append("\\"%s\\": {");
                                      """.formatted(element.elementName()));
            description.ifPresent(desc -> addDescription(method, desc));
            method.addContent("""
                                      builder.append("\\"type\\": \\"array\\",");
                                      """);
            method.addContent("""
                                      builder.append("\\"items\\": { \\"type\\": \\"%s\\" }");
                                      """.formatted(mapTypeName(argument)));
            method.addContent("""
                                      builder.append("}");
                                      """);
            return;
        }
        method.addContent(String.format(OBJECT_PROPERTY_SCHEMA,
                                        element.elementName(),
                                        mapElementName(element) + ".schema()"));
    }

    private static void addDescription(Method.Builder method, String description) {
        method.addContent("""
                                  builder.append("\\"description\\": \\"%s\\",");
                                  """.formatted(description));
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

    static String mapElementName(TypedElementInfo element) {
        return element.typeName().classNameWithEnclosingNames()
                .replace('.', '_')
                + "__JsonSchema";
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
