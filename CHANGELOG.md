# Changelog

All notable changes to this project will be documented in this file.

## [1.0.0]

This is the main release of Helidon MCP. It supports [Model Context Protocol 2024-11-05](https://modelcontextprotocol.io/specification/2024-11-05)
and bring additional support for [Model Context Protocol 2025-03-26](https://modelcontextprotocol.io/specification/2025-03-26).
Helidon MCP is an incubating feature and its API is subject to change.

### NOTABLE CHANGES

Helidon MCP 1.0.0 introduces major improvement on `2025-03-26` MCP specification support:

- Security
- Cancellation feature
- Resource subscription/unsubscription

### BREAKING CHANGES

Helidon MCP 1.0.0 release brings two backward incompatible changes.

- Prompt arguments returns a `List` instead of a `Set`.

Due to issue with argument ordering using a `Set`, it now uses a `List` and stay consistent.

- JSON Schema annotation `@Mcp.JsonSchema` is replaced by `@JsonSchema.Schema`.

Helidon MCP now leverage Helidon JSON Schema introduced in 4.3.0 release. POJOs used as tool inputs do not need
to provide their JSON Schema as string. Helidon will generate it for you!

### CHANGES

- Add emergency level log method [64](https://github.com/helidon-io/helidon-mcp/pull/64)
- Initial support for Helidon JSON schema [61](https://github.com/helidon-io/helidon-mcp/pull/61)
- Add MCP security layer [63](https://github.com/helidon-io/helidon-mcp/pull/63)
- Declarative support for subscribers/unsubscribers and docs [62](https://github.com/helidon-io/helidon-mcp/pull/62)
- Add emergency log level [59](https://github.com/helidon-io/helidon-mcp/pull/59)
- Update Helidon and Langchain4j versions [57](https://github.com/helidon-io/helidon-mcp/pull/57)
- Add support for resource subscribers and unsubscribers [56](https://github.com/helidon-io/helidon-mcp/pull/56)
- Add Cancellation feature [46](https://github.com/helidon-io/helidon-mcp/pull/46)
- Improve support for prompt and resource completions [52](https://github.com/helidon-io/helidon-mcp/pull/52)
- Update arguments() method in McpPrompt to return a List instead of a Set in order to preserve ordering. [54](https://github.com/helidon-io/helidon-mcp/pull/54)
- Support for tool annotations [49](https://github.com/helidon-io/helidon-mcp/pull/49)
- Updates docs with new audio type. Some other minor fixes for consistency. [51](https://github.com/helidon-io/helidon-mcp/pull/51)

## [1.0.0-M2]

This is the second milestone release of Helidon MCP. It supports [Model Context Protocol 2024-11-05](https://modelcontextprotocol.io/specification/2024-11-05) 
and introduces partial support for [Model Context Protocol 2025-03-26](https://modelcontextprotocol.io/specification/2025-03-26).
Helidon MCP is an incubating feature and its API is subject to change.

### CHANGES

- Add support for message field in progress notifications [45](https://github.com/helidon-io/helidon-mcp/pull/45)
- Updates image content API and adds support for audio [44](https://github.com/helidon-io/helidon-mcp/pull/44)
- Contributing work from streamable-http branch into main [43](https://github.com/helidon-io/helidon-mcp/pull/43)
- Uptake Helidon version to 4.3.0-M3 [41](https://github.com/helidon-io/helidon-mcp/pull/41)
- Add resolved parameters for Resource templates [38](https://github.com/helidon-io/helidon-mcp/pull/38)
- Adds initial support for Streamable HTTP transport [33](https://github.com/helidon-io/helidon-mcp/pull/33)
- Add Pagination feature [21](https://github.com/helidon-io/helidon-mcp/pull/21)
- Add calendar manager example [20](https://github.com/helidon-io/helidon-mcp/pull/20)

## [1.0.0-M1]

This is the first milestone release of Helidon MCP. It supports [Model Context Protocol 2024-11-05](https://modelcontextprotocol.io/specification/2024-11-05).
Helidon MCP is an incubating feature and its API is subject to change.

Requirements:

* Java 21
* Helidon 4.3.0 or newer

## CHANGES

Initial release.

[1.0.0]: https://github.com/oracle/helidon/compare/1.0.0-M2...1.0.0
[1.0.0-M2]: https://github.com/oracle/helidon/compare/1.0.0-M1...1.0.0-M2
[1.0.0-M1]: https://github.com/oracle/helidon/compare/main...1.0.0-M1

