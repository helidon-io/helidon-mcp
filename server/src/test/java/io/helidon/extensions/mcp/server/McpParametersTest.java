/*
 * Copyright (c) 2025 Oracle and/or its affiliates.
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

import java.util.List;

import io.helidon.common.mapper.OptionalValue;
import io.helidon.jsonrpc.core.JsonRpcParams;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonStructure;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class McpParametersTest {

    @Test
    void testSimpleString() {
        JsonObject object = Json.createObjectBuilder()
                .add("foo", "bar")
                .build();
        JsonRpcParams rpcParams = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(rpcParams, object);
        String foo = parameters.get("foo").asString().orElse(null);
        assertThat(foo, is("bar"));
    }

    @Test
    void testSimpleBoolean() {
        JsonObject object = Json.createObjectBuilder()
                .add("foo", true)
                .build();
        JsonRpcParams rpcParams = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(rpcParams, object);
        Boolean foo = parameters.get("foo").asBoolean().orElse(null);
        assertThat(foo, is(true));
    }

    @Test
    void testSimpleByte() {
        JsonObject object = Json.createObjectBuilder()
                .add("foo", 1)
                .build();
        JsonRpcParams rpcParams = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(rpcParams, object);
        byte foo = parameters.get("foo").asByte().orElse(null);
        assertThat(foo, is((byte) 1));
    }

    @Test
    void testSimpleShort() {
        JsonObject object = Json.createObjectBuilder()
                .add("foo", 1)
                .build();
        JsonRpcParams rpcParams = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(rpcParams, object);
        short foo = parameters.get("foo").asShort().orElse(null);
        assertThat(foo, is((short) 1));
    }

    @Test
    void testSimpleInteger() {
        JsonObject object = Json.createObjectBuilder()
                .add("foo", 1)
                .build();
        JsonRpcParams rpcParams = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(rpcParams, object);
        int foo = parameters.get("foo").asInteger().orElse(null);
        assertThat(foo, is(1));
    }

    @Test
    void testSimpleLong() {
        JsonObject object = Json.createObjectBuilder()
                .add("foo", 1L)
                .build();
        JsonRpcParams rpcParams = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(rpcParams, object);
        long foo = parameters.get("foo").asLong().orElse(null);
        assertThat(foo, is(1L));
    }

    @Test
    void testSimpleDouble() {
        JsonObject object = Json.createObjectBuilder()
                .add("foo", 1.0D)
                .build();
        JsonRpcParams rpcParams = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(rpcParams, object);
        double foo = parameters.get("foo").asDouble().orElse(null);
        assertThat(foo, is(1.0D));
    }

    @Test
    void testSimpleFloat() {
        JsonObject object = Json.createObjectBuilder()
                .add("foo", 1.0F)
                .build();
        JsonRpcParams rpcParams = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(rpcParams, object);
        float foo = parameters.get("foo").asFloat().orElse(null);
        assertThat(foo, is(1.0F));
    }

    @Test
    void testSimpleList() {
        JsonObject object = Json.createObjectBuilder()
                .add("foo", Json.createArrayBuilder()
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
    void testNestedObject() {
        JsonObject object = Json.createObjectBuilder()
                .add("person", Json.createObjectBuilder()
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
        JsonStructure object = Json.createObjectBuilder()
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
        JsonStructure object = Json.createObjectBuilder()
                .add("foo", Json.createObjectBuilder()
                        .add("foo", "value1")
                        .add("bar", "value2"))
                .build();
        JsonRpcParams params = JsonRpcParams.create(object);
        McpParameters parameters = new McpParameters(params, object);
        Foo foo = parameters.get("foo").as(Foo.class).get();

        assertThat(foo.foo, is("value1"));
        assertThat(foo.bar, is("value2"));
    }

    public static class Foo {
        public String foo;
        public String bar;
    }
}
