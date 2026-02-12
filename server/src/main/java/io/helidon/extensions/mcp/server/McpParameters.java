/*
 * Copyright (c) 2025, 2026 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.helidon.extensions.mcp.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import io.helidon.common.GenericType;
import io.helidon.common.mapper.MapperException;
import io.helidon.common.mapper.Mappers;
import io.helidon.common.mapper.OptionalValue;
import io.helidon.jsonrpc.core.JsonRpcParams;

import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

/**
 * MCP client parameters provided to {@link McpTool} and {@link McpPrompt}.
 */
public final class McpParameters {
    private static final Mappers MAPPERS = Mappers.create();
    private static final EmptyValue EMPTY_VALUE = new EmptyValue();
    private static final McpParameters EMPTY = new McpParameters(JsonValue.NULL, "null");

    private final String key;
    private final JsonValue value;
    private final JsonRpcParams params;

    McpParameters(JsonRpcParams params, JsonValue root) {
        this.params = params;
        this.value = root;
        this.key = "key";
    }

    private McpParameters(JsonRpcParams params, JsonValue root, String key) {
        this.params = params;
        this.value = root;
        this.key = key;
    }

    private McpParameters(JsonValue root, String key) {
        this(JsonRpcParams.create(JsonObject.EMPTY_JSON_OBJECT), root, key);
    }

    /**
     * Get MCP parameter node.
     *
     * @param key node key
     * @return parameter
     */
    public McpParameters get(String key) {
        if (value instanceof JsonObject jsonObject) {
            JsonValue v = jsonObject.get(key);
            if (v != null) {
                if (params.get(key) instanceof JsonObject rpcObject) {
                    JsonRpcParams rpcParams = JsonRpcParams.create(rpcObject);
                    return new McpParameters(rpcParams, v, key);
                }
                if (params.get(key) instanceof JsonArray jsonArray) {
                    JsonRpcParams rpcParams = JsonRpcParams.create(jsonArray);
                    return new McpParameters(rpcParams, v, key);
                }
                return new McpParameters(params, v, key);
            }
            return EMPTY;
        }
        if (value == JsonValue.NULL) {
            return EMPTY;
        }
        throw new IllegalStateException("Cannot get " + value.getValueType() + " as an object");
    }

    /**
     * Get Optional value of the parameter as a string.
     *
     * @return optional string value
     */
    public OptionalValue<String> asString() {
        if (value instanceof JsonString jsonString) {
            return OptionalValue.create(MAPPERS, key, jsonString.getString());
        }
        if (value == JsonValue.NULL) {
            return empty();
        }
        throw new IllegalStateException("Cannot get " + value.getValueType() + " as a string");
    }

    /**
     * If a value is not present, returns {@code true}, otherwise {@code false}.
     *
     * @return {@code true} if a value is not present, otherwise {@code false}
     */
    public boolean isEmpty() {
        return value == JsonValue.NULL;
    }

    /**
     * If a value is present, returns {@code true}, otherwise {@code false}.
     *
     * @return {@code true} if a value is present, otherwise {@code false}
     */
    public boolean isPresent() {
        return value != JsonValue.NULL;
    }

    /**
     * If a value is present, performs the given action with the value,
     * otherwise does nothing.
     *
     * @param action the action to be performed, if a value is present
     * @throws java.lang.NullPointerException if the action is {@code null}
     */
    public void ifPresent(Consumer<McpParameters> action) {
        Objects.requireNonNull(action, "action is null");
        if (isPresent()) {
            action.accept(this);
        }
    }

    /**
     * If the value is a JSON number, returns {@code true}, otherwise {code false}.
     *
     * @return {@code true} if value is a JSON number, otherwise {@code false}
     */
    public boolean isNumber() {
        return value instanceof JsonNumber;
    }

    /**
     * If the value is a JSON string, returns {@code true}, otherwise {code false}.
     *
     * @return {@code true} if value is a JSON string, otherwise {@code false}
     */
    public boolean isString() {
        return value instanceof JsonString;
    }

    /**
     * Get optional value of the parameter as a Byte.
     *
     * @return optional byte value
     */
    public OptionalValue<Byte> asByte() {
        if (value instanceof JsonNumber number) {
            return OptionalValue.create(MAPPERS, key, number.numberValue().byteValue());
        }
        if (value == JsonValue.NULL) {
            return empty();
        }
        throw new IllegalStateException("Cannot get " + value.getValueType() + "as a byte");
    }

    /**
     * Get optional value of the parameter as a Short.
     *
     * @return optional short value
     */
    public OptionalValue<Short> asShort() {
        if (value instanceof JsonNumber number) {
            return OptionalValue.create(MAPPERS, key, number.bigDecimalValue().shortValue());
        }
        if (value == JsonValue.NULL) {
            return empty();
        }
        throw new IllegalStateException("Cannot get " + value.getValueType() + "as a short");
    }

    /**
     * Get optional value of the parameter as an Integer.
     *
     * @return optional integer value
     */
    public OptionalValue<Integer> asInteger() {
        if (value instanceof JsonNumber number) {
            return OptionalValue.create(MAPPERS, key, number.intValue());
        }
        if (value == JsonValue.NULL) {
            return empty();
        }
        throw new IllegalStateException("Cannot get " + value.getValueType() + "as an integer");
    }

    /**
     * Get optional value of the parameter as a Long.
     *
     * @return optional long value
     */
    public OptionalValue<Long> asLong() {
        if (value instanceof JsonNumber number) {
            return OptionalValue.create(MAPPERS, key, number.longValue());
        }
        if (value == JsonValue.NULL) {
            return empty();
        }
        throw new IllegalStateException("Cannot get " + value.getValueType() + "as a long");
    }

    /**
     * Get optional value of the parameter as a Double.
     *
     * @return optional double value
     */
    public OptionalValue<Double> asDouble() {
        if (value instanceof JsonNumber number) {
            return OptionalValue.create(MAPPERS, key, number.doubleValue());
        }
        if (value == JsonValue.NULL) {
            return empty();
        }
        throw new IllegalStateException("Cannot get " + value.getValueType() + "as a double");
    }

    /**
     * Get optional value of the parameter as a Float.
     *
     * @return optional float value
     */
    public OptionalValue<Float> asFloat() {
        if (value instanceof JsonNumber number) {
            return OptionalValue.create(MAPPERS, key, number.bigDecimalValue().floatValue());
        }
        if (value == JsonValue.NULL) {
            return empty();
        }
        throw new IllegalStateException("Cannot get " + value.getValueType() + "as a float");
    }

    /**
     * Get optional value of the parameter as a boolean.
     *
     * @return optional boolean value
     */
    public OptionalValue<Boolean> asBoolean() {
        if (value == JsonValue.TRUE) {
            return OptionalValue.create(MAPPERS, key, true);
        }
        if (value == JsonValue.FALSE) {
            return OptionalValue.create(MAPPERS, key, false);
        }
        if (value == JsonValue.NULL) {
            return empty();
        }
        throw new IllegalStateException("Cannot get " + value.getValueType() + "as a boolean");
    }

    /**
     * Get optional value of the parameter as a list.
     *
     * @return optional list value
     */
    public OptionalValue<List<McpParameters>> asList() {
        if (value == JsonValue.NULL) {
            return empty();
        }
        if (value instanceof JsonArray array) {
            List<McpParameters> list = new ArrayList<>();
            int i = 0;
            for (JsonValue value : array) {
                if (value instanceof JsonObject object) {
                    list.add(new McpParameters(JsonRpcParams.create(object), value, key + "-" + i++));
                    continue;
                }
                if (value instanceof JsonArray jsonArray) {
                    list.add(new McpParameters(JsonRpcParams.create(jsonArray), value, key + "-" + i++));
                    continue;
                }
                list.add(new McpParameters(params, value, key + "-" + i++));
            }
            return OptionalValue.create(MAPPERS, key, list);
        }
        throw new IllegalStateException("Cannot get " + value.getValueType() + "as a list");
    }

    /**
     * Get optional value of the parameter as a list of provided type.
     *
     * @param clazz the type class
     * @param <T>   type
     * @return List of provided type
     */
    public <T> OptionalValue<List<T>> asList(Class<T> clazz) {
        List<T> result = asList()
                .map(list -> list.stream()
                        .map(p -> p.as(clazz)
                                .orElseThrow(() -> new McpException("Cannot convert element to " + clazz.getSimpleName())))
                        .toList())
                .orElseGet(List::of);
        return OptionalValue.create(MAPPERS, key, result);
    }

    /**
     * Get optional value of the parameter as a map.
     *
     * @return optional map value
     */
    public OptionalValue<Map<String, McpParameters>> asMap() {
        if (value instanceof JsonObject object) {
            int i = 0;
            Map<String, McpParameters> map = new HashMap<>();
            for (Map.Entry<String, JsonValue> entry : object.entrySet()) {
                McpParameters parameter;
                if (entry.getValue() instanceof JsonObject jsonObject) {
                    JsonRpcParams rpcParams = JsonRpcParams.create(jsonObject);
                    parameter = new McpParameters(rpcParams, jsonObject, entry.getKey() + "-" + i++);
                } else if (entry.getValue() instanceof JsonArray array) {
                    JsonRpcParams rpcParams = JsonRpcParams.create(array);
                    parameter = new McpParameters(rpcParams, array, entry.getKey() + "-" + i++);
                } else {
                    parameter = new McpParameters(params, entry.getValue(), entry.getKey() + "-" + i++);
                }
                map.put(entry.getKey(), parameter);
            }
            return OptionalValue.create(MAPPERS, key, map);
        }
        if (value == JsonValue.NULL) {
            return empty();
        }
        throw new IllegalStateException("Cannot get " + value.getValueType() + "as a map");
    }

    /**
     * Get optional value of the parameter as the mapping function.
     *
     * @param function mapping function
     * @param <T>      optional value type
     * @return optional value
     */
    public <T> OptionalValue<T> as(Function<McpParameters, T> function) {
        if (value == JsonValue.NULL) {
            return empty();
        }
        return OptionalValue.create(MAPPERS, key, function.apply(this));
    }

    /**
     * Get optional value of the parameter as the mapping class.
     *
     * @param clazz mapping class
     * @param <T>   class type
     * @return optional value
     */
    @SuppressWarnings("unchecked")
    public <T> OptionalValue<T> as(Class<T> clazz) {
        if (value == JsonValue.NULL) {
            return empty();
        }
        return switch (clazz.getName()) {
            case "java.lang.Byte" -> (OptionalValue<T>) asByte();
            case "java.lang.Long" -> (OptionalValue<T>) asLong();
            case "java.lang.Float" -> (OptionalValue<T>) asFloat();
            case "java.lang.Short" -> (OptionalValue<T>) asShort();
            case "java.lang.Double" -> (OptionalValue<T>) asDouble();
            case "java.lang.String" -> (OptionalValue<T>) asString();
            case "java.lang.Boolean" -> (OptionalValue<T>) asBoolean();
            case "java.lang.Integer" -> (OptionalValue<T>) asInteger();
            default -> OptionalValue.create(MAPPERS, key, params.as(clazz));
        };
    }

    /**
     * Get optional value of the parameter as the mapping type.
     *
     * @param type mapping type
     * @param <T>  type
     * @return optional value
     */
    public <T> OptionalValue<T> as(GenericType<T> type) {
        if (value == JsonValue.NULL) {
            return empty();
        }
        return OptionalValue.create(MAPPERS, key, type);
    }

    @SuppressWarnings("unchecked")
    private static <T> OptionalValue<T> empty() {
        return (OptionalValue<T>) EMPTY_VALUE;
    }

    private static final class EmptyValue implements OptionalValue<Object> {

        @SuppressWarnings("unchecked")
        @Override
        public <N> OptionalValue<N> as(Class<N> type) {
            return (OptionalValue<N>) this;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <N> OptionalValue<N> as(GenericType<N> type) {
            return (OptionalValue<N>) this;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <N> OptionalValue<N> as(Function<? super Object, ? extends N> mapper) {
            return (OptionalValue<N>) this;
        }

        @Override
        public Optional<Object> asOptional() throws MapperException {
            return Optional.empty();
        }

        @Override
        public String name() {
            return "empty";
        }

        @Override
        public Object get() {
            throw new NoSuchElementException();
        }
    }

}
