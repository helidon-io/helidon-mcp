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

import io.helidon.common.GenericType;
import io.helidon.common.mapper.OptionalValue;
import io.helidon.json.JsonArray;
import io.helidon.json.JsonObject;
import io.helidon.json.JsonParser;
import io.helidon.json.binding.Json;
import io.helidon.jsonrpc.core.JsonRpcParams;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.JUnitException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class McpParametersTest {
    @Test
    void testSimpleString() {
        JsonObject object = JsonParser.create("{\"foo\":\"bar\"}").readJsonObject();
        JsonRpcParams rpcParams = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(rpcParams);
        String foo = parameters.get("foo").asString().orElse(null);
        assertThat(foo, is("bar"));
    }

    @Test
    void testSimpleBoolean() {
        JsonObject object = JsonParser.create("{\"foo\":true}").readJsonObject();
        JsonRpcParams rpcParams = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(rpcParams);
        Boolean foo = parameters.get("foo").asBoolean().orElse(null);
        assertThat(foo, is(true));
    }

    @Test
    void testSimpleByte() {
        JsonObject object = JsonParser.create("{\"foo\":1}").readJsonObject();
        JsonRpcParams rpcParams = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(rpcParams);
        byte foo = parameters.get("foo").asByte().orElse(null);
        assertThat(foo, is((byte) 1));
    }

    @Test
    void testSimpleShort() {
        JsonObject object = JsonParser.create("{\"foo\":1}").readJsonObject();
        JsonRpcParams rpcParams = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(rpcParams);
        short foo = parameters.get("foo").asShort().orElse(null);
        assertThat(foo, is((short) 1));
    }

    @Test
    void testSimpleInteger() {
        JsonObject object = JsonParser.create("{\"foo\":1}").readJsonObject();
        JsonRpcParams rpcParams = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(rpcParams);
        int foo = parameters.get("foo").asInteger().orElse(null);
        assertThat(foo, is(1));
    }

    @Test
    void testSimpleLong() {
        JsonObject object = JsonParser.create("{\"foo\":1}").readJsonObject();
        JsonRpcParams rpcParams = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(rpcParams);
        long foo = parameters.get("foo").asLong().orElse(null);
        assertThat(foo, is(1L));
    }

    @Test
    void testSimpleDouble() {
        JsonObject object = JsonParser.create("{\"foo\":1.0}").readJsonObject();
        JsonRpcParams rpcParams = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(rpcParams);
        double foo = parameters.get("foo").asDouble().orElse(null);
        assertThat(foo, is(1.0D));
    }

    @Test
    void testSimpleFloat() {
        JsonObject object = JsonParser.create("{\"foo\":1.0}").readJsonObject();
        JsonRpcParams rpcParams = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(rpcParams);
        float foo = parameters.get("foo").asFloat().orElse(null);
        assertThat(foo, is(1.0F));
    }

    @Test
    void testSimpleList() {
        JsonObject object = JsonParser.create("{\"foo\":[\"foo1\",\"foo2\"]}").readJsonObject();
        JsonRpcParams rpcParams = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(rpcParams);
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
        JsonObject object = JsonParser.create("{\"foo\":[\"foo1\",\"foo2\"]}").readJsonObject();
        JsonRpcParams rpcParams = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(rpcParams);
        List<String> foo = parameters.get("foo")
                .asList(String.class)
                .orElse(List.of());
        assertThat(foo, is(List.of("foo1", "foo2")));
    }

    @Test
    void testBooleanList() {
        JsonArray array = JsonParser.create("[true,false]").readJsonArray();
        JsonRpcParams rpcParams = JsonRpcParams.create(array);
        McpParameters parameters = new McpParameters(rpcParams);
        List<Boolean> booleans = parameters.asList(Boolean.class).orElse(List.of());
        assertThat(booleans.size(), is(2));
        assertThat(booleans, is(List.of(true, false)));
    }

    @Test
    void testIntegerList() {
        JsonArray array = JsonParser.create("[1,2]").readJsonArray();
        JsonRpcParams rpcParams = JsonRpcParams.create(array);
        McpParameters parameters = new McpParameters(rpcParams);
        List<Integer> booleans = parameters.asList(Integer.class).orElse(List.of());
        assertThat(booleans.size(), is(2));
        assertThat(booleans, is(List.of(1, 2)));
    }

    @Test
    void testLongList() {
        JsonArray array = JsonParser.create("[1,2]").readJsonArray();
        JsonRpcParams rpcParams = JsonRpcParams.create(array);
        McpParameters parameters = new McpParameters(rpcParams);
        List<Long> booleans = parameters.asList(Long.class).orElse(List.of());
        assertThat(booleans.size(), is(2));
        assertThat(booleans, is(List.of(1L, 2L)));
    }

    @Test
    void testDoubleList() {
        JsonArray array = JsonParser.create("[1.0,2.0]").readJsonArray();
        JsonRpcParams rpcParams = JsonRpcParams.create(array);
        McpParameters parameters = new McpParameters(rpcParams);
        List<Double> booleans = parameters.asList(Double.class).orElse(List.of());
        assertThat(booleans.size(), is(2));
        assertThat(booleans, is(List.of(1D, 2D)));
    }

    @Test
    void testFloatList() {
        JsonArray array = JsonParser.create("[1.0,2.0]").readJsonArray();
        JsonRpcParams rpcParams = JsonRpcParams.create(array);
        McpParameters parameters = new McpParameters(rpcParams);
        List<Float> booleans = parameters.asList(Float.class).orElse(List.of());
        assertThat(booleans.size(), is(2));
        assertThat(booleans, is(List.of(1.0F, 2.0F)));
    }

    @Test
    void testShortList() {
        JsonArray array = JsonParser.create("[1,2]").readJsonArray();
        JsonRpcParams rpcParams = JsonRpcParams.create(array);
        McpParameters parameters = new McpParameters(rpcParams);
        List<Short> booleans = parameters.asList(Short.class).orElse(List.of());
        assertThat(booleans.size(), is(2));
        assertThat(booleans, is(List.of((short) 1, (short) 2)));
    }

    @Test
    void testByteList() {
        JsonArray array = JsonParser.create("[1,2]").readJsonArray();
        JsonRpcParams rpcParams = JsonRpcParams.create(array);
        McpParameters parameters = new McpParameters(rpcParams);
        List<Byte> booleans = parameters.asList(Byte.class).orElse(List.of());
        assertThat(booleans.size(), is(2));
        assertThat(booleans, is(List.of((byte) 1, (byte) 2)));
    }

    @Test
    void testPojoList() {
        JsonArray array = JsonParser.create("[{\"foo\":\"foo1\",\"bar\":\"bar1\"},{\"foo\":\"foo2\",\"bar\":\"bar2\"}]").readJsonArray();
        JsonRpcParams rpcParams = JsonRpcParams.create(array);
        McpParameters parameters = new McpParameters(rpcParams);
        List<Foo> foos = parameters
                .asList(Foo.class)
                .orElse(List.of());
        assertThat(foos.size(), is(2));

        Foo foo1 = foos.getFirst();
        assertThat(foo1.foo(), is("foo1"));
        assertThat(foo1.bar(), is("bar1"));

        Foo foo2 = foos.getLast();
        assertThat(foo2.foo(), is("foo2"));
        assertThat(foo2.bar(), is("bar2"));
    }

    @Test
    void testGenericTypeCasting() {
        JsonArray array = JsonParser.create("[{\"foo\":\"foo1\",\"bar\":\"bar1\"},{\"foo\":\"foo2\",\"bar\":\"bar2\"}]").readJsonArray();
        JsonRpcParams rpcParams = JsonRpcParams.create(array);
        McpParameters parameters = new McpParameters(rpcParams);
        List<Foo> foos = parameters.as(new GenericType<List<Foo>>() { })
                .orElse(List.of());

        assertThat(foos.size(), is(2));
        assertThat(foos.getFirst().foo(), is("foo1"));
        assertThat(foos.getFirst().bar(), is("bar1"));
        assertThat(foos.getLast().foo(), is("foo2"));
        assertThat(foos.getLast().bar(), is("bar2"));
    }

    @Test
    void testListOfMap() {
        JsonArray array = JsonParser.create("""
                                            [{"value1":{"foo":"foo1","bar":"bar1"}},
                                             {"value2":{"foo":"foo2","bar":"bar2"}}]
                                            """).readJsonArray();
        JsonRpcParams rpcParams = JsonRpcParams.create(array);
        McpParameters parameters = new McpParameters(rpcParams);
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
        assertThat(value1.foo(), is("foo1"));
        assertThat(value1.bar(), is("bar1"));

        map = listMap.getLast();
        assertThat(map.size(), is(1));
        assertThat(map.containsKey("value2"), is(true));

        Foo value2 = map.get("value2")
                .as(Foo.class)
                .orElseThrow(() -> new JUnitException("Cannot convert value2 to Foo.class"));
        assertThat(value2.foo(), is("foo2"));
        assertThat(value2.bar(), is("bar2"));
    }

    @Test
    void testMapOfList() {
        JsonObject object = JsonParser.create("""
                                               {"value1":[{"foo":"foo1","bar":"bar1"}],
                                                "value2":[{"foo":"foo2","bar":"bar2"}]}
                                               """).readJsonObject();
        JsonRpcParams rpcParams = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(rpcParams);
        Map<String, McpParameters> map = parameters.asMap().orElse(Map.of());
        assertThat(map.size(), is(2));
        assertThat(map.containsKey("value1"), is(true));
        assertThat(map.containsKey("value2"), is(true));

        List<Foo> value1 = map.get("value1").asList(Foo.class).orElse(List.of());
        assertThat(value1.size(), is(1));

        Foo foo1 = value1.getFirst();
        assertThat(foo1.foo(), is("foo1"));
        assertThat(foo1.bar(), is("bar1"));

        List<Foo> value2 = map.get("value2").asList(Foo.class).orElse(List.of());
        assertThat(value2.size(), is(1));

        Foo foo2 = value2.getFirst();
        assertThat(foo2.foo(), is("foo2"));
        assertThat(foo2.bar(), is("bar2"));
    }

    @Test
    void testNestedObject() {
        JsonObject object = JsonParser.create("{\"person\":{\"name\":\"Frank\",\"age\":10}}").readJsonObject();
        JsonRpcParams rpcParams = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(rpcParams);
        String name = parameters.get("person").get("name").asString().orElse(null);
        int age = parameters.get("person").get("age").asInteger().orElse(-1);

        assertThat(name, is("Frank"));
        assertThat(age, is(10));
    }

    @Test
    void testCasting() {
        JsonObject object = JsonParser.create("{\"foo\":\"value1\",\"bar\":\"value2\"}").readJsonObject();
        JsonRpcParams params = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(params);
        Foo foo = parameters.as(Foo.class).get();

        assertThat(foo.foo(), is("value1"));
        assertThat(foo.bar(), is("value2"));
    }

    @Test
    void testNestedCasting() {
        JsonObject object = JsonParser.create("{\"foo\":{\"foo\":\"value1\",\"bar\":\"value2\"}}").readJsonObject();
        JsonRpcParams params = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(params);
        Foo foo = parameters.get("foo").as(Foo.class).get();

        assertThat(foo.foo(), is("value1"));
        assertThat(foo.bar(), is("value2"));
    }

    @Test
    void testIsNumberInt() {
        JsonObject object = JsonParser.create("{\"foo\":1}").readJsonObject();
        JsonRpcParams params = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(params);
        boolean isNumber = parameters.get("foo").isNumber();

        assertThat(isNumber, is(true));
    }

    @Test
    void testIsNumberDouble() {
        JsonObject object = JsonParser.create("{\"foo\":1.0}").readJsonObject();
        JsonRpcParams params = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(params);
        boolean isNumber = parameters.get("foo").isNumber();

        assertThat(isNumber, is(true));
    }

    @Test
    void testIsNumberString() {
        JsonObject object = JsonParser.create("{\"foo\":\"notANumber\"}").readJsonObject();
        JsonRpcParams params = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(params);
        boolean isNumber = parameters.get("foo").isNumber();

        assertThat(isNumber, is(false));
    }

    @Test
    void testIsString() {
        JsonObject object = JsonParser.create("{\"foo\":\"notANumber\"}").readJsonObject();
        JsonRpcParams params = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(params);
        boolean isNumber = parameters.get("foo").isString();

        assertThat(isNumber, is(true));
    }

    @Test
    void testIsStringNumber() {
        JsonObject object = JsonParser.create("{\"foo\":1}").readJsonObject();
        JsonRpcParams params = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(params);
        boolean isNumber = parameters.get("foo").isString();

        assertThat(isNumber, is(false));
    }

    @Test
    void testIfPresent() {
        AtomicBoolean present = new AtomicBoolean(false);
        JsonObject object = JsonParser.create("{\"foo\":1}").readJsonObject();
        JsonRpcParams params = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(params);
        parameters.get("foo").ifPresent(it -> present.set(true));
        assertThat(present.get(), is(true));
    }

    @Test
    void testIfNotPresent() {
        AtomicBoolean present = new AtomicBoolean(false);
        JsonObject object = JsonParser.create("{\"foo\":1}").readJsonObject();
        JsonRpcParams params = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(params);
        parameters.get("bar").ifPresent(it -> present.set(true));
        assertThat(present.get(), is(false));
    }

    @Test
    void testIfPresentNullPointerException() {
        JsonObject object = JsonParser.create("{\"foo\":1}").readJsonObject();
        JsonRpcParams params = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(params);
        try {
            parameters.get("foo").ifPresent(null);
            assertThat("NullPointerException must be thrown", true, is(false));
        } catch (NullPointerException exception) {
            assertThat(exception.getMessage(), is("action is null"));
        }
    }

    @Test
    void testAsMap() {
        JsonObject object = JsonParser.create("{\"foo\":\"foo\",\"bar\":\"bar\"}").readJsonObject();
        JsonRpcParams params = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(params);

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
        JsonObject object = JsonParser.create("{\"foo\":\"foo\",\"bar\":\"bar\"}").readJsonObject();
        JsonRpcParams params = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(params);

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

    @Json.Entity
    public record Foo(String foo, String bar) {
    }
}
