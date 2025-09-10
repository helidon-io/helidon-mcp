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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.helidon.common.media.type.MediaType;
import io.helidon.jsonrpc.core.JsonRpcParams;

import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

import static io.helidon.extensions.mcp.server.McpJsonRpc.JSON_BUILDER_FACTORY;

class McpResourceTemplate implements McpResource {
    private final Pattern pattern;
    private final McpResource delegate;
    private final List<String> variables;

    McpResourceTemplate(McpResource resource) {
        this.delegate = resource;
        this.variables = new ArrayList<>();
        this.pattern = createPattern(resource.uri());
    }

    @Override
    public String uri() {
        return delegate.uri();
    }

    @Override
    public String name() {
        return delegate.name();
    }

    @Override
    public String description() {
        return delegate.description();
    }

    @Override
    public MediaType mediaType() {
        return delegate.mediaType();
    }

    @Override
    public Function<McpRequest, List<McpResourceContent>> resource() {
        return delegate.resource();
    }

    boolean matches(String uri) {
        return pattern.matcher(uri).matches();
    }

    McpParameters parameters(JsonRpcParams params, String uri) {
        JsonObjectBuilder builder = JSON_BUILDER_FACTORY.createObjectBuilder();
        Matcher matcher = pattern.matcher(uri);
        if (matcher.matches()) {
            for (int i = 0; i < matcher.groupCount(); i++) {
                builder.add(variables.get(i), matcher.group(i + 1));
            }
        }
        JsonObject parameters = builder.build();
        return new McpParameters(JsonRpcParams.create(parameters), parameters);
    }

    private Pattern createPattern(String uri) {
        Matcher matcher = Pattern.compile("\\{(\\w+)}").matcher(uri);
        StringBuilder regex = new StringBuilder();
        while (matcher.find()) {
            variables.add(matcher.group(1));
            matcher.appendReplacement(regex, "([^/]+)");
        }
        matcher.appendTail(regex);
        return Pattern.compile(regex.toString());
    }
}
