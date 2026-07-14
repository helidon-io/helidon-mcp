# Upgrade Guide for Version 1.2.0

Helidon MCP `1.2.0` upgrades the Helidon baseline to `4.5.0` and replaces the server's JSON-B and JSON-P integration
with Helidon JSON Binding. This document summarizes the key changes and explains how to migrate existing code to
`1.2.0`.

## Overview of Changes

- The Helidon version is upgraded to `4.5.0`.
- Helidon MCP now uses Helidon JSON Binding for MCP parameter conversion, structured tool content, and sampling
  metadata.
- Yasson and the JSON-B, JSON-P, and Parsson dependencies are no longer provided by Helidon MCP.
- Custom classes used for MCP JSON serialization or deserialization must have a discoverable Helidon JSON converter.
- `McpSamplingRequest.metadata()` now returns `Optional<Object>`, and its builder accepts `Object` instead of
  `jakarta.json.JsonValue`.
- `McpCancellationResult` is now a sealed interface, and `reason()` returns `Optional<String>` instead of `String`.

The sections below describe how to migrate applications to version `1.2.0`.

## Migrate cancellation result handling

An MCP cancellation notification can omit its reason. To represent this correctly, `McpCancellationResult` is now a
sealed interface and its `reason()` method returns `Optional<String>` instead of `String`.

Previous usage:

```java
String reason = cancellation.result().reason();
```

Updated usage:

```java
McpCancellationResult result = cancellation.result();
if (result.isRequested()) {
    String reason = result.reason().orElse("Cancellation requested");
}
```

`reason()` returns `Optional.empty()` both before cancellation is requested and when a cancellation notification omits
the reason. Use `isRequested()` to distinguish between those states.

Changing `McpCancellationResult` from a class to an interface and changing the return type of `reason()` create binary
and source incompatibilities. Applications compiled against an earlier version must be recompiled, including
applications that only call `isRequested()`. Otherwise, previously compiled code can fail with an
`IncompatibleClassChangeError` or `NoSuchMethodError` at runtime.

## Migrate from JSON-B to Helidon JSON Binding

Helidon MCP now uses Helidon JSON Binding for parameter conversion, structured tool content, and sampling metadata.
Yasson and the JSON-B, JSON-P, and Parsson dependencies are no longer used or provided by Helidon MCP.

### Application impact

Maps, collections, arrays, primitives, and enums continue to work without additional configuration. Applications that
use custom classes with MCP parameter conversion, structured content, or sampling metadata must provide a Helidon JSON
converter. JSON-B annotations and unregistered POJOs are no longer supported by Helidon MCP.

Applications can continue to use JSON-B independently for their own serialization, but they must declare and configure
those dependencies themselves.

### Providing a converter

Helidon MCP creates its own default `JsonBinding` instance, so converters must be discoverable through the Helidon
Service Registry. Registering a converter only with a separate, application-owned `JsonBinding` instance does not make
that converter available to Helidon MCP.

Parameter conversion through `McpParameters.as(...)` requires a deserializer. Structured tool content and sampling
metadata require a serializer. A `JsonConverter` provides both directions.

#### Generate a converter

The recommended approach for application-owned classes is to let Helidon generate and register the converter. Declare
the JSON binding dependency directly:

```xml
<dependency>
    <groupId>io.helidon.json</groupId>
    <artifactId>helidon-json-binding</artifactId>
</dependency>
```

Annotate each custom class with `@Json.Entity`:

```java
import io.helidon.json.binding.Json;

@Json.Entity
record Result(String value) {
}
```

Enable the Helidon annotation processors in the application build. The APT bundle contains both the JSON converter and
Service Registry processors:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <annotationProcessorPaths>
                    <path>
                        <groupId>io.helidon.bundles</groupId>
                        <artifactId>helidon-bundles-apt</artifactId>
                        <version>${helidon.version}</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

Use the same Helidon version as the application. During compilation, Helidon generates a `JsonConverter` and its
Service Registry descriptor. Helidon MCP then discovers the converter automatically at runtime; no MCP-specific
registration is required.

#### Provide a handwritten converter

For a third-party type that cannot be annotated, or when its JSON representation requires custom logic, implement
`JsonConverter` and register the implementation as a Helidon service:

```java
import io.helidon.common.GenericType;
import io.helidon.json.JsonGenerator;
import io.helidon.json.JsonObject;
import io.helidon.json.JsonParser;
import io.helidon.json.binding.JsonConverter;
import io.helidon.service.registry.Service;

@Service.Singleton
final class ExternalResultConverter implements JsonConverter<ExternalResult> {
    private static final GenericType<ExternalResult> TYPE = GenericType.create(ExternalResult.class);

    @Override
    public void serialize(JsonGenerator generator, ExternalResult result, boolean writeNulls) {
        generator.writeObjectStart();
        generator.write("value", result.value());
        generator.writeObjectEnd();
    }

    @Override
    public ExternalResult deserialize(JsonParser parser) {
        JsonObject object = parser.readJsonObject();
        return new ExternalResult(object.stringValue("value").orElseThrow());
    }

    @Override
    public GenericType<ExternalResult> type() {
        return TYPE;
    }
}
```

The `helidon-bundles-apt` configuration shown above generates the Service Registry descriptor that makes the
handwritten converter discoverable. If only one direction is required, an application may instead implement
`JsonSerializer` or `JsonDeserializer` and register it in the same way.

See the [Helidon JSON Processing documentation](https://helidon.io/docs/v4/se/json#json-binding) for converter
generation, annotation processor alternatives, custom serializers and deserializers, and binding factories.

### Sampling metadata incompatibility

`McpSamplingRequest.metadata()` now returns `Optional<Object>`, and the builder accepts `Object` instead of
`jakarta.json.JsonValue`. This is a backward-incompatible API change. Recompile existing applications and replace JSON-P
metadata values with regular Java values or Helidon JSON-bound classes.

Previous usage:

```java
McpSamplingRequest request = McpSamplingRequest.builder()
        .metadata(JsonValue.TRUE)
        .build();
```

Updated usage:

```java
McpSamplingRequest request = McpSamplingRequest.builder()
        .metadata(Map.of("requestId", "example-request"))
        .build();
```

Code compiled against the previous builder signature must be rebuilt to avoid a `NoSuchMethodError` at runtime.
