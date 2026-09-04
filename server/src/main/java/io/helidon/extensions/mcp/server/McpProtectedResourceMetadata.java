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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import io.helidon.builder.api.Prototype;
import io.helidon.common.uri.UriPath;
import io.helidon.http.HeaderNames;
import io.helidon.http.HeaderValues;
import io.helidon.http.Method;
import io.helidon.http.PathMatcher;
import io.helidon.http.PathMatchers;
import io.helidon.http.RequestedUriDiscoveryContext;
import io.helidon.http.ServerRequestHeaders;
import io.helidon.http.Status;
import io.helidon.http.WritableHeaders;
import io.helidon.json.JsonObject;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

final class McpProtectedResourceMetadata {
    private static final String METADATA_URI = "/.well-known/oauth-protected-resource";
    private static final int PORT_PROBE = 65534;
    private static final RequestedUriDiscoveryContext DEFAULT_REQUESTED_URI_DISCOVERY_CONTEXT =
            RequestedUriDiscoveryContext.builder().build();

    private final boolean enabled;
    private final McpServerConfig config;
    private final String serverEndpoint;
    private final String metadataEndpoint;

    McpProtectedResourceMetadata(McpServerConfig config) {
        this.config = config;
        this.serverEndpoint = config.path();
        this.enabled = config.protectedResourceMetadata().isPresent();
        if (!enabled) {
            this.metadataEndpoint = METADATA_URI;
            return;
        }
        McpProtectedResourceMetadataConfig metadataConfig = config.protectedResourceMetadata().orElseThrow();
        if (metadataConfig.resource().isEmpty() && isPathPattern(serverEndpoint)) {
            throw new IllegalArgumentException("Protected resource must be configured when MCP server path is a routing"
                                                       + " pattern: "
                                                       + serverEndpoint);
        }
        this.metadataEndpoint = metadataEndpoint(metadataConfig, serverEndpoint);
        validateRouteConflicts(this.metadataEndpoint, serverEndpoint);
    }

    void setup(HttpRouting.Builder routing) {
        if (!enabled) {
            return;
        }
        routing.route(Method.GET,
                      new ExactRawPathMatcher(metadataEndpoint),
                      this::handle);
    }

    private static String metadataEndpoint(McpProtectedResourceMetadataConfig config, String serverEndpoint) {
        if (config.metadataPath().isPresent()) {
            return config.metadataPath().orElseThrow();
        }
        String metadataPath = config.resource()
                .map(URI::getRawPath)
                .orElse(serverEndpoint);
        if (metadataPath.isEmpty() || metadataPath.equals("/")) {
            return METADATA_URI;
        }
        if (metadataPath.startsWith("/")) {
            return METADATA_URI + metadataPath;
        }
        return METADATA_URI + "/" + metadataPath;
    }

    private static void validateRouteConflicts(String metadataEndpoint, String serverEndpoint) {
        UriPath metadataUriPath = UriPath.create(metadataEndpoint);
        if (PathMatchers.create(serverEndpoint).match(metadataUriPath).accepted()
                || PathMatchers.create(McpServerFeature.DEFAULT_OIDC_METADATA_URI).match(metadataUriPath).accepted()) {
            throw new IllegalArgumentException("Protected resource metadata path conflicts with an existing MCP server route: "
                                                       + metadataEndpoint);
        }
    }

    private static void validateResourceAndAuthorizationServers(URI resource,
                                                                Iterable<URI> authorizationServers) {
        boolean loopbackResource = validateUri(resource, "Protected resource", false);
        for (URI authorizationServer : authorizationServers) {
            validateUri(authorizationServer, "Authorization server issuer", true);
            if (authorizationServer.getScheme().equalsIgnoreCase("http") && !loopbackResource) {
                throw new IllegalArgumentException("Authorization server issuer must use HTTPS unless both it and the"
                                                           + " protected resource use localhost or a loopback literal: "
                                                           + authorizationServer);
            }
        }
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

    private static boolean isPathPattern(String path) {
        return path.contains("{") || path.contains("[") || path.contains("*") || path.contains("\\");
    }

    private static boolean isDefaultPort(String scheme, int port) {
        return (scheme.equalsIgnoreCase("http") && port == 80)
                || (scheme.equalsIgnoreCase("https") && port == 443);
    }

    private static Optional<String> selectedExplicitPort(ServerRequest request,
                                                         String requestedScheme,
                                                         String requestedHost,
                                                         int requestedPort) {
        // UriInfo normalizes omitted and explicit default ports to the same value. Probe only lexically explicit
        // authority candidates through the listener's own discovery context so its source order and trust rules still apply.
        Map<String, String> probes = new LinkedHashMap<>();
        WritableHeaders<?> headers = WritableHeaders.create(request.headers());

        Optional<String> authorityProbe = probeAuthority(request.authority(), requestedHost, requestedPort, probes);
        if (authorityProbe.isPresent()) {
            headers.set(HeaderNames.HOST, authorityProbe.orElseThrow());
        }

        List<String> forwarded = headers.values(HeaderNames.FORWARDED);
        if (!forwarded.isEmpty()) {
            List<String> probedForwarded = new ArrayList<>(forwarded.size());
            boolean forwardedProbed = false;
            for (String value : forwarded) {
                PortProbe forwardedProbe = probeForwarded(value, requestedHost, requestedPort, probes);
                probedForwarded.add(forwardedProbe.value());
                forwardedProbed |= forwardedProbe.changed();
            }
            if (forwardedProbed) {
                headers.set(HeaderNames.FORWARDED, probedForwarded);
            }
        }

        if (probes.isEmpty()) {
            return Optional.empty();
        }

        RequestedUriDiscoveryContext discoveryContext = request.listenerContext()
                .config()
                .requestedUriDiscoveryContext()
                .orElse(DEFAULT_REQUESTED_URI_DISCOVERY_CONTEXT);
        var probedUri = discoveryContext.uriInfo(request.remotePeer().address(),
                                                 request.localPeer().address(),
                                                 request.path().absolute().path(),
                                                 ServerRequestHeaders.create(headers),
                                                 request.query(),
                                                 request.isSecure());
        if (probedUri.port() != PORT_PROBE || !probedUri.scheme().equalsIgnoreCase(requestedScheme)) {
            return Optional.empty();
        }
        return probes.entrySet()
                .stream()
                .filter(entry -> hostsEqual(probedUri.host(), entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    private static Optional<String> probeAuthority(String authority,
                                                   String requestedHost,
                                                   int requestedPort,
                                                   Map<String, String> probes) {
        URI authorityUri;
        try {
            authorityUri = URI.create("http://" + authority);
        } catch (IllegalArgumentException ignored) {
            // Java 21 does not support unnamed catch variables.
            return Optional.empty();
        }
        String authorityHost = authorityUri.getHost();
        if (authorityHost == null
                || authorityUri.getPort() != requestedPort
                || !hostsEqual(authorityHost, requestedHost)) {
            return Optional.empty();
        }
        int portSeparator = authority.lastIndexOf(':');
        String probeHost = newProbeHost(probes);
        probes.put(probeHost, authority.substring(portSeparator + 1));
        return Optional.of(probeHost + ":" + PORT_PROBE);
    }

    private static PortProbe probeForwarded(String value,
                                            String requestedHost,
                                            int requestedPort,
                                            Map<String, String> probes) {
        String[] elements = value.split(",", -1);
        boolean changed = false;
        for (int elementIndex = 0; elementIndex < elements.length; elementIndex++) {
            String[] directives = elements[elementIndex].split(";", -1);
            for (int directiveIndex = 0; directiveIndex < directives.length; directiveIndex++) {
                String directive = directives[directiveIndex];
                int equals = directive.indexOf('=');
                if (equals < 0 || !directive.substring(0, equals).trim().equalsIgnoreCase("host")) {
                    continue;
                }
                String authority = directive.substring(equals + 1);
                boolean quoted = authority.length() > 1
                        && authority.charAt(0) == '"'
                        && authority.charAt(authority.length() - 1) == '"';
                String unquotedAuthority = quoted ? authority.substring(1, authority.length() - 1) : authority;
                Optional<String> authorityProbe = probeAuthority(unquotedAuthority,
                                                                 requestedHost,
                                                                 requestedPort,
                                                                 probes);
                if (authorityProbe.isPresent()) {
                    String replacement = authorityProbe.orElseThrow();
                    directives[directiveIndex] = directive.substring(0, equals + 1)
                            + (quoted ? '"' + replacement + '"' : replacement);
                    changed = true;
                }
            }
            elements[elementIndex] = String.join(";", directives);
        }
        return new PortProbe(String.join(",", elements), changed);
    }

    private static String newProbeHost(Map<String, String> probes) {
        String probeHost;
        do {
            probeHost = "mcp-" + UUID.randomUUID() + ".invalid";
        } while (probes.containsKey(probeHost));
        return probeHost;
    }

    private static boolean hostsEqual(String first, String second) {
        if (first.startsWith("[") && first.endsWith("]")) {
            first = first.substring(1, first.length() - 1);
        }
        if (second.startsWith("[") && second.endsWith("]")) {
            second = second.substring(1, second.length() - 1);
        }
        return first.equalsIgnoreCase(second);
    }

    private URI protectedResource(ServerRequest request) {
        var requestedUri = request.requestedUri();
        String scheme = requestedUri.scheme().toLowerCase(Locale.ROOT);
        String host = requestedUri.host();
        if (host.indexOf(':') >= 0 && !host.startsWith("[")) {
            host = "[" + host + "]";
        }
        StringBuilder resource = new StringBuilder()
                .append(scheme)
                .append("://")
                .append(host);
        int port = requestedUri.port();
        if (port > 0) {
            Optional<String> explicitPort = selectedExplicitPort(request, scheme, host, port);
            if (explicitPort.isPresent()) {
                resource.append(':').append(explicitPort.orElseThrow());
            } else if (!isDefaultPort(scheme, port)) {
                resource.append(':').append(port);
            }
        }
        if (!serverEndpoint.isEmpty()) {
            if (!serverEndpoint.startsWith("/")) {
                resource.append('/');
            }
            resource.append(serverEndpoint);
        }
        return URI.create(resource.toString());
    }

    private void handle(ServerRequest request, ServerResponse response) {
        McpProtectedResourceMetadataConfig metadataConfig = config.protectedResourceMetadata().orElseThrow();
        URI resource = metadataConfig.resource().orElseGet(() -> protectedResource(request));
        validateResourceAndAuthorizationServers(resource, metadataConfig.authorizationServers());
        JsonObject.Builder metadata = JsonObject.builder()
                .set("resource", resource.toString())
                .setStrings("authorization_servers", metadataConfig.authorizationServers()
                        .stream()
                        .map(URI::toString)
                        .toList())
                .setStrings("bearer_methods_supported", List.of("header"));
        if (!metadataConfig.scopesSupported().isEmpty()) {
            metadata.setStrings("scopes_supported", metadataConfig.scopesSupported());
        }
        response.status(Status.OK_200);
        response.header(HeaderValues.CONTENT_TYPE_JSON);
        response.send(metadata.build().toString());
    }

    static final class ConfigSupport
            implements Prototype.BuilderDecorator<McpProtectedResourceMetadataConfig.BuilderBase<?, ?>> {
        @Override
        public void decorate(McpProtectedResourceMetadataConfig.BuilderBase<?, ?> builder) {
            builder.metadataPath().ifPresent(McpProtectedResourceMetadata::validateMetadataPath);
            if (builder.authorizationServers().isEmpty()) {
                throw new IllegalArgumentException("At least one authorization server must be configured");
            }
            if (builder.resource().isPresent()) {
                validateResourceAndAuthorizationServers(builder.resource().orElseThrow(), builder.authorizationServers());
            } else {
                builder.authorizationServers()
                        .forEach(authorizationServer -> validateUri(authorizationServer,
                                                                    "Authorization server issuer",
                                                                    true));
            }
            builder.scopesSupported().forEach(McpProtectedResourceMetadata::validateScope);
        }
    }

    private static final class ExactRawPathMatcher implements PathMatcher {
        private static final PathMatcher ANY_PATH = PathMatchers.any();

        private final String path;

        private ExactRawPathMatcher(String path) {
            this.path = path;
        }

        @Override
        public PathMatchers.MatchResult match(UriPath uriPath) {
            if (!path.equals(uriPath.rawPath())) {
                return PathMatchers.MatchResult.notAccepted();
            }
            return ANY_PATH.match(uriPath);
        }

        @Override
        public PathMatchers.PrefixMatchResult prefixMatch(UriPath uriPath) {
            PathMatchers.MatchResult result = match(uriPath);
            if (!result.accepted()) {
                return PathMatchers.PrefixMatchResult.notAccepted();
            }
            return new PathMatchers.PrefixMatchResult(true,
                                                      result.path(),
                                                      UriPath.createRelative(uriPath, "/"));
        }

        @Override
        public Optional<String> matchingElement() {
            return Optional.of(path);
        }
    }

    private record PortProbe(String value, boolean changed) {
    }
}
