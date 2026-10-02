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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;

import io.helidon.http.HeaderNames;
import io.helidon.http.Status;
import io.helidon.json.JsonObject;
import io.helidon.json.JsonParser;
import io.helidon.webclient.api.HttpClientResponse;
import io.helidon.webclient.api.WebClient;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@ServerTest
class McpProtectedResourceMetadataTest {
    private static final URI RESOURCE = URI.create("https://mcp.example.test/mcp");
    private static final URI AUTHORIZATION_SERVER = URI.create("https://auth.example.test/realms/mcp/");
    private static final URI SECOND_AUTHORIZATION_SERVER = URI.create("https://login.example.test/tenant");
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    private final int port;

    McpProtectedResourceMetadataTest(WebServer server) {
        this.port = server.port();
    }

    @SetUpRoute
    static void routing(HttpRouting.Builder routing) {
        McpServerConfig delegate = McpServerConfig.builder()
                .path("/unused")
                .protectedResourceMetadata(metadata -> metadata
                        .addAuthorizationServer(AUTHORIZATION_SERVER))
                .buildPrototype();
        McpServerConfig customConfig = mock(McpServerConfig.class, delegatesTo(delegate));
        doReturn("/custom-config/").when(customConfig).path();

        routing.addFeature(McpServerFeature.builder()
                                   .protectedResourceMetadata(metadata -> metadata
                                           .resource(RESOURCE)
                                           .addAuthorizationServer(AUTHORIZATION_SERVER)
                                           .addAuthorizationServer(SECOND_AUTHORIZATION_SERVER)
                                           .addScope("openid")
                                           .addScope("mcp:tools"))
                                   .build())
                .addFeature(McpServerFeature.builder()
                                    .path("tenant/mcp/")
                                    .protectedResourceMetadata(metadata -> metadata
                                            .resource(URI.create("https://mcp.example.test/public/mcp/"))
                                            .addAuthorizationServer(AUTHORIZATION_SERVER))
                                    .build())
                .addFeature(McpServerFeature.builder()
                                    .path("/derived/mcp/")
                                    .protectedResourceMetadata(metadata -> metadata
                                            .addAuthorizationServer(AUTHORIZATION_SERVER))
                                    .build())
                .addFeature(McpServerFeature.builder()
                                    .path("/derived/custom")
                                    .protectedResourceMetadata(metadata -> metadata
                                            .metadataPath("/.well-known/oauth-protected-resource/derived-local")
                                            .addAuthorizationServer(AUTHORIZATION_SERVER))
                                    .build())
                .addFeature(McpServerFeature.builder()
                                    .path("/")
                                    .protectedResourceMetadata(metadata -> metadata
                                            .resource(URI.create("https://root.example.test"))
                                            .addAuthorizationServer(AUTHORIZATION_SERVER))
                                    .build())
                .addFeature(McpServerFeature.builder()
                                    .path("/unconfigured")
                                    .build())
                .addFeature(McpServerFeature.builder()
                                    .path("/literal-star")
                                    .protectedResourceMetadata(metadata -> metadata
                                            .resource(URI.create("https://mcp.example.test/literal/*"))
                                            .addAuthorizationServer(AUTHORIZATION_SERVER))
                                    .build())
                .addFeature(McpServerFeature.builder()
                                    .path("/literal-neighbor")
                                    .protectedResourceMetadata(metadata -> metadata
                                            .resource(URI.create("https://mcp.example.test/literal/neighbor"))
                                            .addAuthorizationServer(AUTHORIZATION_SERVER))
                                    .build())
                .addFeature(McpServerFeature.builder()
                                    .path("/literal-plus")
                                    .protectedResourceMetadata(metadata -> metadata
                                            .resource(URI.create("https://mcp.example.test/raw/a+b"))
                                            .addAuthorizationServer(AUTHORIZATION_SERVER))
                                    .build())
                .addFeature(McpServerFeature.builder()
                                    .path("/encoded-slash")
                                    .protectedResourceMetadata(metadata -> metadata
                                            .resource(URI.create("https://mcp.example.test/raw/a%2Fb"))
                                            .addAuthorizationServer(AUTHORIZATION_SERVER))
                                    .build())
                .addFeature(McpServerFeature.builder()
                                    .path("/explicit-default-port")
                                    .protectedResourceMetadata(metadata -> metadata
                                            .resource(URI.create("https://example.test:443/explicit-default-port"))
                                            .addAuthorizationServer(AUTHORIZATION_SERVER))
                                    .build())
                .addFeature(McpServerFeature.builder()
                                    .path("/external-a")
                                    .protectedResourceMetadata(metadata -> metadata
                                            .resource(URI.create("https://a.example.test/shared?tenant=a"))
                                            .metadataPath("/.well-known/oauth-protected-resource/local/a")
                                            .addAuthorizationServer(AUTHORIZATION_SERVER))
                                    .build())
                .addFeature(McpServerFeature.builder()
                                    .path("/external-b")
                                    .protectedResourceMetadata(metadata -> metadata
                                            .resource(URI.create("https://b.example.test/shared?tenant=b"))
                                            .metadataPath("/.well-known/oauth-protected-resource/local/b")
                                            .addAuthorizationServer(SECOND_AUTHORIZATION_SERVER))
                                    .build())
                .addFeature(McpServerFeature.create(customConfig));
    }

    @Test
    void servesPathSpecificProtectedResourceMetadata() throws Exception {
        HttpResponse<String> response = get("/.well-known/oauth-protected-resource/mcp");

        assertThat(response.statusCode(), is(200));
        assertThat(response.headers().firstValue("content-type"), is(Optional.of("application/json")));
        assertThat(response.headers().firstValue("location"), is(Optional.empty()));

        JsonObject metadata = JsonParser.create(response.body()).readJsonObject();
        assertThat(metadata.keysAsStrings(), containsInAnyOrder(
                "resource", "authorization_servers", "bearer_methods_supported", "scopes_supported"));
        assertThat(metadata.stringValue("resource").orElseThrow(), is(RESOURCE.toString()));
        assertThat(stringValues(metadata, "authorization_servers"),
                   contains(AUTHORIZATION_SERVER.toString(), SECOND_AUTHORIZATION_SERVER.toString()));
        assertThat(stringValues(metadata, "bearer_methods_supported"), contains("header"));
        assertThat(stringValues(metadata, "scopes_supported"), contains("openid", "mcp:tools"));
    }

    @Test
    void derivesDiscoveryPathFromCanonicalResource() throws Exception {
        HttpResponse<String> response = get("/.well-known/oauth-protected-resource/public/mcp/");

        assertThat(response.statusCode(), is(200));
        JsonObject metadata = JsonParser.create(response.body()).readJsonObject();
        assertThat(metadata.stringValue("resource").orElseThrow(),
                   is("https://mcp.example.test/public/mcp/"));
        assertThat(metadata.keysAsStrings(), containsInAnyOrder(
                "resource", "authorization_servers", "bearer_methods_supported"));
    }

    @Test
    void derivesResourceFromRequestAuthorityAndConfiguredMcpPath() throws Exception {
        String metadataPath = "/.well-known/oauth-protected-resource/derived/mcp";
        HttpResponse<String> localhostResponse = get("localhost", metadataPath + "?ignored=request-query");
        HttpResponse<String> loopbackResponse = get("127.0.0.1", metadataPath);

        assertThat(localhostResponse.statusCode(), is(200));
        JsonObject localhostMetadata = JsonParser.create(localhostResponse.body()).readJsonObject();
        assertThat(localhostMetadata.stringValue("resource").orElseThrow(),
                   is("http://localhost:" + port + "/derived/mcp"));

        assertThat(loopbackResponse.statusCode(), is(200));
        JsonObject loopbackMetadata = JsonParser.create(loopbackResponse.body()).readJsonObject();
        assertThat(loopbackMetadata.stringValue("resource").orElseThrow(),
                   is("http://127.0.0.1:" + port + "/derived/mcp"));
    }

    @Test
    void normalizesCustomServerConfigPathForRouting() throws Exception {
        String initialize = """
                {
                  "jsonrpc": "2.0",
                  "id": 1,
                  "method": "initialize",
                  "params": {
                    "protocolVersion": "2025-06-18",
                    "capabilities": {},
                    "clientInfo": {"name": "test-client", "version": "1.0.0"}
                  }
                }
                """;
        HttpRequest initializeRequest = HttpRequest.newBuilder(URI.create("http://localhost:"
                                                                                  + port
                                                                                  + "/custom-config"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(initialize))
                .build();
        HttpResponse<String> initializeResponse = HTTP_CLIENT.send(initializeRequest,
                                                                    HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> response = get("/.well-known/oauth-protected-resource/custom-config");

        assertThat(initializeResponse.statusCode(), is(200));
        assertThat(response.statusCode(), is(200));
        JsonObject metadata = JsonParser.create(response.body()).readJsonObject();
        assertThat(metadata.stringValue("resource").orElseThrow(),
                   is("http://localhost:" + port + "/custom-config"));
    }

    @Test
    void preservesExplicitDefaultPortWhenDerivingResource() {
        String metadataPath = "/.well-known/oauth-protected-resource/derived/mcp";
        JsonObject explicitPort = getWithAuthority("localhost:80", metadataPath);
        JsonObject paddedPort = getWithAuthority("localhost:080", metadataPath);
        JsonObject omittedPort = getWithAuthority("localhost", metadataPath);

        assertThat(explicitPort.stringValue("resource").orElseThrow(),
                   is("http://localhost:80/derived/mcp"));
        assertThat(paddedPort.stringValue("resource").orElseThrow(),
                   is("http://localhost:080/derived/mcp"));
        assertThat(omittedPort.stringValue("resource").orElseThrow(),
                   is("http://localhost/derived/mcp"));
    }

    @Test
    void preservesExplicitDefaultPortInConfiguredResource() throws Exception {
        HttpResponse<String> response = get("/.well-known/oauth-protected-resource/explicit-default-port");

        assertThat(response.statusCode(), is(200));
        JsonObject metadata = JsonParser.create(response.body()).readJsonObject();
        assertThat(metadata.stringValue("resource").orElseThrow(),
                   is("https://example.test:443/explicit-default-port"));
    }

    @Test
    void usesCustomMetadataPathWithoutChangingDerivedResource() throws Exception {
        HttpResponse<String> response = get("/.well-known/oauth-protected-resource/derived-local");

        assertThat(response.statusCode(), is(200));
        JsonObject metadata = JsonParser.create(response.body()).readJsonObject();
        assertThat(metadata.stringValue("resource").orElseThrow(),
                   is("http://localhost:" + port + "/derived/custom"));
    }

    @Test
    void servesRootProtectedResourceMetadata() throws Exception {
        HttpResponse<String> response = get("/.well-known/oauth-protected-resource");

        assertThat(response.statusCode(), is(200));
        JsonObject metadata = JsonParser.create(response.body()).readJsonObject();
        assertThat(metadata.stringValue("resource").orElseThrow(), is("https://root.example.test"));
    }

    @Test
    void omitsEndpointWhenMetadataIsNotConfigured() throws Exception {
        HttpResponse<String> response = get("/.well-known/oauth-protected-resource/unconfigured");

        assertThat(response.statusCode(), is(404));
    }

    @Test
    void treatsCanonicalResourcePathAsAnExactRoute() throws Exception {
        HttpResponse<String> literalStar = get("/.well-known/oauth-protected-resource/literal/*");
        HttpResponse<String> neighbor = get("/.well-known/oauth-protected-resource/literal/neighbor");

        assertThat(literalStar.statusCode(), is(200));
        assertThat(JsonParser.create(literalStar.body()).readJsonObject().stringValue("resource").orElseThrow(),
                   is("https://mcp.example.test/literal/*"));
        assertThat(neighbor.statusCode(), is(200));
        assertThat(JsonParser.create(neighbor.body()).readJsonObject().stringValue("resource").orElseThrow(),
                   is("https://mcp.example.test/literal/neighbor"));
    }

    @Test
    void preservesRawCanonicalResourcePathWhenRoutingMetadata() throws Exception {
        HttpResponse<String> literalPlus = get("/.well-known/oauth-protected-resource/raw/a+b");
        HttpResponse<String> encodedSpaceAlias = get("/.well-known/oauth-protected-resource/raw/a%20b");
        HttpResponse<String> encodedSlash = get("/.well-known/oauth-protected-resource/raw/a%2Fb");
        HttpResponse<String> decodedSlashAlias = get("/.well-known/oauth-protected-resource/raw/a/b");

        assertThat(literalPlus.statusCode(), is(200));
        assertThat(JsonParser.create(literalPlus.body()).readJsonObject().stringValue("resource").orElseThrow(),
                   is("https://mcp.example.test/raw/a+b"));
        assertThat(encodedSpaceAlias.statusCode(), is(404));
        assertThat(encodedSlash.statusCode(), is(200));
        assertThat(JsonParser.create(encodedSlash.body()).readJsonObject().stringValue("resource").orElseThrow(),
                   is("https://mcp.example.test/raw/a%2Fb"));
        assertThat(decodedSlashAlias.statusCode(), is(404));
    }

    @Test
    void usesDistinctLocalMetadataPathsForExternalResources() throws Exception {
        HttpResponse<String> first = get("/.well-known/oauth-protected-resource/local/a?ignored=request-query");
        HttpResponse<String> second = get("/.well-known/oauth-protected-resource/local/b");
        HttpResponse<String> canonicalPath = get("/.well-known/oauth-protected-resource/shared?tenant=a");

        assertThat(first.statusCode(), is(200));
        assertThat(JsonParser.create(first.body()).readJsonObject().stringValue("resource").orElseThrow(),
                   is("https://a.example.test/shared?tenant=a"));
        assertThat(second.statusCode(), is(200));
        assertThat(JsonParser.create(second.body()).readJsonObject().stringValue("resource").orElseThrow(),
                   is("https://b.example.test/shared?tenant=b"));
        assertThat(canonicalPath.statusCode(), is(404));
    }

    @Test
    void requiresAuthorizationServer() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                                                           () -> McpProtectedResourceMetadataConfig.builder()
                                                                   .build());

        assertThat(exception.getMessage(), containsString("At least one authorization server"));
    }

    @Test
    void rejectsInvalidResourceUri() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                                                           () -> McpProtectedResourceMetadataConfig.builder()
                                                                   .resource(URI.create("https://mcp.example.test/mcp#fragment"))
                                                                   .addAuthorizationServer(AUTHORIZATION_SERVER)
                                                                   .build());

        assertThat(exception.getMessage(), containsString("Protected resource must not contain a fragment"));
    }

    @Test
    void rejectsInvalidAuthorizationServerIssuer() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                                                           () -> McpProtectedResourceMetadataConfig.builder()
                                                                   .resource(RESOURCE)
                                                                   .addAuthorizationServer(
                                                                           URI.create("https://auth.example.test?tenant=mcp"))
                                                                   .build());

        assertThat(exception.getMessage(), containsString("Authorization server issuer must not contain a query"));
    }

    @Test
    void rejectsInvalidScope() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                                                           () -> McpProtectedResourceMetadataConfig.builder()
                                                                   .resource(RESOURCE)
                                                                   .addAuthorizationServer(AUTHORIZATION_SERVER)
                                                                   .addScope("two scopes")
                                                                   .build());

        assertThat(exception.getMessage(), containsString("Invalid OAuth scope"));
    }

    @Test
    void rejectsInvalidMetadataPath() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                                                           () -> McpProtectedResourceMetadataConfig.builder()
                                                                   .resource(RESOURCE)
                                                                   .metadataPath("/metadata?tenant=mcp")
                                                                   .addAuthorizationServer(AUTHORIZATION_SERVER)
                                                                   .build());

        assertThat(exception.getMessage(), containsString("metadata path must be an absolute path"));
    }

    @ParameterizedTest
    @CsvSource({
            "/mcp, /mcp",
            "mcp/, /mcp",
            "/mcp, /%6dcp",
            "/%6dcp, /mcp",
            "/m+cp, /m%20cp",
            "/api/*, /api/prm",
            "/{path}, /metadata",
            "/, /",
            "/custom, /.well-known/openid-configuration",
            "/custom, /.well-known/%6fpenid-configuration"
    })
    void rejectsMetadataPathConflictingWithExistingGetRoute(String serverPath, String metadataPath) {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                                                           () -> McpServerFeature.builder()
                                                                   .path(serverPath)
                                                                   .protectedResourceMetadata(metadata -> metadata
                                                                           .resource(RESOURCE)
                                                                           .metadataPath(metadataPath)
                                                                           .addAuthorizationServer(AUTHORIZATION_SERVER))
                                                                   .build());

        assertThat(exception.getMessage(), containsString("conflicts with an existing MCP server route"));
    }

    @Test
    void requiresExplicitResourceForPatternServerPath() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                                                           () -> McpServerFeature.builder()
                                                                   .path("/api/*")
                                                                   .protectedResourceMetadata(metadata -> metadata
                                                                           .addAuthorizationServer(AUTHORIZATION_SERVER))
                                                                   .build());

        assertThat(exception.getMessage(), containsString("resource must be configured"));
    }

    @Test
    void rejectsInsecureNonLoopbackResource() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                                                           () -> McpProtectedResourceMetadataConfig.builder()
                                                                   .resource(URI.create("http://mcp.example.test/mcp"))
                                                                   .addAuthorizationServer(AUTHORIZATION_SERVER)
                                                                   .build());

        assertThat(exception.getMessage(), containsString("must use HTTPS unless the host is localhost"));
    }

    @Test
    void rejectsInsecureNonLoopbackAuthorizationServer() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                                                           () -> McpProtectedResourceMetadataConfig.builder()
                                                                   .resource(RESOURCE)
                                                                   .addAuthorizationServer(
                                                                           URI.create("http://auth.example.test/issuer"))
                                                                   .build());

        assertThat(exception.getMessage(), containsString("must use HTTPS unless the host is localhost"));
    }

    @Test
    void rejectsLoopbackHttpAuthorizationServerForPublicResource() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                                                           () -> McpProtectedResourceMetadataConfig.builder()
                                                                   .resource(RESOURCE)
                                                                   .addAuthorizationServer(
                                                                           URI.create("http://localhost:8080/issuer"))
                                                                   .build());

        assertThat(exception.getMessage(), containsString("unless both it and the protected resource"));
    }

    @Test
    void acceptsLoopbackHttpUris() {
        McpProtectedResourceMetadataConfig metadata = McpProtectedResourceMetadataConfig.builder()
                .resource(URI.create("http://127.0.0.2:8081/mcp"))
                .addAuthorizationServer(URI.create("http://[::1]:8080/issuer"))
                .build();

        assertThat(metadata.resource(), is(Optional.of(URI.create("http://127.0.0.2:8081/mcp"))));
    }

    @Test
    void acceptsLoopbackHttpResourceWithHttpsAuthorizationServer() {
        McpProtectedResourceMetadataConfig metadata = McpProtectedResourceMetadataConfig.builder()
                .resource(URI.create("http://localhost:8081/mcp"))
                .addAuthorizationServer(AUTHORIZATION_SERVER)
                .build();

        assertThat(metadata.authorizationServers(), contains(AUTHORIZATION_SERVER));
    }

    private static List<String> stringValues(JsonObject object, String name) {
        return object.arrayValue(name)
                .orElseThrow()
                .values()
                .stream()
                .map(it -> it.asString().value())
                .toList();
    }

    private JsonObject getWithAuthority(String authority, String path) {
        WebClient client = WebClient.builder()
                .baseUri("http://" + authority)
                .build();
        try (HttpClientResponse response = client.get(path)
                .address(new InetSocketAddress("localhost", port))
                .header(HeaderNames.HOST, authority)
                .request()) {
            assertThat(response.status(), is(Status.OK_200));
            return JsonParser.create(response.as(String.class)).readJsonObject();
        } finally {
            client.closeResource();
        }
    }

    private HttpResponse<String> get(String path) throws Exception {
        return get("localhost", path);
    }

    private HttpResponse<String> get(String host, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://" + host + ":" + port + path))
                .GET()
                .build();
        return HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
