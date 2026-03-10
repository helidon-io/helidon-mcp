# Migration Guide for Version 1.1.0

Helidon MCP `1.1.0` provides support for MCP specification `2025-06-18`. This document
summarizes the key changes and explains how to migrate existing code to `1.1.0`.

## Overview of Changes

The Helidon MCP Declarative API remains largely unchanged; however, supported method signatures have changed.
The sections below describe how to migrate MCP components to version `1.1.0`.

## Tools

The `McpToolContent` class is removed, along with the `McpToolContents` factory class.

Previous API:
```java
@Mcp.Tool("description")
List<McpToolContent> tool() {
    return List.of(McpToolContents.textContent("text"));
}
```

Updated API:
```java
@Mcp.Tool("description")
McpToolResult tool() {
    return McpToolResult.create("text");
}
```

For details about the `McpToolResult` class, see [README.md](README.md#tool-result) and
[Migration Guide](../mcp/migration_guide_1.1.md#migrate-from-mcptoolcontents).

## Prompts

The `McpPromptContent` class is removed, along with the `McpPromptContents` factory class.

Previous API:
```java
@Mcp.Prompt("description")
List<McpPromptContent> prompt() {
    return List.of(McpPromptContents.textContent("text"));
}
```

Updated API:
```java
@Mcp.Prompt("description")
McpPromptResult prompt() {
    return McpPromptResult.create("text");
}
```

For details about the `McpPromptResult` class, see [README.md](README.md#prompt-content-types) and
[Migration Guide](../mcp/migration_guide_1.1.md#migrate-from-mcppromptcontents).

## Resources

The `McpResourceContent` class is removed, along with the `McpResourceContents` factory class.

Previous API:
```java
@Mcp.Resource(uri = "http://resource", description = "description")
List<McpResourceContent> resource() {
    return List.of(McpResourceContents.textContent("text"));
}
```

Updated API:
```java
@Mcp.Resource(uri = "http://resource", description = "description")
McpResourceResult resource() {
    return McpResourceResult.create("text");
}
```

For details about the `McpResourceResult` class, see [README.md](README.md#resource-content-types) and
[Migration Guide](../mcp/migration_guide_1.1.md#migrate-from-mcpresourcecontents).

## Completions

The `McpCompletionContent` class is removed, along with the `McpCompletionContents` factory class.

Previous API:
```java
@Mcp.Completion("prompt")
McpCompletionContent completion() {
    return McpCompletionContents.completion("suggestion");
}
```

Updated API:
```java
@Mcp.Completion("prompt")
McpCompletionResult completion() {
    return McpCompletionResult.create("suggestion");
}
```

For details about the `McpCompletionResult` class, see [README.md](README.md#completion-content-type) and
[Migration Guide](../mcp/migration_guide_1.1.md#migrate-mcpcompletion-interface).