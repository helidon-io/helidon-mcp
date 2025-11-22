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

import java.util.ArrayList;
import java.util.List;

import io.helidon.codegen.CodegenException;
import io.helidon.codegen.classmodel.ClassModel;
import io.helidon.codegen.classmodel.Method;
import io.helidon.common.types.AccessModifier;
import io.helidon.common.types.Annotation;
import io.helidon.common.types.Annotations;
import io.helidon.common.types.EnumValue;
import io.helidon.common.types.TypeInfo;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypeNames;
import io.helidon.common.types.TypedElementInfo;

import static io.helidon.extensions.mcp.codegen.McpCodegenUtil.MCP_TYPES;
import static io.helidon.extensions.mcp.codegen.McpCodegenUtil.createClassName;
import static io.helidon.extensions.mcp.codegen.McpCodegenUtil.getElementsWithAnnotation;
import static io.helidon.extensions.mcp.codegen.McpCodegenUtil.isMcpType;
import static io.helidon.extensions.mcp.codegen.McpTypes.FUNCTION_REQUEST_COMPLETION_CONTENT;
import static io.helidon.extensions.mcp.codegen.McpTypes.LIST_STRING;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_COMPLETION;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_COMPLETION_CONTENT;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_COMPLETION_CONTENTS;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_COMPLETION_INTERFACE;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_COMPLETION_TYPE;

class McpCompletionCodegen {
    private final McpRecorder recorder;

    McpCompletionCodegen(McpRecorder recorder) {
        this.recorder = recorder;
    }

    void generate(ClassModel.Builder classModel, TypeInfo type) {
        getElementsWithAnnotation(type, MCP_COMPLETION).forEach(element -> {
            TypeName innerTypeName = createClassName(element, "__Completion");
            Annotation mcpCompletion = element.annotation(MCP_COMPLETION);
            String reference = mcpCompletion.value().orElse("");
            EnumValue referenceType = (EnumValue) mcpCompletion.objectValue("type").orElse(null);

            recorder.completion(innerTypeName);
            classModel.addInnerClass(clazz -> clazz
                    .name(innerTypeName.className())
                    .addInterface(MCP_COMPLETION_INTERFACE)
                    .accessModifier(AccessModifier.PRIVATE)
                    .addMethod(method -> addCompletionReferenceMethod(method, reference))
                    .addMethod(method -> addCompletionReferenceTypeMethod(method, referenceType))
                    .addMethod(method -> addCompletionMethod(method, classModel, element)));
        });
    }

    private void addCompletionReferenceMethod(Method.Builder builder, String reference) {
        builder.name("reference")
                .addAnnotation(Annotations.OVERRIDE)
                .returnType(TypeNames.STRING)
                .addContent("return \"")
                .addContent(reference)
                .addContentLine("\";");
    }

    private void addCompletionReferenceTypeMethod(Method.Builder builder, EnumValue referenceType) {
        String enumValue = referenceType != null ? referenceType.name() : "PROMPT";
        builder.name("referenceType")
                .addAnnotation(Annotations.OVERRIDE)
                .returnType(McpTypes.MCP_COMPLETION_TYPE)
                .addContent("return ")
                .addContent(MCP_COMPLETION_TYPE)
                .addContent(".")
                .addContent(enumValue)
                .addContentLine(";");
    }

    private void addCompletionMethod(Method.Builder builder, ClassModel.Builder classModel, TypedElementInfo element) {
        List<String> parameters = new ArrayList<>();

        builder.name("completion")
                .returnType(returned -> returned.type(FUNCTION_REQUEST_COMPLETION_CONTENT))
                .addAnnotation(Annotations.OVERRIDE);
        builder.addContentLine("return request -> {");

        for (TypedElementInfo parameter : element.parameterArguments()) {
            if (isMcpType(parameters, parameter)) {
                continue;
            }
            if (parameter.typeName().equals(TypeNames.STRING)) {
                parameters.add(parameter.elementName());
                builder.addContent("var ")
                        .addContent(parameter.elementName())
                        .addContentLine(" = request.parameters().get(\"value\").asString().orElse(\"\");");
                continue;
            }
            throw new CodegenException(String.format("Wrong parameter type for method: %s. Supported types are: %s, or String.",
                                  parameter.elementName(),
                                  String.join(", ", MCP_TYPES)));
        }

        String params = String.join(", ", parameters);
        if (element.typeName().equals(LIST_STRING)) {
            builder.addContent("return ")
                    .addContent(MCP_COMPLETION_CONTENTS)
                    .addContent(".completion(delegate.")
                    .addContent(element.elementName())
                    .addContent("(")
                    .addContent(params)
                    .addContentLine("));")
                    .decreaseContentPadding()
                    .addContentLine("};");
            return;
        }
        if (element.typeName().equals(MCP_COMPLETION_CONTENT)) {
            builder.addContent("return delegate.")
                    .addContent(element.elementName())
                    .addContent("(")
                    .addContent(params)
                    .addContentLine(");")
                    .decreaseContentPadding()
                    .addContentLine("};");
            return;
        }
        throw new CodegenException(String.format("Wrong return type for method: %s. Supported types are: %s, or %s.",
                                                 element.elementName(),
                                                 LIST_STRING,
                                                 MCP_COMPLETION_CONTENT.classNameWithTypes()));

    }
}
