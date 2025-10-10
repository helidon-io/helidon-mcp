# Helidon MCP Calendar Application

This application serves as a calendar manager and demonstrates how to use the MCP Inspector for debugging an MCP server.
It allows you to create events and view them through a single registered resource. The calendar is basic and has a single usage: 
record created events.

## Running the MCP Inspector

To start the MCP Inspector, run the following command in your terminal:

```shell
npx @modelcontextprotocol/inspector
```

## Running the Calendar Application

Build and launch the calendar application using the following commands:

```shell
mvn clean package
java -jar target/helidon-calendar-declarative.jar
```

## Using the MCP Inspector

When the MCP Inspector starts, it will automatically open a new browser window. Follow these steps to connect it to your running
MCP server:

1. On the left panel of the Inspector UI, configure the connection settings.
2. Set the **Transport** to `SSE`.
3. Update the **URL** field to: `http://localhost:8081/calendar`
4. Click the **Connect** button to establish a connection to the server.

### Testing the Tool

1. Navigate to the **Tool** tab.
2. Click **List Tools** and select the `addCalendarEventTool` tool from the list.
3. Enter the following parameters on the right panel:

    * **Name**: Frank birthday
    * **Date**: 2021-04-20
    * **Attendees**: Click `switch to JSON` and enter `["Frank"]`.
4. Click **Run Tool**.
5. You should see the message: `New event added to the calendar`

### Testing the Resource

1. Navigate to the **Resource** tab.
2. Click **List Resources**.
3. Select the first resource in the list.
4. Verify that the result displayed includes Frank's birthday event.

### Testing the Resource Template

1. Navigate to the **Resource** tab.
2. Click **List Resources Template**.
3. Select `eventResourceTemplate`.
3. Type `F` for the name to trigger completion.
4. Select `Frank birthday` from pop-up menu.
5. Click `Read Resource`.
4. Verify that the result displayed includes Frank's birthday event.

### Testing the Prompt

1. Navigate to the **Prompts** tab.
2. Click **List Prompts**.
3. Select the first Prompt in the list.
4. Enter the following parameters on the right panel:

    * **Name**: Frank birthday
    * **Date**: 2021-04-20
    * **Attendees**: Frank
5. Click **Get Prompt**

## References

* [MCP Inspector Documentation](https://modelcontextprotocol.io/legacy/tools/inspector)
