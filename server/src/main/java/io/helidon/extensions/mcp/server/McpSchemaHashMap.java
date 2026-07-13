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

import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;

import io.helidon.json.JsonObject;
import io.helidon.json.JsonParser;

/**
 * Cache for JSON schema using a traditional lock instead of {@code sync} block to avoid
 * thread pinning for some version of jdk. Keys are schema as string and value are schema
 * as {@link io.helidon.json.JsonObject}.
 */
class McpSchemaHashMap extends HashMap<String, JsonObject> {
    private static final JsonObject EMPTY_OBJECT_SCHEMA = JsonObject.builder()
            .set("type", "object")
            .set("properties", JsonObject.empty())
            .build();

    private final ReentrantLock lock = new ReentrantLock();

    @Override
    public JsonObject get(Object schema) {
        String stringSchema = schema.toString();
        lock.lock();
        try {
            JsonObject value = super.get(stringSchema);
            if (value == null) {
                JsonObject parsed;
                if (stringSchema.isEmpty()) {
                    parsed = EMPTY_OBJECT_SCHEMA;
                } else {
                    parsed = JsonParser.create(stringSchema).readJsonObject();
                }
                // store the parsed schema
                this.put(stringSchema, parsed);
                value = parsed;
            }
            return value;
        } finally {
            lock.unlock();
        }
    }
}
