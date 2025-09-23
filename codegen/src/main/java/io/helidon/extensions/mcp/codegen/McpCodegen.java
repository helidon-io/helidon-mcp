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
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
import io.helidon.common.types.Annotation;
import io.helidon.common.types.Annotations;
import io.helidon.common.types.ElementKind;
import io.helidon.common.types.EnumValue;
import io.helidon.common.types.ResolvedType;
import io.helidon.common.types.TypeInfo;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypeNames;
import io.helidon.common.types.TypedElementInfo;

import static io.helidon.extensions.mcp.codegen.McpJsonSchemaCodegen.addSchemaMethodBody;
import static io.helidon.extensions.mcp.codegen.McpJsonSchemaCodegen.getDescription;
import static io.helidon.extensions.mcp.codegen.McpTypes.FUNCTION_REQUEST_COMPLETION_CONTENT;
import static io.helidon.extensions.mcp.codegen.McpTypes.FUNCTION_REQUEST_LIST_PROMPT_CONTENT;
import static io.helidon.extensions.mcp.codegen.McpTypes.FUNCTION_REQUEST_LIST_RESOURCE_CONTENT;
import static io.helidon.extensions.mcp.codegen.McpTypes.FUNCTION_REQUEST_LIST_TOOL_CONTENT;
import static io.helidon.extensions.mcp.codegen.McpTypes.HELIDON_MEDIA_TYPE;
import static io.helidon.extensions.mcp.codegen.McpTypes.HELIDON_MEDIA_TYPES;
import static io.helidon.extensions.mcp.codegen.McpTypes.HTTP_FEATURE;
import static io.helidon.extensions.mcp.codegen.McpTypes.HTTP_ROUTING_BUILDER;
import static io.helidon.extensions.mcp.codegen.McpTypes.LIST_MCP_PROMPT_ARGUMENT;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_CANCELLATION;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_COMPLETION;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_COMPLETION_CONTENTS;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_COMPLETION_INTERFACE;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_COMPLETION_TYPE;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_DESCRIPTION;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_FEATURES;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_LOGGER;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_NAME;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_PARAMETERS;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_PATH;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_PROGRESS;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_PROMPT;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_PROMPTS_PAGE_SIZE;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_PROMPT_ARGUMENT;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_PROMPT_CONTENTS;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_PROMPT_INTERFACE;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_REQUEST;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_RESOURCE;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_RESOURCES_PAGE_SIZE;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_RESOURCE_CONTENTS;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_RESOURCE_INTERFACE;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_RESOURCE_TEMPLATES_PAGE_SIZE;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_ROLE;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_ROLE_ENUM;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_SERVER;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_SERVER_CONFIG;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_TOOL;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_TOOLS_PAGE_SIZE;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_TOOL_CONTENTS;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_TOOL_INTERFACE;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_VERSION;
import static io.helidon.service.codegen.ServiceCodegenTypes.SERVICE_ANNOTATION_SINGLETON;

final class McpCodegen implements CodegenExtension {
    private static final TypeName GENERATOR = TypeName.create(McpCodegen.class);
    private static final ResolvedType STRING_LIST = ResolvedType.create(TypeName.builder(TypeNames.LIST)
                                                                                .addTypeArgument(TypeNames.STRING)
                                                                                .build());

    /*
     * Map of MCP component kind associated with their generated class name.
     */
    private final Map<McpKind, List<TypeName>> components = new EnumMap<>(McpKind.class);
    private final CodegenLogger logger;

    McpCodegen(CodegenContext context) {
        logger = context.logger();

        initializeComponents();
    }

    @Override
    public void process(RoundContext roundContext) {
        //noinspection DuplicatedCode
        logger.log(System.Logger.Level.TRACE, "Processing MCP codegen extension with context "
                + roundContext.types().stream().map(Object::toString).collect(Collectors.joining()));
        Collection<TypeInfo> types = roundContext.annotatedTypes(MCP_SERVER);
        for (TypeInfo type : types) {
            process(roundContext, type);
        }
    }

    private void process(RoundContext roundCtx, TypeInfo type) {
        if (type.kind() != ElementKind.CLASS && type.kind() != ElementKind.INTERFACE) {
            throw new CodegenException("Type annotated with " + MCP_SERVER.fqName() + " must be a class or an interface.",
                                       type.originatingElementValue());
        }

        TypeName mcpServerType = type.typeName();
        TypeName generatedType = generatedTypeName(mcpServerType, "McpServer");

        var serverClassModel = ClassModel.builder()
                .type(generatedType)
                .addInterface(HTTP_FEATURE)
                .copyright(CodegenUtil.copyright(GENERATOR,
                                                 mcpServerType,
                                                 generatedType))
                .addAnnotation(CodegenUtil.generatedAnnotation(GENERATOR,
                                                               mcpServerType,
                                                               generatedType,
                                                               "1",
                                                               ""))
                .accessModifier(AccessModifier.PACKAGE_PRIVATE)
                .addAnnotation(Annotation.create(SERVICE_ANNOTATION_SINGLETON));

        serverClassModel.addField(delegate -> delegate
                .accessModifier(AccessModifier.PRIVATE)
                .isFinal(true)
                .name("delegate")
                .type(type.typeName())
                .addContent("new ")
                .addContent(type.typeName())
                .addContent("()"));

        generateTools(generatedType, serverClassModel, type);
        generatePrompts(generatedType, serverClassModel, type);
        generateResources(generatedType, serverClassModel, type);
        generateCompletions(generatedType, serverClassModel, type);

        serverClassModel.addMethod(method -> addRoutingMethod(method, type));
        roundCtx.addGeneratedType(generatedType, serverClassModel, mcpServerType, type.originatingElementValue());
    }

    private void addRoutingMethod(Method.Builder method, TypeInfo type) {
        String defaultServerName = type.typeName().className() + " mcp server";
        String serverName = type.annotation(MCP_SERVER)
                .value()
                .orElse(defaultServerName);

        method.name("setup")
                .accessModifier(AccessModifier.PUBLIC)
                .addAnnotation(Annotations.OVERRIDE)
                .addParameter(rules -> rules.type(HTTP_ROUTING_BUILDER)
                        .name("routing"))
                .addContent(MCP_SERVER_CONFIG)
                .addContent(".Builder builder =")
                .addContent(MCP_SERVER_CONFIG)
                .addContentLine(".builder();")
                .addContent("builder.name(")
                .addContentLiteral(serverName)
                .addContentLine(");");

        type.findAnnotation(MCP_VERSION)
                .flatMap(Annotation::value)
                .ifPresent(ver -> method.addContent("builder.version(")
                        .addContentLiteral(ver)
                        .addContentLine(");"));

        type.findAnnotation(MCP_PATH)
                .flatMap(Annotation::value)
                .ifPresent(path -> method.addContent("builder.path(")
                        .addContentLiteral(path)
                        .addContentLine(");"));

        addPagination(type, method, MCP_TOOLS_PAGE_SIZE, "toolsPageSize");
        addPagination(type, method, MCP_PROMPTS_PAGE_SIZE, "promptsPageSize");
        addPagination(type, method, MCP_RESOURCES_PAGE_SIZE, "resourcesPageSize");
        addPagination(type, method, MCP_RESOURCE_TEMPLATES_PAGE_SIZE, "resourceTemplatesPageSize");

        components.forEach((mcpKind, typeNames) -> {
            for (TypeName typeName : typeNames) {
                method.addContent("builder.")
                        .addContent(mcpKind.methodName)
                        .addContent("(new ")
                        .addContent(typeName)
                        .addContentLine("());");
            }
        });

        method.addContentLine("builder.build().setup(routing);");
        // Clear the components map as code generation if over for this server.
        initializeComponents();
    }

    private void addPagination(TypeInfo type, Method.Builder method, TypeName annotation, String pageSizeSetter) {
        type.findAnnotation(annotation)
                .map(it -> it.value())
                .map(pageSizeValue -> pageSizeValue.orElse("0"))
                .map(pageSize -> method.addContent("builder.")
                        .addContent(pageSizeSetter)
                        .addContent("(")
                        .addContent(pageSize)
                        .addContentLine(");"));

    }

    private void generateCompletions(TypeName generatedType, ClassModel.Builder classModel, TypeInfo type) {
        List<TypedElementInfo> elements = getElementsWithAnnotation(type, MCP_COMPLETION);
        if (elements.isEmpty()) {
            return;
        }

        classModel.addImport(MCP_COMPLETION_TYPE);

        for (TypedElementInfo element : elements) {
            TypeName innerTypeName = createClassName(generatedType, element, "__Completion");
            Annotation mcpCompletion = element.annotation(MCP_COMPLETION);
            String reference = mcpCompletion.value().orElse("");
            EnumValue referenceType = (EnumValue) mcpCompletion.objectValue("type").orElse(null);

            components.get(McpKind.COMPLETION).add(innerTypeName);
            classModel.addInnerClass(clazz -> clazz
                    .name(innerTypeName.className())
                    .addInterface(MCP_COMPLETION_INTERFACE)
                    .accessModifier(AccessModifier.PRIVATE)
                    .addMethod(method -> addCompletionReferenceMethod(method, reference))
                    .addMethod(method -> addCompletionReferenceTypeMethod(method, referenceType))
                    .addMethod(method -> addCompletionMethod(method, classModel, element)));
        }
    }

    private void addCompletionReferenceMethod(Method.Builder builder, String reference) {
        builder.name("reference")
                .addAnnotation(Annotations.OVERRIDE)
                .returnType(TypeNames.STRING)
                .addContentLine("return \"" + reference + "\";");
    }

    private void addCompletionReferenceTypeMethod(Method.Builder builder, EnumValue referenceType) {
        String enumValue = referenceType != null ? referenceType.name() : "PROMPT";
        builder.name("referenceType")
                .addAnnotation(Annotations.OVERRIDE)
                .returnType(McpTypes.MCP_COMPLETION_TYPE)
                .addContentLine("return McpCompletionType." + enumValue + ";");
    }

    private void addCompletionMethod(Method.Builder builder, ClassModel.Builder classModel, TypedElementInfo element) {
        List<String> parameters = new ArrayList<>();
        TypeName returnType = element.typeName();

        builder.name("completion")
                .returnType(returned -> returned.type(FUNCTION_REQUEST_COMPLETION_CONTENT))
                .addAnnotation(Annotations.OVERRIDE);
        builder.addContentLine("return request -> {");

        boolean featuresLocalVar = false;
        boolean parametersLocalVar = false;
        for (TypedElementInfo param : element.parameterArguments()) {
            if (MCP_REQUEST.equals(param.typeName())) {
                parameters.add("request");
                continue;
            }
            if (MCP_FEATURES.equals(param.typeName()) && !featuresLocalVar) {
                addFeaturesLocalVar(builder, classModel);
                parameters.add("features");
                featuresLocalVar = true;
                continue;
            }
            if (MCP_PARAMETERS.equals(param.typeName())) {
                parameters.add("parameters");
                addParametersLocalVar(builder, classModel);
                parametersLocalVar = true;
                continue;
            }
            if (param.typeName().equals(TypeNames.STRING)) {
                if (!parametersLocalVar) {
                    addParametersLocalVar(builder, classModel);
                    parametersLocalVar = true;
                }
                parameters.add(param.elementName());
                builder.addContent("var ")
                        .addContent(param.elementName())
                        .addContentLine(" = parameters.get(\"value\").asString().orElse(\"\");");
                continue;
            }
            throw new CodegenException(
                    String.format("Wrong parameter type for method: %s. Supported types are: %s, %s, or String.",
                                  param.elementName(), MCP_FEATURES, MCP_PARAMETERS));
        }

        String params = String.join(", ", parameters);
        if (ResolvedType.create(returnType).equals(STRING_LIST)) {
            classModel.addImport(MCP_COMPLETION_CONTENTS);
            builder.addContent("return ")
                    .addContent(MCP_COMPLETION_CONTENTS)
                    .addContent(".completion(delegate.")
                    .addContent(element.elementName())
                    .addContent("(")
                    .addContent(params)
                    .addContentLine(").toArray(new String[0]));")
                    .addContentLine("};");
            return;
        }
        builder.addContent("return delegate.")
                .addContent(element.elementName())
                .addContent("(")
                .addContent(params)
                .addContentLine(");")
                .addContentLine("};");
    }

    private void generateResources(TypeName generatedType, ClassModel.Builder classModel, TypeInfo type) {
        List<TypedElementInfo> elements = getElementsWithAnnotation(type, MCP_RESOURCE);
        if (elements.isEmpty()) {
            return;
        }

        for (TypedElementInfo element : elements) {
            TypeName innerTypeName = createClassName(generatedType, element, "__Resource");
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
            components.get(McpKind.RESOURCE).add(innerTypeName);

            classModel.addInnerClass(clazz -> clazz
                    .name(innerTypeName.className())
                    .addInterface(MCP_RESOURCE_INTERFACE)
                    .accessModifier(AccessModifier.PRIVATE)
                    .addMethod(method -> addResourceUriMethod(method, uri))
                    .addMethod(method -> addResourceNameMethod(method, element))
                    .addMethod(method -> addResourceDescriptionMethod(method, description))
                    .addMethod(method -> addResourceMethod(method, uri, classModel, element))
                    .addMethod(method -> addResourceMediaTypeMethod(method, mediaTypeContent)));
        }
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
                .addContentLine("return \"" + description + "\";");
    }

    private void addResourceUriMethod(Method.Builder builder, String uri) {
        builder.name("uri")
                .addAnnotation(Annotations.OVERRIDE)
                .returnType(TypeNames.STRING)
                .addContentLine("return \"" + uri + "\";");
    }

    private void addResourceMediaTypeMethod(Method.Builder builder, String mediaTypeContent) {
        builder.name("mediaType")
                .addAnnotation(Annotations.OVERRIDE)
                .returnType(HELIDON_MEDIA_TYPE)
                .addContent("return ")
                .addContent(HELIDON_MEDIA_TYPES)
                .addContentLine(".create(\"" + mediaTypeContent + "\");");
    }

    private void addResourceMethod(Method.Builder builder, String uri, ClassModel.Builder classModel, TypedElementInfo element) {
        List<String> parameters = new ArrayList<>();
        TypeName returnType = element.signature().type();

        builder.name("resource")
                .addAnnotation(Annotations.OVERRIDE)
                .returnType(returned -> returned.type(FUNCTION_REQUEST_LIST_RESOURCE_CONTENT));
        builder.addContentLine("return request -> {");

        for (TypedElementInfo parameter : element.parameterArguments()) {
            if (MCP_REQUEST.equals(parameter.typeName())) {
                parameters.add("request");
                continue;
            }
            if (MCP_FEATURES.equals(parameter.typeName())) {
                parameters.add("request.features()");
                continue;
            }
            if (MCP_LOGGER.equals(parameter.typeName())) {
                parameters.add("request.features().logger()");
                continue;
            }
            if (MCP_PROGRESS.equals(parameter.typeName())) {
                parameters.add("request.features().progress()");
                continue;
            }
            if (MCP_CANCELLATION.equals(parameter.typeName())) {
                parameters.add("request.features().cancellation()");
                continue;
            }
            if (isResourceTemplate(uri)) {
                if (MCP_PARAMETERS.equals(parameter.typeName())) {
                    parameters.add("request.parameters()");
                    continue;
                }
                if (TypeNames.STRING.equals(parameter.typeName())) {
                    parameters.add(parameter.elementName());
                    builder.addContent("String ")
                            .addContent(parameter.elementName())
                            .addContent(" = request.parameters().get(\"")
                            .addContent(parameter.elementName())
                            .addContentLine("\").asString().orElse(\"\");");
                }
            }
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
        builder.addContent("return delegate.")
                .addContent(element.elementName())
                .addContent("(")
                .addContent(params)
                .addContentLine(");")
                .addContentLine("};");
    }

    private void generatePrompts(TypeName generatedType, ClassModel.Builder classModel, TypeInfo type) {
        List<TypedElementInfo> elements = getElementsWithAnnotation(type, MCP_PROMPT);
        if (elements.isEmpty()) {
            return;
        }

        for (TypedElementInfo element : elements) {
            TypeName innerTypeName = createClassName(generatedType, element, "__Prompt");
            String description = element.annotation(MCP_PROMPT).value().orElse("");
            List<TypeName> prompts = components.get(McpKind.PROMPT);
            if (prompts.contains(innerTypeName)) {
                logger.log(System.Logger.Level.WARNING,
                           "Prompt '%s' already exists. Use @Mcp.Name or change the method name."
                                   .formatted(element.elementName()));
            }
            components.get(McpKind.PROMPT).add(innerTypeName);

            classModel.addInnerClass(clazz -> clazz
                    .name(innerTypeName.className())
                    .addInterface(MCP_PROMPT_INTERFACE)
                    .accessModifier(AccessModifier.PRIVATE)
                    .addMethod(method -> addPromptNameMethod(method, element))
                    .addMethod(method -> addPromptDescriptionMethod(method, description))
                    .addMethod(method -> addPromptArgumentsMethod(method, element))
                    .addMethod(method -> addPromptMethod(method, classModel, element)));
        }
    }

    private void addPromptNameMethod(Method.Builder builder, TypedElementInfo element) {
        String name = element.findAnnotation(MCP_NAME)
                .flatMap(Annotation::value)
                .orElse(element.elementName());
        builder.name("name")
                .addAnnotation(Annotations.OVERRIDE)
                .returnType(TypeNames.STRING)
                .addContentLine("return \"" + name + "\";");
    }

    private void addPromptDescriptionMethod(Method.Builder builder, String description) {
        builder.name("description")
                .addAnnotation(Annotations.OVERRIDE)
                .returnType(TypeNames.STRING)
                .addContentLine("return \"" + description + "\";");
    }

    private void addPromptMethod(Method.Builder builder, ClassModel.Builder classModel, TypedElementInfo element) {
        List<String> parameters = new ArrayList<>();
        TypeName returnType = element.signature().type();
        Optional<String> role = element.findAnnotation(MCP_ROLE)
                .flatMap(annotation -> annotation.value());

        builder.name("prompt")
                .returnType(returned -> returned.type(FUNCTION_REQUEST_LIST_PROMPT_CONTENT))
                .addAnnotation(Annotations.OVERRIDE);
        builder.addContentLine("return request -> {");

        boolean featuresLocalVar = false;
        boolean parametersLocalVar = false;
        for (TypedElementInfo param : element.parameterArguments()) {
            if (MCP_REQUEST.equals(param.typeName())) {
                parameters.add("request");
                continue;
            }
            if (MCP_FEATURES.equals(param.typeName()) && !featuresLocalVar) {
                addFeaturesLocalVar(builder, classModel);
                parameters.add("features");
                featuresLocalVar = true;
                continue;
            }
            if (MCP_LOGGER.equals(param.typeName())) {
                if (!featuresLocalVar) {
                    addFeaturesLocalVar(builder, classModel);
                    featuresLocalVar = true;
                }
                parameters.add("logger");
                classModel.addImport(MCP_LOGGER);
                builder.addContentLine("var logger = features.logger();");
                continue;
            }
            if (MCP_PROGRESS.equals(param.typeName())) {
                if (!featuresLocalVar) {
                    addFeaturesLocalVar(builder, classModel);
                    featuresLocalVar = true;
                }
                parameters.add("progress");
                classModel.addImport(MCP_PROGRESS);
                builder.addContentLine("var progress = features.progress();");
                continue;
            }
            if (MCP_CANCELLATION.equals(param.typeName())) {
                if (!featuresLocalVar) {
                    addFeaturesLocalVar(builder, classModel);
                    featuresLocalVar = true;
                }
                parameters.add("cancellation");
                classModel.addImport(MCP_CANCELLATION);
                builder.addContentLine("var cancellation = features.cancellation();");
                continue;
            }
            if (!parametersLocalVar) {
                addParametersLocalVar(builder, classModel);
                parametersLocalVar = true;
            }
            parameters.add(param.elementName());
            builder.addContent(param.typeName().classNameWithEnclosingNames())
                    .addContent(" ")
                    .addContent(param.elementName())
                    .addContent(" = parameters.get(\"")
                    .addContent(param.elementName())
                    .addContentLine("\").asString().orElse(\"\");");
        }

        String params = String.join(", ", parameters);
        if (returnType.equals(TypeNames.STRING)) {
            builder.addContent("return ")
                    .addContent(List.class)
                    .addContent(".of(")
                    .addContent(MCP_PROMPT_CONTENTS)
                    .addContent(".textContent(delegate.")
                    .addContent(element.elementName())
                    .addContent("(")
                    .addContent(params)
                    .addContent(")")
                    .addContent(", ")
                    .addContent(MCP_ROLE_ENUM)
                    .addContent(".")
                    .addContent(role.orElse("ASSISTANT"))
                    .addContentLine("));")
                    .decreaseContentPadding()
                    .addContentLine("};");
            return;
        }
        builder.addContent("return delegate.")
                .addContent(element.elementName())
                .addContent("(")
                .addContent(params)
                .addContentLine(");")
                .decreaseContentPadding()
                .addContentLine("};");
    }

    private void addPromptArgumentsMethod(Method.Builder builder, TypedElementInfo element) {
        List<String> promptArgs = new ArrayList<>();
        int index = 0;

        builder.name("arguments")
                .addAnnotation(Annotations.OVERRIDE)
                .returnType(LIST_MCP_PROMPT_ARGUMENT);

        for (TypedElementInfo param : element.parameterArguments()) {
            if (MCP_FEATURES.equals(param.typeName())) {
                continue;
            }
            String builderName = "builder" + index++;

            builder.addContent("var ")
                    .addContent(builderName)
                    .addContent(" = ")
                    .addContent(MCP_PROMPT_ARGUMENT)
                    .addContentLine(".builder();");
            builder.addContent(builderName)
                    .addContent(".name(\"")
                    .addContent(param.elementName())
                    .addContentLine("\");");
            builder.addContent(builderName)
                    .addContentLine(".required(true);");

            promptArgs.add(builderName + ".build()");

            if (param.hasAnnotation(MCP_DESCRIPTION)) {
                String description = param.annotation(MCP_DESCRIPTION).value().orElse("");
                builder.addContent(builderName)
                        .addContent(".description(\"")
                        .addContent(description)
                        .addContentLine("\");");
                continue;
            }
            builder.addContent(builderName)
                    .addContent(".description(\"")
                    .addContent(param.elementName())
                    .addContentLine("\");");
        }
        builder.addContent("return ")
                .addContent(List.class)
                .addContent(".of(")
                .addContent(String.join(", ", promptArgs))
                .addContent(");");
    }

    private void generateTools(TypeName generatedType, ClassModel.Builder classModel, TypeInfo type) {
        List<TypedElementInfo> elements = getElementsWithAnnotation(type, MCP_TOOL);
        if (elements.isEmpty()) {
            return;
        }

        for (TypedElementInfo element : elements) {
            TypeName innerTypeName = createClassName(generatedType, element, "__Tool");
            Annotation toolAnnotation = element.annotation(MCP_TOOL);
            String description = toolAnnotation.value().orElse("No description available.");
            components.get(McpKind.TOOL).add(innerTypeName);

            classModel.addInnerClass(clazz -> clazz
                    .name(innerTypeName.className())
                    .addInterface(MCP_TOOL_INTERFACE)
                    .accessModifier(AccessModifier.PRIVATE)
                    .addMethod(method -> addToolNameMethod(method, element))
                    .addMethod(method -> addToolDescriptionMethod(method, description))
                    .addMethod(method -> addToolSchemaMethod(method, element))
                    .addMethod(method -> addToolMethod(method, classModel, element))
                    .addMethod(method -> addToolAnnotationsMethod(method, toolAnnotation)));
        }
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

    private void addFeaturesLocalVar(Method.Builder builder, ClassModel.Builder classModel) {
        classModel.addImport(MCP_FEATURES);
        builder.addContentLine("McpFeatures features = request.features();");
    }

    private void addParametersLocalVar(Method.Builder builder, ClassModel.Builder classModel) {
        classModel.addImport(MCP_PARAMETERS);
        builder.addContentLine("McpParameters parameters = request.parameters();");
    }

    private void addToolMethod(Method.Builder builder, ClassModel.Builder classModel, TypedElementInfo element) {
        List<String> parameters = new ArrayList<>();
        TypeName returnType = element.signature().type();

        builder.name("tool")
                .returnType(returned -> returned.type(FUNCTION_REQUEST_LIST_TOOL_CONTENT))
                .addAnnotation(Annotations.OVERRIDE);
        builder.addContentLine("return request -> {");

        boolean featuresLocalVar = false;
        boolean parametersLocalVar = false;
        for (TypedElementInfo param : element.parameterArguments()) {
            if (MCP_REQUEST.equals(param.typeName())) {
                parameters.add("request");
                continue;
            }
            if (MCP_FEATURES.equals(param.typeName()) && !featuresLocalVar) {
                addFeaturesLocalVar(builder, classModel);
                parameters.add("features");
                featuresLocalVar = true;
                continue;
            }
            if (MCP_LOGGER.equals(param.typeName())) {
                if (!featuresLocalVar) {
                    addFeaturesLocalVar(builder, classModel);
                    featuresLocalVar = true;
                }
                parameters.add("logger");
                builder.addContentLine("var logger = features.logger();");
                continue;
            }
            if (MCP_PROGRESS.equals(param.typeName())) {
                if (!featuresLocalVar) {
                    addFeaturesLocalVar(builder, classModel);
                    featuresLocalVar = true;
                }
                parameters.add("progress");
                classModel.addImport(MCP_PROGRESS);
                builder.addContentLine("var progress = features.progress();");
                continue;
            }
            if (MCP_CANCELLATION.equals(param.typeName())) {
                if (!featuresLocalVar) {
                    addFeaturesLocalVar(builder, classModel);
                    featuresLocalVar = true;
                }
                parameters.add("cancellation");
                classModel.addImport(MCP_CANCELLATION);
                builder.addContentLine("var cancellation = features.cancellation();");
                continue;
            }
            if (TypeNames.STRING.equals(param.typeName())) {
                if (!parametersLocalVar) {
                    addParametersLocalVar(builder, classModel);
                    parametersLocalVar = true;
                }
                parameters.add(param.elementName());
                builder.addContent("var ")
                        .addContent(param.elementName())
                        .addContent(" = parameters.get(\"")
                        .addContent(param.elementName())
                        .addContentLine("\").asString().orElse(\"\");");
                continue;
            }
            if (isBoolean(param.typeName())) {
                if (!parametersLocalVar) {
                    addParametersLocalVar(builder, classModel);
                    parametersLocalVar = true;
                }
                parameters.add(param.elementName());
                builder.addContent("boolean ")
                        .addContent(param.elementName())
                        .addContent(" = parameters.get(\"")
                        .addContent(param.elementName())
                        .addContentLine("\").asBoolean().orElse(false);");
                continue;
            }
            if (isNumber(param.typeName())) {
                if (!parametersLocalVar) {
                    addParametersLocalVar(builder, classModel);
                    parametersLocalVar = true;
                }
                parameters.add(param.elementName());
                builder.addContent("var ")
                        .addContent(param.elementName())
                        .addContent(" = parameters.get(\"")
                        .addContent(param.elementName())
                        .addContent("\").as")
                        .addContent(param.typeName().className())
                        .addContentLine("().orElse(null);");
                continue;
            }
            if (!parametersLocalVar) {
                addParametersLocalVar(builder, classModel);
                parametersLocalVar = true;
            }
            parameters.add(param.elementName());
            builder.addContent(param.typeName().classNameWithEnclosingNames())
                    .addContent(" ")
                    .addContent(param.elementName())
                    .addContent(" = parameters.get(\"")
                    .addContent(param.elementName())
                    .addContent("\").as(")
                    .addContent(param.typeName())
                    .addContentLine(".class).orElse(null);");
        }

        String params = String.join(", ", parameters);
        if (returnType.equals(TypeNames.STRING)) {
            classModel.addImport(MCP_TOOL_CONTENTS);
            builder.addContent("return ")
                    .addContent(List.class)
                    .addContent(".of(")
                    .addContent(MCP_TOOL_CONTENTS)
                    .addContent(".textContent(delegate.")
                    .addContent(element.elementName())
                    .addContent("(")
                    .addContent(params)
                    .addContentLine(")));")
                    .decreaseContentPadding()
                    .addContentLine("};");
            return;
        }
        builder.addContent("return delegate.")
                .addContent(element.elementName())
                .addContent("(")
                .addContent(params)
                .addContentLine(");")
                .decreaseContentPadding()
                .addContentLine("};");
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
                .addContentLine("return \"" + description + "\";");
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

    private boolean isBoolean(TypeName type) {
        return TypeNames.PRIMITIVE_BOOLEAN.equals(type) || TypeNames.BOXED_BOOLEAN.equals(type);
    }

    private boolean isNumber(TypeName type) {
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

    private TypeName createClassName(TypeName generatedType, TypedElementInfo element, String suffix) {
        return TypeName.builder()
                .className(element.findAnnotation(MCP_NAME)
                                   .flatMap(name -> name.value())
                                   .orElse(element.elementName()) + suffix)
                .addEnclosingName(generatedType.className())
                .packageName(generatedType.packageName())
                .build();
    }

    private List<TypedElementInfo> getElementsWithAnnotation(TypeInfo type, TypeName target) {
        return type.elementInfo().stream()
                .filter(element -> element.hasAnnotation(target))
                .collect(Collectors.toList());
    }

    private TypeName generatedTypeName(TypeName factoryTypeName, String suffix) {
        return TypeName.builder()
                .packageName(factoryTypeName.packageName())
                .className(factoryTypeName.classNameWithEnclosingNames().replace('.', '_') + "__" + suffix)
                .build();
    }

    private boolean isIgnoredSchemaElement(TypeName typeName) {
        return MCP_REQUEST.equals(typeName)
                || MCP_FEATURES.equals(typeName)
                || MCP_LOGGER.equals(typeName)
                || MCP_PROGRESS.equals(typeName)
                || MCP_CANCELLATION.equals(typeName);
    }

    private boolean isResourceTemplate(String uri) {
        return uri.contains("{") || uri.contains("}");
    }

    private void initializeComponents() {
        components.put(McpKind.TOOL, new LinkedList<>());
        components.put(McpKind.PROMPT, new LinkedList<>());
        components.put(McpKind.RESOURCE, new LinkedList<>());
        components.put(McpKind.COMPLETION, new LinkedList<>());
    }

    private enum McpKind {
        TOOL("addTool"),
        RESOURCE("addResource"),
        PROMPT("addPrompt"),
        COMPLETION("addCompletion");

        private final String methodName;

        McpKind(String methodName) {
            this.methodName = methodName;
        }
    }
}
