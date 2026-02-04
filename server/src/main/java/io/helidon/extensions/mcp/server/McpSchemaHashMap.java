/*
 * Copyright (c) 2026 Oracle and/or its affiliates.
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

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReaderFactory;

/**
 * Cache for JSON schema using a traditional lock instead of {@code sync} block to avoid
 * thread pinning for some version of jdk. Keys are schema as string and value are schema
 * as {@link jakarta.json.JsonObject}.
 */
class McpSchemaHashMap extends HashMap<String, JsonObject> {
    private static final JsonReaderFactory JSON_READER_FACTORY = Json.createReaderFactory(Map.of());
    private static final JsonObject EMPTY_OBJECT_SCHEMA = Json.createBuilderFactory(Map.of())
            .createObjectBuilder()
            .add("type", "object")
            .add("properties", JsonObject.EMPTY_JSON_OBJECT)
            .build();

    private final ReentrantLock lock = new ReentrantLock();

    @Override
    public JsonObject get(Object schema) {
        JsonObject value = super.get(schema);
        String stringSchema = schema.toString();
        if (value == null) {
            // lock to write the newly parsed schema
            lock.lock();
            try {
                value = super.get(stringSchema);
                // double check that another thread did not write the schema before we locked
                if (value == null) {
                    JsonObject parsed;
                    if (stringSchema.isEmpty()) {
                        parsed = EMPTY_OBJECT_SCHEMA;
                    } else {
                        try (var r = JSON_READER_FACTORY.createReader(new StringReader(stringSchema))) {
                            parsed = r.readObject();
                        }
                    }
                    // store the parsed schema
                    this.put(stringSchema, parsed);
                    value = parsed;
                }
            } finally {
                lock.unlock();
            }
        }
        return value;
    }
}
