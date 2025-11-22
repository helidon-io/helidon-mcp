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
import io.helidon.common.types.TypeInfo;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypeNames;
import io.helidon.common.types.TypedElementInfo;

import static io.helidon.extensions.mcp.codegen.McpCodegenUtil.MCP_TYPES;
import static io.helidon.extensions.mcp.codegen.McpCodegenUtil.createClassName;
import static io.helidon.extensions.mcp.codegen.McpCodegenUtil.getElementsWithAnnotation;
import static io.helidon.extensions.mcp.codegen.McpCodegenUtil.isMcpType;
import static io.helidon.extensions.mcp.codegen.McpCodegenUtil.isResourceTemplate;
import static io.helidon.extensions.mcp.codegen.McpTypes.CONSUMER_REQUEST;
import static io.helidon.extensions.mcp.codegen.McpTypes.FUNCTION_REQUEST_LIST_RESOURCE_CONTENT;
import static io.helidon.extensions.mcp.codegen.McpTypes.HELIDON_MEDIA_TYPE;
import static io.helidon.extensions.mcp.codegen.McpTypes.HELIDON_MEDIA_TYPES;
import static io.helidon.extensions.mcp.codegen.McpTypes.LIST_MCP_RESOURCE_CONTENT;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_NAME;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_RESOURCE;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_RESOURCE_CONTENTS;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_RESOURCE_INTERFACE;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_RESOURCE_SUBSCRIBER;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_RESOURCE_SUBSCRIBER_INTERFACE;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_RESOURCE_UNSUBSCRIBER;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_RESOURCE_UNSUBSCRIBER_INTERFACE;
import static io.helidon.extensions.mcp.codegen.McpTypes.URI_PATH;

class McpResourceCodegen {
    private final McpRecorder recorder;

    McpResourceCodegen(McpRecorder recorder) {
        this.recorder = recorder;
    }

    void generate(ClassModel.Builder classModel, TypeInfo type) {
        generateResources(classModel, type);
        generateSubscribers(classModel, type);
        generateUnsubscribers(classModel, type);
    }

    private void generateResources(ClassModel.Builder classModel, TypeInfo type) {
        getElementsWithAnnotation(type, MCP_RESOURCE).forEach(element -> {
            TypeName innerTypeName = createClassName(element, "__Resource");
            String uri = element.findAnnotation(MCP_RESOURCE)
                    .flatMap(annotation -> annotation.stringValue("uri"))
                    .orElseThrow(() -> new CodegenException("Resource " + element.elementName() + " must have a URI.",
                                                            element.originatingElementValue()));
            String description = element.findAnnotation(MCP_RESOURCE)
                    .flatMap(annotation -> annotation.stringValue("description"))
                    .orElseThrow(() -> new CodegenException("Resource " + element.elementName() + " must have a description.",
                                                            element.originatingElementValue()));
            String mediaTypeContent = element.findAnnotation(MCP_RESOURCE)
                    .flatMap(annotation -> annotation.stringValue("mediaType"))
                    .orElseThrow(() -> new CodegenException("Resource " + element.elementName() + " must have a Media Type.",
                                                            element.originatingElementValue()));
            recorder.resource(innerTypeName);
            classModel.addInnerClass(clazz -> clazz
                    .name(innerTypeName.className())
                    .addInterface(MCP_RESOURCE_INTERFACE)
                    .accessModifier(AccessModifier.PRIVATE)
                    .addMethod(method -> addResourceUriMethod(method, uri))
                    .addMethod(method -> addResourceNameMethod(method, element))
                    .addMethod(method -> addResourceDescriptionMethod(method, description))
                    .addMethod(method -> addResourceMethod(method, uri, classModel, element))
                    .addMethod(method -> addResourceMediaTypeMethod(method, mediaTypeContent)));
        });
    }

    private void generateSubscribers(ClassModel.Builder classModel, TypeInfo type) {
        getElementsWithAnnotation(type, MCP_RESOURCE_SUBSCRIBER).forEach(element -> {
            TypeName innerTypeName = createClassName(element, "__ResourceSubscriber");
            Annotation mcpCompletion = element.annotation(MCP_RESOURCE_SUBSCRIBER);
            String uri = mcpCompletion.value().orElse("");

            recorder.subscriber(innerTypeName);
            classModel.addInnerClass(clazz -> clazz
                    .name(innerTypeName.className())
                    .addInterface(MCP_RESOURCE_SUBSCRIBER_INTERFACE)
                    .accessModifier(AccessModifier.PRIVATE)
                    .addMethod(method -> addSubscriberUriMethod(method, uri))
                    .addMethod(method -> addSubscriberMethod(method, element, "subscribe")));
        });
    }

    private void generateUnsubscribers(ClassModel.Builder classModel, TypeInfo type) {
        getElementsWithAnnotation(type, MCP_RESOURCE_UNSUBSCRIBER).forEach(element -> {
            TypeName innerTypeName = createClassName(element, "__ResourceUnsubscriber");
            Annotation mcpCompletion = element.annotation(MCP_RESOURCE_UNSUBSCRIBER);
            String uri = mcpCompletion.value().orElse("");

            recorder.unsubscriber(innerTypeName);
            classModel.addInnerClass(clazz -> clazz
                    .name(innerTypeName.className())
                    .addInterface(MCP_RESOURCE_UNSUBSCRIBER_INTERFACE)
                    .accessModifier(AccessModifier.PRIVATE)
                    .addMethod(method -> addSubscriberUriMethod(method, uri))
                    .addMethod(method -> addSubscriberMethod(method, element, "unsubscribe")));
        });
    }

    private void addResourceNameMethod(Method.Builder builder, TypedElementInfo element) {
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

    private void addResourceDescriptionMethod(Method.Builder builder, String description) {
        builder.name("description")
                .addAnnotation(Annotations.OVERRIDE)
                .returnType(TypeNames.STRING)
                .addContent("return \"")
                .addContent(description)
                .addContentLine("\";");
    }

    private void addResourceUriMethod(Method.Builder builder, String uri) {
        builder.name("uri")
                .addAnnotation(Annotations.OVERRIDE)
                .returnType(TypeNames.STRING)
                .addContent("return \"")
                .addContent(uri)
                .addContentLine("\";");
    }

    private void addResourceMediaTypeMethod(Method.Builder builder, String mediaTypeContent) {
        builder.name("mediaType")
                .addAnnotation(Annotations.OVERRIDE)
                .returnType(HELIDON_MEDIA_TYPE)
                .addContent("return ")
                .addContent(HELIDON_MEDIA_TYPES)
                .addContent(".create(\"")
                .addContent(mediaTypeContent)
                .addContentLine("\");");
    }

    private void addResourceMethod(Method.Builder builder, String uri, ClassModel.Builder classModel, TypedElementInfo element) {
        List<String> parameters = new ArrayList<>();
        TypeName returnType = element.signature().type();

        builder.name("resource")
                .addAnnotation(Annotations.OVERRIDE)
                .returnType(returned -> returned.type(FUNCTION_REQUEST_LIST_RESOURCE_CONTENT));
        builder.addContentLine("return request -> {");

        for (TypedElementInfo parameter : element.parameterArguments()) {
            if (isMcpType(parameters, parameter)) {
                continue;
            }
            if (isResourceTemplate(uri)) {
                if (TypeNames.STRING.equals(parameter.typeName())) {
                    parameters.add(parameter.elementName());
                    builder.addContent("String encoded_")
                            .addContent(parameter.elementName())
                            .addContent(" = request.parameters().get(\"")
                            .addContent(parameter.elementName())
                            .addContentLine("\").asString().orElse(\"\");");
                    builder.addContent("String ")
                            .addContent(parameter.elementName())
                            .addContent(" = ")
                            .addContent(URI_PATH)
                            .addContent(".create(encoded_")
                            .addContent(parameter.elementName())
                            .addContentLine(").path();");
                    continue;
                }
            }
            throw new CodegenException(String.format("Parameter %s must be one of supported type: %s",
                                                     parameter.elementName(),
                                                     String.join(", ", MCP_TYPES)));
        }
        String params = String.join(", ", parameters);
        if (returnType.equals(TypeNames.STRING)) {
            builder.addContent("return ")
                    .addContent(List.class)
                    .addContent(".of(")
                    .addContent(MCP_RESOURCE_CONTENTS)
                    .addContent(".textContent(delegate.")
                    .addContent(element.elementName())
                    .addContent("(")
                    .addContent(params)
                    .addContentLine(")));")
                    .decreaseContentPadding()
                    .addContentLine("};");
            return;
        }
        if (returnType.equals(LIST_MCP_RESOURCE_CONTENT)) {
            builder.addContent("return delegate.")
                    .addContent(element.elementName())
                    .addContent("(")
                    .addContent(params)
                    .addContentLine(");")
                    .addContentLine("};");
            return;
        }
        throw new CodegenException(String.format("Method %s must return one the following return type: %s",
                                                 element.elementName(),
                                                 String.join(", ", List.of("String",
                                                                           LIST_MCP_RESOURCE_CONTENT.classNameWithTypes()))));
    }

    private void addSubscriberUriMethod(Method.Builder builder, String uri) {
        builder.name("uri")
                .addAnnotation(Annotations.OVERRIDE)
                .returnType(TypeNames.STRING)
                .addContent("return \"")
                .addContent(uri)
                .addContentLine("\";");
    }

    private void addSubscriberMethod(Method.Builder builder, TypedElementInfo element, String methodName) {
        List<String> parameters = new ArrayList<>();

        builder.name(methodName)
                .addAnnotation(Annotations.OVERRIDE)
                .returnType(returned -> returned.type(CONSUMER_REQUEST));
        builder.addContentLine("return request -> {");

        for (TypedElementInfo parameter : element.parameterArguments()) {
            if (isMcpType(parameters, parameter)) {
                continue;
            }
            throw new CodegenException("Parameter " + parameter.elementName() + " is not supported");
        }
        if (element.signature().type().equals(TypeNames.PRIMITIVE_VOID)) {
            String params = String.join(", ", parameters);
            builder.addContent("delegate.")
                    .addContent(element.elementName())
                    .addContent("(")
                    .addContent(params)
                    .addContentLine(");")
                    .decreaseContentPadding()
                    .addContentLine("};");
            return;
        }
        throw new CodegenException("Method " + element.elementName() + " must return void");
    }
}
