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

import io.helidon.codegen.CodegenException;
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
import static io.helidon.extensions.mcp.codegen.McpCodegenUtil.getDescription;
import static io.helidon.extensions.mcp.codegen.McpCodegenUtil.getElementsWithAnnotation;
import static io.helidon.extensions.mcp.codegen.McpCodegenUtil.isBoolean;
import static io.helidon.extensions.mcp.codegen.McpCodegenUtil.isIgnoredSchemaElement;
import static io.helidon.extensions.mcp.codegen.McpCodegenUtil.isList;
import static io.helidon.extensions.mcp.codegen.McpCodegenUtil.isMcpType;
import static io.helidon.extensions.mcp.codegen.McpCodegenUtil.isNumber;
import static io.helidon.extensions.mcp.codegen.McpJsonSchemaCodegen.addSchemaMethodBody;
import static io.helidon.extensions.mcp.codegen.McpTypes.FUNCTION_REQUEST_TOOL_RESULT;
import static io.helidon.extensions.mcp.codegen.McpTypes.LIST_MCP_TOOL_CONTENT;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_DESCRIPTION;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_NAME;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_TOOL;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_TOOL_CONTENTS;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_TOOL_INTERFACE;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_TOOL_RESULT;

class McpToolCodegen {
    private final McpRecorder recorder;

    McpToolCodegen(McpRecorder recorder) {
        this.recorder = recorder;
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
                    .addMethod(method -> addToolOutputSchema(method, toolAnnotation)));
        });
    }

    private void addToolOutputSchema(Method.Builder builder, Annotation toolAnnotation) {
        builder.name("outputSchema")
                .returnType(TypeNames.STRING)
                .addAnnotation(Annotations.OVERRIDE)
                .addContent("return \"")
                .addContent(toolAnnotation.getValue("outputSchema").orElse(""))
                .addContent("\";");
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
                .returnType(returned -> returned.type(FUNCTION_REQUEST_TOOL_RESULT))
                .addAnnotation(Annotations.OVERRIDE);
        builder.addContentLine("return request -> {");

        for (TypedElementInfo param : element.parameterArguments()) {
            if (isMcpType(parameters, param)) {
                continue;
            }
            if (TypeNames.STRING.equals(param.typeName())) {
                parameters.add(param.elementName());
                builder.addContent("var ")
                        .addContent(param.elementName())
                        .addContent(" = request.parameters().get(\"")
                        .addContent(param.elementName())
                        .addContentLine("\").asString().orElse(\"\");");
                continue;
            }
            if (isBoolean(param.typeName())) {
                parameters.add(param.elementName());
                builder.addContent("boolean ")
                        .addContent(param.elementName())
                        .addContent(" = request.parameters().get(\"")
                        .addContent(param.elementName())
                        .addContentLine("\").asBoolean().orElse(false);");
                continue;
            }
            if (isNumber(param.typeName())) {
                parameters.add(param.elementName());
                builder.addContent("var ")
                        .addContent(param.elementName())
                        .addContent(" = request.parameters().get(\"")
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
                        .addContent(" = toList(request.parameters().get(\"")
                        .addContent(param.elementName())
                        .addContentLine("\").asList().orElse(null));");
                continue;
            }
            parameters.add(param.elementName());
            builder.addContent(param.typeName().classNameWithEnclosingNames())
                    .addContent(" ")
                    .addContent(param.elementName())
                    .addContent(" = request.parameters().get(\"")
                    .addContent(param.elementName())
                    .addContent("\").as(")
                    .addContent(param.typeName())
                    .addContentLine(".class).orElse(null);");
        }

        String params = String.join(", ", parameters);
        if (returnType.equals(TypeNames.STRING)) {
            builder.addContent("return ")
                    .addContent(MCP_TOOL_RESULT)
                    .addContent(".builder().contents(")
                    .addContent(List.class)
                    .addContent(".of(")
                    .addContent(MCP_TOOL_CONTENTS)
                    .addContent(".textContent(delegate.")
                    .addContent(element.elementName())
                    .addContent("(")
                    .addContent(params)
                    .addContentLine(")))).build();")
                    .decreaseContentPadding()
                    .addContentLine("};");
            return;
        }
        if (returnType.equals(LIST_MCP_TOOL_CONTENT)) {
            builder.addContent("return ")
                    .addContent(MCP_TOOL_RESULT)
                    .addContent(".builder().contents(delegate.")
                    .addContent(element.elementName())
                    .addContent("(")
                    .addContent(params)
                    .addContentLine(")).build();")
                    .decreaseContentPadding()
                    .addContentLine("};");
            return;
        }
        if (returnType.equals(MCP_TOOL_RESULT)) {
            builder.addContent("return delegate.")
                    .addContent(element.elementName())
                    .addContent("(")
                    .addContent(params)
                    .addContentLine(");")
                    .decreaseContentPadding()
                    .addContentLine("};");
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
                .returnType(McpTypes.MCP_TOOL_ANNOTATIONS)
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
                .addContentLine("return builder.build();");
    }
}
