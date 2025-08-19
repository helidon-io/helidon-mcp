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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.helidon.codegen.CodegenContext;
import io.helidon.codegen.CodegenException;
import io.helidon.codegen.CodegenLogger;
import io.helidon.codegen.CodegenUtil;
import io.helidon.codegen.RoundContext;
import io.helidon.codegen.classmodel.ClassModel;
import io.helidon.codegen.classmodel.Field;
import io.helidon.codegen.classmodel.Method;
import io.helidon.codegen.spi.CodegenExtension;
import io.helidon.common.types.AccessModifier;
import io.helidon.common.types.Annotation;
import io.helidon.common.types.Annotations;
import io.helidon.common.types.ElementKind;
import io.helidon.common.types.TypeInfo;
import io.helidon.common.types.TypeName;
import io.helidon.common.types.TypeNames;
import io.helidon.common.types.TypedElementInfo;

import static io.helidon.extensions.mcp.codegen.McpJsonSchemaCodegen.addSchemaMethodBody;
import static io.helidon.extensions.mcp.codegen.McpJsonSchemaCodegen.getDescription;
import static io.helidon.extensions.mcp.codegen.McpTypes.FUNCTION_REQUEST_LIST_COMPLETION_CONTENT;
import static io.helidon.extensions.mcp.codegen.McpTypes.HTTP_FEATURE;
import static io.helidon.extensions.mcp.codegen.McpTypes.HTTP_ROUTING_BUILDER;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_COMPLETION;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_COMPLETION_CONTENTS;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_COMPLETION_INTERFACE;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_DESCRIPTION;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_FEATURES;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_LOGGER;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_NAME;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_PARAMETERS;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_PATH;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_PROGRESS;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_PROMPT;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_PROMPT_CONTENT;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_PROMPT_CONTENTS;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_PROMPT_INTERFACE;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_REQUEST;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_RESOURCE;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_RESOURCE_CONTENT;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_RESOURCE_CONTENTS;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_RESOURCE_INTERFACE;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_ROLE;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_ROLE_ENUM;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_SERVER;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_SERVER_CONFIG;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_TOOL;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_TOOL_CONTENT;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_TOOL_CONTENTS;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_TOOL_INTERFACE;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_VERSION;
import static io.helidon.service.codegen.ServiceCodegenTypes.SERVICE_ANNOTATION_SINGLETON;

final class McpCodegen implements CodegenExtension {
    private static final TypeName GENERATOR = TypeName.create(McpCodegen.class);
    private static final String STRING_FQN = String.class.getName();
    private final CodegenLogger logger;

    /**
     * Map of MCP component kind associated with their generated class name.
     */
    private final Map<McpKind, List<String>> components;

    McpCodegen(CodegenContext context) {
        logger = context.logger();
        components = new HashMap<>();
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

        serverClassModel.addField(Field.builder()
                                          .accessModifier(AccessModifier.PRIVATE)
                                          .isFinal(true)
                                          .name("delegate")
                                          .type(type.typeName())
                                          .addContent("new ")
                                          .addContent(type.typeName().className())
                                          .addContent("()")
                                          .build());

        generateTools(serverClassModel, type);
        generatePrompts(serverClassModel, type);
        generateResources(serverClassModel, type);
        generateCompletions(serverClassModel, type);

        serverClassModel.addMethod(method -> addRoutingMethod(method, type));
        roundCtx.addGeneratedType(generatedType, serverClassModel, mcpServerType, type.originatingElementValue());
    }

    private void addRoutingMethod(Method.Builder method, TypeInfo type) {
        method.name("setup")
                .accessModifier(AccessModifier.PUBLIC)
                .returnType(TypeNames.PRIMITIVE_VOID)
                .addAnnotation(Annotations.OVERRIDE)
                .addParameter(rules -> rules.type(HTTP_ROUTING_BUILDER).name("routing"));

        method.addContent("McpServerConfig.Builder builder =");
        method.addContent(MCP_SERVER_CONFIG);
        method.addContentLine(".builder();");

        String defaultServerName = type.typeName().className() + " mcp server";
        Annotation description = type.annotation(MCP_SERVER);
        method.addContentLine("builder.name(\"" + description.value().orElse(defaultServerName) + "\");");

        if (type.hasAnnotation(MCP_VERSION)) {
            Annotation version = type.annotation(MCP_VERSION);
            method.addContentLine("builder.version(\"" + version.value().orElse("0.0.1") + "\");");
        }

        if (type.hasAnnotation(MCP_PATH)) {
            Annotation path = type.annotation(MCP_PATH);
            method.addContentLine("builder.path(\"" + path.value().orElse("") + "\");");
        }

        for (Map.Entry<McpKind, List<String>> entry : components.entrySet()) {
            for (String value : entry.getValue()) {
                switch (entry.getKey()) {
                case TOOL -> method.addContentLine("builder.addTool(new " + value + "());");
                case RESOURCE -> method.addContentLine("builder.addResource(new " + value + "());");
                case PROMPT -> method.addContentLine("builder.addPrompt(new " + value + "());");
                case COMPLETION -> method.addContentLine("builder.addCompletion(new " + value + "());");
                default -> throw new CodegenException("Unknown mcp component: " + entry.getKey());
                }
            }
        }

        method.addContentLine("builder.build().setup(routing);");
        // Clear the components map as code generation if over for this server.
        initializeComponents();
    }

    private void generateCompletions(ClassModel.Builder classModel, TypeInfo type) {
        List<TypedElementInfo> elements = getElementsWithAnnotation(type, MCP_COMPLETION);
        if (elements.isEmpty()) {
            return;
        }

        for (TypedElementInfo element : elements) {
            String className = createClassName(element, "__Completion");
            String reference = element.annotation(MCP_COMPLETION).value().orElse("");

            components.get(McpKind.COMPLETION).add(className);
            classModel.addInnerClass(clazz -> clazz
                    .name(className)
                    .addInterface(MCP_COMPLETION_INTERFACE)
                    .accessModifier(AccessModifier.PRIVATE)
                    .addMethod(method -> addCompletionReferenceMethod(method, reference))
                    .addMethod(method -> addCompletionMethod(method, classModel, element)));
        }
    }

    private void addCompletionReferenceMethod(Method.Builder builder, String reference) {
        builder.name("reference")
                .addAnnotation(Annotations.OVERRIDE)
                .returnType(TypeNames.STRING)
                .addContentLine("return \"" + reference + "\";");
    }

    private void addCompletionMethod(Method.Builder builder, ClassModel.Builder classModel, TypedElementInfo element) {
        List<String> parameters = new ArrayList<>();
        String returnType = element.signature().type().toString();

        builder.name("completion")
                .returnType(returned -> returned.type(FUNCTION_REQUEST_LIST_COMPLETION_CONTENT))
                .addAnnotation(Annotations.OVERRIDE);
        builder.addContentLine("return request -> {");

        boolean featuresLocalVar = false;
        boolean parametersLocalVar = false;
        for (TypedElementInfo param : element.parameterArguments()) {
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
            if (STRING_FQN.equals(param.typeName().fqName())) {
                if (!parametersLocalVar) {
                    addParametersLocalVar(builder, classModel);
                    parametersLocalVar = true;
                }
                parameters.add(param.elementName());
                builder.addContent("String ")
                        .addContent(param.elementName())
                        .addContentLine(" = parameters.get(\"value\").asString().orElse(\"\");");
                continue;
            }
            throw new CodegenException(
                    String.format("Wrong parameter type for method: %s. Supported types are: %s, %s, or String.",
                                  param.elementName(), MCP_FEATURES, MCP_PARAMETERS));
        }

        String params = String.join(", ", parameters);
        if ("java.util.List<java.lang.String>".equals(returnType)) {
            classModel.addImport(MCP_COMPLETION_CONTENTS);
            builder.addContent("return McpCompletionContents.completion(delegate.")
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

    private void generateResources(ClassModel.Builder classModel, TypeInfo type) {
        List<TypedElementInfo> elements = getElementsWithAnnotation(type, MCP_RESOURCE);
        if (elements.isEmpty()) {
            return;
        }

        TypeName mediaType = TypeName.create("io.helidon.common.media.type.MediaType");

        classModel.addImport("io.helidon.common.media.type.MediaTypes");
        classModel.addImport(mediaType);

        for (TypedElementInfo element : elements) {
            String className = createClassName(element, "__Resource");
            String uri = element.findAnnotation(MCP_RESOURCE)
                    .flatMap(annotation -> annotation.stringValue("uri"))
                    .orElseThrow(() -> new CodegenException("Resource " + element.elementName() + " must have a URI."));
            String description = element.findAnnotation(MCP_RESOURCE)
                    .flatMap(annotation -> annotation.stringValue("description"))
                    .orElseThrow(() -> new CodegenException("Resource " + element.elementName() + " must have a description."));
            String mediaTypeContent = element.findAnnotation(MCP_RESOURCE)
                    .flatMap(annotation -> annotation.stringValue("mediaType"))
                    .orElseThrow(() -> new CodegenException("Resource " + element.elementName() + " must have a Media Type."));
            components.get(McpKind.RESOURCE).add(className);

            classModel.addInnerClass(clazz -> clazz
                    .name(className)
                    .addInterface(MCP_RESOURCE_INTERFACE)
                    .accessModifier(AccessModifier.PRIVATE)
                    .addMethod(method -> addResourceNameMethod(method, element))
                    .addMethod(method -> addResourceDescriptionMethod(method, description))
                    .addMethod(method -> addResourceUriMethod(method, uri))
                    .addMethod(method -> addResourceMediaTypeMethod(method, mediaType, mediaTypeContent))
                    .addMethod(method -> addResourceMethod(method, classModel, element)));
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

    private void addResourceMediaTypeMethod(Method.Builder builder, TypeName mediaType, String mediaTypeContent) {
        builder.name("mediaType")
                .addAnnotation(Annotations.OVERRIDE)
                .returnType(mediaType)
                .addContentLine("return MediaTypes.create(\"" + mediaTypeContent + "\");");
    }

    private void addResourceMethod(Method.Builder builder, ClassModel.Builder classModel, TypedElementInfo element) {
        List<String> parameters = new ArrayList<>();
        String returnType = element.signature().type().fqName();

        classModel.addImport(Function.class);
        classModel.addImport(TypeNames.LIST);
        classModel.addImport(MCP_FEATURES);
        builder.name("resource")
                .returnType(returned -> returned.type("Function<McpFeatures, List<" + MCP_RESOURCE_CONTENT + ">>"))
                .addAnnotation(Annotations.OVERRIDE);
        builder.addContentLine("return features -> {");

        for (TypedElementInfo parameter : element.parameterArguments()) {
            if (MCP_FEATURES.equals(parameter.typeName())) {
                parameters.add("features");
                continue;
            }
            if (MCP_LOGGER.equals(parameter.typeName())) {
                parameters.add("logger");
                classModel.addImport(MCP_LOGGER);
                builder.addContentLine("McpLogger logger = features.logger();");
                continue;
            }
            if (MCP_PROGRESS.equals(parameter.typeName())) {
                parameters.add("progress");
                classModel.addImport(MCP_PROGRESS);
                builder.addContentLine("McpProgress progress = features.progress();");
            }
        }
        String params = String.join(", ", parameters);
        if (STRING_FQN.equals(returnType)) {
            classModel.addImport(MCP_RESOURCE_CONTENTS);
            builder.addContent("return List.of(McpResourceContents.textContent(delegate.")
                    .addContent(element.elementName())
                    .addContent("(")
                    .addContent(params)
                    .addContentLine(")));")
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

    private void generatePrompts(ClassModel.Builder classModel, TypeInfo type) {
        List<TypedElementInfo> elements = getElementsWithAnnotation(type, MCP_PROMPT);
        if (elements.isEmpty()) {
            return;
        }

        classModel.addImport(Set.class)
                .addImport(TypeName.create("io.helidon.extensions.mcp.server.McpPromptArgument"));

        for (TypedElementInfo element : elements) {
            String className = createClassName(element, "__Prompt");
            String description = element.annotation(MCP_PROMPT).value().orElse("");
            List<String> prompts = components.get(McpKind.PROMPT);
            if (prompts.contains(className)) {
                logger.log(System.Logger.Level.WARNING,
                           "Prompt '%s' already exists. Use @Mcp.Name or change the method name."
                                   .formatted(element.elementName()));
            }
            components.get(McpKind.PROMPT).add(className);

            classModel.addInnerClass(clazz -> clazz
                    .name(className)
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
        String returnType = element.signature().type().fqName();
        Optional<String> role = element.findAnnotation(MCP_ROLE).flatMap(annotation -> annotation.value());

        classModel.addImport(Function.class);
        classModel.addImport(MCP_REQUEST);
        classModel.addImport(TypeNames.LIST);
        builder.name("prompt")
                .returnType(returned -> returned.type("Function<McpRequest, List<" + MCP_PROMPT_CONTENT + ">>"))
                .addAnnotation(Annotations.OVERRIDE);
        builder.addContentLine("return request -> {");

        boolean featuresLocalVar = false;
        boolean parametersLocalVar = false;
        for (TypedElementInfo param : element.parameterArguments()) {
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
                builder.addContentLine("McpLogger logger = features.logger();");
                continue;
            }
            if (MCP_PROGRESS.equals(param.typeName())) {
                if (!featuresLocalVar) {
                    addFeaturesLocalVar(builder, classModel);
                    featuresLocalVar = true;
                }
                parameters.add("progress");
                classModel.addImport(MCP_PROGRESS);
                builder.addContentLine("McpProgress progress = features.progress();");
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
        if (STRING_FQN.equals(returnType)) {
            classModel.addImport(MCP_ROLE_ENUM);
            classModel.addImport(MCP_PROMPT_CONTENTS);
            builder.addContent("return List.of(McpPromptContents.textContent(delegate.")
                    .addContent(element.elementName())
                    .addContent("(")
                    .addContent(params)
                    .addContent(")");
            role.ifPresentOrElse(it -> builder.addContentLine(", " + it + "));"),
                                 () -> builder.addContentLine(", McpRole.ASSISTANT));"));
            builder.addContentLine("};");
            return;
        }
        builder.addContent("return delegate.")
                .addContent(element.elementName())
                .addContent("(")
                .addContent(params)
                .addContentLine(");")
                .addContentLine("};");
    }

    private void addPromptArgumentsMethod(Method.Builder builder, TypedElementInfo element) {
        List<String> promptArgs = new ArrayList<>();
        int index = 0;

        builder.name("arguments")
                .addAnnotation(Annotations.OVERRIDE)
                .returnType(TypeName.create("Set<McpPromptArgument>"));

        for (TypedElementInfo param : element.parameterArguments()) {
            if (MCP_FEATURES.equals(param.typeName())) {
                continue;
            }
            String builderName = "builder" + index++;

            builder.addContent("var ")
                    .addContent(builderName)
                    .addContentLine(" = McpPromptArgument.builder();");
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
        builder.addContent("return Set.of(")
                .addContent(String.join(", ", promptArgs))
                .addContent(");");
    }

    private void generateTools(ClassModel.Builder classModel, TypeInfo type) {
        List<TypedElementInfo> elements = getElementsWithAnnotation(type, MCP_TOOL);
        if (elements.isEmpty()) {
            return;
        }

        for (TypedElementInfo element : elements) {
            String className = createClassName(element, "__Tool");
            String description = element.annotation(MCP_TOOL).value().orElse("No description available.");
            components.get(McpKind.TOOL).add(className);

            classModel.addInnerClass(clazz -> clazz
                    .name(className)
                    .addInterface(MCP_TOOL_INTERFACE)
                    .accessModifier(AccessModifier.PRIVATE)
                    .addMethod(method -> addToolNameMethod(method, element))
                    .addMethod(method -> addToolDescriptionMethod(method, description))
                    .addMethod(method -> addToolSchemaMethod(method, element))
                    .addMethod(method -> addToolMethod(method, classModel, element)));
        }
    }

    private void addToolSchemaMethod(Method.Builder builder, TypedElementInfo element) {
        Method.Builder method = builder.name("schema")
                .returnType(TypeNames.STRING)
                .addAnnotation(Annotations.OVERRIDE);

        List<TypedElementInfo> fields = new ArrayList<>();
        for (TypedElementInfo param : element.parameterArguments()) {
            if (MCP_FEATURES.equals(param.typeName())) {
                continue;
            }
            if (MCP_PROGRESS.equals(param.typeName())) {
                continue;
            }
            if (MCP_LOGGER.equals(param.typeName())) {
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
        String returnType = element.signature().type().fqName();

        classModel.addImport(Function.class);
        classModel.addImport(MCP_REQUEST);
        classModel.addImport(TypeNames.LIST);
        builder.name("tool")
                .returnType(returned -> returned.type("Function<McpRequest, List<" + MCP_TOOL_CONTENT + ">>"))
                .addAnnotation(Annotations.OVERRIDE);
        builder.addContentLine("return request -> {");

        boolean featuresLocalVar = false;
        boolean parametersLocalVar = false;
        for (TypedElementInfo param : element.parameterArguments()) {
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
                builder.addContentLine("McpLogger logger = features.logger();");
                continue;
            }
            if (MCP_PROGRESS.equals(param.typeName())) {
                if (!featuresLocalVar) {
                    addFeaturesLocalVar(builder, classModel);
                    featuresLocalVar = true;
                }
                parameters.add("progress");
                classModel.addImport(MCP_PROGRESS);
                builder.addContentLine("McpProgress progress = features.progress();");
                continue;
            }
            if (TypeNames.STRING.equals(param.typeName())) {
                if (!parametersLocalVar) {
                    addParametersLocalVar(builder, classModel);
                    parametersLocalVar = true;
                }
                parameters.add(param.elementName());
                builder.addContent("String ")
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
                builder.addContent(param.typeName().declaredName())
                        .addContent(param.elementName())
                        .addContent(" = parameters.get(\"")
                        .addContent(param.elementName())
                        .addContentLine("\").asBoolean().orElse(false);");
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
                    .addContent(param.typeName().classNameWithEnclosingNames())
                    .addContentLine(".class).orElse(null);");
        }

        String params = String.join(", ", parameters);
        if (STRING_FQN.equals(returnType)) {
            classModel.addImport(MCP_TOOL_CONTENTS);
            builder.addContent("return List.of(McpToolContents.textContent(delegate.")
                    .addContent(element.elementName())
                    .addContent("(")
                    .addContent(params)
                    .addContentLine(")));")
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

    private String createClassName(TypedElementInfo element, String suffix) {
        return element.findAnnotation(MCP_NAME)
                .flatMap(name -> name.value())
                .orElse(element.elementName()) + suffix;
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

    private void initializeComponents() {
        components.put(McpKind.TOOL, new LinkedList<>());
        components.put(McpKind.PROMPT, new LinkedList<>());
        components.put(McpKind.RESOURCE, new LinkedList<>());
        components.put(McpKind.COMPLETION, new LinkedList<>());
    }

    private enum McpKind {
        TOOL,
        RESOURCE,
        PROMPT,
        COMPLETION
    }
}
