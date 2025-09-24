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
Use the`McpServerFeature` builder to register MCP components. The server's name and version are shared with clients during 
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
The schema, written according to [JSON Schema Specification](https://json-schema.org/specification), defines the expected input format. The business logic is 
implemented in the `process` method and uses `McpRequest` to access inputs.

#### Interface

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
    public String schema() {
        return """
                    {
                        "type": "object",
                        "properties": {
                            "productId": {
                                "description": "The unique identifier for a product",
                                "type": "integer"
                            }
                        }
                    }
                    """;
    }

    @Override
    public List<McpToolContent> process(McpRequest request) {
        int productId = request.parameters()
                .get("productId")
                .asInteger()
                .orElse(0);
        return List.of(McpToolContents.textContent("productId: " + productId));
    }
}
```

#### Builder

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
                        .tool(request -> McpToolContents.textContent("text"))
                        .build())));
    }
}
```

#### Tool Content Types

Helidon supports four types of tool content:

- **Text**: Text content with the default `text/plain` media type.
- **Image**: Image content with a custom media type.
- **Audio**: Audio content with a custom media type.
- **Resource**: A reference to an `McpResource` via a URI (must be registered on the server).

Use the `McpToolContents` factory to create tool contents:

```java
McpToolContent text = McpToolContents.textContent("text");
McpToolContent resource = McpToolContents.resourceContent("http://path");
McpToolContent image = McpToolContents.imageContent(pngImageBytes(), MediaTypes.create("image/png"));
McpToolContent audio = McpToolContents.audioContent(wavAudioBytes(), MediaTypes.create("audio/wav"));
```

#### JSON Schema

The JSON Schema defines the required input fields for a tool. It helps the client understand expected input formats and provides 
validation. Define it by returning a JSON string from the `schema()` method.

### Prompts

`Prompts` allow servers to provide structured messages and instructions for interacting with language models. They improve 
instruction quality and help LLMs generate better results. Each instruction is associated with a `Role` (either `assistant` or 
`user`) indicating who is providing the input. When calling a prompt, clients must supply argument values, which are defined with 
names, descriptions, and whether they are required. Use the `McpPromptArgument` builder to define arguments.

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
    public List<McpPromptArgument> arguments() {
        return List.of(McpPromptArgument.builder()
                                       .name("name")
                                       .description("Argument description")
                                       .required(true)
                                       .build());
    }

    @Override
    public List<McpPromptContent> prompt(McpRequest request) {
        return List.of(McpPromptContents.textContent("text", McpRole.USER));
    }
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
                        .addArgument(argument -> argument.name("name")
                            .description("Argument description")
                            .required(true))
                        .prompt(request -> McpPromptContents.textContent("text", Role.USER))
                        .build())));
    }
}
```

#### Prompt Content Types

Helidon supports four prompt content types:

- **Text**: Text content with a default `text/plain` media type.
- **Image**: Image content with a custom media type.
- **Audio**: Audio content with a custom media type.
- **Resource**: URI references to `McpResource` instances.

`Prompt` content can be created using `McpPromptContents` factory, and used as a result of the `Prompt` execution.

```java
McpPromptContent text = McpPromptContents.textContent("text", Role.USER);
McpPromptContent resource = McpPromptContents.resourceContent("http://resource", Role.USER);
McpPromptContent image = McpPromptContents.imageContent(pngImageBytes(), MediaTypes.create("image/png"), Role.USER);
McpPromptContent audio = McpPromptContents.audioContent(wavAudioBytes(), MediaTypes.create("audio/wav"), Role.USER);
```

### Resources

`Resources` allow servers to share data that provides context to language models, such as files, database schemas, or
application-specific information. Clients can list and read resources, which are defined by name, description, and media type.

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
    public MediaType mediaType() {
        return MediaTypes.TEXT_PLAIN;
    }

    @Override
    public List<McpResourceContent> read(McpRequest request) {
        return List.of(McpResourceContents.textContent("text"));
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
                        .uri("https://path")
                        .description("Resource description")
                        .mediaType(MediaTypes.TEXT_PLAIN)
                        .ressource(request -> McpResourceContents.textContent("text"))
                        .build())));
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
        return "MyResource";
    }

    @Override
    public String description() {
        return "Resource description";
    }

    @Override
    public MediaType mediaType() {
        return MediaTypes.TEXT_PLAIN;
    }

    @Override
    public List<McpResourceContent> read(McpRequest request) {
        String path = request.parameters()
                .get("path")
                .asString()
                .orElse("Unknown");
        return List.of(McpResourceContents.textContent(path));
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
                        .mediaType(MediaTypes.TEXT_PLAIN)
                        .ressource(request -> {
                            String path = request.parameters()
                                    .get("path")
                                    .asString()
                                    .orElse("Unknown");
                            return McpResourceContents.textContent(path);
                        })
                        .build())));
    }
}
```

#### Resource Content Types

Helidon supports two resource content types:

- **Text**: Text content with `text/plain` media type.
- **Binary**: Binary content with custom media type content.

`Resource` content can be created using `McpResourceContents` factory:

```java
McpResourceContent text = McpResourceContents.textContent("text");
McpResourceContent binary = McpResourceContents.binaryContent(gzipBytes(), "application/gzip");
```

### Completion

The `Completion` feature offers auto-suggestions for prompt arguments or resource template parameters, making the server easier 
to use and explore. Each completion is bound to a prompt name or a URI template. Access arguments from `McpRequest`. 
The completion's type, either prompt or resource template, is returned by the `referenceType` method.

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
    public McpCompletionContent complete(McpRequest request) {
        String name = request.parameters().get("name").asString().orElse("Unknown");
        String value = request.parameters().get("value").asString().orElse("Unknown");

        return McpCompletionContents.completion("suggestion");
    }
}
```

#### Builder

Define completions in the server builder:

```java
class McpServer {
    public static void main(String[] args) {
        WebServer.builder()
            .routing(routing -> routing.addFeature(
                McpHttpFeatureConfig.builder()
                    .addCompletion(completion -> completion
                        .reference("MyPrompt")
                        .completion(request -> McpCompletionContents.completion("suggestion"))
                        .build())));
    }
}
```

#### Completion Content Type

Only one type is supported — a list of suggestions:

```java
McpCompletionContent content = McpCompletionContents.completion("suggestion");
```

## MCP Parameters

Client parameters are available in `McpTool`, `McpPrompt`, and `McpCompletion` business logic via the `McpParameters` API:

```java
void process(McpRequest request) {
    McpParameters parameters = request.parameters();

    parameters.get("list").asList().get();
    parameters.get("age").asInteger().orElse(18);
    parameters.get("authorized").asBoolean().orElse(false);
    parameters.get("name").asString().orElse("defaultName");
    parameters.get("address").as(Address.class).orElseThrow();
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
    public List<McpToolContent> process(McpRequest request) {
        McpLogger logger = request.features().logger();

        logger.info("Logging data");
        logger.debug("Logging data");
        logger.notice("Logging data");
        logger.warn("Logging data");
        logger.error("Logging data");
        logger.critical("Logging data");
        logger.alert("Logging data");

        return List.of(McpToolContents.textContent("text"));
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
    public List<McpToolContent> process(McpRequest request) {
        McpProgress progress = request.features().progress();
        progress.total(100);
        for (int i = 1; i <= 10; i++) {
            longRunningTask();
            progress.send(i * 10);
        }
        return List.of(McpToolContents.textContent("text"));
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
```

### Cancellation

The MCP Cancellation feature enables verification of whether a client has issued a cancellation request. Such requests are 
typically made when a process is taking an extended amount of time, and the client opts not to wait for the completion of 
the operation. Cancellation status can be accessed from the `McpFeatures` class.

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
    public Function<McpRequest, List<McpToolContent>> tool() {
        return this::process;
    }

    private List<McpToolContent> process(McpRequest request) {
        long now = System.currentTimeMillis();
        long timeout = now + TimeUnit.SECONDS.toMillis(5);
        McpCancellation cancellation = request.features().cancellation();

        while (now < timeout) {
            if (cancellation.verify().isRequested()) {
                String reason = cancellation.verify().reason();
                return List.of(McpToolContents.textContent(reason));
            }
            longRunningOperation();
            now = System.currentTimeMillis();
        }
        return List.of(McpToolContents.textContent("text"));
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
            .routing(routing -> routing.addFeature(
                McpHttpFeatureConfig.builder()
                    .config(config.get("mcp.server"))));
    }
}
```

## References

- [MCP Specification](https://modelcontextprotocol.io/introduction)
- [JSON Schema Specification](https://json-schema.org)
