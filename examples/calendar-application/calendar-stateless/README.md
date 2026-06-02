# Helidon MCP Stateless Calendar Application

This application serves as a calendar manager and demonstrates Helidon MCP stateless mode. It exposes the same tools,
resources, prompts, and completions as the stateful Calendar example, but configures the MCP server with:

```yaml
mcp:
  server:
    stateless: true
```

In stateless mode, MCP clients can call methods such as `tools/list`, `tools/call`, `resources/read`, and `prompts/get`
directly over Streamable HTTP without first creating an MCP session through `initialize`.

The calendar stores created events in a temporary file for the lifetime of the application process.

## Session-Dependent Features

This example intentionally avoids MCP features that depend on a durable client session. A client that calls the server
without `initialize` has not negotiated capabilities, and the server does not keep request-to-request MCP session state for
that client. Because of that, the stateless add-event tool does not emit server-to-client logging notifications, progress
notifications, or resource subscription updates.

Those features are useful in stateful MCP flows, but they need an initialized session and a transport that can receive
notifications. The stateless example focuses on request-response operations that work independently: tools, prompts,
resources, resource templates, and completions.

## Running the Calendar Application

Build and launch the stateless calendar application using the following commands:

```shell
mvn clean package
java -jar target/helidon-mcp-calendar-stateless-server.jar
```

The MCP endpoint is available at:

```text
http://localhost:8081/calendar
```

## Running the MCP Inspector

To start the MCP Inspector, run the following command in your terminal:

```shell
npx @modelcontextprotocol/inspector
```

When the MCP Inspector opens, configure it with:

1. Set the **Transport** to `Streamable HTTP`.
2. Set the **URL** to `http://localhost:8081/calendar`.
3. Click the **Connect** button.

The server also supports SSE transport for clients that use session-oriented connections, but Streamable HTTP is the natural
transport for demonstrating stateless calls.

## Testing the Tool

1. Navigate to the **Tools** tab.
2. Click **List Tools** and select the `add-calendar-event` tool from the list.
3. Enter the following parameters:

    * **Name**: Frank birthday
    * **Date**: 2021-04-20
    * **Attendees**: Click `switch to JSON` and enter `["Frank"]`.
4. Click **Run Tool**.
5. Verify that the response contains `New event added to the calendar`.

## Testing the Resource

1. Navigate to the **Resources** tab.
2. Click **List Resources**.
3. Select the `calendar-events` resource.
4. Verify that the result includes Frank's birthday event.

## Testing the Resource Template

1. Navigate to the **Resources** tab.
2. Click **List Resource Templates**.
3. Select `calendar-events-resource-template`.
4. Type `F` for the `name` argument to trigger completion.
5. Select `Frank-birthday` from the completion menu.
6. Click **Read Resource**.
7. Verify that the result includes Frank's birthday event.

## Testing the Prompt

1. Navigate to the **Prompts** tab.
2. Click **List Prompts**.
3. Select the `create-event` prompt.
4. Enter the following parameters:

    * **Name**: Frank birthday
    * **Date**: 2021-04-20
    * **Attendees**: Frank
5. Click **Get Prompt**.

## Automated Tests

The module includes a `StatelessClientTest` that sends JSON-RPC requests directly to `/calendar` without calling
`initialize`. It verifies tool, prompt, resource, resource template, and completion behavior in stateless mode.

Run the tests and checkstyle with:

```shell
mvn test checkstyle:check
```

## References

* [MCP Inspector Documentation](https://modelcontextprotocol.io/legacy/tools/inspector)
