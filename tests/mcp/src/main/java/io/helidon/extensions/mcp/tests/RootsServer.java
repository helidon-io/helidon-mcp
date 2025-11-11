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

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import io.helidon.extensions.mcp.server.McpRequest;
import io.helidon.extensions.mcp.server.McpRoot;
import io.helidon.extensions.mcp.server.McpServerFeature;
import io.helidon.extensions.mcp.server.McpTool;
import io.helidon.extensions.mcp.server.McpToolContent;
import io.helidon.extensions.mcp.server.McpToolContents;
import io.helidon.extensions.mcp.server.McpToolErrorException;
import io.helidon.webserver.http.HttpRouting;

class RootsServer {
    private RootsServer() {
    }

    static void setUpRoute(HttpRouting.Builder builder) {
        builder.addFeature(McpServerFeature.builder()
                                   .path("/")
                                   .addTool(new RootNameTool())
                                   .addTool(new RootUriTool()));
    }

    private static class RootNameTool implements McpTool {
        @Override
        public String name() {
            return "roots-name-tool";
        }

        @Override
        public String description() {
            return "Returns roots names";
        }

        @Override
        public String schema() {
            return "";
        }

        @Override
        public Function<McpRequest, List<McpToolContent>> tool() {
            return request -> {
                List<McpRoot> roots = request.features().roots().listRoots();
                return roots.stream()
                        .map(McpRoot::name)
                        .flatMap(Optional::stream)
                        .map(McpToolContents::textContent)
                        .toList();
            };
        }
    }

    private static class RootUriTool implements McpTool {
        @Override
        public String name() {
            return "roots-uri-tool";
        }

        @Override
        public String description() {
            return "Returns roots URIs";
        }

        @Override
        public String schema() {
            return "";
        }

        @Override
        public Function<McpRequest, List<McpToolContent>> tool() {
            return request -> {
                if (!request.features().roots().enabled()) {
                    throw new McpToolErrorException("Roots is disabled");
                }
                List<McpRoot> roots = request.features().roots().listRoots();
                return roots.stream()
                        .map(McpRoot::uri)
                        .map(URI::toASCIIString)
                        .map(McpToolContents::textContent)
                        .toList();
            };
        }
    }
}
