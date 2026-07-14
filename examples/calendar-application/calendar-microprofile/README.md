# Helidon MCP Calendar MicroProfile Application

This application serves as a calendar manager and demonstrates how to run a declarative Helidon MCP server in a
Helidon MicroProfile application. It exposes tools, resources, resource templates, prompts, and completions for
creating and reading calendar events.

The calendar stores created events in a temporary file for the lifetime of the application process.

## Running the MCP Inspector

To start the MCP Inspector, run the following command in your terminal:

```shell
npx @modelcontextprotocol/inspector
```

## Running the Calendar Application

From the `examples/calendar-application/calendar-microprofile` directory, build and launch the calendar application
using the following commands:

```shell
mvn clean package
java -jar target/helidon4-extensions-mcp-calendar-microprofile.jar
```

The MCP endpoint is available at:

```text
http://localhost:8080/calendar
```

## Using the MCP Inspector

When the MCP Inspector opens, configure it as follows:

1. Set the **Transport** to `Streamable HTTP`.
2. Set the **URL** to `http://localhost:8080/calendar`.
3. Click the **Connect** button.

### Testing the Tool

1. Navigate to the **Tools** tab.
2. Click **List Tools** and select the `addCalendarEvent` tool.
3. Switch the arguments editor to JSON and enter:

    ```json
    {
      "event": {
        "name": "Frank-birthday",
        "date": "2021-04-20",
        "attendees": ["Frank"]
      }
    }
    ```

4. Click **Run Tool**.
5. Verify that the response contains `New event added to the calendar`.

### Testing the Resource

1. Navigate to the **Resources** tab.
2. Click **List Resources**.
3. Select the `eventsResource` resource.
4. Verify that the result includes the `Frank-birthday` event.

### Testing the Resource Template

1. Navigate to the **Resources** tab.
2. Click **List Resource Templates**.
3. Select the `eventResourceTemplate` resource template.
4. Type `Frank` for the `name` argument to trigger completion.
5. Select `Frank-birthday` from the completion menu.
6. Click **Read Resource**.
7. Verify that the result includes the `Frank-birthday` event.

### Testing the Prompt

1. Navigate to the **Prompts** tab.
2. Click **List Prompts**.
3. Select the `createEventPrompt` prompt.
4. Enter the following parameters:

    * **Name**: Frank-birthday
    * **Date**: 2021-04-20
    * **Attendees**: Frank

5. Click **Get Prompt**.
6. Verify that the generated prompt describes the calendar event.

## References

* [MCP Inspector Documentation](https://modelcontextprotocol.io/docs/tools/inspector)
