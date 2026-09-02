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

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

import io.helidon.builder.api.Prototype;

final class McpProtectedResourceMetadataConfigSupport
        implements Prototype.BuilderDecorator<McpProtectedResourceMetadataConfig.BuilderBase<?, ?>> {
    @Override
    public void decorate(McpProtectedResourceMetadataConfig.BuilderBase<?, ?> builder) {
        if (builder.resource().isEmpty()) {
            return;
        }
        URI resource = builder.resource().orElseThrow();
        boolean loopbackResource = validateUri(resource, "Protected resource", false);
        builder.metadataPath().ifPresent(McpProtectedResourceMetadataConfigSupport::validateMetadataPath);
        if (builder.authorizationServers().isEmpty()) {
            throw new IllegalArgumentException("At least one authorization server must be configured");
        }
        for (URI authorizationServer : builder.authorizationServers()) {
            validateUri(authorizationServer, "Authorization server issuer", true);
            if (authorizationServer.getScheme().equalsIgnoreCase("http") && !loopbackResource) {
                throw new IllegalArgumentException("Authorization server issuer must use HTTPS unless both it and the"
                                                           + " protected resource use localhost or a loopback literal: "
                                                           + authorizationServer);
            }
        }
        builder.scopesSupported().forEach(McpProtectedResourceMetadataConfigSupport::validateScope);
    }

    private static boolean validateUri(URI uri, String description, boolean rejectQuery) {
        String scheme = uri.getScheme();
        if (scheme == null
                || !(scheme.equalsIgnoreCase("https") || scheme.equalsIgnoreCase("http"))
                || uri.getHost() == null
                || uri.getRawUserInfo() != null
                || uri.getPort() == 0
                || uri.getPort() > 65535) {
            throw new IllegalArgumentException(description + " must be an absolute HTTP URI: " + uri);
        }
        String host = uri.getHost();
        boolean loopbackHost = isLoopbackHost(host);
        if (!scheme.equalsIgnoreCase("https") && !loopbackHost) {
            throw new IllegalArgumentException(description
                                                       + " must use HTTPS unless the host is localhost"
                                                       + " or a loopback literal: "
                                                       + uri);
        }
        if (uri.getFragment() != null) {
            throw new IllegalArgumentException(description + " must not contain a fragment: " + uri);
        }
        if (rejectQuery && uri.getQuery() != null) {
            throw new IllegalArgumentException(description + " must not contain a query: " + uri);
        }
        return loopbackHost;
    }

    private static boolean isLoopbackHost(String host) {
        if (host.equalsIgnoreCase("localhost")) {
            return true;
        }
        String literal = host;
        if (literal.startsWith("[") && literal.endsWith("]")) {
            literal = literal.substring(1, literal.length() - 1);
        }
        boolean addressLiteral = literal.indexOf(':') >= 0
                || literal.chars().allMatch(character -> Character.isDigit(character) || character == '.');
        if (!addressLiteral) {
            return false;
        }
        try {
            return InetAddress.getByName(literal).isLoopbackAddress();
        } catch (UnknownHostException ignored) {
            // Java 21 does not support unnamed catch variables.
            return false;
        }
    }

    private static void validateMetadataPath(String path) {
        URI uri;
        try {
            uri = URI.create(path);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Protected resource metadata path must be a valid HTTP path: " + path,
                                               e);
        }
        if (path.isBlank()
                || !path.startsWith("/")
                || uri.isAbsolute()
                || uri.getRawAuthority() != null
                || uri.getRawQuery() != null
                || uri.getRawFragment() != null) {
            throw new IllegalArgumentException("Protected resource metadata path must be an absolute path without a"
                                                       + " query or fragment: "
                                                       + path);
        }
    }

    private static void validateScope(String scope) {
        if (scope.isEmpty()
                || !scope.chars().allMatch(character -> character == 0x21
                || (character >= 0x23 && character <= 0x5B)
                || (character >= 0x5D && character <= 0x7E))) {
            throw new IllegalArgumentException("Invalid OAuth scope: " + scope);
        }
    }
}
