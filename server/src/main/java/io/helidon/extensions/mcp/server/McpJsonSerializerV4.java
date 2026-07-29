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

import java.util.List;
import java.util.Set;

import io.helidon.json.JsonArray;
import io.helidon.json.JsonObject;
import io.helidon.json.JsonValue;

/**
 * JSON serializer for {@code 2025-11-25} MCP specification.
 */
class McpJsonSerializerV4 extends McpJsonSerializerV3 {

    @Override
    public JsonObject.Builder createJsonInitializeResponse(Set<McpCapability> capabilities, McpServerConfig config) {
        return super.createJsonInitializeResponse(capabilities, config)
                .set("protocolVersion", McpProtocolVersion.VERSION_2025_11_25.text());
    }

    @Override
    List<McpSamplingMessage> parseMessages(McpRole role, JsonValue content) {
        if (content instanceof JsonArray array) {
            return array.values().stream()
                    .map(JsonValue::asObject)
                    .map(value -> parseMessage(role, value))
                    .toList();
        }
        return super.parseMessages(role, content);
    }
}
