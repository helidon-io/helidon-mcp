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
package io.helidon.extensions.mcp.tests.declarative;

import java.io.StringReader;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

class JsonSchemaGenerationTest {

    @Test
    void testFooSchema() {
        String s = McpToolsServer_Foo__JsonSchema.schema();
        JsonObject json = Json.createReader(new StringReader(s)).readObject();
        assertThat(json.getString("type"), is("object"));
        JsonValue properties = json.get("properties");
        assertThat(properties, notNullValue());
        assertThat(properties, instanceOf(JsonObject.class));
        JsonObject propertiesJson = (JsonObject) properties;
        assertThat(propertiesJson.size(), is(2));
        assertThat(propertiesJson.getJsonObject("foo").getString("type"), is("string"));
        assertThat(propertiesJson.getJsonObject("bar").getString("type"), is("integer"));
    }

    @Test
    void testAlertSchema() {
        String s = McpMixedComponentServer_Alert__JsonSchema.schema();
        JsonObject json = Json.createReader(new StringReader(s)).readObject();
        assertThat(json.getString("type"), is("object"));
        JsonValue properties = json.get("properties");
        assertThat(properties, notNullValue());
        assertThat(properties, instanceOf(JsonObject.class));
        JsonObject propertiesJson = (JsonObject) properties;
        assertThat(propertiesJson.size(), is(3));
        assertThat(propertiesJson.getJsonObject("name").getString("type"), is("string"));
        assertThat(propertiesJson.getJsonObject("priority").getString("type"), is("integer"));
        assertThat(((JsonObject) properties).containsKey("location"), is(true));
    }

    @Test
    void testLocationSchema() {
        String s = McpMixedComponentServer_Location__JsonSchema.schema();
        JsonObject json = Json.createReader(new StringReader(s)).readObject();
        assertThat(json.getString("type"), is("object"));
        JsonValue properties = json.get("properties");
        assertThat(properties, notNullValue());
        assertThat(properties, instanceOf(JsonObject.class));
        JsonObject propertiesJson = (JsonObject) properties;
        assertThat(propertiesJson.size(), is(2));
        assertThat(propertiesJson.getJsonObject("latitude").getString("type"), is("number"));
        assertThat(propertiesJson.getJsonObject("longitude").getString("type"), is("number"));
    }
}
