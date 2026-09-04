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

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.function.Consumer;

import io.helidon.common.configurable.AllowList;
import io.helidon.http.HeaderName;
import io.helidon.http.HeaderNames;
import io.helidon.http.RequestedUriDiscoveryContext;
import io.helidon.http.Status;
import io.helidon.json.JsonObject;
import io.helidon.json.JsonParser;
import io.helidon.webclient.api.HttpClientResponse;
import io.helidon.webclient.api.HttpClientRequest;
import io.helidon.webclient.api.WebClient;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.WebServerConfig;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;
import io.helidon.webserver.testing.junit5.SetUpServer;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@ServerTest
class McpProtectedResourceMetadataForwardedTest {
    private static final String METADATA_PATH = "/.well-known/oauth-protected-resource/derived/mcp";

    private final int port;

    McpProtectedResourceMetadataForwardedTest(WebServer server) {
        this.port = server.port();
    }

    @SetUpServer
    static void server(WebServerConfig.Builder server) {
        server.requestedUriDiscoveryContext(discovery -> discovery
                .addDiscoveryType(RequestedUriDiscoveryContext.RequestedUriDiscoveryType.X_FORWARDED)
                .addDiscoveryType(RequestedUriDiscoveryContext.RequestedUriDiscoveryType.FORWARDED)
                .trustedProxies(AllowList.create(config -> config.allowAll(true))));
    }

    @SetUpRoute
    static void routing(HttpRouting.Builder routing) {
        routing.addFeature(McpServerFeature.builder()
                                   .path("/derived/mcp")
                                   .protectedResourceMetadata(metadata -> metadata
                                           .addAuthorizationServer(URI.create("https://auth.example.test")))
                                   .build());
    }

    @Test
    void doesNotLeakBackendDefaultPortIntoForwardedResource() {
        JsonObject metadata = get("example.test:443",
                                  HeaderNames.FORWARDED,
                                  "host=example.test;proto=https");

        assertThat(metadata.stringValue("resource").orElseThrow(),
                   is("https://example.test/derived/mcp"));
    }

    @Test
    void preservesExplicitDefaultPortFromForwardedResource() {
        JsonObject metadata = get("backend.test:8443",
                                  HeaderNames.FORWARDED,
                                  "for=198.51.100.1;host=ignored.test:443;proto=https,"
                                          + "for=127.0.0.1;host=\"example.test:443\";proto=https");

        assertThat(metadata.stringValue("resource").orElseThrow(),
                   is("https://example.test:443/derived/mcp"));
    }

    @Test
    void preservesOriginalForwardedPortToken() {
        JsonObject defaultPort = get("backend.test:8443",
                                     HeaderNames.FORWARDED,
                                     "host=\"example.test:0443\";proto=https");
        JsonObject nonDefaultPort = get("backend.test:8443",
                                        HeaderNames.FORWARDED,
                                        "host=\"example.test:08443\";proto=https");

        assertThat(defaultPort.stringValue("resource").orElseThrow(),
                   is("https://example.test:0443/derived/mcp"));
        assertThat(nonDefaultPort.stringValue("resource").orElseThrow(),
                   is("https://example.test:08443/derived/mcp"));
    }

    @Test
    void preservesExplicitDefaultPortFromForwardedIpv6Resource() {
        JsonObject metadata = get("backend.test:8443",
                                  HeaderNames.FORWARDED,
                                  "host=\"[::1]:80\";proto=http");

        assertThat(metadata.stringValue("resource").orElseThrow(),
                   is("http://[::1]:80/derived/mcp"));
    }

    @Test
    void preservesExplicitDefaultPortWhenForwardingHeaderIsIgnored() {
        JsonObject metadata = get("localhost:80",
                                  HeaderNames.X_FORWARDED_FOR,
                                  "203.0.113.1");

        assertThat(metadata.stringValue("resource").orElseThrow(),
                   is("http://localhost:80/derived/mcp"));
    }

    @Test
    void doesNotUseHostPortTokenWhenXForwardedPortOverridesIt() {
        JsonObject metadata = get("example.test:443", request -> request
                .header(HeaderNames.X_FORWARDED_FOR, "198.51.100.1", "127.0.0.1")
                .header(HeaderNames.X_FORWARDED_PROTO, "https")
                .header(HeaderNames.X_FORWARDED_PORT, "443"));

        assertThat(metadata.stringValue("resource").orElseThrow(),
                   is("https://example.test/derived/mcp"));
    }

    @Test
    void doesNotTreatXForwardedPortAsExplicitUriSyntax() {
        JsonObject metadata = get("example.test", request -> request
                .header(HeaderNames.X_FORWARDED_FOR, "198.51.100.1", "127.0.0.1")
                .header(HeaderNames.X_FORWARDED_HOST, "example.test")
                .header(HeaderNames.X_FORWARDED_PROTO, "https")
                .header(HeaderNames.X_FORWARDED_PORT, "443"));

        assertThat(metadata.stringValue("resource").orElseThrow(),
                   is("https://example.test/derived/mcp"));
    }

    private JsonObject get(String authority, HeaderName discoveryHeader, String discoveryValue) {
        return get(authority, request -> request.header(discoveryHeader, discoveryValue));
    }

    private JsonObject get(String authority, Consumer<HttpClientRequest> requestConfig) {
        WebClient client = WebClient.builder()
                .baseUri("http://" + authority)
                .build();
        HttpClientRequest request = client.get(METADATA_PATH)
                .address(new InetSocketAddress("localhost", port))
                .header(HeaderNames.HOST, authority);
        requestConfig.accept(request);
        try (HttpClientResponse response = request.request()) {
            assertThat(response.status(), is(Status.OK_200));
            return JsonParser.create(response.as(String.class)).readJsonObject();
        } finally {
            client.closeResource();
        }
    }
}
