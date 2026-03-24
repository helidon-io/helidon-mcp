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
package io.helidon.extensions.mcp.tests.declarative;

import io.helidon.json.JsonObject;
import io.helidon.json.JsonParser;
import io.helidon.service.registry.Services;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class JsonSchemaGenerationTest {

    @Test
    void testFooSchema() {
        String s = Services.get(Foo__JsonSchema.class).jsonSchema();
        JsonObject json = JsonParser.create(s).readJsonObject();
        assertThat(json.stringValue("type").orElseThrow(), is("object"));
        JsonObject propertiesJson = json.objectValue("properties").orElseThrow();
        assertThat(propertiesJson.size(), is(2));
        assertThat(propertiesJson.objectValue("foo")
                           .flatMap(property -> property.stringValue("type"))
                           .orElseThrow(), is("string"));
        assertThat(propertiesJson.objectValue("bar")
                           .flatMap(property -> property.stringValue("type"))
                           .orElseThrow(), is("integer"));
    }

    @Test
    void testAlertSchema() {
        String s = Services.get(Alert__JsonSchema.class).jsonSchema();
        JsonObject json = JsonParser.create(s).readJsonObject();
        assertThat(json.stringValue("type").orElseThrow(), is("object"));
        JsonObject propertiesJson = json.objectValue("properties").orElseThrow();
        assertThat(propertiesJson.size(), is(3));
        assertThat(propertiesJson.objectValue("name")
                           .flatMap(property -> property.stringValue("type"))
                           .orElseThrow(), is("string"));
        assertThat(propertiesJson.objectValue("priority")
                           .flatMap(property -> property.stringValue("type"))
                           .orElseThrow(), is("integer"));
        assertThat(propertiesJson.containsKey("location"), is(true));
    }

    @Test
    void testLocationSchema() {
        String s = Services.get(Location__JsonSchema.class).jsonSchema();
        JsonObject json = JsonParser.create(s).readJsonObject();
        assertThat(json.stringValue("type").orElseThrow(), is("object"));
        JsonObject propertiesJson = json.objectValue("properties").orElseThrow();
        assertThat(propertiesJson.size(), is(2));
        assertThat(propertiesJson.objectValue("latitude")
                           .flatMap(property -> property.stringValue("type"))
                           .orElseThrow(), is("integer"));
        assertThat(propertiesJson.objectValue("longitude")
                           .flatMap(property -> property.stringValue("type"))
                           .orElseThrow(), is("integer"));
    }
}
