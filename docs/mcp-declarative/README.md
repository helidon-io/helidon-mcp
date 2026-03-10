# Helidon MCP Extension

Server-side Helidon support for the Model Context Protocol (MCP).

## Overview

The Model Context Protocol (MCP) defines a standardized way for LLMs (Large Language Models) to interact with internal and
external data sources. MCP uses a client-server architecture in which clients (typically AI agents) initiate communication and
servers expose capabilities for data access, retrieval, and interaction. Helidon provides MCP server-side support that can be
consumed by any client implementing the MCP specification.

## Maven Coordinates

To get started with your first Helidon-powered MCP server, add this dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.helidon.extensions.mcp</groupId>
    <artifactId>helidon4-extensions-mcp-server</artifactId>
</dependency>
```

Also include the following annotation processor setup:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>io.helidon.extensions.mcp</groupId>
                <artifactId>helidon4-extensions-mcp-codegen</artifactId>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

## Usage

This section describes how to create and configure core MCP components in Helidon.

### MCP Server

Servers provide the primary integration point for adding context to language models through MCP. Clients discover a server via a
configurable HTTP endpoint. Servers manage client connections and expose the capabilities described later in this guide. Helidon
represents an MCP server as an `HttpFeature`, registered as part of web server routing. You can host multiple MCP servers by
defining multiple classes annotated with `@Mcp.Server`, each using a distinct `@Mcp.Path`. Each path must be unique and serves as
an independent entry point for MCP clients. Helidon does not enforce naming or versioning conventions; these values are shared
with the client during connection initialization.

```java
@Mcp.Server
class McpServer {
}
```

#### Configuration

- **`@Mcp.Server`**: Defines the class as an MCP server and can override the default server name.
- **`@Mcp.Path`**: Sets the HTTP endpoint path for the server.
- **`@Mcp.Version`**: Establishes the server version.

```java
@Mcp.Path("/mcp")
@Mcp.Version("0.0.1")
@Mcp.Server("MyServer")
class McpServer {
}
```

### Tool

`Tools` enable models to interact with external systems, such as querying databases, calling APIs, or performing computations.
Define a tool by annotating a method with `@Mcp.Tool`. Method names become tool names unless overridden with `@Mcp.Name`. Input
schemas are generated using the [JSON Schema Specification](https://json-schema.org/specification); Helidon auto generates schemas when inputs are primitive types
(non-POJO). `Tools` are automatically registered when defined within a server class. You can inject `McpToolRequest` as a 
parameter to your tool method. It extends `McpRequest` and provides access to the `McpTool` instance via the `tool()` method.

```java
@Mcp.Server
class Server {

    @Mcp.Tool("Tool description")
    McpToolResult myToolMethod(String input) {
        return McpToolResult.create("Input: " + input);
    }
}
```

#### Configuration

- **`@Mcp.Name`**: Overrides tool method name.
- **`@JsonSchema.Schema`**: Explicitly defines POJO input structures.

```java
@Mcp.Server
class Server {

    @Mcp.Tool("Tool description")
    @Mcp.Name("MyTool")
    McpToolResult myToolMethod(Coordinate coordinate) {
        String result = String.format("latitude: %s, longitude: %s", 
                                      coordinate.latitude(), 
                                      coordinate.longitude());
        return McpToolResult.builder().addTextContent(result).build();
    }

    @JsonSchema.Schema
    record Coordinate(int latitude, int longitude) {
    }
}
```

#### Structured content and output schema

Structured content is returned as a JSON object in the `structuredContent` field of a result. For backwards compatibility,
a tool that returns structured content SHOULD also return the serialized JSON in a text content block. If there is no content 
added to the `McpToolResult` builder, Helidon will serialize the structured content and add it by itself.
Tools have to provide an output schema for validation of structured results if it is using structured content.

To add an output schema to the tool, use the `@Mcp.ToolOutputSchema` or `@Mcp.ToolOutputSchemaText` annotations:

- **`@Mcp.ToolOutputSchema`**: Defines the output schema using a class (POJO). Helidon will generate the JSON schema from the class.
- **`@Mcp.ToolOutputSchemaText`**: Defines the output schema using a literal JSON schema string.

```java
@Mcp.Tool("Tool returns a structured content")
@Mcp.ToolOutputSchema(Foo.class)
McpToolResult toolWithSchema(McpToolRequest request) {
    return McpToolResult.builder()
            .structuredContent(new Foo("bar"))
            .build();
}

@Mcp.Tool("Tool returns a structured content with text schema")
@Mcp.ToolOutputSchemaText("{\"type\": \"object\", \"properties\": {\"value\": {\"type\": \"string\"}}}")
McpToolResult toolWithTextSchema(McpToolRequest request) {
    return McpToolResult.builder()
                .structuredContent(new Foo("bar"))
            .build();
}

@JsonSchema.Schema
public record Foo(String bar) {
}
```

#### JSON Schema

Use JSON Schema to validate and describe input parameters and their structure. You can define schemas via the `@JsonSchema.Schema`
annotation. The complete documentation is available on [Helidon documentation](https://helidon.io/docs/v4/se/json/schema).

#### Tool Result

Helidon supports six types of tool result content:

- **Text**: Text content with the default `text/plain` media type.
- **Image**: Image content with a custom media type.
- **Audio**: Audio content with a custom media type.
- **Resource links**: A reference to a resource.
- **Text Resource**: Text resource content with the default `text/plain` media type.
- **Binary Resource**: Binary resource content with a custom media type.

Use the `McpToolResult` builder to create tool contents:

```java
McpToolResult result = McpToolResult.builder()
        .addTextContent("text")
        .addImageContent(pngImageBytes(), MediaTypes.create("image/png"))
        .addAudioContent(wavAudioBytes(), MediaTypes.create("audio/wav"))
        .addResourceLinkContent("name", "https://foo")
        .addTextResourceContent("text")
        .addBinaryResourceContent(gzipBytes(), MediaTypes.create("application/gzip"))
        .build();
```

### Prompt

`Prompts` allow servers to provide structured messages and instructions for interacting with language models. They guide MCP 
usage and help the LLM produce accurate responses. Create prompts with `@Mcp.Prompt` annotation. Use `@Mcp.Name` to override 
the method’s name and `@Mcp.Role` to specify the speaker for text-only prompts.

```java
@Mcp.Server
class Server {

    @Mcp.Prompt("Echo Prompt")
    McpPromptResult echoPrompt(String argument) {
        return McpPromptResult.create(argument);
    }
}
```

#### Configuration

- **`@Mcp.Name`**: Custom prompt name identifier
- **`@Mcp.Role`**: Specifies the role for prompt content
- **`@Mcp.Description`**: Documents individual argument

```java
@Mcp.Server
class Server {

    @Mcp.Prompt("Prompt description")
    @Mcp.Name("MyPrompt")
    @Mcp.Role(McpRole.USER)
    String echoPrompt(@Mcp.Description("Argument description") String argument) {
        return argument;
    }
}
```

#### Prompt Result

Five prompt result content types can be created:

- **Text**: Text content with a default `text/plain` media type.
- **Image**: Image content with a custom media type.
- **Audio**: Audio content with a custom media type.
- **Text Resource**: Text resource content with a default `text/plain` media type.
- **Binary Resource**: Binary resource content with a custom media type.

Create prompt content with the `McpPromptResult` builder:

```java
McpPromptResult result = McpPromptResult.builder()
        .addTextContent("text")
        .addImageContent(pngImageBytes(), MediaTypes.create("image/png"))
        .addAudioContent(wavAudioBytes(), MediaTypes.create("audio/wav"))
        .addTextResourceContent("text")
        .addBinaryResourceContent(gzipBytes(), MediaTypes.create("application/gzip"))
        .build();
```

### Resource

`Resources` allow servers to share data that provides context to language models, such as files, database schemas, or 
application-specific information. Clients can list and read them. Resources are identified by name, description, and media type.
Define resources using `@Mcp.Resource`.

```java
@Mcp.Server
class Server {

    @Mcp.Resource(
            uri = "file://path",
            description = "Resource description",
            mediaType = MediaTypes.TEXT_PLAIN_VALUE)
    McpResourceResult resource() {
        return McpResourceResult.create("text");
    }
}
```

#### Configuration

Use `String` return types for text-only resources. The `@Mcp.Name` annotation lets you override the default resource name.
You can inject `McpResourceRequest` as a parameter to your resource method.

```java
@Mcp.Server
class Server {
    
    @Mcp.Resource(
            uri = "file://path",
            description = "Resource description",
            mediaType = MediaTypes.TEXT_PLAIN_VALUE)
    @Mcp.Name("MyResource")
    String resource(McpResourceRequest request) {
        return "text";
    }
}
```

### Resource Templates

Resource Templates utilize [URI templates](https://datatracker.ietf.org/doc/html/rfc6570) to facilitate dynamic resource discovery.
The URI template is matched against the corresponding URI in the client request. To define a resource or template, the same
API as `McpResource` is employed. Parameters enclosed in `{}` denote template variables, which can be accessed via `McpParameters`
using keys that correspond to these variables.

#### Configuration

Use `String` return types for text-only resources. The `@Mcp.Name` annotation lets you override the default resource name.

```java
@Mcp.Server
class Server {
    
    @Mcp.Resource(
            uri = "file://{path}",
            description = "Resource description",
            mediaType = MediaTypes.TEXT_PLAIN_VALUE)
    @Mcp.Name("MyResource")
    String resource(String path) {
        return "File at path " + path + " does not exist.";
    }
}
```

#### Resource Result

Helidon supports two resource result content types:

- **Text**: Text content with a default `text/plain` media type.
- **Binary**: Binary content with a custom media type.

Create resource content with the `McpResourceResult` builder:

```java
McpResourceResult text = McpResourceResult.create("data");
McpResourceResult binary = McpResourceResult.builder()
        .addBinaryContent(data, MediaTypes.APPLICATION_JSON)
        .build();
```

### Resource Subscribers

MCP clients can subscribe and get notified when the content of a resource is updated. 
For example, if a resource represents a database table, a client can get notified when 
a new row is added and re-read the resource to get the latest data. If a client is no
longer interested in receiving update notifications, it can issue an unsubscribe
request.

Generally, the MCP server processes subscribe and unsubscribe requests without
any user-provided code executed on the server side. Clients subscribe 
and unsubscribe (within the same session) using the resource URI, and updates 
are propagated to all active subscribers in all sessions.
Helidon MCP supports server-side subscribers and unsubscribers when custom logic must
be executed on the server to handle those events (for example, a subscription may
start a thread to monitor database updates and stop it when the unsubscription
arrives).

The following example shows our resource example together with a server-side
subscriber and unsubscriber:

```java
@Mcp.Server
@Mcp.Path("/subscribers")
class McpSubscribersServer {

    @Mcp.Resource(
            uri = "http://dbtable",
            mediaType = MediaTypes.TEXT_PLAIN_VALUE,
            description = "table data")
    String resource() {
        return dbTableDataAsCsv();
    }

    @Mcp.ResourceSubscriber("http://dbtable")
    void subscribe(McpSubscribeRequest request) {
        startDbTableMonitor();
    }

    @Mcp.ResourceUnsubscriber("http://dbtable")
    void unsubscribe(McpUnsubscribeRequest request) {
        stopDbTableMonitor();
    }
}
```

MCP subscriptions are available via the injectable _features_ instance and can 
send notifications manually:

```java
@Mcp.ResourceSubscriber("http://dbtable")
void subscribe(McpSubscribeRequest request, McpFeatures features) {
    if (wasUpdated()) {
        features.subscriptions().sendUpdate("http://dbtable");
    }
}
```

MCP clients automatically issue a resource read every time an update notification
arrives.

### Completion

The `Completion` feature offers auto-suggestions for prompt arguments or resource template parameters, making the server easier
to use and explore. Bind completions to prompts (by name) or resource templates (by URI) using `@Mcp.Completion`. You can inject 
`McpCompletionRequest` as a parameter to your completion method. It extends `McpRequest` and provides `name()` and `value()` 
methods to get the argument name and its current value.

```java
@Mcp.Server
class Server {

    @Mcp.Completion("create")
    McpCompletionResult completionPromptArgument(McpCompletionRequest request) {
        return McpCompletionResult.create(request.value() + ".");
    }
}
```

The default type of completion is prompt. When creating a completion for a resource template, use the 
`type` property in the `McpCompletion` annotation as shown next:

```java
@Mcp.Server
class Server {

    @Mcp.Completion(value = "resource/{path1}", type = McpCompletionType.RESOURCE)
    McpCompletionResult completionPromptArgument(McpCompletionRequest request) {
        return McpCompletionResult.create(request.value() + ".");
    }
}
```

#### Configuration

You can also define completion methods with `String` parameter(s) that return `List<String>`:

```java
@Mcp.Server
class Server {

    @Mcp.Completion("http://path")
    List<String> completionPromptArgument(String value) {
        return List.of(value + ".");
    }
}
```

#### Completion Result

Completion content results in a list of suggestions:

```java
McpCompletionResult result = McpCompletionResult.create("suggestion1", "suggestion2");
```

## MCP Parameters

Client parameters are available in `McpTool`, `McpPrompt`, and `McpCompletion` business logic via the `McpParameters` API.
This class provides a flexible way to access and convert parameters from the client request.

### Basic Usage

You can access parameters by their key and convert them to various types.

```java
void process(McpToolRequest request) {
    McpParameters parameters = request.parameters();

    // Access nested parameters
    McpParameters address = parameters.get("address");

    // Convert to primitive types
    String name = parameters.get("name").asString().orElse("defaultName");
    int age = parameters.get("age").asInteger().orElse(18);
    boolean authorized = parameters.get("authorized").asBoolean().orElse(false);

    // Convert to a list of strings
    List<String> roles = parameters.get("roles").asList(String.class).orElse(List.of());

    // Convert to a custom POJO
    Address homeAddress = address.as(Address.class).orElseThrow();
}
```

### Checking for Presence

You can check if a parameter is present or empty.

```java
void parameters(McpRequest request) {
    McpParameters param = request.parameters().get("optionalParam");
    if (param.isPresent()) {
        // ...
    }
    if (param.isEmpty()) {
        // ...
    }
}
```

### Advanced Conversions

The `McpParameters` API also supports more advanced conversions.

```java
void convert(McpToolRequest request) {
    // Convert to a list of McpParameters to iterate over a JSON array
    List<McpParameters> items = request.parameters().get("items").asList().get();
    for (McpParameters item : items) {
        String itemName = item.get("name").asString().get();
        double itemPrice = item.get("price").asDouble().get();
    }

    // Convert to a map
    Map<String, McpParameters> metadata = request.parameters().get("metadata").asMap().get();
}
```

## McpRequest

The `McpRequest` object is the base interface for every request, providing access to client-side data and features.

- **`parameters()`**: Returns `McpParameters` for accessing client-provided parameters.
- **`meta()`**: Returns `McpParameters` for accessing client-provided metadata.
- **`features()`**: Returns `McpFeatures` for accessing advanced features such as logging, progress, cancellation, elicitation, sampling, and roots.
- **`protocolVersion()`**: Returns the negotiated protocol version between the server and the client.
- **`sessionContext()`**: Returns a `Context` for session-scoped data.
- **`requestContext()`**: Returns a `Context` for request-scoped data.

## Context Management

The `McpRequest` provides access to two types of context:

- **Session Context**: Used to store data that persists throughout the duration of the client's session.
- **Request Context**: Used to store data specific to the current request.

You can access these by adding `McpRequest` as a parameter to your tool or prompt method.

```java
@Mcp.Tool("Tool with state")
McpToolResult toolWithState(McpRequest request) {
    Context sessionContext = request.sessionContext();
    int callCount = sessionContext.get("callCount", Integer.class).orElse(0);
    sessionContext.register("callCount", ++callCount);
    
    return McpToolResult.create("This tool has been called " + callCount + " times in this session.");
}
```

## Features

Helidon provides additional server-side features—accessible through `McpFeatures` via `McpRequest`.

### Logging

Traditional Java logging may not be visible to AI clients. Helidon’s `Logging` feature bridges this gap by sending log messages 
directly to clients:

```java
@Mcp.Server
class Server {

    @Mcp.Tool("Tool description")
    McpToolResult getLocationCoordinates(McpFeatures features) {
        McpLogger logger = features.logger();

        logger.info("Logging info");
        logger.debug("Debugging info");
        logger.notice("Notice info");
        logger.warn("Warning");
        logger.error("Error message");
        logger.critical("Critical issue");
        logger.alert("Alert message");

        return McpToolResult.create();
    }
}
```

### Progress

For long-running tasks, clients can request progress updates. Use `McpProgress` to send updates to clients manually:

```java
@Mcp.Server
class Server {

    @Mcp.Tool("Tool description")
    McpToolResult getLocationCoordinates(McpFeatures features) {
        McpProgress progress = features.progress();
        progress.total(100);
        for (int i = 1; i <= 10; i++) {
            longRunningTask();
            progress.send(i * 10);
        }
        return McpToolResult.create();
    }
}
```

### Pagination

Pagination enables the server to return results in smaller, manageable chunks instead of delivering the entire dataset at once.
In MCP servers, pagination is applied when clients request lists of components, such as tools. The page size can be configured 
using the following set of annotations:

```java
@Mcp.Server
@Mcp.ToolsPageSize(1)
@Mcp.PromptsPageSize(1)
@Mcp.ResourcesPageSize(1)
@Mcp.ResourceTemplatesPageSize(1)
class Server {
}
```

### Cancellation

The MCP Cancellation feature enables verification of whether a client has issued a cancellation request. Such requests are
typically made when a process is taking an extended amount of time, and the client opts not to wait for the completion of
the operation. Cancellation status can be accessed from the `McpFeatures` class or directly as method parameter.

#### Example

Example of a Tool checking for cancellation request.

```java
@Mcp.Tool("Cancellation Tool")
McpToolResult cancellationTool(McpCancellation cancellation) {
    long now = System.currentTimeMillis();
    long timeout = now + TimeUnit.SECONDS.toMillis(5);

    while (now < timeout) {
        if (cancellation.verify().isRequested()) {
            String reason = cancellation.verify().reason();
            return McpToolResult.create(reason);
        }
        longRunningOperation();
        now = System.currentTimeMillis();
    }
    return McpToolResult.create();
}
```

### Elicitation

See the full [elicitation documentation details](../mcp/README.md#elicitation)

#### Example

```java
@Mcp.Tool("Collect additional user input from the connected client.")
McpToolResult elicitationTool(McpElicitation elicitation) {
    if (!elicitation.enabled()) {
        return McpToolResult.builder()
                .error(true)
                .addTextContent("Elicitation is not supported by the connected client")
                .build();
    }

    String schema = "{\"type\":\"object\",\"properties\":{\"email\":{\"type\":\"string\"}}}";

    try {
        McpElicitationResponse response = elicitation.request(req -> req
                .message("Please provide your email address.")
                .schema(schema)
                .timeout(Duration.ofSeconds(30)));

        if (response.action() != McpElicitationAction.ACCEPT) {
            return McpToolResult.create("User did not provide the requested input.");
        }

        String email = response.content()
                .map(content -> content.get("email").asString().orElse("unknown"))
                .orElse("unknown");
        return McpToolResult.create("Captured email: " + email);
    } catch (McpElicitationException e) {
        return McpToolResult.builder()
                .error(true)
                .addTextContent(e.getMessage())
                .build();
    }
}
```

### Sampling

See the full [sampling documentation details](../mcp/README.md#sampling)

#### Example

Below is an example of a tool that uses the Sampling feature. `McpSampling` object can be used as method parameter.

```java
@Mcp.Tool("Uses MCP Sampling to ask the connected client model.")
McpToolResult samplingTool(McpSampling sampling) {
    if (!sampling.enabled()) {
        return McpToolResult.builder()
                .error(true)
                .addTextContent("This tool requires sampling feature")
                .build();
    }

    try {
        McpSamplingResponse response = sampling.request(req -> req
                .timeout(Duration.ofSeconds(10))
                .systemPrompt("You are a concise, helpful assistant.")
                .addTextMessage(McpSamplingTextMessage.builder().text("Write a 3-line summary of Helidon MCP Sampling.").role(McpRole.USER).build()));
        return McpToolResult.builder()
                .addTextContent(response.asTextMessage().text())
                .build();
    } catch (McpSamplingException e) {
        return McpToolResult.builder()
                .error(true)
                .addTextContent(e.getMessage())
                .build();
    }
}
```

### Roots

See the full [roots documentation details](../mcp/README.md#roots)

#### Example

Below is an example of a tool that uses the Roots feature. `McpRoots` object can be used as method parameter.

```java
@Mcp.Tool("Request MCP Roots to the connected client.")
McpToolResult rootsTool(McpRoots mcpRoots) {
    if (!mcpRoots.enabled()) {
        return McpToolResult.builder()
                .error(true)
                .addTextContent("Roots are not supported by the client")
                .build();
    }
    List<McpRoot> roots = mcpRoots.listRoots();
    McpRoot root = roots.getFirst();
    URI uri = root.uri();
    String name = root.name().orElse("Unknown");
    return McpToolResult.create("Server updated roots");
}
```

## References

- [MCP Specification](https://modelcontextprotocol.io/introduction)
- [JSON Schema Specification](https://json-schema.org)
