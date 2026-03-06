# Helidon MCP Extension

Helidon support for the Model Context Protocol (MCP).

## Overview

The Model Context Protocol (MCP) defines a standard communication method that enables LLMs (Large Language Models) to interact 
with both internal and external data sources. More than just a protocol, MCP establishes a connected environment of AI agents 
capable of accessing real-time information. MCP follows a client-server architecture: clients, typically used by AI agents, 
initiate communication, while servers manage access to data sources and provide data retrieval capabilities. Helidon offers 
server-side support and can be accessed by any client that implements the MCP specification.

## Maven Coordinates

To create your first MCP server using Helidon, add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.helidon.extensions.mcp</groupId>
    <artifactId>helidon4-extensions-mcp-server</artifactId>
</dependency>
```

## Usage

This section walks you through creating and configuring various MCP components.

### MCP Server

Servers provide the fundamental building blocks for adding context to language models via MCP. It is accessible through a 
configurable HTTP endpoint. The server manages client connections and provides features described in the following sections. 
The MCP server is implemented as a Helidon `HttpFeature` and registered with the web server's routing. To host multiple servers, 
simply register additional `McpServerFeature` instances with unique paths. Each path acts as a unique entry point for MCP clients. 
Use the `McpServerFeature` builder to register MCP components. The server's name and version are shared with clients during 
connection initialization, but Helidon imposes no constraints on how you manage them.

**Example: Creating an MCP server**

```java
class McpServer {
    public static void main(String[] args) {
        WebServer.builder()
            .routing(routing -> routing.addFeature(
                McpServerFeature.builder()
                    .path("/mcp")
                    .version("0.0.1")
                    .name("MyServer")
                    .build()));
    }
}
```

### Tool

`Tools` enable models to interact with external systems: for example, by querying databases, calling APIs, or performing 
computations. To define a tool, provide a name, description, input schema, and business logic. Use the `addTool` method 
from the `McpServerFeature` builder to register it with the server. The name and description help LLMs understand its purpose. 
The schema, written according to [JSON Schema Specification](https://json-schema.org/specification), defines the expected input 
format. The business logic is implemented in the `tool` method and uses `McpToolRequest` to access inputs. The `McpToolRequest` 
extends `McpRequest` and provides access to the `McpTool` instance via the `tool()` method.

#### Tool Interface

Implement the `McpTool` interface to define a tool.

```java
class MyTool implements McpTool {
    @Override
    public String name() {
        return "MyTool";
    }

    @Override
    public String description() {
        return "Tool description";
    }

    @Override
    public Optional<String> title() {
        return Optional.of("Tool Title");
    }

    @Override
    public String schema() {
        // Schema class is part of the Helidon JSON Schema API.
        // For more details, see: https://helidon.io/docs/v4/se/json/schema
        return Schema.builder()
                .rootObject(root -> root
                        .addStringProperty("name", name -> name.description("Event name").required(true))
                        .addIntegerProperty("productId",
                                            productId -> productId.description("The unique identifier for a product")))
                .build()
                .generate();
    }

    @Override
    public McpToolResult tool(McpToolRequest request) {
        int productId = request.arguments()
                .get("productId")
                .asInteger()
                .orElse(0);
        return McpToolResult.create("productId: " + productId);
    }
}
```

#### Tool request

Client tool invocation request is accessible through `McpToolRequest`:

- `name()` - Access the client tool name requested.
- `arguments()` - Access the client invocation arguments

```java
@Override
public McpToolResult tool(McpToolRequest request) {
    String name = request.name();
    int productId = request.arguments()
            .get("productId")
            .asInteger()
            .orElse(0);
    return McpToolResult.create("productId: " + productId);
}
```

#### Tool Builder

You can also define a `Tool` directly within the server builder:

```java
class McpServer {
    public static void main(String[] args) {
        WebServer.builder()
            .routing(routing -> routing.addFeature(
                McpServerFeature.builder()
                    .addTool(tool -> tool.name("name")
                        .description("description")
                        .schema("schema")
                        .tool(request -> McpToolResult.create("text"))
                        .build())));
    }
}
```

#### Structured content and output schema

Structured content is returned as a JSON object in the `structuredContent` field of a result. For backwards compatibility, 
a tool that returns structured content SHOULD also return the serialized JSON in a `TextContent` block. If there is no content 
added to the `McpToolResult` builder, Helidon will serialize the structured content and add it by itself.
Tools have to provide an output schema for validation of structured results if it is using structured content.

To add an output schema to the tool, implement the `outputSchema` method:
```java
@Override
public Optional<String> outputSchema() {
    // Schema.builder() is part of the Helidon JSON Schema API.
    // For more details, see: https://helidon.io/docs/v4/se/json/schema
    String schema = Schema.builder()
            .rootObject(root -> root
                    .addStringProperty("name", name -> name.description("Event name")
                            .required(true)))
            .build()
            .generate();
    return Optional.of(schema);
}
```

or add it through the builder:
```java
McpServerFeature.builder()
            .addTool(tool -> tool.name("name")
                .description("description")
                .title("Tool Title")
                .schema("schema")
                .outputSchema("outputSchema")
                .tool(request -> McpToolResult.create("text"))
                .build());
```

#### Tool Result

Six types of tool result content can be created:

- **Text**: Text content with the default `text/plain` media type.
- **Image**: Image content with a custom media type.
- **Audio**: Audio content with a custom media type.
- **Resource links**: A reference to a resource that does not have to be registered on the server.
- **Text Resource**: Text resource content with the default `text/plain` media type.
- **Binary Resource**: Binary resource content with a custom media type.

Use the `McpToolResult` builder to create tool contents:

```java
McpToolResult create() {
    return McpToolResult.builder()
            .addTextContent(text -> text.text("text"))
            .addImageContent(image -> image.data(pngImageBytes())
                                           .mediaType(MediaTypes.create("image/png")))
            .addAudioContent(audio -> audio.data(wavAudioBytes())
                                           .mediaType(MediaTypes.create("audio/wav")))
            .addResourceLinkContent(link -> link.size(10)
                                                .name("name")
                                                .title("title")
                                                .uri("https://foo")
                                                .description("description")
                                                .mediaType(MediaTypes.APPLICATION_JSON))
            .addTextResourceContent(resource -> resource.text("text")
                                                        .uri(URI.create("https://foo"))
                                                        .mediaType(MediaTypes.TEXT_PLAIN))
            .addBinaryResourceContent(resource -> resource.data(gzipBytes())
                                                          .uri(URI.create("https://foo"))
                                                          .mediaType(MediaTypes.create("application/gzip")))
            .build();
}
```

Or you can use shortcut methods with required parameters:

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

#### JSON Schema

The JSON Schema defines the required input fields for a tool. It helps the client understand expected input formats and provides 
validation. Define it by returning a JSON string from the `schema()` method.

### Prompts

`Prompts` allow servers to provide structured messages and instructions for interacting with language models. They improve 
instruction quality and help LLMs generate better results. Each instruction is associated with a `Role` (either `assistant` or 
`user`) indicating who is providing the input. When calling a prompt, clients must supply argument values, which are defined with 
names, descriptions, and whether they are required. Use the `McpPromptArgument` builder to define arguments. The `prompt` method 
receives an `McpPromptRequest` which extends `McpRequest` and provides access to the `McpPrompt` instance via the `prompt()` method.

#### Interface

Implement the `McpPrompt` interface and register the prompt using `addPrompt`.

```java
class MyPrompt implements McpPrompt {
    @Override
    public String name() {
        return "MyPrompt";
    }

    @Override
    public String description() {
        return "Prompt description";
    }

    @Override
    public Optional<String> title() {
        return Optional.of("Prompt Title");
    }

    @Override
    public List<McpPromptArgument> arguments() {
        return List.of(McpPromptArgument.builder()
                                       .name("name")
                                       .description("Argument description")
                                       .required(true)
                                       .build());
    }

    @Override
    public McpPromptResult prompt(McpPromptRequest request) {
        return McpPromptResult.create("text");
    }
}
```

#### Prompt request

Client prompt invocation request is accessible through `McpPromptRequest`:

- `name()` - Access the client prompt name requested.
- `arguments()` - Access the client invocation arguments

```java
@Override
public McpPromptResult prompt(McpPromptRequest request) {
    String name = request.name();
    int productId = request.arguments()
            .get("productId")
            .asInteger()
            .orElse(0);
    return McpPromptResult.create("productId: " + productId);
}
```

#### Builder

You can also create a `Prompt` directly via the builder:

```java
class McpServer {
    public static void main(String[] args) {
        WebServer.builder()
            .routing(routing -> routing.addFeature(
                McpServerFeature.builder()
                    .addPrompt(prompt -> prompt.name("name")
                        .description("description")
                        .title("Prompt Title")
                        .addArgument(argument -> argument.name("name")
                            .description("Argument description")
                            .required(true))
                        .prompt(request -> McpPromptResult.create("text"))
                        .build())));
    }
}
```

#### Prompt Result

Five prompt content types can be created:

- **Text**: Text content with a default `text/plain` media type.
- **Image**: Image content with a custom media type.
- **Audio**: Audio content with a custom media type.
- **Text Resource**: Text resource content with a default `text/plain` media type.
- **Binary Resource**: Binary resource content with a custom media type.

`Prompt` content can be created using `McpPromptResult` builder:

```java
McpPromptResult create() {
    return McpPromptResult.builder()
            .addTextContent(text -> text.text("text")
                                        .role(McpRole.ASSISTANT))
            .addImageContent(image -> image.data(pngImageBytes())
                                           .mediaType(MediaTypes.create("image/png"))
                                           .role(McpRole.ASSISTANT))
            .addAudioContent(audio -> audio.data(wavAudioBytes())
                                           .mediaType(MediaTypes.create("audio/wav"))
                                           .role(McpRole.ASSISTANT))
            .addTextResourceContent(resource -> resource.text("text")
                                                        .uri(URI.create("https://example.com"))
                                                        .mediaType(MediaTypes.create("text/plain"))
                                                        .role(McpRole.ASSISTANT))
            .addBinaryResourceContent(resource -> resource.data(pngImageBytes())
                                                                  .uri(URI.create("https://example.com"))
                                                                  .mediaType(MediaTypes.create("text/plain"))
                                                                  .role(McpRole.ASSISTANT))
            .build();
}
```

Or you can use shortcut methods with required parameters:

```java
McpPromptResult result = McpPromptResult.builder()
        .addTextContent("text")
        .addImageContent(pngImageBytes(), MediaTypes.create("image/png"))
        .addAudioContent(wavAudioBytes(), MediaTypes.create("audio/wav"))
        .addTextResourceContent("text")
        .addBinaryResourceContent(gzipBytes(), MediaTypes.create("application/gzip"))
        .build();
```

### Resources

`Resources` allow servers to share data that provides context to language models, such as files, database schemas, or
application-specific information. Clients can list and read resources, which are defined by name, description, and media type.
The `read` method receives an `McpResourceRequest` which extends `McpRequest`.

#### Interface

Implement the `McpResource` interface and register it via `addResource`.

```java
class MyResource implements McpResource {
    @Override
    public String uri() {
        return "https://path";
    }

    @Override
    public String name() {
        return "MyResource";
    }

    @Override
    public String description() {
        return "Resource description";
    }

    @Override
    public Optional<String> title() {
        return Optional.of("Resource title");
    }

    @Override
    public MediaType mediaType() {
        return MediaTypes.TEXT_PLAIN;
    }

    @Override
    public McpResourceResult read(McpResourceRequest request) {
        return McpResourceResult.create(content);
    }
}
```

#### Resource request

Client resource read request is accessible through `McpResourceRequest`:

- `uri()` - Access the client resource `URI` requested.

```java
@Override
public McpResourceResult read(McpResourceRequest request) {
    URI uri = request.uri();
    return McpResourceResult.create(uri.toASCIIString());
}
```

#### Builder

Define a resource in the builder using `addResource`.

```java
class McpServer {
    public static void main(String[] args) {
        WebServer.builder()
            .routing(routing -> routing.addFeature(
                McpServerFeature.builder()
                    .addResource(resource -> resource.name("MyResource")
                        .uri("https://path")
                        .title("Resource Title")
                        .description("Resource description")
                        .mediaType(MediaTypes.TEXT_PLAIN)
                        .resource(request -> McpResourceResult.create("text")))));
    }
}
```

### Resource Templates

Resource Templates utilize [URI templates](https://datatracker.ietf.org/doc/html/rfc6570) to facilitate dynamic resource discovery. 
The URI template is matched against the corresponding URI in the client request. To define a resource or template, the same 
API as `McpResource` is employed. Parameters enclosed in `{}` denote template variables, which can be accessed via `McpParameters` 
using keys that correspond to these variables.

#### Interface

Implement the `McpResource` interface and register it via `addResource`.

```java
class MyResource implements McpResource {
    @Override
    public String uri() {
        return "https://{path}";
    }

    @Override
    public String name() {
        return "MyResourceTemplate";
    }

    @Override
    public String description() {
        return "Resource template description";
    }

    @Override
    public Optional<String> title() {
        return Optional.of("Resource template title");
    }

    @Override
    public MediaType mediaType() {
        return MediaTypes.TEXT_PLAIN;
    }

    @Override
    public McpResourceResult read(McpResourceRequest request) {
        String path = request.parameters()
                .get("path")
                .asString()
                .orElse("Unknown");
        return McpResourceResult.create(path);
    }
}
```

#### Builder

Define a resource in the builder using `addResource`.

```java
class McpServer {
    public static void main(String[] args) {
        WebServer.builder()
            .routing(routing -> routing.addFeature(
                McpServerFeature.builder()
                    .addResource(resource -> resource.name("MyResource")
                        .uri("https://{path}")
                        .description("Resource description")
                        .title("Resource Template Title")
                        .mediaType(MediaTypes.TEXT_PLAIN)
                        .resource(request -> request.parameters().get("path").asString().map(McpResourceResult::create).get())
                        .build())));
    }
}
```

#### Resource Result

Two resource content types can be created:

- **Text**: Text content with `text/plain` media type.
- **Binary**: Binary content with custom media type content.

`Resource` content can be created using `McpResourceResult` builder:

```java
McpResourceResult create() {
    return McpResourceResult.builder()
            .addTextContent(text -> text.text("text")
                                        .mediaType(MediaTypes.TEXT_PLAIN))
            .addBinaryContent(binary -> binary.data(gzipBytes())
                                              .mediaType(MediaTypes.create("application/gzip")))
            .build();
}
```

Or you can use shortcut methods:

```java
McpResourceResult text = McpResourceResult.create("text");
McpResourceResult binary = McpResourceResult.builder()
                                            .addBinaryContent(gzipBytes(), MediaTypes.create("application/gzip"))
                                            .build();
```

### Resource Subscribers

MCP clients can subscribe and get notified when the content of a resource is updated.
If a client is no longer interested in receiving update notifications, it can issue an 
unsubscribe request.

Generally, the MCP server processes subscribe and unsubscribe requests without
any user-provided code executed on the server side. Clients simply subscribe
and unsubscribe (within the same session) using the resource URI and updates
are propagated to all active subscribers in all sessions.
Helidon MCP supports server-side subscribers and unsubscribers in case logic needs
to be executed server side to handle those events. 

#### Interface

Implement the `McpResourceSubscriber` interface and register it via `addResourceSubscriber`. Interfaces for 
subscribers and unsubscribers are similar:

- `McpResourceSubscriber` – server-side hook invoked when a client subscribes. The `subscribe` method receives an `McpSubscribeRequest`.
- `McpResourceUnsubscriber` – server-side hook invoked when a client unsubscribes. The `unsubscribe` method receives an `McpUnsubscribeRequest`.

We focus on subscribers in the next section.

```java
class MyResourceSubscriber implements McpResourceSubscriber {

    private final MyResource resource;

    MyResourceSubscriber(MyResource resource) {
        this.resource = resource;
    }

    @Override
    public String uri() {
        return resource.uri();
    }

    @Override
    public void subscribe(McpSubscribeRequest request) {
        monitorResource(uri());
    }
}
```

MCP subscriptions are available via the features instance and can
send notifications manually as follows:

The API surface for this feature is `McpSubscriptions`.

```java
@Override
public void subscribe(McpSubscribeRequest request) {
    if (wasUpdated()) {
        McpFeatures features = request.features();
        features.subscriptions().sendUpdate(uri());
    }
}
```

#### Builder

Define a resource in the builder using `addResourceSubscriber`.

```java
class McpServer {
    public static void main(String[] args) {
        WebServer.builder()
                .routing(routing -> routing.addFeature(
                    McpServerFeature.builder()
                        .addResourceSubscriber(subscriber ->
                            subscriber.uri("http://myresource")
                                      .subscribe(r -> monitorResource("http://myresource")))));
    }
}
```

### Completion

The `Completion` feature offers auto-suggestions for prompt arguments or resource template parameters, making the server easier 
to use and explore. Each completion is bound to a prompt name or a `URI` template. The `completion` method receives an 
`McpCompletionRequest` which extends `McpRequest` and provides `name()`, `value()` and `context()` methods to get 
the argument name, its current value, and previously resolved arguments. The completion's type, either prompt or 
resource template, is returned by the `referenceType` method.

#### Interface

Implement `McpCompletion` and register with `addCompletion`.

```java
class MyCompletion implements McpCompletion {
    @Override
    public String reference() {
        return "MyPrompt";
    }

    @Override
    public McpCompletionType referenceType() {
        return McpCompletionType.PROMPT;
    }

    @Override
    public McpCompletionResult completion(McpCompletionRequest request) {
        String name = request.name();
        String value = request.value();
        // Context when provided can contain previously resolved variables.
        McpCompletionContext context = request.context().orElse(null);
        Map<String, String> arguments = context.arguments();
        return McpCompletionResult.create("suggestion");
    }
}
```

#### Completion Context

The `McpCompletionContext` provides additional information about the current completion request, specifically 
the values of other arguments that have already been provided by the user. This allows for context-aware 
suggestions (e.g., suggesting a city based on a previously selected country).

- **`arguments()`**: Returns a `Map<String, String>` of previously resolved completion arguments.

#### Builder

Define completions in the server builder:

```java
class McpServer {
    public static void main(String[] args) {
        WebServer.builder()
            .routing(routing -> routing.addFeature(
                McpServerFeature.builder()
                    .addCompletion(completion -> completion
                        .reference("MyPrompt")
                        .completion(request -> McpCompletionResult.create("suggestion"))
                        .build())));
    }
}
```

#### Completion Result

Create the completion result using the list of suggestion.

```java
McpCompletionResult result = McpCompletionResult.create("suggestion1", "suggestion2");
```

Use the builder for specific use cases where the total number of suggestions exceeds 100 items.

```java
McpCompletionResult result = McpCompletionResult.builder()
        .values("suggestion1", "suggestion2")
        .total(3)
        .hasMore(true)
        .build();
```

## McpRequest

The `McpRequest` object is the base interface for every request, providing access to client-side data and features.

- **`parameters()`**: Returns `McpParameters` for accessing client-provided parameters.
- **`meta()`**: Returns `McpParameters` for accessing client-provided metadata.
- **`features()`**: Returns `McpFeatures` for accessing advanced features like logging, sampling, and progress.
- **`protocolVersion()`**: Returns the negotiated protocol version between the server and the client.
- **`sessionContext()`**: Returns a `Context` for session-scoped data.
- **`requestContext()`**: Returns a `Context` for request-scoped data.

```java
@Override
public McpToolResult tool(McpToolRequest request) {
    String protocol = request.protocolVersion();
    // ... use protocol
    return McpToolResult.create("Protocol version: " + protocol);
}
```

### Context Management

The `McpRequest` provides access to two types of context:

- **Session Context**: Used to store data that persists throughout the duration of the client's session.
- **Request Context**: Used to store data specific to the current request.

This is useful for maintaining state between multiple tool calls or prompts within the same session.

```java
@Override
public McpToolResult tool(McpToolRequest request) {
    Context sessionContext = request.sessionContext();
    int callCount = sessionContext.get("callCount", Integer.class).orElse(0);
    sessionContext.register("callCount", ++callCount);
    return McpToolResult.create("This tool has been called " + callCount + " times in this session.");
}
```

## MCP Parameters

Client parameters are available in `McpTool`, `McpPrompt`, and `McpCompletion` business logic via the `McpParameters` API.
This class provides a flexible way to access and convert parameters from the client request.

### Basic Usage

You can access parameters by their key and convert them to various types.

```java
void process(McpRequest request) {
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

## Features

Additional server-side features are available through `McpFeatures`, accessible from `McpRequest`.

### Logging

Instead of using traditional Java logging (which is invisible to AI clients), the MCP server can send logs directly to the client 
using `McpLogger`.

#### Example

```java
class LoggingTool implements McpTool {
    @Override
    public String name() {
        return "LoggingTool";
    }

    @Override
    public String description() {
        return "A tool using logging";
    }

    @Override
    public String schema() {
        return "schema";
    }

    @Override
    public McpToolResult tool(McpToolRequest request) {
        McpLogger logger = request.features().logger();

        logger.info("Logging data");
        logger.debug("Logging data");
        logger.notice("Logging data");
        logger.warn("Logging data");
        logger.error("Logging data");
        logger.critical("Logging data");
        logger.alert("Logging data");

        return McpToolResult.create("text");
    }
}
```

### Progress

For long-running tasks, MCP clients can receive progress updates. Use the `McpProgress` API to send updates manually.

#### Example

```java
class ProgressTool implements McpTool {
    @Override
    public String name() {
        return "ProgressTool";
    }

    @Override
    public String description() {
        return "A tool that uses progress notifications.";
    }

    @Override
    public String schema() {
        return "schema";
    }

    @Override
    public McpToolResult tool(McpToolRequest request) {
        McpProgress progress = request.features().progress();
        progress.total(100);
        for (int i = 1; i <= 10; i++) {
            longRunningTask();
            progress.send(i * 10);
        }
        return McpToolResult.create("text");
    }
}
```

### Pagination

Pagination enables the server to return results in smaller, manageable chunks instead of delivering the entire dataset at once. 
In MCP servers, pagination is automatically applied when clients request lists of components, such as tools. The size of each 
paginated response can be configured using the `*-page-size` property.

```yaml
mcp:
  server:
    tools-page-size: "1"
    prompts-page-size: "1"
    resources-page-size: "1"
    resource-templates-page-size: "1"
```

Or directly on the server configuration builder:

```java
McpServerFeature.builder()
               .toolsPageSize(1)
               .promptsPageSize(1)
               .resourcesPageSize(1)
               .resourceTemplatesPageSize(1)
               .build();
```

### Cancellation

The MCP Cancellation feature enables verification of whether a client has issued a cancellation request. Such requests are 
typically made when a process is taking an extended amount of time, and the client opts not to wait for the completion of 
the operation. Cancellation status can be accessed from the `McpFeatures` class.

The API returns a `McpCancellationResult` which contains:

- `isRequested()` – whether cancellation was requested
- `reason()` – cancellation reason provided by the client

#### Example

Example of a Tool checking for cancellation request.

```java
private class CancellationTool implements McpTool {
    @Override
    public String name() {
        return "cancellation-tool";
    }

    @Override
    public String description() {
        return "Tool running a long process";
    }

    @Override
    public String schema() {
        return "schema";
    }

    @Override
    public McpToolResult tool(McpToolRequest request) {
        long now = System.currentTimeMillis();
        long timeout = now + TimeUnit.SECONDS.toMillis(5);
        McpCancellation cancellation = request.features().cancellation();

        while (now < timeout) {
            if (cancellation.verify().isRequested()) {
                String reason = cancellation.verify().reason();
                return McpToolResult.create(reason);
            }
            longRunningOperation();
            now = System.currentTimeMillis();
        }
        return McpToolResult.create("text");
    }
}
```

### Sampling

The MCP Sampling feature provides a standardized mechanism that allows servers to request LLM sampling operations from language 
models through connected clients. It enables servers to seamlessly integrate AI capabilities into their workflows without 
requiring API keys. Like other MCP features, sampling can be accessed via the MCP request features.
Sampling support is optional for clients, and servers can verify its availability using the `enabled` method:

```java
var sampling = request.features().sampling();
if (!sampling.enabled()) {
}
```

If the client supports sampling, you can send a sampling request using the request method. A builder is provided to configure
and customize the sampling request as needed:

```java
McpSamplingRequest request = McpSamplingRequest.builder()
                .maxTokens(1)
                .temperature(0.1)
                .costPriority(0.1)
                .speedPriority(0.1)
                .hints(List.of("hint1"))
                .metadata(JsonValue.TRUE)
                .intelligencePriority(0.1)
                .systemPrompt("system prompt")
                .timeout(Duration.ofSeconds(10))
                .stopSequences(List.of("stop1"))
                .includeContext(McpIncludeContext.NONE)
                .addTextMessage(McpSamplingTextMessage.builder()
                                                     .text("text")
                                                     .role(McpRole.USER)
                                                     .build())
                .build();
```

Once your request is built, send it using the sampling feature.

#### Sampling data model

Three types of sampling request messages can be created:

- **Text**: Text message content.
- **Image**: Image message content with a custom media type.
- **Audio**: Audio message content with a custom media type.

Sampling request messages can be created using `McpSamplingRequest` builder:

```java
McpSamplingRequest request = McpSamplingRequest.builder()
        .addTextMessage(message -> message.text("Explain Helidon MCP in one paragraph.")
                                          .role(McpRole.USER))
        .addImageMessage(image -> image.data(pngBytes)
                                       .mediaType(MediaTypes.create("image/png"))
                                       .role(McpRole.USER))
        .addAudioMessage(audio -> audio.data(wavBytes)
                                       .mediaType(MediaTypes.create("audio/wav"))
                                       .role(McpRole.USER))
        .build();
```

Once your request is built, send it using the sampling feature. The `request` method may throw an `McpSamplingException` if an
error occurs during processing. On success, it returns an `McpSamplingResponse` containing the response message, the model used,
and optionally a stop reason.

Sampling responses may include a `McpStopReason` (for example `END_TURN`, `STOP_SEQUENCE`, or `MAX_TOKENS`).

```java
try {
    McpSamplingResponse response = sampling.request(req -> req.addTextMessage("text"));
} catch (McpSamplingException exception) {
    // Manage error
}
```
#### Example

Below is an example of a tool that uses the Sampling feature.

```java
class SamplingTool implements McpTool {
    @Override
    public String name() {
        return "sampling-tool";
    }

    @Override
    public String description() {
        return "Uses MCP Sampling to ask the connected client model.";
    }

    @Override
    public String schema() {
        return "";
    }

    @Override
    public McpToolResult tool(McpToolRequest request) {
        var sampling = request.features().sampling();

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
                    .addTextMessage("Write a 3-line summary of Helidon MCP Sampling."));
            return McpToolResult.create(response.asTextMessage().text());
        } catch (McpSamplingException e) {
            return McpToolResult.builder()
                    .error(true)
                    .addTextContent(e.getMessage())
                    .build();
        }
    }
}
```

### Roots

Roots establish the boundaries within the filesystem that define where servers are permitted to operate. They determine which 
directories and files a server can access. Servers can request the current list of roots from compatible clients and receive 
notifications whenever that list is updated.

If a roots-related operation fails, Helidon may throw `McpRootException`.

#### Example

```java
class RootNameTool implements McpTool {
    @Override
    public String name() {
        return "roots-name-tool";
    }

    @Override
    public String description() {
        return "Get the list of roots available";
    }

    @Override
    public String schema() {
        return "";
    }

    @Override
    public McpToolResult tool(McpToolRequest request) {
        McpRoots mcpRoots = request.features().roots();
        if (!mcpRoots.enabled()) {
            return McpToolResult.builder()
                    .addTextContent("Roots are not supported by the client")
                    .error(true)
                    .build();
        }
        List<McpRoot> roots = mcpRoots.listRoots();
        McpRoot root = roots.getFirst();
        URI uri = root.uri();
        String name = root.name().orElse("Unknown");
        return McpToolResult.create("Server updated roots");
    }
}
```

## Configuration

MCP server configuration can be defined using Helidon configuration files. Example in YAML:

```yaml
mcp:
  server:
    name: "MyServer"
    version: "0.0.1"
    path: "/mcp"
```

Register the configuration in code:

```java
class McpServer {
    public static void main(String[] args) {
        Config config = Config.create();

        WebServer.builder()
            .routing(routing -> routing.addFeature(McpServerFeature.builder()
                                                        .config(config.get("mcp.server"))
                                                        .build()));
    }
}
```

## References

- [MCP Specification](https://modelcontextprotocol.io/introduction)
- [JSON Schema Specification](https://json-schema.org)
