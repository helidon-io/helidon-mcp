# Upgrade Guide for Version 27.0.0

Helidon MCP `27.0.0` upgrades the baseline to Helidon `27.0.0` and Java 27. It replaces the sampling API's type-specific
message model with ordered message envelopes and content blocks, and adds tool-enabled sampling for MCP specification
`2025-11-25`. This document summarizes the backward-incompatible changes and explains how to migrate existing code from
`1.2.x` to `27.0.0`.

## Overview of Changes

- Java 27 or newer and Helidon `27.0.0` replace the Java 21 and Helidon `4.5.0` baseline.
- Helidon 27 does not include MicroProfile support; the MicroProfile calendar example is removed.
- The separate sampling request lists for text, image, and audio messages are replaced by one ordered `messages()` list.
- `McpSamplingMessage` now represents a role-bearing envelope containing ordered `McpSamplingContent` blocks.
- The `McpSampling*Message` leaf types and `McpSamplingMessageType` are replaced by `McpSampling*Content` types and
  `McpSamplingContentType`.
- Sampling responses continue to expose one value through `message()`, which now returns a message envelope; the
  `as*Message()` methods are replaced by first-content convenience methods.
- Image and audio `data()` methods return decoded raw bytes, and `decodeBase64Data()` is removed.
- `McpSamplingContentType` includes `TOOL_USE` and `TOOL_RESULT`.
- `McpStopReason` adds `TOOL_USE`; exhaustive switches must handle the new constant.
- Sampling requests select registered server tools by name, and Helidon executes sampling tool loops automatically.
- Sampling messages and content support protocol `_meta`; annotated content supports audience, priority, and last-modified data.
- `McpRequest` exposes protocol `_meta` through `metadata()`; the former `meta()` accessor is removed.

The sections below describe how to migrate applications to version `27.0.0`.

## Update Java and Helidon dependencies

Use JDK 27 or newer to build and run the application, and update the application's compiler release, CI jobs, and container
runtime accordingly. Update the Helidon application parent or dependency BOM to `27.0.0`, and the Helidon MCP BOM and any
explicitly versioned MCP dependencies or annotation processors to `27.0.0`.

The Maven group ID remains `io.helidon.extensions.mcp`. The BOM and server artifact IDs remain
`helidon4-extensions-mcp-bom` and `helidon4-extensions-mcp-server`; keep the `helidon4` prefix when updating their versions.

Helidon 27 does not include MicroProfile support. Applications using the previous MicroProfile integration must migrate to
Helidon SE or the declarative API with the Helidon Service Registry before adopting this release. The MicroProfile calendar
example has been removed; use the [SE calendar example](../../examples/calendar-application/calendar/README.md) or the
[declarative calendar example](../../examples/calendar-application/calendar-declarative/README.md) as a starting point.

## Migrate request metadata access

`McpRequest` now extends `McpMetadata`. Replace the former `meta()` accessor with the optional `metadata()` accessor:

```java
// Before
String traceId = request.meta().get("traceId").asString().orElse("unknown");

// After
String traceId = request.metadata()
        .map(metadata -> metadata.get("traceId").asString().orElse("unknown"))
        .orElse("unknown");
```

`McpMetadata.metadata()` returns `Optional<McpParameters>`. The generated request builder provides
`metadata(McpParameters)` and `metadata()`. The former `McpRequest.meta()` accessor and generated builder methods
`meta(McpParameters)` and `meta()` are removed. Replace builder calls as well:

```java
// Before
builder.meta(metadata);
McpParameters current = builder.meta().orElseThrow();

// After
builder.metadata(metadata);
McpParameters current = builder.metadata().orElseThrow();
```

`McpRequest` builders are public for framework use but are not intended for application request construction. Applications
should consume request instances supplied by the server. Existing binaries that invoke a removed `meta` method can fail with
`NoSuchMethodError`; recompile them after applying these source changes. Custom `McpRequest` implementations must implement
`metadata()`. Use `McpParameters.create(Object)` to serialize a map or custom type with Helidon JSON binding; the value must
produce a JSON object.

## Migrate sampling requests to message envelopes

In `1.2.x`, a sampling request stores text, image, and audio messages in separate lists. In `27.0.0`, each
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

Use separate message envelopes when content blocks have different roles or must remain separate messages. Protocol version
`2024-11-05` supports text and image content blocks, `2025-03-26` adds audio, and `2025-06-18` adds content `_meta` and
`McpAnnotations.lastModified` while message `_meta` remains unsupported. Protocol version `2025-11-25` adds tool-use and
tool-result blocks, message `_meta`, and multiple content blocks per envelope; those blocks retain their insertion order on the
wire. For earlier protocol versions, use one content block per envelope and only the content types and metadata fields supported
by the negotiated version.

The principal type and accessor replacements are:

| `1.2.x` API | `27.0.0` API |
| --- | --- |
| `McpSamplingTextMessage` | `McpSamplingTextContent` |
| `McpSamplingImageMessage` | `McpSamplingImageContent` |
| `McpSamplingAudioMessage` | `McpSamplingAudioContent` |
| `McpSamplingMediaMessage` | `McpSamplingMediaContent` |
| `McpSamplingMessageType` | `McpSamplingContentType` |
| `textMessages()`, `imageMessages()`, `audioMessages()` | `messages()` |

The type-specific `clear*Messages()` methods, list setters and adders, leaf-message adders, and consumer-builder overloads are
also removed. Replace them with `clearMessages()`, `messages(...)`, `addMessages(...)`, and `addMessage(...)`. The existing
`addTextMessage(String)` convenience method remains available, and `addTextMessage(McpRole, String)` is new in `27.0.0`.
The single-argument convenience method retains the `ASSISTANT` role; use the overload with `McpRole.USER` for user messages.

## Migrate sampling response access

Sampling responses now contain one message envelope. Iterate over that envelope's ordered content blocks to handle responses
with multiple blocks.

Previous usage:

```java
McpSamplingMessage message = response.message();
String text = response.asTextMessage().text();
```

Updated usage:

```java
String text = response.asTextContent().text();
for (McpSamplingContent content : response.message().contents()) {
    // Process each ordered content block.
}
```

The first-content convenience methods are now `asTextContent()`, `asImageContent()`, `asAudioContent()`, and
`asToolUseContent()`. Each checks only the first content block and throws `McpSamplingException` if that block has a different
type or the message is empty; these methods do not search for a matching block.

## Use raw media bytes

`McpSamplingMediaContent.data()` returns raw bytes for both application-built content and content parsed from sampling responses.
Do not decode the returned bytes again. The former `decodeBase64Data()` method is removed. Use `encodeBase64Data()` only when a
base64 string is required.

For a response whose first content block is an image, replace `response.asImageMessage().decodeBase64Data()` with:

```java
byte[] image = response.asImageContent().data();
```

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

`McpStopReason` also adds `TOOL_USE`. Update exhaustive switches over this enum. `response.stopReason()` returns the
recognized standard reason, or an empty optional when the client omits it or supplies a non-standard reason. Use
`response.rawStopReason()` when the exact client-provided value is needed.

## Check sampling context support

For MCP `2025-11-25`, basic sampling support no longer implies support for including context. Before setting
`includeContext(McpIncludeContext.THIS_SERVER)` or `includeContext(McpIncludeContext.ALL_SERVERS)`, check
`sampling.enabledContext()`. Otherwise, omit context or use `McpIncludeContext.NONE`; requesting unsupported context throws
`McpSamplingException`. With earlier protocol versions, Helidon continues to allow context inclusion whenever the client
supports sampling.

## Enable sampling with registered tools

Tool-enabled sampling is new in `27.0.0`. Register each `McpTool` with the server, then select the tools available to one sampling
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

// Within a handler registered with this server:
McpSampling sampling = toolRequest.features().sampling();
if (!sampling.enabledTools()) {
    throw new IllegalStateException("The client does not support sampling with tools");
}
McpSamplingResponse response = sampling.request(request);
```

Before sending a request with tool names or a tool choice, verify `sampling.enabledTools()`. Only names selected by the request
are offered to the client and eligible for automatic invocation. Each selected name must match exactly one registered server
tool, and names in one request must be unique.

When a response contains tool-use content, `McpSampling.request(...)` invokes the selected tools, appends the assistant message
and tool results, and continues sampling. It returns the final response without tool-use content. Configure the maximum number
of tool rounds and cumulative tool executions with `mcp.server.max-sampling-tool-iterations` or
`McpServerFeature.builder().maxSamplingToolIterations(...)`. The default is `10`, and the value must be greater than zero.
Both limits are shared by all sampling calls through the originating request's `McpFeatures`, including calls made by invoked
tools. Multiple tool calls in one response count as one round, but each invocation counts toward the execution limit.
Once either limit is reached, Helidon requests a final response with `McpToolChoice.NONE`; further tool-use responses fail
with `McpSamplingException`. Tools execute sequentially, and the request timeout applies separately to each sampling
exchange with the client, not to the entire conversation or to tool execution.

## Use annotations and access protocol metadata

Annotated sampling content supports audience and priority in every supported version, while `lastModified` requires
`2025-06-18` or later.

```java
McpSamplingTextContent content = McpSamplingTextContent.builder()
        .text("Important context")
        .annotations(McpAnnotations.builder()
                .addAudience(McpRole.USER)
                .priority(0.8)
                .build())
        .metadata(McpParameters.create(Map.of("traceId", "trace-1")))
        .build();
```

Protocol `_meta` is serialized on sampling content blocks starting with `2025-06-18` and on message envelopes starting with
`2025-11-25`. Read metadata through `metadata()` using the same `McpParameters` access API as request metadata:

```java
String traceId = response.asTextContent().metadata()
        .map(metadata -> metadata.get("traceId").asString().orElse("unknown"))
        .orElse("unknown");
```

This protocol `_meta` is distinct from the sampling request builder's `metadata(Object)` setter and
`McpSamplingRequest.metadata()` accessor, which remain provider-specific sampling metadata as described in the
[1.2.0 upgrade guide](upgrade_guide_1.2.md#sampling-metadata-incompatibility).

## Update custom implementations of generated interfaces

Version `27.0.0` adds abstract methods to generated public interfaces. Applications that use the Helidon builders receive the
new defaults automatically. Applications that implement these interfaces directly must update their implementations:

- `McpServerConfig` adds `maxSamplingToolIterations()`. Return the configured limit, or `10` to retain the default behavior.
- `McpServerConfig` also adds `description()` and `icons()`. Return `Optional<String>` and `List<McpIcon>`, respectively;
  use `Optional.empty()` and an empty list when these values are absent. Its new `websiteUrl()` method has a default
  implementation returning `Optional.empty()`.
- `McpToolConfig`, `McpPromptConfig`, and `McpResourceConfig` add `icons()`. Return `List<McpIcon>`, or an empty list when
  there are no icons.
- `McpToolTextContent`, `McpToolImageContent`, `McpToolAudioContent`, `McpToolTextResourceContent`,
  `McpToolBinaryResourceContent`, and `McpToolResourceLinkContent` add `annotations()` and `metadata()`. Return
  `Optional<McpAnnotations>` and `Optional<McpParameters>`, respectively; use `Optional.empty()` when the corresponding
  value is absent.
- For `McpToolTextResourceContent` and `McpToolBinaryResourceContent`, `metadata()` represents the outer embedded-content
  `_meta` field. The flattened API does not expose the nested resource-content `_meta` field.
- `McpToolResourceLinkContent` also adds `icons()`.

These additions are source-incompatible with custom implementations because recompilation requires implementations for the new
abstract methods. Previously compiled custom implementations can fail with `AbstractMethodError` when version `27.0.0` invokes
one of the new methods. Recompile and update such implementations before upgrading.

## Recompile applications

The removed and renamed public sampling types, request accessors, response accessors, and builder methods create source and
binary incompatibilities. Recompile applications after updating their sampling code. Previously compiled code can otherwise fail
with errors such as `NoClassDefFoundError`, `NoSuchMethodError`, or `IncompatibleClassChangeError`.
