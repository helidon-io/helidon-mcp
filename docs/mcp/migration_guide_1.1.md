# Migration guide to version 1.1

Helidon MCP `1.1.0` provides support for MCP specification `2025-06-18`. This document 
presents the noticeable changes and a way to upgrade your post `1.1.0` code.

# Overview of changes

- When MCP components used to return a `List<Mcp*Content>`, they now return `Mcp*Result`.
- End of MCP components `Function<McpRequest, List<Mcp*Content>>` in favor of `Mcp*Result method(Mcp*Request request)`.
- Contents are now created directly on the result builder and do not require the factory classes anymore.
- Having typed requests allows easier access to specific request data without the need of parsing them through the `McpParameters`.
- The same applies to results and provides higher result customization.

The following chapters describe in detail how to migrate your MCP components to `1.1.0` version.

## Tools

### Migrate McpTool interface

Old code:
```java
@Override
public Function<McpRequest, List<McpToolContent>> tool() {
    return request -> List.of(McpToolContents.textContent("text"));
}
```

New code:
```java
@Override
public McpToolResult tool(McpToolRequest request) {
    return McpToolResult.create("text");
}
```

For more details about the McpToolResult builder, refer to [this section](README.md#tool-result-builder-and-content-types) of the documentation.

### Migrate McpRequest to McpToolRequest

The `McpToolRequest` extends `McpRequest` and this makes the migration easier. To access tool arguments provided by the client, 
use now the `McpToolRequest#arguments()` method.

Old code:
```java
@Override
public Function<McpRequest, List<McpToolContent>> tool() {
    return request -> {
        String input = request.parameters().get("input").asString().orElse("");
        return List.of();
    };
}
```

New code:
```java
@Override
public McpToolResult tool(McpToolRequest request) {
    String input = request.arguments().get("input").asString().orElse("");
    return McpToolResult.create("text");
}
```

The method `parameters` is still available on the request but provide all the JSON-RPC parameters provided by the client.

### Migrate from McpToolContents

The `McpToolContents` factory is removed in favor of builders. The `McpToolContent` types can be created using their builders.

- McpToolTextContent

Old code:
```java
McpToolContent text = McpToolContents.textContent("text");
```

New code:
```java
McpToolTextContent content = McpToolTextContent.builder().text("text").build();
```

The other way is to create it on the result builder directly:
```java
McpToolResult result = McpToolResult.builder()
        .addTextContent("text")
        .build();
//This is the equivalent of
McpToolResult result = McpToolResult.create("text");
```

- McpToolImageContent

Old code:
```java
McpToolContent image = McpToolContents.imageContent("text".getBytes(), MediaTypes.TEXT_PLAIN);
```

New code:
```java
McpToolImageContent content = McpToolImageContent.builder()
        .data("text".getBytes())
        .mediaType(MediaTypes.TEXT_PLAIN)
        .build();
```

The other way is to create it on the result builder directly:
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

Old code:
```java
McpToolContent audio = McpToolContents.audioContent("audio".getBytes(), MediaTypes.TEXT_PLAIN);
```

New code:
```java
McpToolAudioContent content = McpToolAudioContent.builder()
        .data("text".getBytes())
        .mediaType(MediaTypes.TEXT_PLAIN)
        .build();
```

Create an instance of McpToolAudioContent in the result builder:
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

Old code:
```java
McpResourceContent content = McpResourceContents.textContent("text");
McpToolContent resource = McpToolContents.resourceContent(URI.create("https://foo"), content);
```

New code:
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

Create an instance of McpToolTextResourceContent in the result builder:
```java
McpToolResult result = McpToolResult.builder()
        .addTextResourceContent(resource -> resource
                .uri(URI.create("http://resource"))
                .text("resource")
                .mediaType(MediaTypes.TEXT_PLAIN))
        .build();
```

- McpToolBinaryResourceContent

Old code:
```java
McpResourceContent content = McpResourceContents.binaryContent("binary".getBytes(), MediaTypes.APPLICATION_JSON);
McpToolContent resource = McpToolContents.resourceContent(URI.create("https://foo"), content);
```

New code:
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

Create an instance of McpToolTextResourceContent in the result builder:
```java
McpToolResult result = McpToolResult.builder()
        .addBinaryResourceContent(resource -> resource
                .data("resource")
                .uri(URI.create("http://resource"))
                .mediaType(MediaTypes.TEXT_PLAIN))
        .build();
```

We believe this approach provides more flexibility and fits more use cases.

### Migrate from McpToolErrorException

The purpose of this exception is to return a tool error. This is not required anymore, the tool 
error flag can be used on the result builder instead.

Old code:
```java
throw new McpToolErrorException("exception text");
```

New code:
```java
return McpToolResult.builder()
                    .error(true)
                    .addTextContent("exception text")
                    .build();
```

## Prompts

### Migrate McpPrompt interface

Old code:
```java
@Override
public Function<McpRequest, List<McpPromptContent>> prompt() {
    return request -> List.of(McpPromptContents.textContent("text"));
}
```

New code:
```java
@Override
public McpPromptResult prompt(McpPromptRequest request) {
    return McpPromptResult.create("text");
}
```

For more details about the McpPromptResult builder, refer to [this section](README.md#prompt-result-builder-and-content-types) of the documentation.

### Migrate McpRequest to McpPromptRequest

The `McpPromptRequest` extends `McpRequest`. To access prompt arguments provided by the client,
use now the `McpPromptRequest#arguments()` method.

Old code:
```java
@Override
public Function<McpRequest, List<McpPromptContent>> prompt() {
    return request -> {
        String input = request.parameters().get("input").asString().orElse("");
        return List.of();
    };
}
```

New code:
```java
@Override
public McpPromptResult prompt(McpPromptRequest request) {
    String input = request.arguments().get("input").asString().orElse("");
    return McpPromptResult.create("text");
}
```

### Migrate from McpPromptContents

The `McpPromptContents` factory is removed in favor of builders. The `McpPromptContent` types can be created using their builders.

- McpPromptTextContent

Old code:
```java
McpPromptContent text = McpPromptContents.textContent("text");
```

New code:
```java
McpPromptTextContent content = McpPromptTextContent.builder()
        .text("text")
        .role(McpRole.USER)
        .build();
```

The other way is to create it on the result builder directly:
```java
McpPromptResult result = McpPromptResult.builder()
        .addTextContent("text")
        .build();
//This is the equivalent of
McpPromptResult result = McpPromptResult.create("text");
```

- McpPromptImageContent

Old code:
```java
McpPromptContent image = McpPromptContents.imageContent("binary".getBytes(), MediaTypes.TEXT_PLAIN);
```

New code:
```java
McpPromptImageContent content = McpPromptImageContent.builder()
        .data("binary".getBytes())
        .mediaType(MediaTypes.TEXT_PLAIN)
        .role(McpRole.USER)
        .build();
```

The other way is to create it on the result builder directly:
```java
McpPromptResult result = McpPromptResult.builder()
        .addImageContent("binary".getBytes(), MediaTypes.TEXT_PLAIN)
        .build();
```

- McpPromptAudioContent

Old code:
```java
McpPromptContent audio = McpPromptContents.audioContent("audio".getBytes(), MediaTypes.TEXT_PLAIN);
```

New code:
```java
McpPromptAudioContent content = McpPromptAudioContent.builder()
        .data("audio".getBytes())
        .mediaType(MediaTypes.TEXT_PLAIN)
        .role(McpRole.USER)
        .build();
```

The other way is to create it on the result builder directly:
```java
McpPromptResult result = McpPromptResult.builder()
        .addAudioContent("audio".getBytes(), MediaTypes.TEXT_PLAIN)
        .build();
```

- McpPromptTextResourceContent

Old code:
```java
McpResourceContent content = McpResourceContents.textContent("text");
McpPromptContent resource = McpPromptContents.resourceContent(URI.create("https://foo"), content);
```

New code:
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

The other way is to create it on the result builder directly:
```java
McpPromptResult result = McpPromptResult.builder()
        .addTextResourceContent(resource -> resource
                .uri(URI.create("https://resource"))
                .text("text")
                .mediaType(MediaTypes.TEXT_PLAIN))
        .build();
```

- McpPromptBinaryResourceContent

Old code:
```java
McpResourceContent content = McpResourceContents.binaryContent("binary".getBytes(), MediaTypes.APPLICATION_JSON);
McpPromptContent resource = McpPromptContents.resourceContent(URI.create("https://foo"), content);
```

New code:
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

The other way is to create it on the result builder directly:
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

Old code:
```java
@Override
public Function<McpRequest, List<McpResourceContent>> resource() {
    return request -> List.of(McpResourceContents.textContent("text"));
}
```

New code:
```java
@Override
public McpResourceResult resource(McpResourceRequest request) {
    return McpResourceResult.create("text");
}
```

For more details about the McpResourceResult builder, refer to [this section](README.md#resource-result-builder-and-content-types) of the documentation.

### Migrate McpRequest to McpResourceRequest

The `McpResourceRequest` extends `McpRequest`. To access the URI being read, use the `McpResourceRequest#uri()` method.

Old code:
```java
@Override
public Function<McpRequest, List<McpResourceContent>> resource() {
    return request -> {
        URI uri = URI.create(request.parameters().get("uri").asString().orElse(""));
        return List.of();
    };
}
```

New code:
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

Old code:
```java
McpResourceContent text = McpResourceContents.textContent("text");
```

New code:
```java
McpResourceTextContent content = McpResourceTextContent.builder()
        .text("text")
        .mediaType(MediaTypes.TEXT_PLAIN)
        .build();
```

The other way is to create it on the result builder directly:
```java
McpResourceResult result = McpResourceResult.builder()
        .addTextContent("text")
        .build();
//This is the equivalent of
McpResourceResult result = McpResourceResult.create("text");
```

- McpResourceBinaryContent

Old code:
```java
McpResourceContent binary = McpResourceContents.binaryContent("binary".getBytes(), MediaTypes.APPLICATION_JSON);
```

New code:
```java
McpResourceBinaryContent content = McpResourceBinaryContent.builder()
        .data("binary".getBytes())
        .mediaType(MediaTypes.APPLICATION_JSON)
        .build();
```

The other way is to create it on the result builder directly:
```java
McpResourceResult result = McpResourceResult.builder()
        .addBinaryContent("binary".getBytes(), MediaTypes.APPLICATION_JSON)
        .build();
```

## Completion

### Migrate McpCompletion interface

Old code:
```java
@Override
public Function<McpRequest, McpCompletionContent> completion() {
    return request -> McpCompletionContents.completion("suggestion1", "suggestion2");
}
```

New code:
```java
@Override
public McpCompletionResult completion(McpCompletionRequest request) {
    return McpCompletionResult.create("suggestion1", "suggestion2");
}
```

For more details about the McpCompletionResult builder, refer to [this section](README.md#completion-result-builder) of the documentation.

### Migrate McpRequest to McpCompletionRequest

The `McpCompletionRequest` extends `McpRequest`. To access completion details, use the methods:
- `McpCompletionRequest#name()`: name of the argument or resource template being completed
- `McpCompletionRequest#value()`: current value of the field being completed
- `McpCompletionRequest#context()`: additional context (e.g., other arguments already filled)

Old code:
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

New code:
```java
@Override
public McpCompletionResult completion(McpCompletionRequest request) {
    String name = request.name();
    String value = request.value();
    return McpCompletionResult.create("suggestion1", "suggestion2");
}
```
