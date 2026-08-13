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
package io.helidon.extensions.mcp.server;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import io.helidon.json.JsonObject;

/**
 * MCP Sampling feature.
 */
public final class McpSampling extends McpFeature {
    static final int DEFAULT_MAX_TOOL_ITERATIONS = 10;

    private static final System.Logger LOGGER = System.getLogger(McpSampling.class.getName());

    private final McpFeatures features;
    private final boolean enabled;
    private final boolean enabledContext;
    private final boolean enabledTools;
    private final int maxToolIterations;
    private final List<McpTool> registeredTools;

    McpSampling(McpSession session, McpTransport transport, McpFeatures features) {
        super(session, transport);
        this.features = features;
        this.enabled = session.capabilities().contains(McpCapability.SAMPLING);
        this.enabledContext = session.capabilities().contains(McpCapability.SAMPLING_CONTEXT);
        this.enabledTools = session.capabilities().contains(McpCapability.SAMPLING_TOOLS);
        McpServerConfig config = session.context()
                .get(McpServerConfigBlueprint.class, McpServerConfig.class)
                .orElseThrow(() -> new McpInternalException("MCP server configuration not found"));
        this.maxToolIterations = config.maxSamplingToolIterations();
        this.registeredTools = config.tools();
    }

    /**
     * Whether the connected client supports sampling feature.
     *
     * @return {@code true} if the connected client supports sampling feature,
     * {@code false} otherwise.
     */
    public boolean enabled() {
        return enabled;
    }

    /**
     * Whether the connected client supports sampling context inclusion.
     *
     * @return {@code true} if the connected client supports
     * sampling context inclusion, {@code false} otherwise.
     */
    public boolean enabledContext() {
        return enabledContext;
    }

    /**
     * Whether the connected client supports sampling with tools.
     *
     * @return {@code true} if the connected client supports sampling with tools,
     * {@code false} otherwise
     */
    public boolean enabledTools() {
        return enabledTools;
    }

    /**
     * Send the provided sampling request to the client and return its final response. If a response requests tools,
     * each matching selected server tool is invoked and its result is submitted automatically before sampling continues.
     *
     * @param request sampling request
     * @return sampling response
     * @throws io.helidon.extensions.mcp.server.McpSamplingException when an error occurs
     */
    public McpSamplingResponse request(Consumer<McpSamplingRequest.Builder> request) throws McpSamplingException {
        var builder = McpSamplingRequest.builder();
        request.accept(builder);
        return request(builder.build());
    }

    /**
     * Send the provided sampling request to the client and return its final response. If a response requests tools,
     * each matching selected server tool is invoked and its result is submitted automatically before sampling continues.
     *
     * @param request sampling request
     * @return sampling response
     * @throws io.helidon.extensions.mcp.server.McpSamplingException when an error occurs
     */
    public McpSamplingResponse request(McpSamplingRequest request) throws McpSamplingException {
        if (!enabled) {
            throw new McpSamplingException("Sampling feature is not supported by client");
        }
        if (request.usesContext() && !enabledContext) {
            throw new McpSamplingException("Sampling context is not supported by client");
        }
        if (request.usesTool() && !enabledTools) {
            throw new McpSamplingException("Sampling tools are not supported by client");
        }

        Map<String, McpTool> tools = samplingTools(request.tools());
        List<McpTool> toolDefinitions = List.copyOf(tools.values());
        Set<String> toolUseIds = samplingToolUseIds(request.messages());
        McpSamplingRequest currentRequest = request;
        int toolIterations = 0;
        int toolExecutions = 0;

        while (true) {
            checkRequestActive();
            McpSamplingResponse response = requestOnce(currentRequest, toolDefinitions);
            checkRequestActive();
            List<McpSamplingToolUseContent> toolUses = response.message().contents().stream()
                    .filter(McpSamplingToolUseContent.class::isInstance)
                    .map(McpSamplingToolUseContent.class::cast)
                    .toList();
            if (toolUses.isEmpty()) {
                if (response.rawStopReason().filter(McpStopReason.TOOL_USE.text()::equals).isPresent()) {
                    throw new McpSamplingException("Sampling response stopped for tool use without tool use content");
                }
                if (currentRequest.toolChoice().filter(choice -> choice == McpToolChoice.REQUIRED).isPresent()) {
                    throw new McpSamplingException("Sampling response did not use a required tool");
                }
                return response;
            }
            if (!enabledTools) {
                throw new McpSamplingException("Sampling tools are not supported by client");
            }
            if (toolIterations == maxToolIterations) {
                throw new McpSamplingException("Sampling tool iteration limit reached");
            }
            if (currentRequest.toolChoice().filter(choice -> choice == McpToolChoice.NONE).isPresent()) {
                throw new McpSamplingException("Sampling client returned a tool use when tool choice is none");
            }
            if (toolUses.size() > maxToolIterations - toolExecutions) {
                throw new McpSamplingException("Sampling tool execution limit reached");
            }
            for (McpSamplingToolUseContent toolUse : toolUses) {
                if (!toolUseIds.add(toolUse.id())) {
                    throw new McpSamplingException("Sampling tool use identifier was reused: " + toolUse.id());
                }
            }
            toolExecutions += toolUses.size();

            McpSamplingMessage.Builder results = McpSamplingMessage.builder().role(McpRole.USER);
            for (McpSamplingToolUseContent toolUse : toolUses) {
                checkRequestActive();
                results.addContent(McpSamplingToolResultContent.builder()
                                           .toolUseId(toolUse.id())
                                           .result(invokeTool(tools, toolUse))
                                           .build());
            }

            McpSamplingRequest.Builder nextRequest = McpSamplingRequest.builder(currentRequest)
                    .addMessage(response.message())
                    .addMessage(results.build());
            toolIterations++;
            if (toolIterations == maxToolIterations || toolExecutions == maxToolIterations) {
                nextRequest.toolChoice(McpToolChoice.NONE);
            } else if (currentRequest.toolChoice()
                    .filter(choice -> choice == McpToolChoice.REQUIRED)
                    .isPresent()) {
                nextRequest.toolChoice(McpToolChoice.AUTO);
            }
            currentRequest = nextRequest.build();
        }
    }

    private void checkRequestActive() {
        if (session().state() == McpSession.State.DISCONNECTED) {
            throw new McpInternalException("Session disconnected");
        }
        if (features.cancellation().result().isRequested()) {
            throw new McpSamplingException("Sampling request cancelled");
        }
    }

    private McpSamplingResponse requestOnce(McpSamplingRequest request, List<McpTool> tools) {
        long id = session().jsonRpcId();
        JsonObject payload = session().serializer().createSamplingRequest(id, request, tools);
        session().prepareResponse(id);
        try {
            transport().send(payload);
            JsonObject response = session().pollResponse(id, request.timeout());
            return session().serializer().createSamplingResponse(response);
        } finally {
            session().discardResponse(id);
        }
    }

    private Map<String, McpTool> samplingTools(List<String> toolNames) {
        Map<String, McpTool> tools = new LinkedHashMap<>();
        if (toolNames.isEmpty()) {
            return tools;
        }

        Map<String, McpTool> registeredToolsByName = new LinkedHashMap<>();
        Set<String> duplicateRegisteredToolNames = new HashSet<>();
        for (McpTool registeredTool : registeredTools) {
            String registeredToolName = registeredTool.name();
            McpTool existingTool = registeredToolsByName.putIfAbsent(registeredToolName, registeredTool);
            if (existingTool != null) {
                duplicateRegisteredToolNames.add(registeredToolName);
            }
        }

        for (String toolName : toolNames) {
            McpTool tool = registeredToolsByName.get(toolName);
            if (tool == null) {
                throw new McpSamplingException("Sampling tool is not registered: " + toolName);
            }
            if (duplicateRegisteredToolNames.contains(toolName)) {
                throw new McpSamplingException("Registered sampling tool names must be unique: " + toolName);
            }
            McpTool duplicate = tools.putIfAbsent(toolName, tool);
            if (duplicate != null) {
                throw new McpSamplingException("Sampling tool names must be unique: " + toolName);
            }
        }
        return tools;
    }

    private Set<String> samplingToolUseIds(List<McpSamplingMessage> messages) {
        Set<String> toolUseIds = new HashSet<>();
        messages.stream()
                .flatMap(message -> message.contents().stream())
                .filter(McpSamplingToolUseContent.class::isInstance)
                .map(McpSamplingToolUseContent.class::cast)
                .map(McpSamplingToolUseContent::id)
                .forEach(id -> {
                    if (!toolUseIds.add(id)) {
                        throw new McpSamplingException("Sampling tool use identifier was reused: " + id);
                    }
                });
        return toolUseIds;
    }

    private McpToolResult invokeTool(Map<String, McpTool> tools, McpSamplingToolUseContent toolUse) {
        McpTool tool = tools.get(toolUse.name());
        if (tool == null) {
            return createToolErrorResult("Tool with name " + toolUse.name() + " is not available");
        }

        JsonObject.Builder parametersBuilder = JsonObject.builder()
                .set("name", toolUse.name())
                .set("arguments", toolUse.input().value());
        toolUse.metadata().ifPresent(metadata -> parametersBuilder.set("_meta", metadata.value()));
        McpParameters parameters = new McpParameters(parametersBuilder.build());
        McpRequest request = McpRequest.builder()
                .parameters(parameters)
                .meta(parameters.get("_meta"))
                .features(features)
                .protocolVersion(session().protocolVersion().text())
                .sessionContext(session().context())
                .requestContext(features.requestContext())
                .build();
        try {
            McpToolResult result = tool.tool(new McpToolRequestImpl(request));
            if (result == null) {
                return createToolErrorResult("Tool with name " + toolUse.name() + " returned no result");
            }
            return result;
        } catch (RuntimeException e) {
            LOGGER.log(System.Logger.Level.TRACE, "Sampling tool execution failed: "
                            + toolUse.name(), e);
            return createToolErrorResult("Tool with name " + toolUse.name() + " failed");
        }
    }

    private McpToolResult createToolErrorResult(String message) {
        return McpToolResult.builder()
                .addTextContent(message)
                .error(true)
                .build();
    }
}
