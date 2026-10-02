<!--@frontmatter
description: "API for server-side MCP"
-->
# Helidon MCP

## Overview

The Model Context Protocol (MCP) defines a standardized way for LLMs (Large Language Models) to interact with internal and 
external data sources. MCP uses a client-server architecture in which clients (typically AI agents) initiate communication and 
servers expose capabilities for data access, retrieval, and interaction. Helidon provides MCP server-side support that can be 
consumed by any client implementing the MCP specification.

## Maven Coordinates

To create your first MCP server using Helidon, add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.helidon.extensions.mcp</groupId>
    <artifactId>helidon4-extensions-mcp-server</artifactId>
</dependency>
```

## Usage

This section describes how to create and configure core MCP components in Helidon.

### MCP Server

Servers provide the primary integration point for adding context to language models through MCP. A server is exposed through a 
configurable HTTP endpoint, manages client connections, and provides the capabilities described in later sections. In Helidon, 
an MCP server is implemented as an `HttpFeature` and registered in web server routing. To host multiple MCP servers, register 
multiple `McpServerFeature` instances with distinct paths. Each path serves as a separate entry point for MCP clients. Use the 
`McpServerFeature` builder to register tools, prompts, resources, and other MCP components. Server name and version are shared 
with clients during initialization, and Helidon does not enforce naming or versioning conventions.

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
                    .description("Provides tools and resources for Example")
                    .websiteUrl("https://example.com/mcp")
                    .build()));
    }
}
```

#### Stateless mode

By default, MCP servers run in stateful mode (`stateless = false`). In this mode, clients initialize first and then continue
using the established server session.

When stateless mode is enabled, initialization is optional and clients can call MCP methods directly (for example, `tools/list`).
The server does not keep request-to-request MCP session state.

This has two important consequences:

- Data stored in the session's context does not persist across independent requests.
- Client capabilities are normally negotiated during initialization; if a client skips this phase, capability-dependent
  features will be unavailable.

Enable stateless mode with configuration:

```yaml
mcp:
  server:
    stateless: true
```

Or with the server builder:

```java
McpServerFeature.builder()
        .stateless(true)
        .build();
```

#### OAuth protected resource metadata

An MCP server that uses OAuth must publish
[OAuth 2.0 Protected Resource Metadata](https://www.rfc-editor.org/rfc/rfc9728.html). Configure at least one authorization
server issuer. The issuer must exactly match the `issuer` value in the authorization server's discovery document; it is not
an authorization, token, or discovery endpoint. Scopes are optional.

```yaml
mcp:
  server:
    path: "/mcp"
    protected-resource-metadata:
      authorization-servers:
        - "https://login.example.com/tenant"
      scopes-supported:
        - "mcp:tools"
```

```java
McpServerFeature.builder()
        .path("/mcp")
        .protectedResourceMetadata(metadata -> metadata
                .addAuthorizationServer(URI.create("https://login.example.com/tenant"))
                .addScope("mcp:tools"))
        .build();
```

When `resource` is omitted, Helidon derives it for each request from the trusted requested URI's scheme and authority plus the
configured MCP server path. For example, a request through `https://mcp.example.com` produces the resource
`https://mcp.example.com/mcp`. Helidon's requested URI honors forwarded request information only from configured trusted
proxies. Helidon uses the MCP server path for the well-known route and, in this example, serves metadata at
`/.well-known/oauth-protected-resource/mcp`. MCP clients use this endpoint when a `401 Unauthorized` response does not include
a `WWW-Authenticate` `resource_metadata` parameter. Keep the metadata endpoint unauthenticated and ensure a reverse proxy
forwards its public scheme and authority correctly.

Configure `resource` explicitly when the canonical, externally visible resource cannot be derived from the request origin and
the MCP server path, for example when a reverse proxy rewrites the public MCP path. This also applies when the canonical URI
spells a default port but the proxy reports it only through `X-Forwarded-Port`, which does not indicate whether the client
included that port explicitly. An explicit resource is also required when the MCP server path is a routing pattern or when a
query component is part of the resource identifier:

```yaml
mcp:
  server:
    path: "/mcp"
    protected-resource-metadata:
      resource: "https://mcp.example.com/api/mcp"
      authorization-servers:
        - "https://login.example.com/tenant"
```

An explicit `resource` always takes precedence over request-based derivation. By default, Helidon derives the well-known route
from the explicit resource URI path, so this example uses `/.well-known/oauth-protected-resource/api/mcp`.

When multiple canonical resources with the same path are routed through one Helidon server, configure a distinct local
`metadata-path` for each and have the reverse proxy map each canonical well-known URL, including its query, to its local path.
The override affects only local routing; it does not change an explicit or derived `resource`. In particular, setting only
`metadata-path` does not make that path the protected resource URI. Local metadata paths on the same HTTP routing must be
unique and must not match the MCP endpoint or `/.well-known/openid-configuration`.

If `protected-resource-metadata` is omitted, Helidon does not publish the endpoint. Within the block,
`authorization-servers` is required and `scopes-supported` is optional. In production, use HTTPS for both the resource and
issuer URIs. An HTTP resource URI is accepted for localhost or loopback development. An HTTP authorization server URI is
accepted only when both it and the protected resource use localhost or loopback literal hosts.

### Icons

Server's configuration, tools, prompts, resources, and resource templates implement `McpIcons` and can expose one or more icons to clients using the
[MCP `2025-11-25` protocol version](https://modelcontextprotocol.io/specification/2025-11-25).
Older negotiated protocol versions omit this metadata.

Create an icon and add it to the server or any of its component builders:

```java
McpIcon icon = McpIcon.builder()
        .source("https://example.com/icon.svg")
        .mediaType(MediaTypes.create("image/svg+xml"))
        .addSize("48x48")
        .addSize("96x96")
        .theme(McpIconTheme.DARK)
        .build();

McpServerFeature.builder()
        .addIcon(icon)
        .addTool(tool -> tool
                .name("tool")
                .description("Tool description")
                .schema("")
                .addIcon(icon)
                .tool(request -> McpToolResult.create("result")));
```

The source can be an HTTP(S) URL or a `data:` URI with a Base64-encoded payload. Sizes use the `WxH` format or `any`.
Leave the theme unspecified when the icon is suitable for both light and dark backgrounds.

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

Structured content is serialized using Helidon JSON binding. Custom classes must be annotated with `@Json.Entity` and
compiled with the Helidon JSON annotation processor. JSON-B annotations and unregistered POJOs are not supported.

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

You can also use shortcut methods that accept only required parameters:

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

Create prompt content with the `McpPromptResult` builder:

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

You can also use shortcut methods that accept only required parameters:

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

Create resource content with the `McpResourceResult` builder:

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

You can also use shortcut methods:

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
any user-provided code executed on the server side. Clients subscribe
and unsubscribe (within the same session) using the resource URI, and updates
are propagated to all active subscribers in all sessions.
Helidon MCP supports server-side subscribers and unsubscribers when custom logic must
be executed on the server to handle those events.

#### Interface

Implement the `McpResourceSubscriber` interface and register it via `addResourceSubscriber`. Interfaces for 
subscribers and unsubscribers are similar:

- `McpResourceSubscriber` – server-side hook invoked when a client subscribes. The `subscribe` method receives an `McpSubscribeRequest`.
- `McpResourceUnsubscriber` – server-side hook invoked when a client unsubscribes. The `unsubscribe` method receives an `McpUnsubscribeRequest`.

The following section focuses on subscribers.

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
send notifications manually:

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
- **`metadata()`**: Returns optional `McpParameters` for accessing client-provided protocol metadata. Use
  `McpParameters.create(Object)` to create metadata from maps or custom Helidon JSON types.
- **`features()`**: Returns `McpFeatures` for accessing advanced features such as logging, progress, cancellation, elicitation, sampling, and roots.
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

This capability is useful for maintaining state between multiple tool calls or prompts within the same session.

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

Custom parameter types are deserialized using Helidon JSON binding. Annotate them with `@Json.Entity` and enable the
Helidon JSON annotation processor so a converter is generated:

```java
@Json.Entity
record Address(String street, String city) {
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
- `reason()` – optional cancellation reason provided by the client

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
            McpCancellationResult result = cancellation.result();
            if (result.isRequested()) {
                String reason = result.reason().orElse("Cancellation requested");
                return McpToolResult.create(reason);
            }
            longRunningOperation();
            now = System.currentTimeMillis();
        }
        return McpToolResult.create("text");
    }
}
```

### Elicitation

The `Elicitation` feature allows a server to request additional user interaction through the connected MCP client during request
processing. Form mode collects non-sensitive structured data in the client. URL mode directs the user to an external URL for
sensitive or out-of-band interactions such as third-party authorization or payment processing.

Elicitation support is optional on the client side, and clients negotiate form and URL modes separately. Use `enabled()` to check
form support and `enabledUrl()` to check URL support.

#### Form mode

Always verify form support before sending a form request:

```java
McpElicitation elicitation = request.features().elicitation();
if (!elicitation.enabled()) {
}
```

A form elicitation request includes:

- `message` – prompt shown to the user by the client
- `schema` – JSON Schema describing the expected response payload
- `timeout` – optional timeout (defaults to 5 minutes)

Client responses include:

- `action()` – `ACCEPT`, `DECLINE`, or `CANCEL`
- `content()` – response payload as `McpParameters` (present when accepted)

Servers must not request sensitive information such as passwords, API keys, access tokens, or payment credentials through form
mode. Use URL mode for these interactions.

Starting with MCP protocol version `2025-11-25`, enum fields support all five schema forms defined by the
[MCP specification](https://modelcontextprotocol.io/specification/2025-11-25/schema#enumschema):

- legacy titled single-select with `enum` and `enumNames` for backward compatibility
- untitled single-select with `enum`
- titled single-select with `oneOf` entries containing `const` and `title`
- untitled multi-select with `items.type` and `items.enum`
- titled multi-select with `items.anyOf` entries containing `const` and `title`

Prefer the standards-based titled single-select form over the legacy `enumNames` form.

Multi-select responses contain lists of strings. For example, a titled multi-select property can be requested and read as follows:

```java
String schema = """
        {
          "type": "object",
          "properties": {
            "colors": {
              "type": "array",
              "minItems": 1,
              "maxItems": 2,
              "items": {
                "anyOf": [
                  {"const": "red", "title": "Red"},
                  {"const": "blue", "title": "Blue"}
                ]
              },
              "default": ["red"]
            }
          }
        }
        """;

McpElicitationResponse response = elicitation.request(req -> req
        .message("Choose colors")
        .schema(schema));
List<String> colors = response.content()
        .map(content -> content.get("colors").asList(String.class).orElse(List.of()))
        .orElse(List.of());
```

##### Example

```java
class ElicitationTool implements McpTool {
    @Override
    public String name() {
        return "elicitation-tool";
    }

    @Override
    public String description() {
        return "Collects additional user input using MCP elicitation.";
    }

    @Override
    public String schema() {
        return "";
    }

    @Override
    public McpToolResult tool(McpToolRequest request) {
        McpElicitation elicitation = request.features().elicitation();
        if (!elicitation.enabled()) {
            return McpToolResult.builder()
                    .error(true)
                    .addTextContent("Elicitation is not supported by the connected client")
                    .build();
        }

        String userSchema = """
                {
                  "type": "object",
                  "properties": {
                    "email": {"type": "string"}
                  }
                }
                """;

        try {
            McpElicitationResponse response = elicitation.request(req -> req
                    .message("Please provide your email address.")
                    .schema(userSchema)
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
}
```

#### URL mode

URL mode is available with MCP protocol version `2025-11-25`. A URL elicitation request includes:

- `message` – explanation shown to the user by the client
- `elicitationId` – identifier that is unique within the server and opaque to the client
- `url` – valid absolute `URI` the user should navigate to
- `timeout` – optional timeout for the client's response (defaults to 5 minutes)

Before sending a URL request, verify URL support independently:

```java
McpElicitation elicitation = request.features().elicitation();
if (!elicitation.enabledUrl()) {
}
```

The URL must not contain sensitive user information or grant pre-authenticated access to a protected resource. Use HTTPS outside
development environments, and bind the external interaction to the authenticated user on the server.

```java
String elicitationId = UUID.randomUUID().toString();
McpElicitationResponse response = elicitation.requestUrl(req -> req
        .message("Authorize access to the payment service.")
        .elicitationId(elicitationId)
        .url(URI.create("https://payments.example.com/authorize?elicitationId=" + elicitationId))
        .timeout(Duration.ofSeconds(30)));

if (response.action() == McpElicitationAction.ACCEPT) {
    // The user consented to open the URL. Verify completion out of band.
}
```

An `ACCEPT` response indicates consent to the URL interaction, not completion of the external flow. URL responses do not contain
form content.

When the original request cannot continue until one or more external interactions have completed, return the URL prerequisites
to the client by throwing `McpUrlElicitationRequiredException` instead of sending a nested elicitation request:

```java
throw new McpUrlElicitationRequiredException(McpElicitationUrlRequest.builder()
        .message("Authorize access to the payment service.")
        .elicitationId(elicitationId)
        .url(URI.create("https://payments.example.com/authorize?elicitationId=" + elicitationId))
        .build());
```

This ends the original request with JSON-RPC error code `-32042` and includes the URL requests in
`error.data.elicitations`. The client can present those URLs and retry the original request after the external interactions have
completed. This error mechanism is available only with MCP protocol version `2025-11-25`.

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

Sampling context inclusion is negotiated separately. `McpSamplingRequest.usesContext()` reports whether `includeContext` is
set to `THIS_SERVER` or `ALL_SERVERS`. Before sending such a request, verify that the client also supports context inclusion
using `enabledContext()`:

```java
if (!sampling.enabledContext()) {
    // The client supports sampling, but not sampling context inclusion.
}
```

Tool-enabled sampling is also negotiated separately. Before adding tool names or a tool choice to a sampling request, verify that
the client supports them using `enabledTools()`:

```java
if (!sampling.enabledTools()) {
    // The client supports basic sampling, but not tool-enabled sampling.
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
                .metadata(Map.of("requestId", "example-request"))
                .intelligencePriority(0.1)
                .systemPrompt("system prompt")
                .timeout(Duration.ofSeconds(10))
                .stopSequences(List.of("stop1"))
                .includeContext(McpIncludeContext.NONE)
                .addTextMessage(McpRole.USER, "text")
                .build();
```

`McpIncludeContext.NONE` does not require context support. If `THIS_SERVER` or `ALL_SERVERS` is requested when
`enabledContext()` is `false`, the `request` method throws an `McpSamplingException`.

Sampling request metadata is serialized using Helidon JSON binding. This value uses the wire `metadata` field and the
`McpSamplingRequest.metadata()` option, which accepts `Object`; maps, collections, arrays, primitives, and converter-backed
custom classes are supported. This is separate from the protocol `_meta` field exposed through `metadata()`. See
[Migrate from JSON-B to Helidon JSON Binding](./upgrade/upgrade_guide_1.2.md#migrate-from-json-b-to-helidon-json-binding) for the
compatibility impact and migration steps.

Once your request is built, send it using the sampling feature.

#### Sampling data model

Each sampling message is an envelope with one role and an ordered collection of content blocks. A content block can contain
text, image, audio, a tool use, or a tool result. Use `McpSamplingMessage.builder()` to create an envelope explicitly, then add
it to the ordered `McpSamplingRequest.messages()` collection with `addMessage`. For a single text-content message, the request
builder also provides `addTextMessage(String)` and `addTextMessage(McpRole, String)` convenience methods.

- **Text**: Text message content.
- **Image**: Image message content with a custom media type.
- **Audio**: Audio message content with a custom media type.
- **Tool use**: A client-model request to invoke a named sampling tool with structured input.
- **Tool result**: The corresponding server result, represented by an `McpToolResult`.

Text, image, and audio blocks can also carry `McpAnnotations` for audience, priority, and last-modified metadata. Every
sampling content block and sampling message envelope supports its own wire `_meta` value through `metadata(...)` and
`metadata()`. The public tool-content types, such as `McpToolTextContent` and `McpToolImageContent`, expose the same
annotations and metadata options. These builders accept `McpParameters`; use `McpParameters.create(Object)` to serialize maps
or converter-backed custom types with Helidon JSON binding. The supplied value must produce a JSON object.

Create sampling request messages with the `McpSamplingRequest` builder:

Sampling content and metadata support depends on the negotiated protocol version:

- `2024-11-05` supports text and image content blocks.
- `2025-03-26` adds audio content blocks.
- `2025-06-18` adds content `_meta` and `McpAnnotations.lastModified`; message `_meta` remains unsupported.
- `2025-11-25` adds tool-use and tool-result blocks, message `_meta`, and multiple content blocks per message envelope.

For protocol versions before `2025-11-25`, create a separate message envelope for each content block and use only the content
types and metadata fields supported by the negotiated version.

```java
McpSamplingRequest request = McpSamplingRequest.builder()
        .addMessage(McpSamplingMessage.builder()
                            .role(McpRole.USER)
                            .addContent(McpSamplingTextContent.create(
                                    "Explain Helidon MCP in one paragraph."))
                            .addContent(McpSamplingImageContent.builder()
                                                .data(pngBytes)
                                                .mediaType(MediaTypes.create("image/png"))
                                                .build())
                            .addContent(McpSamplingAudioContent.builder()
                                                .data(wavBytes)
                                                .mediaType(MediaTypes.create("audio/wav"))
                                                .build())
                            .build())
        .build();
```

Once your request is built, send it using the sampling feature. The `request` method may throw an `McpSamplingException` if an
error occurs during processing. On success, it returns an `McpSamplingResponse` containing the final response message, the model
used, and optionally a stop reason. `response.message()` returns the message envelope, and `message.contents()` returns its
immutable, ordered content blocks. The convenience methods `asTextContent()`, `asImageContent()`, `asAudioContent()`, and
`asToolUseContent()` access only the first content block and may throw an `McpSamplingException` when the message is empty or its
first block has a different type.

Sampling responses may include a stop reason string. The `rawStopReason()` method preserves the exact value returned by the
client, including provider-specific values. The `stopReason()` method maps recognized standard values to `McpStopReason`
(`END_TURN`, `STOP_SEQUENCE`, `MAX_TOKENS`, or `TOOL_USE`). It returns an empty `Optional` when the client omits the stop reason
or returns a non-standard value. Use `rawStopReason()` to distinguish those cases: it is empty only when the client omitted the
value.

```java
try {
    McpSamplingResponse response = sampling.request(req -> req.addTextMessage("text"));
    for (McpSamplingContent content : response.message().contents()) {
        // Process each response content block.
    }
    response.stopReason().ifPresent(reason -> {
        // Process a recognized standard stop reason.
    });
    response.rawStopReason().ifPresent(reason -> {
        // Process the exact stop reason returned by the client.
    });
} catch (McpSamplingException exception) {
    // Handle error
}
```

#### Sampling with tools

Tool-enabled sampling lets the client model request calls to tools registered with the MCP server. A sampling request selects
the tools available to the model by name; it does not register additional tools. `McpToolChoice.AUTO` lets the model decide
whether to use a tool, `REQUIRED` requires at least one tool call, and `NONE` prevents tool calls.

Here, a tool named `weather` is already registered through `McpServerFeature.builder().addTool(...)`.

```java
McpSamplingRequest request = McpSamplingRequest.builder()
        .addTool("weather")
        .toolChoice(McpToolChoice.AUTO)
        .addTextMessage(McpRole.USER, "What is the weather in Prague?")
        .build();

McpSamplingResponse response = sampling.request(request);
```

Providing `tools` or `toolChoice` when `enabledTools()` is `false` causes `request` to throw an `McpSamplingException`. A
request's `usesTool()` method reports whether it contains tools, a tool choice, or tool content. A tool-enabled response can
contain one or more `McpSamplingToolUseContent` blocks in its assistant message. Helidon automatically invokes each matching
selected tool, appends the complete assistant message and one user message containing all matching
`McpSamplingToolResultContent` blocks, then samples again. Results for parallel tool uses stay together in the same user message
and retain tool-use order. The original request remains unchanged, and `request(...)` returns only the final response that has no
tool-use blocks.

Only registered tools whose names are included in `McpSamplingRequest.tools()` are eligible for automatic invocation. Registered
tools are not offered automatically, and an unregistered request tool name causes `request(...)` to throw an
`McpSamplingException` before sending the sampling request. If the client nevertheless requests a tool that was not offered,
Helidon returns an error tool result so the model can recover. Tool callbacks must return a non-null `McpToolResult`; use
`McpToolResult.create()` for an empty result. An exception from a selected tool is returned as an error tool result. Tool callbacks
run synchronously and sequentially. The request timeout applies independently to each sampling exchange.

Helidon allows ten tool-execution rounds and ten cumulative tool executions by default. Configure both limits with
`mcp.server.max-sampling-tool-iterations` or `McpServerFeature.builder().maxSamplingToolIterations(...)`. The value must be
greater than zero. The limits are shared by all sampling calls made through the originating request's `McpFeatures`, including
sampling calls made by selected tools. Parallel tool calls in one response count as one round, but each call counts as one
execution. Helidon rejects an entire batch that would exceed the remaining execution budget before invoking any callback. After
either limit is reached, Helidon sends one additional request with tool choice `NONE`; another tool-use response then causes an
`McpSamplingException`. A `REQUIRED` choice changes to `AUTO` after its first tool round because the requirement has been
fulfilled.

Build tool results with the type-specific builder methods, such as `addTextContent(...)`, `addImageContent(...)`,
`addAudioContent(...)`, `addTextResourceContent(...)`, `addBinaryResourceContent(...)`, and
`addResourceLinkContent(...)`. Read the corresponding typed lists from `textContents()`, `imageContents()`,
`audioContents()`, `textResourceContents()`, `binaryResourceContents()`, and `resourceLinkContents()`. Content retains its
insertion order within each typed list. On the wire, the lists are grouped in that same order: text, image, audio, text
resource, binary resource, then resource link.

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
            return McpToolResult.create(response.asTextContent().text());
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
        return "Retrieve the list of available roots";
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
    description: "Provides tools and resources for Example"
    website-url: "https://example.com/mcp"
    icons:
      - source: "https://example.com/server.svg"
        media-type: "image/svg+xml"
        sizes: ["any"]
        theme: "DARK"
    path: "/mcp"
    stateless: true
    max-sampling-tool-iterations: 10
```

The optional description, website URL, and icons are included in `serverInfo` when the negotiated protocol version is
`2025-11-25`.

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
- [MCP Authorization](https://modelcontextprotocol.io/specification/2025-11-25/basic/authorization)
- [OAuth 2.0 Protected Resource Metadata](https://www.rfc-editor.org/rfc/rfc9728.html)
- [JSON Schema Specification](https://json-schema.org)
