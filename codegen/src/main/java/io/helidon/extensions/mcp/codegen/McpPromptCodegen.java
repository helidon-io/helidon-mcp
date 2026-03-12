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

import io.helidon.codegen.CodegenException;
import io.helidon.codegen.classmodel.ClassModel;
import io.helidon.codegen.classmodel.Method;
import io.helidon.common.types.AccessModifier;
import io.helidon.common.types.Annotation;
import io.helidon.common.types.Annotations;
import io.helidon.common.types.TypeInfo;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypeNames;
import io.helidon.common.types.TypedElementInfo;

import static io.helidon.extensions.mcp.codegen.McpCodegenUtil.MCP_TYPES;
import static io.helidon.extensions.mcp.codegen.McpCodegenUtil.createClassName;
import static io.helidon.extensions.mcp.codegen.McpCodegenUtil.getElementsWithAnnotation;
import static io.helidon.extensions.mcp.codegen.McpCodegenUtil.isIgnoredSchemaElement;
import static io.helidon.extensions.mcp.codegen.McpCodegenUtil.isMcpType;
import static io.helidon.extensions.mcp.codegen.McpTypes.LIST_MCP_PROMPT_ARGUMENT;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_DESCRIPTION;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_NAME;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_PROMPT;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_PROMPT_ARGUMENT;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_PROMPT_INTERFACE;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_PROMPT_REQUEST;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_PROMPT_RESULT;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_ROLE;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_ROLE_ENUM;

class McpPromptCodegen {
    private final McpRecorder recorder;

    McpPromptCodegen(McpRecorder recorder) {
        this.recorder = recorder;
    }

    void generate(ClassModel.Builder classModel, TypeInfo type) {
        getElementsWithAnnotation(type, MCP_PROMPT).forEach(element -> {
            TypeName innerTypeName = createClassName(element, "__Prompt");
            String description = element.annotation(MCP_PROMPT).value().orElse("");

            recorder.prompt(innerTypeName);
            classModel.addInnerClass(clazz -> clazz
                    .name(innerTypeName.className())
                    .addInterface(MCP_PROMPT_INTERFACE)
                    .accessModifier(AccessModifier.PRIVATE)
                    .addMethod(method -> addPromptNameMethod(method, element))
                    .addMethod(method -> addPromptDescriptionMethod(method, description))
                    .addMethod(method -> addPromptArgumentsMethod(method, element))
                    .addMethod(method -> addPromptMethod(method, classModel, element)));
        });
    }

    private void addPromptNameMethod(Method.Builder builder, TypedElementInfo element) {
        String name = element.findAnnotation(MCP_NAME)
                .flatMap(Annotation::value)
                .orElse(element.elementName());
        builder.name("name")
                .addAnnotation(Annotations.OVERRIDE)
                .returnType(TypeNames.STRING)
                .addContent("return ")
                .addContentLiteral(name)
                .addContentLine(";");
    }

    private void addPromptDescriptionMethod(Method.Builder builder, String description) {
        builder.name("description")
                .addAnnotation(Annotations.OVERRIDE)
                .returnType(TypeNames.STRING)
                .addContent("return ")
                .addContentLiteral(description)
                .addContentLine(";");
    }

    private void addPromptMethod(Method.Builder builder, ClassModel.Builder classModel, TypedElementInfo element) {
        List<String> parameters = new ArrayList<>();
        TypeName returnType = element.signature().type();
        String role = element.findAnnotation(MCP_ROLE)
                .flatMap(Annotation::value)
                .orElse("ASSISTANT");

        builder.name("prompt")
                .returnType(returned -> returned.type(MCP_PROMPT_RESULT))
                .addParameter(parameter -> parameter.type(MCP_PROMPT_REQUEST).name("request"))
                .addAnnotation(Annotations.OVERRIDE);

        for (TypedElementInfo param : element.parameterArguments()) {
            if (isMcpType(parameters, param)) {
                continue;
            }
            if (param.typeName().equals(MCP_PROMPT_REQUEST)) {
                parameters.add("request");
                continue;
            }
            if (param.typeName().equals(TypeNames.STRING)) {
                parameters.add(param.elementName());
                builder.addContent(param.typeName().classNameWithEnclosingNames())
                        .addContent(" ")
                        .addContent(param.elementName())
                        .addContent(" = request.arguments().get(")
                        .addContentLiteral(param.elementName())
                        .addContentLine(").asString().orElse(\"\");");
                continue;
            }
            throw new CodegenException(String.format("Wrong parameter type for method: %s. Supported types are: %s, or String.",
                                                     element.elementName(),
                                                     String.join(", ", MCP_TYPES)));
        }

        String params = String.join(", ", parameters);
        if (returnType.equals(MCP_PROMPT_RESULT)) {
            builder.addContent("return delegate.")
                    .addContent(element.elementName())
                    .addContent("(")
                    .addContent(params)
                    .addContentLine(");");
            return;
        }
        if (returnType.equals(TypeNames.STRING)) {
            builder.addContent("return ")
                    .addContent(MCP_PROMPT_RESULT)
                    .addContentLine(".builder()")
                    .increaseContentPadding()
                    .addContent(".addTextContent(t -> t.text(")
                    .addContent("delegate.")
                    .addContent(element.elementName())
                    .addContent("(")
                    .addContent(params)
                    .addContent(")).role(")
                    .addContent(MCP_ROLE_ENUM)
                    .addContent(".")
                    .addContent(role)
                    .addContentLine("))")
                    .addContentLine(".build();");
            return;
        }
        throw new CodegenException(String.format("Wrong return type for method: %s. Supported types are: %s, or String.",
                                                 element.elementName(),
                                                 MCP_PROMPT_RESULT.classNameWithTypes()));
    }

    private void addPromptArgumentsMethod(Method.Builder builder, TypedElementInfo element) {
        List<String> promptArgs = new ArrayList<>();
        int index = 0;

        builder.name("arguments")
                .addAnnotation(Annotations.OVERRIDE)
                .returnType(LIST_MCP_PROMPT_ARGUMENT);

        for (TypedElementInfo param : element.parameterArguments()) {
            if (isIgnoredSchemaElement(param.typeName())) {
                continue;
            }
            if (!param.typeName().equals(TypeNames.STRING)) {
                throw new CodegenException(String.format("Prompt parameters must be one of the following types: %s, or String.",
                                                         String.join(", ", MCP_TYPES)));
            }
            String builderName = "builder" + index++;

            builder.addContent("var ")
                    .addContent(builderName)
                    .addContent(" = ")
                    .addContent(MCP_PROMPT_ARGUMENT)
                    .addContentLine(".builder();")
                    .addContent(builderName)
                    .addContent(".name(")
                    .addContentLiteral(param.elementName())
                    .addContentLine(");")
                    .addContent(builderName)
                    .addContentLine(".required(true);");

            promptArgs.add(builderName + ".build()");

            if (param.hasAnnotation(MCP_DESCRIPTION)) {
                String description = param.annotation(MCP_DESCRIPTION).value().orElse("");
                builder.addContent(builderName)
                        .addContent(".description(")
                        .addContentLiteral(description)
                        .addContentLine(");");
                continue;
            }
            builder.addContent(builderName)
                    .addContent(".description(")
                    .addContentLiteral(param.elementName())
                    .addContentLine(");");
        }
        builder.addContent("return ")
                .addContent(List.class)
                .addContent(".of(")
                .addContent(String.join(", ", promptArgs))
                .addContent(");");
    }
}
