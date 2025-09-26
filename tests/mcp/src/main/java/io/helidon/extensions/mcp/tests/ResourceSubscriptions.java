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
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;

import io.helidon.common.context.Context;
import io.helidon.common.media.type.MediaType;
import io.helidon.common.media.type.MediaTypes;
import io.helidon.extensions.mcp.server.McpRequest;
import io.helidon.extensions.mcp.server.McpResource;
import io.helidon.extensions.mcp.server.McpResourceContent;
import io.helidon.extensions.mcp.server.McpResourceContents;
import io.helidon.extensions.mcp.server.McpServerFeature;
import io.helidon.webserver.http.HttpRouting;

/**
 * Test resource subscriptions/unsubscriptions without using
 * {@link io.helidon.extensions.mcp.server.McpResourceSubscriber} or
 * {@link io.helidon.extensions.mcp.server.McpResourceUnsubscriber}.
 */
class ResourceSubscriptions {

    private ResourceSubscriptions() {
    }

    static void setUpRoute(HttpRouting.Builder builder) {
        MyResource myResource = new MyResource();
        builder.addFeature(McpServerFeature.builder()
                                   .path("/")
                                   .addResource(myResource));
    }

    private static final Context CONTEXT = Context.create();

    static Context context() {
        return CONTEXT;
    }

    private static final class MyResource implements McpResource {

        @Override
        public String uri() {
            return "http://myresource";
        }

        @Override
        public String name() {
            return "myresource";
        }

        @Override
        public String description() {
            return "My Resource";
        }

        @Override
        public MediaType mediaType() {
            return MediaTypes.APPLICATION_OCTET_STREAM;
        }

        @Override
        public Function<McpRequest, List<McpResourceContent>> resource() {
            return this::read;
        }

        List<McpResourceContent> read(McpRequest request) {
            CountDownLatch readLatch = context().get(CountDownLatch.class).orElseThrow();
            if (readLatch.getCount() > 0) {
                readLatch.countDown();
                request.features().subscriptions().sendUpdate(uri());       // trigger another resource read
            }
            return List.of(McpResourceContents.textContent("text"));
        }
    }
}
