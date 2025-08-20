# Helidon Weather Application

The Helidon MCP weather application shows how to use Helidon MCP APIs in a real world use case.
When LLM struggles to access external data, MCP protocol provides a solution.

This example gives users real time information about weather alerts in any US states. It is composed of 
two major modules:

- First application uses an LLM and MCP client.
- Second application is a Helidon MCP Server.

## Prerequisite

The application is using Ollama but can be edited to use any other LLM.
After downloading Ollama from its official website, run the `llama3.1` model.
```shell
ollama run llama3.1
```

## MCP Client Application

The MCP Client application is located under the `mcp-client` directory. This Helidon based application
uses Langchain4j to interact with both LLM and MCP server. In this scenario, the LLM is a weather 
journalist and must reply to the user questions concerning the current weather alerts in US. Users
question are sent through HTTP GET request containing the actual question in the query parameters.

## MCP Server Application

The MCP Server applications are located under `mcp-server` and `mcp-server-declarative`. They are 
mirroring each other and while this first shows Helidon API in an imperative way, the second uses the
declarative. The MCP server has a tool that access the US National Weather Service public API to get 
real time weather alert. Once up and running, any application can connect to the server and use this 
tool.

## Run And Exercise The Application

First, make sure ollama is running.
```shell
ollama run llama3.1
```

Build the whole weather application from the `weather-application` directory.
```shell
mvn clean package
```

Then run on of the server application.
```shell
java -jar mcp-server/target/helidon-mcp-weather-server.jar
```

In another terminal, run the client application.
```shell
java -jar mcp-client/target/helidon-mcp-weather-client.jar
```

You can now exercise the application.
```shell
curl -G -X GET "http://localhost:8080/weather" \
  --data-urlencode "question=Is there a weather alert in state CA?"
```
