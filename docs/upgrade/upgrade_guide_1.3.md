# Upgrade Guide for Version 1.3.0

Helidon MCP `1.3.0` replaces the sampling API's type-specific message model with ordered message envelopes and content blocks.
It also adds tool-enabled sampling for MCP specification `2025-11-25`. This document summarizes the backward-incompatible
changes and explains how to migrate existing code from `1.2.x` to `1.3.0`.

## Overview of Changes

- The separate sampling request lists for text, image, and audio messages are replaced by one ordered `messages()` list.
- `McpSamplingMessage` now represents a role-bearing envelope containing ordered `McpSamplingContent` blocks.
- The `McpSampling*Message` leaf types and `McpSamplingMessageType` are replaced by `McpSampling*Content` types and
  `McpSamplingContentType`.
- Sampling responses continue to expose one value through `message()`, which now returns a message envelope; the
  `as*Message()` methods are replaced by first-content convenience methods.
- Image and audio `data()` methods return decoded raw bytes, and `decodeBase64Data()` is removed.
- `McpSamplingContentType` includes `TOOL_USE` and `TOOL_RESULT`.
- Sampling requests select registered server tools by name, and Helidon executes sampling tool loops automatically.
- Sampling messages and content support protocol `_meta`; annotated content supports audience, priority, and last-modified data.

The sections below describe how to migrate applications to version `1.3.0`.

## Migrate sampling requests to message envelopes

In `1.2.x`, a sampling request stores text, image, and audio messages in separate lists. In `1.3.0`, each
`McpSamplingMessage` contains one role and an ordered list of content blocks.

Previous usage:

```java
McpSamplingRequest request = McpSamplingRequest.builder()
        .addTextMessage(text -> text
                .role(McpRole.USER)
                .text("Summarize this image."))
        .addImageMessage(image -> image
                .role(McpRole.USER)
                .data(imageBytes)
                .mediaType(MediaTypes.create("image/png")))
        .build();
```

Updated usage:

```java
McpSamplingRequest request = McpSamplingRequest.builder()
        .addMessage(McpSamplingMessage.builder()
                .role(McpRole.USER)
                .addContent(McpSamplingTextContent.create("Summarize this image."))
                .build())
        .addMessage(McpSamplingMessage.builder()
                .role(McpRole.USER)
                .addContent(McpSamplingImageContent.builder()
                        .data(imageBytes)
                        .mediaType(MediaTypes.create("image/png"))
                        .build())
                .build())
        .build();
```

For a simple text message, use the convenience method:

```java
McpSamplingRequest request = McpSamplingRequest.builder()
        .addTextMessage(McpRole.USER, "Summarize this text.")
        .build();
```

Use separate message envelopes when content blocks have different roles or must remain separate messages. A separate envelope
for each content block, as shown above, works with every supported protocol version. Multiple content blocks in one envelope are
supported only when protocol version `2025-11-25` is negotiated; those blocks retain their insertion order on the wire.

The principal type and accessor replacements are:

| `1.2.x` API | `1.3.0` API |
| --- | --- |
| `McpSamplingTextMessage` | `McpSamplingTextContent` |
| `McpSamplingImageMessage` | `McpSamplingImageContent` |
| `McpSamplingAudioMessage` | `McpSamplingAudioContent` |
| `McpSamplingMediaMessage` | `McpSamplingMediaContent` |
| `McpSamplingMessageType` | `McpSamplingContentType` |
| `textMessages()`, `imageMessages()`, `audioMessages()` | `messages()` |

The type-specific `clear*Messages()` methods, list setters and adders, leaf-message adders, and consumer-builder overloads are
also removed. Replace them with `clearMessages()`, `messages(...)`, `addMessages(...)`, and `addMessage(...)`. The existing
`addTextMessage(String)` convenience method remains available, and `addTextMessage(McpRole, String)` is new in `1.3.0`.

## Migrate sampling response access

Sampling responses now contain one message envelope. Iterate over that envelope's ordered content blocks instead of iterating
over leaf messages.

Previous usage:

```java
McpSamplingMessage message = response.message();
String text = response.asTextMessage().text();
byte[] image = response.asImageMessage().decodeBase64Data();
```

Updated usage:

```java
String text = response.asTextContent().text();
byte[] image = response.asImageContent().data();
for (McpSamplingContent content : response.message().contents()) {
    // Process each ordered content block.
}
```

The first-content convenience methods are now `asTextContent()`, `asImageContent()`, `asAudioContent()`, and
`asToolUseContent()`.

## Use raw media bytes

`McpSamplingMediaContent.data()` returns raw bytes for both application-built content and content parsed from sampling responses.
Do not decode the returned bytes again. The former `decodeBase64Data()` method is removed. Use `encodeBase64Data()` only when a
base64 string is required.

## Handle the expanded sampling content types

Code that previously switched on `McpSamplingMessageType` must switch on each content block's `McpSamplingContentType`.
Exhaustive switches must also handle the new `TOOL_USE` and `TOOL_RESULT` constants.

```java
for (McpSamplingContent content : response.message().contents()) {
    switch (content.type()) {
        case TEXT -> handleText((McpSamplingTextContent) content);
        case IMAGE -> handleImage((McpSamplingImageContent) content);
        case AUDIO -> handleAudio((McpSamplingAudioContent) content);
        case TOOL_USE -> handleToolUse((McpSamplingToolUseContent) content);
        case TOOL_RESULT -> handleToolResult((McpSamplingToolResultContent) content);
    }
}
```

`TOOL_USE` represents an assistant request to invoke a sampling tool. `TOOL_RESULT` represents the corresponding result in a
follow-up user message.

## Enable sampling with registered tools

Tool-enabled sampling is new in `1.3.0`. Register each `McpTool` with the server, then select the tools available to one sampling
request by name. `McpSamplingRequest.tools()` is a `List<String>`; it does not contain tool implementations.

```java
McpTool weather = new WeatherTool();
McpServerFeature server = McpServerFeature.builder()
        .addTool(weather)
        .build();

McpSamplingRequest request = McpSamplingRequest.builder()
        .addTool(weather.name())
        .toolChoice(McpToolChoice.AUTO)
        .addTextMessage(McpRole.USER, "What is the weather in Prague?")
        .build();

McpSamplingResponse response = sampling.request(request);
```

Before sending a request with tool names or a tool choice, verify `sampling.enabledTools()`. Only names selected by the request
are offered to the client and eligible for automatic invocation. Each selected name must match exactly one registered server
tool, and names in one request must be unique.

When a response contains tool-use content, `McpSampling.request(...)` invokes the selected tools, appends the assistant message
and tool results, and continues sampling. It returns the final response without tool-use content. Configure the maximum number
of tool rounds with `mcp.server.max-sampling-tool-iterations` or
`McpServerFeature.builder().maxSamplingToolIterations(...)`.

## Use annotations and protocol metadata

Sampling message envelopes and content blocks can carry protocol `_meta` through `metadata(McpParameters)`. Annotated sampling
content can also carry `McpAnnotations`, including audience roles, priority, and last-modified data.

```java
McpSamplingTextContent content = McpSamplingTextContent.builder()
        .text("Important context")
        .annotations(McpAnnotations.builder()
                .addAudience(McpRole.USER)
                .priority(0.8)
                .build())
        .metadata(McpParameters.create(JsonObject.builder()
                .set("traceId", "trace-1")
                .build()))
        .build();
```

This protocol `_meta` is distinct from `McpSamplingRequest.metadata(Object)`, which remains provider-specific sampling metadata
as described in the `1.2.0` upgrade guide.

## Update custom implementations of generated interfaces

Version `1.3.0` adds abstract methods to generated public interfaces. Applications that use the Helidon builders receive the
new defaults automatically. Applications that implement these interfaces directly must update their implementations:

- `McpServerConfig` adds `maxSamplingToolIterations()`. Return the configured limit, or `10` to retain the default behavior.
- `McpToolTextContent`, `McpToolImageContent`, `McpToolAudioContent`, `McpToolTextResourceContent`,
  `McpToolBinaryResourceContent`, and `McpToolResourceLinkContent` add `annotations()` and `metadata()`. Return
  `Optional.empty()` when the content has no annotations or protocol `_meta` value.
- For `McpToolTextResourceContent` and `McpToolBinaryResourceContent`, `metadata()` represents the outer embedded-content
  `_meta` field. The flattened API does not expose the nested resource-content `_meta` field.
- `McpToolResourceLinkContent` also adds `icons()`.

These additions are source-incompatible with custom implementations because recompilation requires implementations for the new
abstract methods. Previously compiled custom implementations can fail with `AbstractMethodError` when version `1.3.0` invokes
one of the new methods. Recompile and update such implementations before upgrading.

## Recompile applications

The removed and renamed public sampling types, request accessors, response accessors, and builder methods create source and
binary incompatibilities. Recompile applications after updating their sampling code. Previously compiled code can otherwise fail
with errors such as `NoClassDefFoundError`, `NoSuchMethodError`, or `IncompatibleClassChangeError`.
