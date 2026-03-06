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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import io.helidon.codegen.CodegenContext;
import io.helidon.codegen.CodegenException;
import io.helidon.codegen.CodegenLogger;
import io.helidon.codegen.classmodel.ClassModel;
import io.helidon.codegen.classmodel.Method;
import io.helidon.common.types.AccessModifier;
import io.helidon.common.types.Annotation;
import io.helidon.common.types.Annotations;
import io.helidon.common.types.ElementKind;
import io.helidon.common.types.TypeInfo;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypeNames;
import io.helidon.common.types.TypedElementInfo;

import static io.helidon.extensions.mcp.codegen.McpCodegenUtil.addToListMethod;
import static io.helidon.extensions.mcp.codegen.McpCodegenUtil.createClassName;
import static io.helidon.extensions.mcp.codegen.McpCodegenUtil.generateSafeMultiLine;
import static io.helidon.extensions.mcp.codegen.McpCodegenUtil.getDescription;
import static io.helidon.extensions.mcp.codegen.McpCodegenUtil.getElementsWithAnnotation;
import static io.helidon.extensions.mcp.codegen.McpCodegenUtil.isBoolean;
import static io.helidon.extensions.mcp.codegen.McpCodegenUtil.isIgnoredSchemaElement;
import static io.helidon.extensions.mcp.codegen.McpCodegenUtil.isList;
import static io.helidon.extensions.mcp.codegen.McpCodegenUtil.isMcpType;
import static io.helidon.extensions.mcp.codegen.McpCodegenUtil.isNumber;
import static io.helidon.extensions.mcp.codegen.McpJsonSchemaCodegen.addSchemaMethodBody;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_DESCRIPTION;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_NAME;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_TOOL;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_TOOL_INTERFACE;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_TOOL_OUTPUT_SCHEMA;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_TOOL_OUTPUT_SCHEMA_TEXT;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_TOOL_REQUEST;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_TOOL_RESULT;
import static io.helidon.extensions.mcp.codegen.McpTypes.OPTIONAL_STRING;
import static io.helidon.extensions.mcp.codegen.McpTypes.OPTIONAL_TOOL_ANNOTATIONS;

class McpToolCodegen {
    private final McpRecorder recorder;
    private final CodegenLogger logger;

    McpToolCodegen(McpRecorder recorder, CodegenContext context) {
        this.recorder = recorder;
        this.logger = context.logger();
    }

    void generate(ClassModel.Builder classModel, TypeInfo type) {
        getElementsWithAnnotation(type, MCP_TOOL).forEach(element -> {
            TypeName innerToolName = createClassName(element, "__Tool");
            Annotation toolAnnotation = element.annotation(MCP_TOOL);
            String description = toolAnnotation.value().orElse("No description available.");

            recorder.tool(innerToolName);
            classModel.addInnerClass(clazz -> clazz
                    .name(innerToolName.className())
                    .addInterface(MCP_TOOL_INTERFACE)
                    .accessModifier(AccessModifier.PRIVATE)
                    .addMethod(method -> addToolNameMethod(method, element))
                    .addMethod(method -> addToolDescriptionMethod(method, description))
                    .addMethod(method -> addToolSchemaMethod(method, element))
                    .addMethod(method -> addToolMethod(method, classModel, element))
                    .addMethod(method -> addToolAnnotationsMethod(method, toolAnnotation))
                    .addMethod(method -> addToolOutputSchema(method, element)));
        });
    }

    private void addToolOutputSchema(Method.Builder builder, TypedElementInfo element) {
        Optional<Annotation> schema = element.findAnnotation(MCP_TOOL_OUTPUT_SCHEMA);
        Optional<Annotation> textSchema = element.findAnnotation(MCP_TOOL_OUTPUT_SCHEMA_TEXT);
        builder.name("outputSchema")
                .returnType(OPTIONAL_STRING)
                .addAnnotation(Annotations.OVERRIDE);

        if (schema.isEmpty() && textSchema.isEmpty()) {
            builder.addContent("return Optional.empty();");
            return;
        }

        if (schema.isPresent() && textSchema.isPresent()) {
            String message = String.format("Annotation %s will be ignored.",
                                           MCP_TOOL_OUTPUT_SCHEMA_TEXT.classNameWithEnclosingNames());
            logger.log(System.Logger.Level.WARNING, message);
        }

        if (schema.isPresent()) {
            String outputSchema = schema.flatMap(Annotation::typeValue)
                    .map(TypeName::classNameWithTypes)
                    .map(value -> value + "__JsonSchema")
                    .orElseThrow(() -> new CodegenException("Cannot parse output schema"));
            builder.addContent("return Optional.of(Services.get(")
                    .addContent(outputSchema)
                    .addContent(".class).jsonSchema());");
            return;
        }

        String outputShema = textSchema.flatMap(t -> t.stringValue())
                .orElseThrow(() -> new CodegenException("Cannot parse output text schema"))
                .replace("\"", "\\\"");
        builder.addContent("return Optional.of(");
        generateSafeMultiLine(builder, outputShema);
        builder.addContentLine(");");
    }

    private void addToolSchemaMethod(Method.Builder builder, TypedElementInfo element) {
        Method.Builder method = builder.name("schema")
                .returnType(TypeNames.STRING)
                .addAnnotation(Annotations.OVERRIDE);

        List<TypedElementInfo> fields = new ArrayList<>();
        for (TypedElementInfo param : element.parameterArguments()) {
            if (isIgnoredSchemaElement(param.typeName())) {
                continue;
            }
            Optional<String> description = getDescription(param);
            var field = TypedElementInfo.builder()
                    .elementName(param.elementName())
                    .typeName(param.typeName())
                    .kind(ElementKind.FIELD)
                    .accessModifier(AccessModifier.PUBLIC);
            description.ifPresent(desc -> field.addAnnotation(Annotation.create(MCP_DESCRIPTION, desc)));
            fields.add(field.build());
        }

        if (!fields.isEmpty()) {
            addSchemaMethodBody(method, fields);
        } else {
            method.addContentLine("return \"\";");
        }
    }

    private void addToolMethod(Method.Builder builder, ClassModel.Builder classModel, TypedElementInfo element) {
        List<String> parameters = new ArrayList<>();
        TypeName returnType = element.signature().type();

        builder.name("tool")
                .addAnnotation(Annotations.OVERRIDE)
                .returnType(returned -> returned.type(MCP_TOOL_RESULT))
                .addParameter(parameter -> parameter.type(MCP_TOOL_REQUEST).name("request"));

        for (TypedElementInfo param : element.parameterArguments()) {
            if (isMcpType(parameters, param)) {
                continue;
            }
            if (param.typeName().equals(MCP_TOOL_REQUEST)) {
                parameters.add("request");
                continue;
            }
            if (TypeNames.STRING.equals(param.typeName())) {
                parameters.add(param.elementName());
                builder.addContent("var ")
                        .addContent(param.elementName())
                        .addContent(" = request.arguments().get(\"")
                        .addContent(param.elementName())
                        .addContentLine("\").asString().orElse(\"\");");
                continue;
            }
            if (isBoolean(param.typeName())) {
                parameters.add(param.elementName());
                builder.addContent("boolean ")
                        .addContent(param.elementName())
                        .addContent(" = request.arguments().get(\"")
                        .addContent(param.elementName())
                        .addContentLine("\").asBoolean().orElse(false);");
                continue;
            }
            if (isNumber(param.typeName())) {
                parameters.add(param.elementName());
                builder.addContent("var ")
                        .addContent(param.elementName())
                        .addContent(" = request.arguments().get(\"")
                        .addContent(param.elementName())
                        .addContent("\").as")
                        .addContent(param.typeName().className())
                        .addContentLine("().orElse(null);");
                continue;
            }
            if (isList(param.typeName())) {
                TypeName typeArg = param.typeName().typeArguments().getFirst();
                addToListMethod(classModel, typeArg);
                parameters.add(param.elementName());
                builder.addContent("var ")
                        .addContent(param.elementName())
                        .addContent(" = toList(request.arguments().get(\"")
                        .addContent(param.elementName())
                        .addContentLine("\").asList().orElse(null));");
                continue;
            }
            parameters.add(param.elementName());
            builder.addContent(param.typeName().classNameWithEnclosingNames())
                    .addContent(" ")
                    .addContent(param.elementName())
                    .addContent(" = request.arguments().get(\"")
                    .addContent(param.elementName())
                    .addContent("\").as(")
                    .addContent(param.typeName())
                    .addContentLine(".class).orElse(null);");
        }

        String params = String.join(", ", parameters);
        if (returnType.equals(TypeNames.STRING)) {
            builder.addContent("return ")
                    .addContent(MCP_TOOL_RESULT)
                    .addContentLine(".builder()")
                    .increaseContentPadding()
                    .addContent(".addTextContent(delegate.")
                    .addContent(element.elementName())
                    .addContent("(")
                    .addContent(params)
                    .addContentLine("))")
                    .addContentLine(".build();");
            return;
        }
        if (returnType.equals(MCP_TOOL_RESULT)) {
            builder.addContent("return delegate.")
                    .addContent(element.elementName())
                    .addContent("(")
                    .addContent(params)
                    .addContentLine(");");
            return;
        }
        throw new CodegenException(String.format("Method %s must return one the following return type: %s",
                                                 element.elementName(), MCP_TOOL_RESULT));
    }

    private void addToolNameMethod(Method.Builder builder, TypedElementInfo element) {
        String name = element.findAnnotation(MCP_NAME)
                .flatMap(Annotation::value)
                .orElse(element.elementName());
        builder.name("name")
                .addAnnotation(Annotations.OVERRIDE)
                .returnType(TypeNames.STRING)
                .addContent("return \"")
                .addContent(name)
                .addContentLine("\";");
    }

    private void addToolDescriptionMethod(Method.Builder builder, String description) {
        builder.name("description")
                .addAnnotation(Annotations.OVERRIDE)
                .returnType(TypeNames.STRING)
                .addContent("return \"")
                .addContent(description)
                .addContentLine("\";");
    }

    private void addToolAnnotationsMethod(Method.Builder builder, Annotation toolAnnotation) {
        builder.name("annotations")
                .addAnnotation(Annotations.OVERRIDE)
                .returnType(OPTIONAL_TOOL_ANNOTATIONS)
                .addContentLine("var builder = McpToolAnnotations.builder();")
                .addContent("builder.title(\"")
                .addContent(toolAnnotation.stringValue("title").orElse(""))
                .addContentLine("\")")
                .increaseContentPadding()
                .addContent(".readOnlyHint(")
                .addContent(toolAnnotation.booleanValue("readOnlyHint").orElse(false).toString())
                .addContentLine(")")
                .addContent(".destructiveHint(")
                .addContent(toolAnnotation.booleanValue("destructiveHint").orElse(true).toString())
                .addContentLine(")")
                .addContent(".idempotentHint(")
                .addContent(toolAnnotation.booleanValue("idempotentHint").orElse(false).toString())
                .addContentLine(")")
                .addContent(".openWorldHint(")
                .addContent(toolAnnotation.booleanValue("openWorldHint").orElse(true).toString())
                .addContentLine(");")
                .decreaseContentPadding()
                .addContent("return ")
                .addContent(Optional.class)
                .addContentLine(".of(builder.build());");
    }
}
