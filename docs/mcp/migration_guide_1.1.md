# Migration Guide for Version 1.1.0

Helidon MCP `1.1.0` provides support for MCP specification `2025-06-18`. This document 
summarizes the key changes and explains how to migrate existing code to `1.1.0`.

## Overview of Changes

- When MCP components used to return a `List<Mcp*Content>`, they now return `Mcp*Result`.
- MCP component signatures based on `Function<McpRequest, List<Mcp*Content>>` are replaced with typed methods such as `Mcp*Result method(Mcp*Request request)`.
- Content is now created directly on result builders and no longer requires factory classes.
- Typed request objects provide direct access to request-specific data, without parsing all values through `McpParameters`.
- Result types follow the same pattern and provide more customization options.

The sections below describe how to migrate MCP components to version `1.1.0`.

## Tools

### Migrate McpTool interface

Previous API:
```java
@Override
public Function<McpRequest, List<McpToolContent>> tool() {
    return request -> List.of(McpToolContents.textContent("text"));
}
```

Updated API:
```java
@Override
public McpToolResult tool(McpToolRequest request) {
    return McpToolResult.create("text");
}
```

For details about the `McpToolResult` builder, see [Tool Result](README.md#tool-result-builder-and-content-types).

### Migrate McpRequest to McpToolRequest

The `McpToolRequest` type extends `McpRequest`. To access tool arguments provided by the client,
use `McpToolRequest#arguments()`.

Previous API:
```java
@Override
public Function<McpRequest, List<McpToolContent>> tool() {
    return request -> {
        String input = request.parameters().get("input").asString().orElse("");
        return List.of();
    };
}
```

Updated API:
```java
@Override
public McpToolResult tool(McpToolRequest request) {
    String input = request.arguments().get("input").asString().orElse("");
    return McpToolResult.create("text");
}
```

The `parameters()` method remains available and exposes the full JSON-RPC parameter payload from the client.

### Migrate from McpToolContents

The `McpToolContents` factory is removed in favor of builders. `McpToolContent` types are now created with dedicated builders.

- McpToolTextContent

Previous API:
```java
McpToolContent text = McpToolContents.textContent("text");
```

Updated API:
```java
McpToolTextContent content = McpToolTextContent.builder().text("text").build();
```

Alternatively, create it directly in the result builder:
```java
McpToolResult result = McpToolResult.builder()
        .addTextContent("text")
        .build();
//This is the equivalent of
McpToolResult result = McpToolResult.create("text");
```

- McpToolImageContent

Previous API:
```java
McpToolContent image = McpToolContents.imageContent("text".getBytes(), MediaTypes.TEXT_PLAIN);
```

Updated API:
```java
McpToolImageContent content = McpToolImageContent.builder()
        .data("text".getBytes())
        .mediaType(MediaTypes.TEXT_PLAIN)
        .build();
```

Alternatively, create it directly in the result builder:
```java
McpToolResult result = McpToolResult.builder()
        .addImageContent("text".getBytes(), MediaTypes.TEXT_PLAIN)
        .build();
```
Or
```java
McpToolResult result = McpToolResult.builder()
        .addImageContent(image -> image.data("text".getBytes())
                                       .mediaType(MediaTypes.TEXT_PLAIN))
        .build();
```

- McpToolAudioContent

Previous API:
```java
McpToolContent audio = McpToolContents.audioContent("audio".getBytes(), MediaTypes.TEXT_PLAIN);
```

Updated API:
```java
McpToolAudioContent content = McpToolAudioContent.builder()
        .data("text".getBytes())
        .mediaType(MediaTypes.TEXT_PLAIN)
        .build();
```

Alternatively, create `McpToolAudioContent` directly in the result builder:
```java
McpToolResult result = McpToolResult.builder()
        .addAudioContent("audio".getBytes(), MediaTypes.TEXT_PLAIN)
        .build();
```
Or
```java
McpToolResult result = McpToolResult.builder()
        .addAudioContent(audio -> audio.data("audio".getBytes())
                                       .mediaType(MediaTypes.TEXT_PLAIN))
        .build();
```

- McpToolTextResourceContent

Previous API:
```java
McpResourceContent content = McpResourceContents.textContent("text");
McpToolContent resource = McpToolContents.resourceContent(URI.create("https://foo"), content);
```

Updated API:
```java
McpToolTextResourceContent content = McpToolTextResourceContent.builder()
        .uri(URI.create("http://resource"))
        .text("resource")
        .mediaType(MediaTypes.TEXT_PLAIN)
        .build();
McpToolResult result = McpToolResult.builder()
        .addTextResourceContent(content)
        .build();
```

Alternatively, create `McpToolTextResourceContent` directly in the result builder:
```java
McpToolResult result = McpToolResult.builder()
        .addTextResourceContent(resource -> resource
                .uri(URI.create("http://resource"))
                .text("resource")
                .mediaType(MediaTypes.TEXT_PLAIN))
        .build();
```

- McpToolBinaryResourceContent

Previous API:
```java
McpResourceContent content = McpResourceContents.binaryContent("binary".getBytes(), MediaTypes.APPLICATION_JSON);
McpToolContent resource = McpToolContents.resourceContent(URI.create("https://foo"), content);
```

Updated API:
```java
McpToolBinaryResourceContent content = McpToolBinaryResourceContent.builder()
        .data("binary".getBytes())
        .uri(URI.create("http://localhost:8080"))
        .mediaType(MediaTypes.APPLICATION_OCTET_STREAM)
        .build();
McpToolResult result = McpToolResult.builder()
        .addBinaryResourceContent(content)
        .build();
```

Alternatively, create `McpToolTextResourceContent` directly in the result builder:
```java
McpToolResult result = McpToolResult.builder()
        .addBinaryResourceContent(resource -> resource
                .data("resource")
                .uri(URI.create("http://resource"))
                .mediaType(MediaTypes.TEXT_PLAIN))
        .build();
```

This approach is more flexible and supports a wider range of use cases.

### Migrate from McpToolErrorException

This exception was previously used to indicate tool errors. This is no longer required;
set the tool error flag on the result builder instead.

Previous API:
```java
throw new McpToolErrorException("exception text");
```

Updated API:
```java
return McpToolResult.builder()
                    .error(true)
                    .addTextContent("exception text")
                    .build();
```

## Prompts

### Migrate McpPrompt interface

Previous API:
```java
@Override
public Function<McpRequest, List<McpPromptContent>> prompt() {
    return request -> List.of(McpPromptContents.textContent("text"));
}
```

Updated API:
```java
@Override
public McpPromptResult prompt(McpPromptRequest request) {
    return McpPromptResult.create("text");
}
```

For details about the `McpPromptResult` builder, see [Prompt Result](README.md#prompt-result-builder-and-content-types).

### Migrate McpRequest to McpPromptRequest

The `McpPromptRequest` extends `McpRequest`. To access prompt arguments provided by the client,
use `McpPromptRequest#arguments()`.

Previous API:
```java
@Override
public Function<McpRequest, List<McpPromptContent>> prompt() {
    return request -> {
        String input = request.parameters().get("input").asString().orElse("");
        return List.of();
    };
}
```

Updated API:
```java
@Override
public McpPromptResult prompt(McpPromptRequest request) {
    String input = request.arguments().get("input").asString().orElse("");
    return McpPromptResult.create("text");
}
```

### Migrate from McpPromptContents

The `McpPromptContents` factory is removed in favor of builders. `McpPromptContent` types are now created with dedicated builders.

- McpPromptTextContent

Previous API:
```java
McpPromptContent text = McpPromptContents.textContent("text");
```

Updated API:
```java
McpPromptTextContent content = McpPromptTextContent.builder()
        .text("text")
        .role(McpRole.USER)
        .build();
```

Alternatively, create it directly in the result builder:
```java
McpPromptResult result = McpPromptResult.builder()
        .addTextContent("text")
        .build();
//This is the equivalent of
McpPromptResult result = McpPromptResult.create("text");
```

- McpPromptImageContent

Previous API:
```java
McpPromptContent image = McpPromptContents.imageContent("binary".getBytes(), MediaTypes.TEXT_PLAIN);
```

Updated API:
```java
McpPromptImageContent content = McpPromptImageContent.builder()
        .data("binary".getBytes())
        .mediaType(MediaTypes.TEXT_PLAIN)
        .role(McpRole.USER)
        .build();
```

Alternatively, create it directly in the result builder:
```java
McpPromptResult result = McpPromptResult.builder()
        .addImageContent("binary".getBytes(), MediaTypes.TEXT_PLAIN)
        .build();
```

- McpPromptAudioContent

Previous API:
```java
McpPromptContent audio = McpPromptContents.audioContent("audio".getBytes(), MediaTypes.TEXT_PLAIN);
```

Updated API:
```java
McpPromptAudioContent content = McpPromptAudioContent.builder()
        .data("audio".getBytes())
        .mediaType(MediaTypes.TEXT_PLAIN)
        .role(McpRole.USER)
        .build();
```

Alternatively, create it directly in the result builder:
```java
McpPromptResult result = McpPromptResult.builder()
        .addAudioContent("audio".getBytes(), MediaTypes.TEXT_PLAIN)
        .build();
```

- McpPromptTextResourceContent

Previous API:
```java
McpResourceContent content = McpResourceContents.textContent("text");
McpPromptContent resource = McpPromptContents.resourceContent(URI.create("https://foo"), content);
```

Updated API:
```java
McpPromptTextResourceContent content = McpPromptTextResourceContent.builder()
        .uri(URI.create("https://resource"))
        .text("text")
        .mediaType(MediaTypes.TEXT_PLAIN)
        .build();
McpPromptResult result = McpPromptResult.builder()
        .addTextResourceContent(content)
        .build();
```

Alternatively, create it directly in the result builder:
```java
McpPromptResult result = McpPromptResult.builder()
        .addTextResourceContent(resource -> resource
                .uri(URI.create("https://resource"))
                .text("text")
                .mediaType(MediaTypes.TEXT_PLAIN))
        .build();
```

- McpPromptBinaryResourceContent

Previous API:
```java
McpResourceContent content = McpResourceContents.binaryContent("binary".getBytes(), MediaTypes.APPLICATION_JSON);
McpPromptContent resource = McpPromptContents.resourceContent(URI.create("https://foo"), content);
```

Updated API:
```java
McpPromptBinaryResourceContent content = McpPromptBinaryResourceContent.builder()
        .data("binary".getBytes())
        .uri(URI.create("http://localhost:8080"))
        .mediaType(MediaTypes.APPLICATION_OCTET_STREAM)
        .build();
McpPromptResult result = McpPromptResult.builder()
        .addBinaryResourceContent(content)
        .build();
```

Alternatively, create it directly in the result builder:
```java
McpPromptResult result = McpPromptResult.builder()
        .addBinaryResourceContent(resource -> resource
                .data("resource")
                .uri(URI.create("http://resource"))
                .mediaType(MediaTypes.TEXT_PLAIN))
        .build();
```

## Resources

### Migrate McpResource interface

Previous API:
```java
@Override
public Function<McpRequest, List<McpResourceContent>> resource() {
    return request -> List.of(McpResourceContents.textContent("text"));
}
```

Updated API:
```java
@Override
public McpResourceResult resource(McpResourceRequest request) {
    return McpResourceResult.create("text");
}
```

For details about the `McpResourceResult` builder, see [Resource Result](README.md#resource-result-builder-and-content-types).

### Migrate McpRequest to McpResourceRequest

The `McpResourceRequest` extends `McpRequest`. To access the URI being read, use the `McpResourceRequest#uri()` method.

Previous API:
```java
@Override
public Function<McpRequest, List<McpResourceContent>> resource() {
    return request -> {
        URI uri = URI.create(request.parameters().get("uri").asString().orElse(""));
        return List.of();
    };
}
```

Updated API:
```java
@Override
public McpResourceResult resource(McpResourceRequest request) {
    URI uri = request.uri();
    return McpResourceResult.create("text");
}
```

### Migrate from McpResourceContents

The `McpResourceContents` factory is removed in favor of builders. The `McpResourceContent` types can be 
created using their builders.

- McpResourceTextContent

Previous API:
```java
McpResourceContent text = McpResourceContents.textContent("text");
```

Updated API:
```java
McpResourceTextContent content = McpResourceTextContent.builder()
        .text("text")
        .mediaType(MediaTypes.TEXT_PLAIN)
        .build();
```

Alternatively, create it directly in the result builder:
```java
McpResourceResult result = McpResourceResult.builder()
        .addTextContent("text")
        .build();
//This is the equivalent of
McpResourceResult result = McpResourceResult.create("text");
```

- McpResourceBinaryContent

Previous API:
```java
McpResourceContent binary = McpResourceContents.binaryContent("binary".getBytes(), MediaTypes.APPLICATION_JSON);
```

Updated API:
```java
McpResourceBinaryContent content = McpResourceBinaryContent.builder()
        .data("binary".getBytes())
        .mediaType(MediaTypes.APPLICATION_JSON)
        .build();
```

Alternatively, create it directly in the result builder:
```java
McpResourceResult result = McpResourceResult.builder()
        .addBinaryContent("binary".getBytes(), MediaTypes.APPLICATION_JSON)
        .build();
```

## Completion

### Migrate McpCompletion interface

Previous API:
```java
@Override
public Function<McpRequest, McpCompletionContent> completion() {
    return request -> McpCompletionContents.completion("suggestion1", "suggestion2");
}
```

Updated API:
```java
@Override
public McpCompletionResult completion(McpCompletionRequest request) {
    return McpCompletionResult.create("suggestion1", "suggestion2");
}
```

For details about the `McpCompletionResult` builder, see [Completion Result](README.md#completion-result-builder).

### Migrate McpRequest to McpCompletionRequest

The `McpCompletionRequest` extends `McpRequest`. To access completion details, use the methods:
- `McpCompletionRequest#name()`: name of the argument or resource template being completed
- `McpCompletionRequest#value()`: current value of the field being completed
- `McpCompletionRequest#context()`: additional context (e.g., other arguments already filled)

Previous API:
```java
@Override
public Function<McpRequest, McpCompletionContent> completion() {
    return request -> {
        String name = request.parameters().get("name").asString().orElse("");
        String value = request.parameters().get("value").asString().orElse("");
        return McpCompletionContents.completion("suggestion1", "suggestion2");
    };
}
```

Updated API:
```java
@Override
public McpCompletionResult completion(McpCompletionRequest request) {
    String name = request.name();
    String value = request.value();
    return McpCompletionResult.create("suggestion1", "suggestion2");
}
```
