# Helidon MCP Extension

Helidon support for the Model Context Protocol (MCP).

## Overview

The Model Context Protocol (MCP) defines a standard communication method that enables LLMs (Large Language Models) to interact
with both internal and external data sources. More than just a protocol, MCP establishes a connected environment of AI agents
capable of accessing real-time information. MCP follows a client-server architecture: clients, typically used by AI agents,
initiate communication, while servers manage access to data sources and provide data retrieval capabilities. Helidon offers
server-side support and can be accessed by any client that implements the MCP specification.

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

This section walks you through creating and configuring various MCP components.

### MCP Server

Servers provide the fundamental building blocks for adding context to language models via MCP. Clients discover a server via a 
configurable `HTTP` endpoint. Servers manage connections and support features detailed later in this guide. Helidon represents an 
MCP server as an `HttpFeature`, which is registered as part of your web server’s routing. You can create multiple MCP servers by 
defining multiple classes annotated with `@Mcp.Server`, each using a distinct `@Mcp.Path`. Each path must be unique and serves as 
the client’s entry point. Helidon imposes no restrictions on naming or versioning; those values are simply shared with the client 
upon connection.

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
Define a tool by annotating a method with `@Mcp.Tool`. Method names become tool names unless overridden with`@Mcp.Name`. Input 
schemas are generated using [JSON Schema Specification](https://json-schema.org/specification); Helidon auto-generates schemas 
when inputs are primitive types (non-POJO). `Tools` are automatically registered when defined within a server class.

```java
@Mcp.Server
class Server {

    @Mcp.Tool("Tool description")
    List<McpToolContent> myToolMethod(String input) {
        return List.of(McpToolContents.textContent("Input: " + input));
    }
}
```

#### Configuration

- **`@Mcp.Name`**: Overrides tool method name.
- **`@Mcp.JsonSchema`**: Explicitly defines POJO input structures.

```java
@Mcp.Server
class Server {

    @Mcp.Tool("Tool description")
    @Mcp.Name("MyTool")
    List<McpToolContent> myToolMethod(Coordinate coordinate) {
        String result = String.format("latitude: %s, longitude: %s", 
                                      coordinate.latitude, 
                                      coordinate.latitude);
        return List.of(McpToolContents.textContent(result));
    }

    @Mcp.JsonSchema("""
            {
                "type": "object",
                "description": "Latitude and longitude coordinates",
                "properties": {
                    "latitude": {
                        "type": "integer"
                    },
                    "longitude": {
                        "type": "integer"
                    }
                }
            }
            """)
    static class Coordinate {
        int latitude;
        int longitude;
    }
}
```

#### JSON Schema

Use JSON Schema to validate and describe input parameters and their structure. You can define schemas via the `@Mcp.JsonSchema`
annotation.

#### Tool Content Types

Helidon supports three types of tool output:

- **Text**: Text content with the default `text/plain` media type.
- **Image**: Image content with a custom media type.
- **Resource**: A reference to an `McpResource` via a URI (must be registered on the server).

Use the `McpToolContents` factory to create tool contents:

```java
McpToolContent text = McpToolContents.textContent("text");
McpToolContent resource = McpToolContents.resourceContent("http://path");
McpToolContent image = McpToolContents.imageContent("base64", MediaTypes.create("image/png"));
```

### Prompt

`Prompts` allow servers to provide structured messages and instructions for interacting with language models. They guide MCP 
usage and help the LLM produce accurate responses. Create prompts with `@Mcp.Prompt` annotation. Use `@Mcp.Name` to override 
the method’s name and `@Mcp.Role` to specify the speaker for text-only prompts.

```java
@Mcp.Server
class Server {

    @Mcp.Prompt("Prompt description")
    List<McpPromptContent> myPromptMethod(String argument) {
        return List.of(McpPromptContents.textContent(argument + ".", McpRole.USER));
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
    String myPromptMethod(@Mcp.Description("Argument description") String argument) {
        return argument + ".";
    }
}
```

#### Prompt Content Types

Helidon supports three prompt content types:

- **Text**: Text content with a default `text/plain` media type.
- **Image**: Image content with a custom media type.
- **Resource**: URI references to `McpResource` instances.

`Prompt` content can be created using `McpPromptContents` factory, and used as result of the `Prompt` execution.

```java
McpPromptContent text = McpPromptContents.textContent("text", Role.USER);
McpPromptContent resource = McpPromptContents.resourceContent("http://path", Role.USER);
McpPromptContent image = McpPromptContents.imageContent("base64", MediaTypes.create("image/png"), Role.USER);
```

### Resource

`Resources` allow servers to share data that provides context to language models, such as files, database schemas, or 
application-specific information. Clients can list and read them. Resources are identified by name, description, and media type.

**Resource Templates** use [URI templates](https://datatracker.ietf.org/doc/html/rfc6570) to define parameterized URIs. They help 
clients discover dynamic content; note that templates are not directly readable. 
Define both resources and templates using `@Mcp.Resource`:

```java
@Mcp.Server
class Server {

    @Mcp.Resource(
            uri = "file://path",
            description = "Resource description",
            mediaType = MediaTypes.TEXT_PLAIN_VALUE)
    List<McpResourceContent> resource() {
        return List.of(McpResourceContents.textContent("text"));
    }
}
```

#### Configuration

Use `String` return types for text-only resources. The `@Mcp.Name` annotation lets you override the default resource name.

```java
@Mcp.Server
class Server {
    
    @Mcp.Resource(
            uri = "file://path",
            description = "Resource description",
            mediaType = MediaTypes.TEXT_PLAIN_VALUE)
    @Mcp.Name("MyResource")
    String resource(McpRequest request) {
        return "text";
    }
}
```

#### Resource Content Types

Helidon supports two resource content types:

- **Text**: Text content with a default `text/plain` media type.
- **Binary**: Binary content with a custom media type.

`Resource` content can be created using `McpResourceContents` factory:

```java
McpResourceContent text = McpResourceContents.textContent("data");
McpResourceContent binary = McpResourceContents.binaryContent("{\"foo\":\"bar\"}", MediaTypes.APPLICATION_JSON);
```

### Completion

The `Completion` feature offers auto-suggestions for prompt arguments or resource template parameters, making the server easier
to use and explore. Bind completions to prompts (by name) or resource templates (by URI) using `@Mcp.Completion`:

```java
@Mcp.Server
class Server {

    @Mcp.Completion("http://path")
    McpCompletionContent completionPromptArgument(McpRequest request) {
        String value = request.parameters().get("value").asString().orElse(null);
        return McpCompletionContents.completion(value + ".");
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

#### Completion Content Type

Completion content results in a list of suggestions:

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

Helidon provides additional server-side features—accessible through `McpFeatures` via `McpRequest`.

### Logging

Traditional Java logging may not be visible to AI clients. Helidon’s `Logging` feature bridges this gap by sending log messages 
directly to clients:

```java
@Mcp.Server
class Server {

    @Mcp.Tool("Tool description")
    List<McpToolContent> getLocationCoordinates(McpFeatures features) {
        McpLogger logger = features.logger();

        logger.info("Logging info");
        logger.debug("Debugging info");
        logger.notice("Notice info");
        logger.warn("Warning");
        logger.error("Error message");
        logger.critical("Critical issue");
        logger.alert("Alert message");

        return List.of(McpToolContents.textContent("Text"));
    }
}
```

### Progress

For long-running tasks, clients can request progress updates. Use `McpProgress` to send updates to clients manually:

```java
@Mcp.Server
class Server {

    @Mcp.Tool("Tool description")
    List<McpToolContent> getLocationCoordinates(McpFeatures features) {
        McpProgress progress = features.progress();
        progress.total(100);
        for (int i = 1; i <= 10; i++) {
            longRunningTask();
            progress.send(i * 10);
        }
        return List.of(McpToolContents.textContent("text"));
    }
}
```

## References

- [MCP Specification](https://modelcontextprotocol.io/introduction)
- [JSON Schema Specification](https://json-schema.org)
