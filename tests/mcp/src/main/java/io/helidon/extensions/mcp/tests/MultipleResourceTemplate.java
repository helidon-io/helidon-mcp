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

package io.helidon.extensions.mcp.tests;

import java.util.List;
import java.util.function.Function;

import io.helidon.common.media.type.MediaType;
import io.helidon.common.media.type.MediaTypes;
import io.helidon.extensions.mcp.server.McpException;
import io.helidon.extensions.mcp.server.McpRequest;
import io.helidon.extensions.mcp.server.McpResource;
import io.helidon.extensions.mcp.server.McpResourceContent;
import io.helidon.extensions.mcp.server.McpServerFeature;
import io.helidon.webserver.http.HttpRouting;

class MultipleResourceTemplate {
    static final String RESOURCE1_URI = "http://{path}";
    static final String RESOURCE2_URI = "http://{path}/{path}";
    static final String RESOURCE3_URI = "http://{foo}/{bar}";

    private MultipleResourceTemplate() {
    }

    static void setUpRoute(HttpRouting.Builder builder) {
        builder.addFeature(McpServerFeature.builder()
                                   .path("/")
                                   .addResource(resource -> resource
                                           .name("resource1")
                                           .uri(RESOURCE1_URI)
                                           .description("Resource 1")
                                           .mediaType(MediaTypes.TEXT_PLAIN)
                                           .resource(param -> {
                                               throw new McpException("Resource Template does not provide resource content");
                                           }))

                                   .addResource(resource -> resource
                                           .name("resource2")
                                           .uri(RESOURCE2_URI)
                                           .description("Resource 2")
                                           .mediaType(MediaTypes.APPLICATION_JSON)
                                           .resource(param -> {
                                               throw new McpException("Resource Template does not provide resource content");
                                           }))
                                   .addResource(new MyResource()));
    }

    private static final class MyResource implements McpResource {
        @Override
        public String uri() {
            return RESOURCE3_URI;
        }

        @Override
        public String name() {
            return "resource3";
        }

        @Override
        public String description() {
            return "Resource 3";
        }

        @Override
        public MediaType mediaType() {
            return MediaTypes.APPLICATION_OCTET_STREAM;
        }

        @Override
        public Function<McpRequest, List<McpResourceContent>> resource() {
            return this::read;
        }

        List<McpResourceContent> read(McpRequest features) {
            throw new McpException("Resource Template does not provide resource content");
        }
    }
}
