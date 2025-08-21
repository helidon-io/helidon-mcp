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

## Usage

This section describes how to create MCP components.

### MCP Server

MCP Server exposes MCP components which can be listed, called or read by the clients. It can be reached through a configurable 
`HTTP` endpoint. The server manages connections with clients using a set of features described later in this document.
A server is designed as a Helidon `HttpFeature` and is registered to the webserver `routing`. To register several server, 
add another `McpServerFeature` with a different `path`. It is the entry point for any MCP client and must be unique. 
The `McpServerFeature` builder exposes methods to register MCP components. The server name and version are shared with client 
during connection. There is no requirement form Helidon for the name and version management.

Create an MCP server.
```java
class McpServer {
    public static void main(String[] args) {
        WebServer.builder()
            .routing(routing -> routing.addFeature(
                McpServerFeature.builder()
                    .path("/mcp")
                    .version("0.0.1")
                    .name("helidon-mcp-server")
                    .build()));
    }
}
```

### Tool

A `Tool` is a function that computes a set of inputs and return a result. Mcp clients uses tools to interact with the outside world 
to reach real time data through API calls, access to databases or performing any kind of computation. Specify a name, description, 
schema and the processing business logic to create a `Tool` instance. Use the `addTool` method from the `McpServerFeature` builder
to register your `Tool` to the server. The name and description are used by LLMs to interpret the `Tool` usage. The schema is a 
[Json Schema](https://json-schema.org/specification) that describes the `Tool` inputs in order for the client to send the data in 
the correct format. The business logic is executed by the `process` method and the inputs accessed using the `McpRequest`.

#### Interface

Implement the `Tool` interface that provides the required methods.

Example of an implementation of `Tool` interface.

```java
class MyTool implements McpTool {
    @Override
    public String name() {
        return "tool-name";
    }

    @Override
    public String description() {
        return "Tool description";
    }

    @Override
    public String schema() {
        return """
                    {
                        "title": "My Title"
                    }
                    """;
    }

    @Override
    public List<McpToolContent> process(McpRequest request) {
        return List.of(McpToolContents.textContent("data"));
    }
}
```

#### Builder

A `Tool` can be created directly on the server builder.

```java
class McpServer {
    public static void main(String[] args) {
        WebServer.builder()
            .routing(routing -> routing.addFeature(
                McpServerFeature.builder()
                    .addTool(tool -> tool.name("name")
                        .description("description")
                        .schema("schema")
                        .tool(request -> McpToolContents.imageContent("base64", MediaTypes.TEXT_PLAIN))
                        .build())));
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
[specification](https://json-schema.org/specification). Json schema provides a first validation step for your `Tool` inputs.
The schema is associated with your `Tool` using the `Tool#schema()` method that returns a `String`. 

### Prompts

`Prompts` are set of instruction using parameters and often referred as templates. It highlights how to exercise the
MCP server in an efficient way. It helps users to find the correct instructions and ensures better answers from LLMs. 
The instructions are associated with a `Role`, either `assistant` or `user`, to describe who's making the demands.
To call a `Prompt`, the MCP client needs to provide the template arguments. Each argument are given a name and description
and can be optional. Helidon has a `McpPromptArgument` builder to create instances.

#### Interface

Implements the `McpPrompt` interface and register it to the `McpServerFeature` using `addPrompt` method.

```java
class MyPrompt implements McpPrompt {
    @Override
    public String name() {
        return "prompt-name";
    }

    @Override
    public String description() {
        return "Prompt description";
    }

    @Override
    public Set<McpPromptArgument> arguments() {
        return Set.of(McpPromptArgument.builder()
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

`Prompt` can be created in the `McpServerFeature` builder using the `addPrompt` method.

```java
class McpServer {
    public static void main(String[] args) {
        WebServer.builder()
            .routing(routing -> routing.addFeature(
                McpServerFeature.builder()
                    .addPrompt(prompt -> prompt.name("name")
                        .description("description")
                        .addArgument(argument -> argument.name("arg-name")
                            .description("arg-description")
                            .required(true))
                        .prompt(request -> McpPromptContents.textContent("text", Role.USER))
                        .build())));
    }
}
```

#### Prompts Content Types

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

### Resources

`Resources` are data sources that provides context to LLM. Identified by a unique `URI`, a resource can be for example, a file, 
web page or application specific data. MCP clients can list `Resources` and read their content. Each `Resource` has a name and
description that describe its content. The content format is defined by `MediaType` to help AI agents reading the data.

`Resource Template` are using [`URI` template](https://datatracker.ietf.org/doc/html/rfc6570) to facilitate resource discovery.
Parameters are enclosed with curly brackets and can be resolved by MCP clients. `Resource Template` can not be read and do not
provide content.

Both type of `Resource` are created using the same API. Use `URI` template that contains parameter to create a 
`Resource Template`. 

#### Interface

Implements the `McpResource` interface and register it to the `McpServerFeature` using `addResource` method.

```java
class MyResource implements McpResource {

    @Override
    public String uri() {
        return "http://path";
    }

    @Override
    public String name() {
        return "resource-name";
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
        return List.of(McpResourceContents.textContent("data"));
    }
}
```

#### Builder

`Resource` can be created in the `McpServerFeature` builder using the `addResource` method.

```java
class McpServer {
    public static void main(String[] args) {
        WebServer.builder()
            .routing(routing -> routing.addFeature(
                McpServerFeature.builder()
                    .addResource(resource -> resource.name("name")
                        .uri("uri")
                        .description("description")
                        .mediaType(MediaTypes.TEXT_PLAIN)
                        .ressource(request -> McpResourceContents.textContent(""))
                        .build())));
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

#### Interface

Implements the `McpCompletion` interface and register it to the `McpServerFeature` using `addCompletion` method.

```java
class MyCompletion implements McpCompletion {

    @Override
    public String reference() {
        return "reference";
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

`Completion` can be created in the `McpServerFeature` builder using the `addCompletion` method.

```java
class McpServer {
    public static void main(String[] args) {
        WebServer.builder()
            .routing(routing -> routing.addFeature(
                McpHttpFeatureConfig.builder()
                    .addCompletion(completion -> completion
                        .reference("reference")
                        .completion(request -> McpCompletionContents.completion("suggestion"))
                        .build())));
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
class LoggingTool implements McpTool {
    @Override
    public String name() {
        return "logging";
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

Some task on the MCP server side might be time-consuming. The MCP Client can request `Progress` notification to stay up to date.
`Progress` feature can be accessed from the `McpFeatures` class. The server sends manually notification to the client as shown in 
the example bellow.

#### Example

Sending progress notifications during a long running tool task execution.

```java
class ProgressTool implements McpTool {
    @Override
    public String name() {
        return "progress";
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

## Configuration

MCP server properties can be configured through Helidon configuration. Example using a `YAML` file.

```yaml
mcp:
  server:
    name: "mcp-server"
    version: "0.0.1"
    path: "/mcp"
```

Register the configuration to the `McpServerFeature`.

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

## Reference

- [MCP specification](https://modelcontextprotocol.io/introduction)
- [JSON Schema specification](https://json-schema.org)
