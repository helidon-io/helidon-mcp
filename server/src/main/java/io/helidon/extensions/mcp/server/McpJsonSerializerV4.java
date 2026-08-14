/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
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

import java.net.URI;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.json.JsonArray;
import io.helidon.json.JsonObject;
import io.helidon.json.JsonString;
import io.helidon.json.JsonValue;

/**
 * JSON serializer for {@code 2025-11-25} MCP specification.
 */
class McpJsonSerializerV4 extends McpJsonSerializerV3 {

    @Override
    public JsonObject.Builder createJsonInitializeResponse(Set<McpCapability> capabilities, McpServerConfig config) {
        return super.createJsonInitializeResponse(capabilities, config)
                .set("protocolVersion", McpProtocolVersion.VERSION_2025_11_25.text());
    }

    @Override
    public JsonObject.Builder serverInfo(McpServerConfig config) {
        JsonObject.Builder builder = super.serverInfo(config);
        config.websiteUrl().ifPresent(websiteUrl -> builder.set("websiteUrl", websiteUrl));
        return builder;
    }

    @Override
    public JsonObject.Builder toJson(McpSamplingRequest request, List<McpTool> tools) {
        validateToolMessages(request.messages());
        JsonObject.Builder params = super.toJson(request, tools);

        if (!tools.isEmpty()) {
            List<JsonValue> serializedTools = tools.stream()
                    .map(this::toJson)
                    .map(JsonObject.Builder::build)
                    .map(JsonValue.class::cast)
                    .toList();
            params.setValues("tools", serializedTools);
        }
        request.toolChoice().ifPresent(choice -> params.set("toolChoice", JsonObject.builder()
                .set("mode", choice.text())
                .build()));
        return params;
    }

    @Override
    public JsonObject.Builder toJson(McpSamplingMessage message) {
        List<JsonValue> contents = message.contents().stream()
                .map(this::toJson)
                .map(JsonObject.Builder::build)
                .map(JsonValue.class::cast)
                .toList();
        JsonObject.Builder builder = JsonObject.builder()
                .set("role", message.role().text());
        if (contents.size() == 1) {
            builder.set("content", contents.getFirst());
        } else {
            builder.setValues("content", contents);
        }
        message.metadata()
                .ifPresent(metadata -> builder.set("_meta", parameterObject(metadata, "Sampling message metadata")));
        return builder;
    }

    @Override
    public JsonObject.Builder toJson(McpSamplingContent content) {
        JsonObject.Builder builder;
        if (content instanceof McpSamplingToolUseContent toolUse) {
            builder = toJson(toolUse);
        } else if (content instanceof McpSamplingToolResultContent toolResult) {
            builder = toJson(toolResult);
        } else {
            builder = super.toJson(content);
        }
        content.metadata()
                .ifPresent(metadata -> builder.set("_meta", parameterObject(metadata, "Sampling content metadata")));
        if (content instanceof McpSamplingAnnotatedContent annotated) {
            annotated.annotations().ifPresent(annotations -> builder.set("annotations", toJson(annotations)));
        }
        return builder;
    }

    @Override
    public Optional<JsonObject.Builder> toJson(McpContent content) {
        Optional<JsonObject.Builder> result = super.toJson(content);
        if (content instanceof McpToolResourceLinkContent link) {
            result.ifPresent(builder -> {
                if (!link.icons().isEmpty()) {
                    builder.setValues("icons", link.icons().stream().map(this::toJson).toList());
                }
            });
        }
        return result;
    }

    @Override
    public McpSamplingResponse createSamplingResponse(JsonObject object) throws McpSamplingException {
        McpSamplingResponse response = super.createSamplingResponse(object);
        validateSamplingResponse(response.message());
        return response;
    }

    @Override
    List<McpSamplingContent> parseContents(JsonValue content) {
        if (content instanceof JsonArray array) {
            return array.values().stream()
                    .map(JsonValue::asObject)
                    .map(this::parseContent)
                    .toList();
        }
        return super.parseContents(content);
    }

    @Override
    McpSamplingContent parseContent(JsonObject object) {
        McpSamplingContentType type = object.stringValue("type")
                .map(value -> value.toUpperCase(Locale.ROOT))
                .map(McpSamplingContentType::valueOf)
                .orElseThrow();
        return switch (type) {
            case TOOL_USE -> parseToolUseContent(object);
            case TOOL_RESULT -> parseToolResultContent(object);
            case TEXT, IMAGE, AUDIO -> super.parseContent(object);
        };
    }

    private JsonObject.Builder toJson(McpSamplingToolUseContent content) {
        return JsonObject.builder()
                .set("type", content.type().text())
                .set("id", content.id())
                .set("name", content.name())
                .set("input", parameterObject(content.input(), "Sampling tool input"));
    }

    private JsonObject.Builder toJson(McpSamplingToolResultContent content) {
        McpToolResult result = content.result();
        List<JsonValue> contents = new ArrayList<>();
        for (McpToolContent resultContent : McpToolSupport.aggregateContent(result)) {
            toJson(resultContent).map(JsonObject.Builder::build).ifPresent(contents::add);
        }
        JsonObject.Builder builder = JsonObject.builder()
                .set("type", content.type().text())
                .set("toolUseId", content.toolUseId())
                .setValues("content", contents)
                .set("isError", result.error());
        result.structuredContent()
                .map(McpJsonBinding::serializeObject)
                .ifPresent(structured -> builder.set("structuredContent", structured));
        return builder;
    }

    private McpSamplingToolUseContent parseToolUseContent(JsonObject object) {
        JsonObject input = object.objectValue("input").orElseThrow();
        McpSamplingToolUseContent.Builder builder = McpSamplingToolUseContent.builder()
                .id(object.stringValue("id").orElseThrow())
                .name(object.stringValue("name").orElseThrow())
                .input(new McpParameters(input));
        object.objectValue("_meta")
                .map(McpParameters::new)
                .ifPresent(builder::metadata);
        return builder.build();
    }

    private McpSamplingToolResultContent parseToolResultContent(JsonObject object) {
        McpToolResult.Builder resultBuilder = McpToolResult.builder()
                .error(object.booleanValue("isError").orElse(false));
        for (JsonValue value : object.arrayValue("content").orElseThrow().values()) {
            McpToolContent content = parseToolResultBlock(value.asObject());
            if (content instanceof McpToolTextContent text) {
                resultBuilder.addTextContent(text);
            } else if (content instanceof McpToolImageContent image) {
                resultBuilder.addImageContent(image);
            } else if (content instanceof McpToolAudioContent audio) {
                resultBuilder.addAudioContent(audio);
            } else if (content instanceof McpToolTextResourceContent textResource) {
                resultBuilder.addTextResourceContent(textResource);
            } else if (content instanceof McpToolBinaryResourceContent binaryResource) {
                resultBuilder.addBinaryResourceContent(binaryResource);
            } else if (content instanceof McpToolResourceLinkContent resourceLink) {
                resultBuilder.addResourceLinkContent(resourceLink);
            } else {
                throw new McpSamplingException("Unsupported sampling tool result content type");
            }
        }
        object.objectValue("structuredContent")
                .map(content -> McpJsonBinding.deserialize(content, Map.class))
                .ifPresent(resultBuilder::structuredContent);
        McpSamplingToolResultContent.Builder builder = McpSamplingToolResultContent.builder()
                .toolUseId(object.stringValue("toolUseId").orElseThrow())
                .result(resultBuilder.build());
        object.objectValue("_meta")
                .map(McpParameters::new)
                .ifPresent(builder::metadata);
        return builder.build();
    }

    private McpToolContent parseToolResultBlock(JsonObject content) {
        return switch (content.stringValue("type").orElseThrow()) {
            case "text" -> {
                McpToolTextContent.Builder builder = McpToolTextContent.builder()
                        .text(content.stringValue("text").orElseThrow());
                parseAnnotations(content).ifPresent(builder::annotations);
                content.objectValue("_meta").map(McpParameters::new).ifPresent(builder::metadata);
                yield builder.build();
            }
            case "image" -> {
                McpToolImageContent.Builder builder = McpToolImageContent.builder()
                        .data(content.stringValue("data")
                                      .map(value -> Base64.getDecoder().decode(value))
                                      .orElseThrow())
                        .mediaType(MediaTypes.create(content.stringValue("mimeType").orElseThrow()));
                parseAnnotations(content).ifPresent(builder::annotations);
                content.objectValue("_meta").map(McpParameters::new).ifPresent(builder::metadata);
                yield builder.build();
            }
            case "audio" -> {
                McpToolAudioContent.Builder builder = McpToolAudioContent.builder()
                        .data(content.stringValue("data")
                                      .map(value -> Base64.getDecoder().decode(value))
                                      .orElseThrow())
                        .mediaType(MediaTypes.create(content.stringValue("mimeType").orElseThrow()));
                parseAnnotations(content).ifPresent(builder::annotations);
                content.objectValue("_meta").map(McpParameters::new).ifPresent(builder::metadata);
                yield builder.build();
            }
            case "resource_link" -> parseToolResourceLink(content);
            case "resource" -> parseToolEmbeddedResource(content);
            default -> throw new McpSamplingException("Unsupported sampling tool result content type");
        };
    }

    private McpToolResourceLinkContent parseToolResourceLink(JsonObject content) {
        McpToolResourceLinkContent.Builder builder = McpToolResourceLinkContent.builder()
                .name(content.stringValue("name").orElseThrow())
                .uri(content.stringValue("uri").orElseThrow());
        content.stringValue("title").ifPresent(builder::title);
        content.stringValue("description").ifPresent(builder::description);
        content.stringValue("mimeType").map(MediaTypes::create).ifPresent(builder::mediaType);
        content.longValue("size").ifPresent(builder::size);
        content.arrayValue("icons").ifPresent(icons -> icons.values().stream()
                .map(JsonValue::asObject)
                .map(this::parseIcon)
                .forEach(builder::addIcon));
        parseAnnotations(content).ifPresent(builder::annotations);
        content.objectValue("_meta").map(McpParameters::new).ifPresent(builder::metadata);
        return builder.build();
    }

    private McpToolContent parseToolEmbeddedResource(JsonObject content) {
        JsonObject resource = content.objectValue("resource").orElseThrow();
        URI uri = URI.create(resource.stringValue("uri").orElseThrow());
        if (resource.stringValue("text").isPresent()) {
            McpToolTextResourceContent.Builder builder = McpToolTextResourceContent.builder()
                    .uri(uri)
                    .mediaType(resource.stringValue("mimeType")
                                       .map(MediaTypes::create)
                                       .orElse(MediaTypes.TEXT_PLAIN))
                    .text(resource.stringValue("text").orElseThrow());
            parseAnnotations(content).ifPresent(builder::annotations);
            content.objectValue("_meta").map(McpParameters::new).ifPresent(builder::metadata);
            return builder.build();
        }
        McpToolBinaryResourceContent.Builder builder = McpToolBinaryResourceContent.builder()
                .uri(uri)
                .mediaType(resource.stringValue("mimeType")
                                   .map(MediaTypes::create)
                                   .orElseGet(() -> MediaTypes.create("application/octet-stream")))
                .data(resource.stringValue("blob")
                              .map(value -> Base64.getDecoder().decode(value))
                              .orElseThrow());
        parseAnnotations(content).ifPresent(builder::annotations);
        content.objectValue("_meta").map(McpParameters::new).ifPresent(builder::metadata);
        return builder.build();
    }

    private McpIcon parseIcon(JsonObject icon) {
        McpIcon.Builder builder = McpIcon.builder().src(icon.stringValue("src").orElseThrow());
        icon.stringValue("mimeType").map(MediaTypes::create).ifPresent(builder::mediaType);
        icon.arrayValue("sizes").ifPresent(sizes -> sizes.values().stream()
                .map(JsonValue::asString)
                .map(JsonString::value)
                .forEach(builder::addSize));
        icon.stringValue("theme")
                .map(value -> value.toUpperCase(Locale.ROOT))
                .map(McpIconTheme::valueOf)
                .ifPresent(builder::theme);
        return builder.build();
    }

    private JsonObject toJson(McpIcon icon) {
        JsonObject.Builder builder = JsonObject.builder().set("src", icon.src());
        icon.mediaType().ifPresent(mediaType -> builder.set("mimeType", mediaType.text()));
        if (!icon.sizes().isEmpty()) {
            builder.setStrings("sizes", icon.sizes());
        }
        icon.theme().ifPresent(theme -> builder.set("theme", theme.text()));
        return builder.build();
    }

    private void validateSamplingResponse(McpSamplingMessage message) {
        List<McpSamplingToolUseContent> toolUses = message.contents().stream()
                .filter(McpSamplingToolUseContent.class::isInstance)
                .map(McpSamplingToolUseContent.class::cast)
                .toList();
        if (!toolUses.isEmpty() && message.role() != McpRole.ASSISTANT) {
            throw new McpSamplingException("Sampling response must have an assistant message role");
        }
        if (message.contents().stream().anyMatch(McpSamplingToolResultContent.class::isInstance)) {
            throw new McpSamplingException("Sampling response must not contain tool result content");
        }
        if (toolUses.stream().map(McpSamplingToolUseContent::id).distinct().count() != toolUses.size()) {
            throw new McpSamplingException("Sampling tool use identifiers must be unique within a message");
        }
    }

    private void validateToolMessages(List<McpSamplingMessage> messages) {
        Set<String> expectedResults = null;
        for (McpSamplingMessage message : messages) {
            validateContentRoles(message);
            List<McpSamplingToolUseContent> toolUses = message.contents().stream()
                    .filter(McpSamplingToolUseContent.class::isInstance)
                    .map(McpSamplingToolUseContent.class::cast)
                    .toList();
            List<McpSamplingToolResultContent> toolResults = message.contents().stream()
                    .filter(McpSamplingToolResultContent.class::isInstance)
                    .map(McpSamplingToolResultContent.class::cast)
                    .toList();

            if (expectedResults != null) {
                Set<String> resultIds = new HashSet<>();
                toolResults.stream()
                        .map(McpSamplingToolResultContent::toolUseId)
                        .forEach(resultIds::add);
                if (toolResults.size() != message.contents().size()
                        || resultIds.size() != toolResults.size()
                        || !expectedResults.equals(resultIds)) {
                    throw new McpSamplingException("Sampling tool uses must be followed by matching tool results");
                }
                expectedResults = null;
                continue;
            }
            if (!toolResults.isEmpty()) {
                throw new McpSamplingException("Sampling tool result does not match a preceding tool use");
            }
            if (!toolUses.isEmpty()) {
                Set<String> toolUseIds = new HashSet<>();
                toolUses.stream()
                        .map(McpSamplingToolUseContent::id)
                        .forEach(toolUseIds::add);
                if (toolUseIds.size() != toolUses.size()) {
                    throw new McpSamplingException("Sampling tool use identifiers must be unique within a message");
                }
                expectedResults = toolUseIds;
            }
        }
        if (expectedResults != null) {
            throw new McpSamplingException("Sampling tool uses must be followed by matching tool results");
        }
    }

    private void validateContentRoles(McpSamplingMessage message) {
        List<McpSamplingToolUseContent> toolUses = message.contents().stream()
                .filter(McpSamplingToolUseContent.class::isInstance)
                .map(McpSamplingToolUseContent.class::cast)
                .toList();
        boolean usesTool = !toolUses.isEmpty();
        boolean hasToolResult = message.contents().stream().anyMatch(McpSamplingToolResultContent.class::isInstance);
        if (usesTool && message.role() != McpRole.ASSISTANT) {
            throw new McpSamplingException("Sampling tool use content must have an assistant message role");
        }
        if (hasToolResult && message.role() != McpRole.USER) {
            throw new McpSamplingException("Sampling tool result content must have a user message role");
        }
        if (hasToolResult
                && message.contents().stream().anyMatch(content -> !(content instanceof McpSamplingToolResultContent))) {
            throw new McpSamplingException("Sampling messages with tool results must contain only tool results");
        }
        if (toolUses.stream().map(McpSamplingToolUseContent::id).distinct().count() != toolUses.size()) {
            throw new McpSamplingException("Sampling tool use identifiers must be unique within a message");
        }
    }
}
