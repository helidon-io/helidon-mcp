# Helidon MCP Extension

Helidon support of Model Context Protocol (MCP).

## Overview

The MCP protocol is a standard way of communication for LLMs to interact with internal and external data sources. More than a
protocol, MCP aims to create an environment of AI agents interconnected with the ability to reach real time information.
To achieve that goal, the protocol has a client-server architecture. The clients are used by AI agents to initiate communication
with servers. On the other hand, servers manage the access to data sources and provides a way to read their content to the
clients. Helidon provides support for the server side and can be accessed by any other clients that implement MCP specification.

## Maven Coordinates

To create your first MCP server powered by Helidon. Add the following dependency to your `pom.xml`.

```xml
<dependency>
    <groupId>io.helidon.extensions.mcp</groupId>
    <artifactId>helidon-extensions-mcp-server</artifactId>
</dependency>
```

Add the following annotation processor to your `pom.xml`.

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>io.helidon.extensions.mcp</groupId>
                <artifactId>helidon-extensions-mcp-codegen</artifactId>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

## Usage

This section describes how to create MCP components.

### MCP Server

MCP Server exposes MCP components which can be listed, called or read by the clients. It can be reached through a configurable
`HTTP` endpoint. The server manages connections with clients using a set of features described later in this document.
A server is designed as a Helidon `HttpFeature` and is registered to the webserver `routing`. To register several server,
create other class annotated with `@Mcp.Server` and a different `@Mcp.Path`. It is the entry point for any MCP client and must 
be unique. The server name and version are shared with client during connection. There is no requirement form Helidon for the 
name and version management.

```java
@Mcp.Server
class McpServer {
}
```

#### Configuration

- `@Mcp.Server` annotation is required to create an instance of Mcp server and the attribute override the server name. 
- `@Mcp.Path` defines the server endpoint.
- `@Mcp.Version` sets the server version.

```java
@Mcp.Path("/mcp")
@Mcp.Version("0.0.1")
@Mcp.Server("server-name")
class McpServer {
}
```

### Tool

A `Tool` is a function that computes a set of inputs and return a result. Mcp clients uses tools to interact with the outside 
world to reach real time data through API calls, access to databases or performing any kind of computation. Name, description, 
schema and the processing business logic describe a `Tool`. Use the `@Mcp.Tool` annotation on a method to create a `Tool`. The 
attribute is the `Tool` description. The `Tool` is named after the method name. The schema is a
[Json Schema](https://json-schema.org/specification) that describes the `Tool` inputs in order for the client to send the data in
the correct format. If the method parameters does not contain `POJO` then the schema is generated at build time by Helidon.
`Tools` are automatically registered to the MCP server by being located in the server class. 

```java
@Mcp.Server
class Server {

    @Mcp.Tool("Tool description")
    List<McpToolContent> myToolMethod(String input) {
        return List.of(McpToolContents.textContent("text"));
    }
}
```

#### Configuration

- `@Mcp.Name` overrides the tool name and is located on a `Tool` method. 
- `@Mcp.JsonSchema` is located on `POJO` class that is used as `Tool` input.

```java
@Mcp.Server
class Server {

    @Mcp.Tool("Tool description")
    @Mcp.Name("tool-name")
    List<McpToolContent> myToolMethod(Coordinate coordinate) {
        return List.of(McpToolContents.textContent("text"));
    }

    @Mcp.JsonSchema("""
            {
                "type": "object",
                "description": "Latitude and longitude coordinates",
                "properties": {
                    "latitude": {
                        "type": "number"
                    },
                    "longitude": {
                        "type": "number"
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

#### Tool Content Types

Helidon provides three kind of tool content:

- **Text**: Text content with a default `text/plain` media type.
- **Image**: Image content with a custom media type.
- **Resource**: Resource content that reference a `McpResource` by `URI`. The `McpResource` has to be registered on the server.

`Tool` content can be created using `McpToolContents` factory, and used as result of the `Tool` execution.

```java
McpToolContent text = McpToolContents.textContent("text");
McpToolContent resource = McpToolContents.resourceContent("http://resource");
McpToolContent image = McpToolContents.imageContent("base64", MediaTypes.APPLICATION_OCTET_STREAM);
```

#### JSON Schema

Json schema describes to MCP Client what inputs are required by the `Tool` being invoked. It provides a description of each
inputs so the LLM understands their meaning. It enforces the input format and conditions can be applied following the
[specification](https://json-schema.org/specification). Json schema provides a first validation step for your `Tool` inputs. Create them using `@Mcp.JsonSchema`
annotation.

### Prompt

`Prompts` are set of instruction using parameters and often referred as templates. It highlights how to exercise the
MCP server in an efficient way. It helps users to find the correct instructions and ensures better answers from LLMs.
The instructions are associated with a `Role`, either `assistant` or `user`, to describe who's making the demands.
To call a `Prompt`, the MCP client needs to provide the template arguments. Each argument are given a name and description
and can be optional. Helidon has a `McpPromptArgument` builder to create instances.

```java
@Mcp.Server
class Server {

    @Mcp.Prompt("Prompt description")
    List<McpPromptContent> myToolMethod(String argument) {
        return List.of(McpPromptContents.textContent("text", McpRole.USER));
    }
}
```

#### Configuration

`Prompt` that return text only can use `String` as returned type.

- `@Mcp.Name` overrides the prompt name and is located on a `Prompt` method. 
- `@Mcp.Role` annotation is used for specific `Prompt` that contains only text. The annotation is ignored in other cases. 
- `@Mcp.Description` describes the `Prompt` argument.

```java
@Mcp.Server
class Server {

    @Mcp.Prompt("Prompt description")
    @Mcp.Name("prompt-name")
    @Mcp.Role(McpRole.USER)
    String myToolMethod(@Mcp.Description("Argument description") String argument) {
        return "text";
    }
}
```

#### Prompt Content Types

Helidon provides three kind of prompt content:

- **Text**: Text content with a default `text/plain` media type.
- **Image**: Image content with a custom media type.
- **Resource**: Resource content that reference a `McpResource` by `URI`. The `McpResource` has to be registered on the server.

`Prompt` content can be created using `McpPromptContents` factory, and used as result of the `Prompt` execution.

```java
McpPromptContent text = McpPromptContents.textContent("text", Role.USER);
McpPromptContent resource = McpPromptContents.resourceContent("http://resource", Role.USER);
McpPromptContent image = McpPromptContents.imageContent("base64", MediaTypes.APPLICATION_OCTET_STREAM, Role.USER);
```

### Resource

`Resources` are data sources that provides context to LLM. Identified by a unique `URI`, a resource can be for example, a file,
web page or application specific data. MCP clients can list `Resources` and read their content. Each `Resource` has a name and
description that describe its content. The content format is defined by `MediaType` to help AI agents reading the data.

`Resource Templates` are using [`URI` template](https://datatracker.ietf.org/doc/html/rfc6570) to facilitate resource discovery.
Parameters are enclosed with curly brackets and can be resolved by MCP clients. `Resource Template` can not be read and do not
provide content.

Both type of `Resource` are created using the same API. Use `URI` template that contains parameter to create a
`Resource Template`.

```java
@Mcp.Server
class Server {

    @Mcp.Resource(
            uri = "file://path",
            description = "Resource description",
            mediaType = MediatTypes.TEXT_PLAIN_VALUE)
    List<McpResourceContent> resource() {
        return List.of(McpResourceContents.textContent("text"));
    }
}
```

#### Configuration

Resources that return text only can use `String` as a returned type.

- `@Mcp.Name` annotation overrides the `Resource` name.

```java
@Mcp.Server
class Server {

    @Mcp.Resource(
            uri = "file://path",
            description = "Resource description",
            mediaType = MediatTypes.TEXT_PLAIN_VALUE)
    @Mcp.Name("resource-name")
    String resource(McpRequest request) {
        return "text";
    }
}
```

#### Resource Content Types

Helidon provides two kind of resource content:

- **text**: Text content with a default `text/plain` media type.
- **binary**: Binary content with a custom media type.

`Resource` content can be created using `McpResourceContents` factory, and used as result of the `Resource` read execution.

```java
McpResourceContent text = McpResourceContents.textContent("data");
McpResourceContent binary = McpResourceContents.binaryContent("{\"foo\":\"bar\"}", MediaTypes.APPLICATION_JSON);
```

### Completion

MCP server provides `Completion` feature to offer auto-completion suggestions for `Prompt` arguments and resource template URIs.
Templates are created to guide AI agents in using MCP server and `Completion` is an additional step in that direction. For
`Resource` template, the MCP server can suggest where files are located. For `Prompt`, the MCP server could suggest a name or
an address. Therefore, to use `Completion` makes it even easier to use MCP server and discover functionalities.

`Completion` are bind to a `Prompt` or `Resource Template` by a reference. In case of a `Prompt`, the reference must be the
`Prompt` name. For a `Resource Template`, the reference is the `Resource URI Template`. The argument or parameter can be accessed
from the `McpRequest` as shown bellow.

```java
@Mcp.Server
class Server {

    @Mcp.Completion("reference")
    McpCompletionContent completionPromptArgument(McpRequest request) {
        String value = request.parameters().get("value").asString().orElse(null);
        return McpCompletionContents.completion("suggestion");
    }
}
```

#### Configuration

`Completion` can get the argument value as method parameter and return `List<String>`.

```java
@Mcp.Server
class Server {

    @Mcp.Completion("reference")
    List<String> completionPromptArgument(String value) {
        return List.of("suggestion");
    }
}
```

#### Completion Content Type

There is one kind of content type for completion that return a list of suggestion.

```java
McpCompletionContent content = McpCompletionContents.completion("suggestion");
```

## Mcp Parameters

The client parameters are accessible from `McpTool`, `McpPrompt` and `McpCompletion` business logic method.
They can be safely accessed using the `McpParameters` class as follows:

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

The MCP protocol provides additional features, available from the server side. Those are accessible through `McpFeatures`from
the `McpRequest` class.

### Logging

Where classic Java logging would print logs from the server side, it can be difficult for AI agents to keep track of what is
happening on the server side. The `Logging` features is filling this gap and provides logging notification to the MCP clients.
Access the `McpLogger` to log messages from the `McpRequest` as shown bellow.

#### Example

Logging notification used during a tool process.

```java
@Mcp.Server
class Server {

    @Mcp.Tool("Tool description")
    List<McpToolContent> getLocationCoordinates(McpFeatures features) {
        McpLogger logger = features.logger();

        logger.info("Logging data");
        logger.debug("Logging data");
        logger.notice("Logging data");
        logger.warn("Logging data");
        logger.error("Logging data");
        logger.critical("Logging data");
        logger.alert("Logging data");

        return List.of(McpToolContents.textContent("Text"));
    }
}
```

### Progress

Some task on the MCP server side might be time-consuming. The MCP Client can request `Progress` notification to stay up to date.
`Progress` feature can be accessed from the `McpFeatures` class. The server sends manually notification to the client as shown in
the example bellow.

#### Example

Sending progress notifications during a long running tool task execution.

```java
@Mcp.Server
class Server {

    @Mcp.Tool("Tool description")
    List<McpToolContent> getLocationCoordinates(McpFeatures features) {
        McpProgress logger = features.progress();
        progress.total(100);
        for (int i = 1; i <= 10; i++) {
            longRunningTask();
            progress.send(i * 10);
        }
        return List.of(McpToolContents.textContent("text"));
    }
}
```

## Reference

- [MCP specification](https://modelcontextprotocol.io/introduction)
- [JSON Schema specification](https://json-schema.org)
