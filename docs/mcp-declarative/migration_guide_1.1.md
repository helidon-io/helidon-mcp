# Migration guide to version 1.1

Helidon MCP `1.1.0` provides support for MCP specification `2025-06-18`. This document
presents the noticeable changes and a way to upgrade your post `1.1.0` code.

# Overview of changes

Helidon MCP Declarative API was not impacted and stays the same. Nevertheless, the supported method signature has
changed. The following chapters describe in detail how to migrate your MCP components to `1.1.0` version.

## Tools

The `McpToolContent` class is removed, along with the `McpToolContents` factory class.

Old code:
```java
@Mcp.Tool("description")
List<McpToolContent> tool() {
    return List.of(McpToolContents.textContent("text"));
}
```

New code:
```java
@Mcp.Tool("description")
McpToolResult tool() {
    return McpToolResult.create("text");
}
```

For more detail about the `McpToolResult` class, see [README.md](README.md#tool-result) and 
[migration guide](../mcp/migration_guide_1.1.md#migrate-from-mcptoolcontents).

## Prompts

The `McpPromptContent` class is removed, along with the `McpPromptContents` factory class.

Old code:
```java
@Mcp.Prompt("description")
List<McpPromptContent> prompt() {
    return List.of(McpPromptContents.textContent("text"));
}
```

New code:
```java
@Mcp.Prompt("description")
McpPromptResult prompt() {
    return McpPromptResult.create("text");
}
```

For more detail about the `McpPromptResult` class, see [README.md](README.md#prompt-content-types) and
[migration guide](../mcp/migration_guide_1.1.md#migrate-from-mcppromptcontents).

## Resources

The `McpResourceContent` class is removed, along with the `McpResourceContents` factory class.

Old code:
```java
@Mcp.Resource(uri = "http://resource", description = "description")
List<McpResourceContent> resource() {
    return List.of(McpResourceContents.textContent("text"));
}
```

New code:
```java
@Mcp.Resource(uri = "http://resource", description = "description")
McpResourceResult resource() {
    return McpResourceResult.create("text");
}
```

For more detail about the `McpResourceResult` class, see [README.md](README.md#resource-content-types) and
[migration guide](../mcp/migration_guide_1.1.md#migrate-from-mcpresourcecontents).

## Completions

The `McpCompletionContent` class is removed, along with the `McpCompletionContents` factory class.

Old code:
```java
@Mcp.Completion("prompt")
McpCompletionContent completion() {
    return McpCompletionContents.completion("suggestion");
}
```

New code:
```java
@Mcp.Completion("prompt")
McpCompletionResult completion() {
    return McpCompletionResult.create("suggestion");
}
```

For more detail about the `McpCompletionResult` class, see [README.md](README.md#completion-content-type) and
[migration guide](../mcp/migration_guide_1.1.md#migrate-mcpcompletion-interface).