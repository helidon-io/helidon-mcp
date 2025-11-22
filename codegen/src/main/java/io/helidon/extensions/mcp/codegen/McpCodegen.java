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

import java.lang.System.Logger.Level;
import java.util.Collection;
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
import io.helidon.common.types.TypeInfo;
import io.helidon.common.types.TypeName;

import static io.helidon.extensions.mcp.codegen.McpCodegenUtil.generatedTypeName;
import static io.helidon.extensions.mcp.codegen.McpTypes.GLOBAL_SERVICE_REGISTRY;
import static io.helidon.extensions.mcp.codegen.McpTypes.HTTP_FEATURE;
import static io.helidon.extensions.mcp.codegen.McpTypes.HTTP_ROUTING_BUILDER;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_PATH;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_PROMPTS_PAGE_SIZE;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_RESOURCES_PAGE_SIZE;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_RESOURCE_TEMPLATES_PAGE_SIZE;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_SERVER;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_SERVER_CONFIG;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_TOOLS_PAGE_SIZE;
import static io.helidon.extensions.mcp.codegen.McpTypes.MCP_VERSION;
import static io.helidon.service.codegen.ServiceCodegenTypes.SERVICE_ANNOTATION_SINGLETON;

final class McpCodegen implements CodegenExtension {
    private static final TypeName GENERATOR = TypeName.create(McpCodegen.class);

    private final McpRecorder recorder;
    private final CodegenLogger logger;
    private final McpToolCodegen toolCodegen;
    private final McpPromptCodegen promptCodegen;
    private final McpResourceCodegen resourceCodegen;
    private final McpCompletionCodegen completionCodegen;

    McpCodegen(CodegenContext context) {
        logger = context.logger();
        recorder = new McpRecorder();
        toolCodegen = new McpToolCodegen(recorder);
        promptCodegen = new McpPromptCodegen(recorder);
        resourceCodegen = new McpResourceCodegen(recorder);
        completionCodegen = new McpCompletionCodegen(recorder);
    }

    @Override
    public void process(RoundContext roundContext) {
        logger.log(Level.TRACE, "Processing MCP codegen extension with context "
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
                .type(type.typeName())
                .name("delegate"));

        serverClassModel.addConstructor(constructor -> {
            constructor.accessModifier(AccessModifier.PUBLIC);
            constructor.addContentLine("try {")
                    .addContent("delegate = ")
                    .addContent(GLOBAL_SERVICE_REGISTRY)
                    .addContent(".registry().get(")
                    .addContent(type.typeName())
                    .addContentLine(".class);")
                    .decreaseContentPadding()
                    .addContentLine("} catch (Exception e) {")
                    .addContent("delegate = new ")
                    .addContent(type.typeName())
                    .addContentLine("();")
                    .addContentLine("}");
        });

        toolCodegen.generate(serverClassModel, type);
        promptCodegen.generate(serverClassModel, type);
        resourceCodegen.generate(serverClassModel, type);
        completionCodegen.generate(serverClassModel, type);

        serverClassModel.addMethod(method -> addRoutingMethod(method, type));
        roundCtx.addGeneratedType(generatedType, serverClassModel, mcpServerType, type.originatingElementValue());
        recorder.clear();
    }

    private void addRoutingMethod(Method.Builder method, TypeInfo type) {
        String defaultServerName = type.typeName().className() + " mcp server";
        String serverName = type.annotation(MCP_SERVER)
                .value()
                .orElse(defaultServerName);

        method.name("setup")
                .accessModifier(AccessModifier.PUBLIC)
                .addAnnotation(Annotations.OVERRIDE)
                .addParameter(rules -> rules.type(HTTP_ROUTING_BUILDER).name("routing"))
                .addContent(MCP_SERVER_CONFIG)
                .addContent(".Builder builder = ")
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

        recorder.tools().forEach(name -> registerMcpComponent(method, "addTool", name));
        recorder.prompts().forEach(name -> registerMcpComponent(method, "addPrompt", name));
        recorder.resources().forEach(name -> registerMcpComponent(method, "addResource", name));
        recorder.completions().forEach(name -> registerMcpComponent(method, "addCompletion", name));
        recorder.subscribers().forEach(name -> registerMcpComponent(method, "addResourceSubscriber", name));
        recorder.unsubscribers().forEach(name -> registerMcpComponent(method, "addResourceUnsubscriber", name));

        method.addContentLine("builder.build().setup(routing);");
    }

    private void registerMcpComponent(Method.Builder builder, String method, TypeName type) {
        builder.addContent("builder.")
                .addContent(method)
                .addContent("(new ")
                .addContent(type)
                .addContentLine("());");
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

}
