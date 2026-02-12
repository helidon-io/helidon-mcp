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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import io.helidon.common.mapper.OptionalValue;
import io.helidon.jsonrpc.core.JsonRpcParams;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonStructure;
import jakarta.json.spi.JsonProvider;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.JUnitException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class McpParametersTest {
    private static final JsonProvider JSON_PROVIDER = JsonProvider.provider();

    @Test
    void testSimpleString() {
        JsonObject object = JSON_PROVIDER.createObjectBuilder()
                .add("foo", "bar")
                .build();
        JsonRpcParams rpcParams = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(rpcParams, object);
        String foo = parameters.get("foo").asString().orElse(null);
        assertThat(foo, is("bar"));
    }

    @Test
    void testSimpleBoolean() {
        JsonObject object = JSON_PROVIDER.createObjectBuilder()
                .add("foo", true)
                .build();
        JsonRpcParams rpcParams = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(rpcParams, object);
        Boolean foo = parameters.get("foo").asBoolean().orElse(null);
        assertThat(foo, is(true));
    }

    @Test
    void testSimpleByte() {
        JsonObject object = JSON_PROVIDER.createObjectBuilder()
                .add("foo", 1)
                .build();
        JsonRpcParams rpcParams = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(rpcParams, object);
        byte foo = parameters.get("foo").asByte().orElse(null);
        assertThat(foo, is((byte) 1));
    }

    @Test
    void testSimpleShort() {
        JsonObject object = JSON_PROVIDER.createObjectBuilder()
                .add("foo", 1)
                .build();
        JsonRpcParams rpcParams = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(rpcParams, object);
        short foo = parameters.get("foo").asShort().orElse(null);
        assertThat(foo, is((short) 1));
    }

    @Test
    void testSimpleInteger() {
        JsonObject object = JSON_PROVIDER.createObjectBuilder()
                .add("foo", 1)
                .build();
        JsonRpcParams rpcParams = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(rpcParams, object);
        int foo = parameters.get("foo").asInteger().orElse(null);
        assertThat(foo, is(1));
    }

    @Test
    void testSimpleLong() {
        JsonObject object = JSON_PROVIDER.createObjectBuilder()
                .add("foo", 1L)
                .build();
        JsonRpcParams rpcParams = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(rpcParams, object);
        long foo = parameters.get("foo").asLong().orElse(null);
        assertThat(foo, is(1L));
    }

    @Test
    void testSimpleDouble() {
        JsonObject object = JSON_PROVIDER.createObjectBuilder()
                .add("foo", 1.0D)
                .build();
        JsonRpcParams rpcParams = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(rpcParams, object);
        double foo = parameters.get("foo").asDouble().orElse(null);
        assertThat(foo, is(1.0D));
    }

    @Test
    void testSimpleFloat() {
        JsonObject object = JSON_PROVIDER.createObjectBuilder()
                .add("foo", 1.0F)
                .build();
        JsonRpcParams rpcParams = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(rpcParams, object);
        float foo = parameters.get("foo").asFloat().orElse(null);
        assertThat(foo, is(1.0F));
    }

    @Test
    void testSimpleList() {
        JsonObject object = JSON_PROVIDER.createObjectBuilder()
                .add("foo", JSON_PROVIDER.createArrayBuilder()
                        .add("foo1")
                        .add("foo2"))
                .build();
        JsonRpcParams rpcParams = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(rpcParams, object);
        List<String> foo = parameters.get("foo")
                .asList()
                .get()
                .stream()
                .map(McpParameters::asString)
                .map(OptionalValue::get)
                .toList();
        assertThat(foo, is(List.of("foo1", "foo2")));
    }

    @Test
    void testStringList() {
        JsonObject object = JSON_PROVIDER.createObjectBuilder()
                .add("foo", JSON_PROVIDER.createArrayBuilder()
                        .add("foo1")
                        .add("foo2"))
                .build();
        JsonRpcParams rpcParams = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(rpcParams, object);
        List<String> foo = parameters.get("foo")
                .asList(String.class)
                .orElse(List.of());
        assertThat(foo, is(List.of("foo1", "foo2")));
    }

    @Test
    void testBooleanList() {
        JsonArray array = JSON_PROVIDER.createArrayBuilder()
                .add(true)
                .add(false)
                .build();
        JsonRpcParams rpcParams = JsonRpcParams.create(array);
        McpParameters parameters = new McpParameters(rpcParams, array);
        List<Boolean> booleans = parameters.asList(Boolean.class).orElse(List.of());
        assertThat(booleans.size(), is(2));
        assertThat(booleans, is(List.of(true, false)));
    }

    @Test
    void testIntegerList() {
        JsonArray array = JSON_PROVIDER.createArrayBuilder()
                .add(1)
                .add(2)
                .build();
        JsonRpcParams rpcParams = JsonRpcParams.create(array);
        McpParameters parameters = new McpParameters(rpcParams, array);
        List<Integer> booleans = parameters.asList(Integer.class).orElse(List.of());
        assertThat(booleans.size(), is(2));
        assertThat(booleans, is(List.of(1, 2)));
    }

    @Test
    void testLongList() {
        JsonArray array = JSON_PROVIDER.createArrayBuilder()
                .add(1L)
                .add(2L)
                .build();
        JsonRpcParams rpcParams = JsonRpcParams.create(array);
        McpParameters parameters = new McpParameters(rpcParams, array);
        List<Long> booleans = parameters.asList(Long.class).orElse(List.of());
        assertThat(booleans.size(), is(2));
        assertThat(booleans, is(List.of(1L, 2L)));
    }

    @Test
    void testDoubleList() {
        JsonArray array = JSON_PROVIDER.createArrayBuilder()
                .add(1D)
                .add(2D)
                .build();
        JsonRpcParams rpcParams = JsonRpcParams.create(array);
        McpParameters parameters = new McpParameters(rpcParams, array);
        List<Double> booleans = parameters.asList(Double.class).orElse(List.of());
        assertThat(booleans.size(), is(2));
        assertThat(booleans, is(List.of(1D, 2D)));
    }

    @Test
    void testFloatList() {
        JsonArray array = JSON_PROVIDER.createArrayBuilder()
                .add(1.0F)
                .add(2.0F)
                .build();
        JsonRpcParams rpcParams = JsonRpcParams.create(array);
        McpParameters parameters = new McpParameters(rpcParams, array);
        List<Float> booleans = parameters.asList(Float.class).orElse(List.of());
        assertThat(booleans.size(), is(2));
        assertThat(booleans, is(List.of(1.0F, 2.0F)));
    }

    @Test
    void testShortList() {
        JsonArray array = JSON_PROVIDER.createArrayBuilder()
                .add(1)
                .add(2)
                .build();
        JsonRpcParams rpcParams = JsonRpcParams.create(array);
        McpParameters parameters = new McpParameters(rpcParams, array);
        List<Short> booleans = parameters.asList(Short.class).orElse(List.of());
        assertThat(booleans.size(), is(2));
        assertThat(booleans, is(List.of((short) 1, (short) 2)));
    }

    @Test
    void testByteList() {
        JsonArray array = JSON_PROVIDER.createArrayBuilder()
                .add(1)
                .add(2)
                .build();
        JsonRpcParams rpcParams = JsonRpcParams.create(array);
        McpParameters parameters = new McpParameters(rpcParams, array);
        List<Byte> booleans = parameters.asList(Byte.class).orElse(List.of());
        assertThat(booleans.size(), is(2));
        assertThat(booleans, is(List.of((byte) 1, (byte) 2)));
    }

    @Test
    void testPojoList() {
        JsonArray array = JSON_PROVIDER.createArrayBuilder()
                .add(JSON_PROVIDER.createObjectBuilder()
                             .add("foo", "foo1")
                             .add("bar", "bar1"))
                .add(JSON_PROVIDER.createObjectBuilder()
                             .add("foo", "foo2")
                             .add("bar", "bar2"))
                .build();
        JsonRpcParams rpcParams = JsonRpcParams.create(array);
        McpParameters parameters = new McpParameters(rpcParams, array);
        List<Foo> foos = parameters
                .asList(Foo.class)
                .orElse(List.of());
        assertThat(foos.size(), is(2));

        Foo foo1 = foos.getFirst();
        assertThat(foo1.foo, is("foo1"));
        assertThat(foo1.bar, is("bar1"));

        Foo foo2 = foos.getLast();
        assertThat(foo2.foo, is("foo2"));
        assertThat(foo2.bar, is("bar2"));
    }

    @Test
    void testListOfMap() {
        JsonArray array = JSON_PROVIDER.createArrayBuilder()
                .add(JSON_PROVIDER.createObjectBuilder()
                             .add("value1", JSON_PROVIDER.createObjectBuilder()
                                     .add("foo", "foo1")
                                     .add("bar", "bar1")))
                .add(JSON_PROVIDER.createObjectBuilder()
                             .add("value2", JSON_PROVIDER.createObjectBuilder()
                                     .add("foo", "foo2")
                                     .add("bar", "bar2")))
                .build();
        JsonRpcParams rpcParams = JsonRpcParams.create(array);
        McpParameters parameters = new McpParameters(rpcParams, array);
        List<Map<String, McpParameters>> listMap = parameters.asList()
                .map(list -> list.stream()
                        .map(McpParameters::asMap)
                        .filter(OptionalValue::isPresent)
                        .map(OptionalValue::get)
                        .toList())
                .orElse(List.of());
        assertThat(listMap.size(), is(2));

        Map<String, McpParameters> map = listMap.getFirst();
        assertThat(map.size(), is(1));
        assertThat(map.containsKey("value1"), is(true));

        Foo value1 = map.get("value1")
                .as(Foo.class)
                .orElseThrow(() -> new JUnitException("Cannot convert value1 to Foo.class"));
        assertThat(value1.foo, is("foo1"));
        assertThat(value1.bar, is("bar1"));

        map = listMap.getLast();
        assertThat(map.size(), is(1));
        assertThat(map.containsKey("value2"), is(true));

        Foo value2 = map.get("value2")
                .as(Foo.class)
                .orElseThrow(() -> new JUnitException("Cannot convert value2 to Foo.class"));
        assertThat(value2.foo, is("foo2"));
        assertThat(value2.bar, is("bar2"));
    }

    @Test
    void testMapOfList() {
        JsonObject object = JSON_PROVIDER.createObjectBuilder()
                .add("value1", JSON_PROVIDER.createArrayBuilder()
                        .add(JSON_PROVIDER.createObjectBuilder()
                                     .add("foo", "foo1")
                                     .add("bar", "bar1")))
                .add("value2", JSON_PROVIDER.createArrayBuilder()
                        .add(JSON_PROVIDER.createObjectBuilder()
                                     .add("foo", "foo2")
                                     .add("bar", "bar2")))
                .build();
        JsonRpcParams rpcParams = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(rpcParams, object);
        Map<String, McpParameters> map = parameters.asMap().orElse(Map.of());
        assertThat(map.size(), is(2));
        assertThat(map.containsKey("value1"), is(true));
        assertThat(map.containsKey("value2"), is(true));

        List<Foo> value1 = map.get("value1").asList(Foo.class).orElse(List.of());
        assertThat(value1.size(), is(1));

        Foo foo1 = value1.getFirst();
        assertThat(foo1.foo, is("foo1"));
        assertThat(foo1.bar, is("bar1"));

        List<Foo> value2 = map.get("value2").asList(Foo.class).orElse(List.of());
        assertThat(value2.size(), is(1));

        Foo foo2 = value2.getFirst();
        assertThat(foo2.foo, is("foo2"));
        assertThat(foo2.bar, is("bar2"));
    }

    @Test
    void testNestedObject() {
        JsonObject object = JSON_PROVIDER.createObjectBuilder()
                .add("person", JSON_PROVIDER.createObjectBuilder()
                        .add("name", "Frank")
                        .add("age", 10))
                .build();
        JsonRpcParams rpcParams = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(rpcParams, object);
        String name = parameters.get("person").get("name").asString().orElse(null);
        int age = parameters.get("person").get("age").asInteger().orElse(-1);

        assertThat(name, is("Frank"));
        assertThat(age, is(10));
    }

    @Test
    void testCasting() {
        JsonStructure object = JSON_PROVIDER.createObjectBuilder()
                .add("foo", "value1")
                .add("bar", "value2")
                .build();
        JsonRpcParams params = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(params, object);
        Foo foo = parameters.as(Foo.class).get();

        assertThat(foo.foo, is("value1"));
        assertThat(foo.bar, is("value2"));
    }

    @Test
    void testNestedCasting() {
        JsonStructure object = JSON_PROVIDER.createObjectBuilder()
                .add("foo", JSON_PROVIDER.createObjectBuilder()
                        .add("foo", "value1")
                        .add("bar", "value2"))
                .build();
        JsonRpcParams params = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(params, object);
        Foo foo = parameters.get("foo").as(Foo.class).get();

        assertThat(foo.foo, is("value1"));
        assertThat(foo.bar, is("value2"));
    }

    @Test
    void testIsNumberInt() {
        JsonStructure object = JSON_PROVIDER.createObjectBuilder()
                .add("foo", 1)
                .build();
        JsonRpcParams params = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(params, object);
        boolean isNumber = parameters.get("foo").isNumber();

        assertThat(isNumber, is(true));
    }

    @Test
    void testIsNumberDouble() {
        JsonStructure object = JSON_PROVIDER.createObjectBuilder()
                .add("foo", 1.0)
                .build();
        JsonRpcParams params = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(params, object);
        boolean isNumber = parameters.get("foo").isNumber();

        assertThat(isNumber, is(true));
    }

    @Test
    void testIsNumberString() {
        JsonStructure object = JSON_PROVIDER.createObjectBuilder()
                .add("foo", "notANumber")
                .build();
        JsonRpcParams params = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(params, object);
        boolean isNumber = parameters.get("foo").isNumber();

        assertThat(isNumber, is(false));
    }

    @Test
    void testIsString() {
        JsonStructure object = JSON_PROVIDER.createObjectBuilder()
                .add("foo", "notANumber")
                .build();
        JsonRpcParams params = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(params, object);
        boolean isNumber = parameters.get("foo").isString();

        assertThat(isNumber, is(true));
    }

    @Test
    void testIsStringNumber() {
        JsonStructure object = JSON_PROVIDER.createObjectBuilder()
                .add("foo", 1)
                .build();
        JsonRpcParams params = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(params, object);
        boolean isNumber = parameters.get("foo").isString();

        assertThat(isNumber, is(false));
    }

    @Test
    void testIfPresent() {
        AtomicBoolean present = new AtomicBoolean(false);
        JsonStructure object = JSON_PROVIDER.createObjectBuilder()
                .add("foo", 1)
                .build();
        JsonRpcParams params = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(params, object);
        parameters.get("foo").ifPresent(it -> present.set(true));
        assertThat(present.get(), is(true));
    }

    @Test
    void testIfNotPresent() {
        AtomicBoolean present = new AtomicBoolean(false);
        JsonStructure object = JSON_PROVIDER.createObjectBuilder()
                .add("foo", 1)
                .build();
        JsonRpcParams params = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(params, object);
        parameters.get("bar").ifPresent(it -> present.set(true));
        assertThat(present.get(), is(false));
    }

    @Test
    void testIfPresentNullPointerException() {
        JsonStructure object = JSON_PROVIDER.createObjectBuilder()
                .add("foo", 1)
                .build();
        JsonRpcParams params = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(params, object);
        try {
            parameters.get("foo").ifPresent(null);
            assertThat("NullPointerException must be thrown", true, is(false));
        } catch (NullPointerException exception) {
            assertThat(exception.getMessage(), is("action is null"));
        }
    }

    @Test
    void testAsMap() {
        JsonObject object = JSON_PROVIDER.createObjectBuilder()
                .add("foo", "foo")
                .add("bar", "bar")
                .build();
        JsonRpcParams params = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(params, object);

        Map<String, McpParameters> map = parameters.asMap()
                .orElseGet(HashMap::new);
        assertThat(map.size(), is(2));
        assertThat(map.containsKey("foo"), is(true));
        assertThat(map.containsKey("bar"), is(true));

        McpParameters foo = map.get("foo");
        assertThat(foo.isPresent(), is(true));
        assertThat(foo.asString().orElse(""), is("foo"));

        McpParameters bar = map.get("bar");
        assertThat(bar.isPresent(), is(true));
        assertThat(bar.asString().orElse(""), is("bar"));
    }

    @Test
    void testAsStringMap() {
        JsonObject object = JSON_PROVIDER.createObjectBuilder()
                .add("foo", "foo")
                .add("bar", "bar")
                .build();
        JsonRpcParams params = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(params, object);

        Map<String, String> map = parameters.asMap()
                .orElseGet(HashMap::new)
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                                          e -> e.getValue().asString().orElse("")));

        assertThat(map.size(), is(2));
        assertThat(map.containsKey("foo"), is(true));
        assertThat(map.containsKey("bar"), is(true));
        assertThat(map.get("foo"), is("foo"));
        assertThat(map.get("bar"), is("bar"));
    }

    public static class Foo {
        public String foo;
        public String bar;
    }
}
