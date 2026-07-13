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

package io.helidon.extensions.mcp.examples.secured;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;
import io.helidon.extensions.mcp.server.McpServerConfig;
import io.helidon.json.JsonParser;
import io.helidon.security.providers.oidc.OidcFeature;
import io.helidon.webserver.WebServer;
import io.helidon.webserver.WebServerConfig;
import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.ServerTest;
import io.helidon.webserver.testing.junit5.SetUpRoute;
import io.helidon.webserver.testing.junit5.SetUpServer;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.StreamableHttpMcpTransport;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@Testcontainers(disabledWithoutDocker = true)
@ServerTest
class Langchain4jKeycloakTest {
    private static final String REALM = "mcp-realm";
    private static final String CLIENT_ID = "mcp-client";
    private static final String SCOPE = "openid mcp-scope";
    private static final String USERNAME = "mcp-user";
    private static final String PASSWORD = "mcp-password";
    private static final String TOOL_NAME = "secured-tool";

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    @Container
    private static final KeycloakContainer KEYCLOAK = new KeycloakContainer("quay.io/keycloak/keycloak:24.0.5")
            .withRealmImportFile("/mcp-test-realm.json")
            .waitingFor(KeycloakContainer.LOG_WAIT_STRATEGY);
    private static String accessToken;

    private final int port;

    Langchain4jKeycloakTest(WebServer server) {
        this.port = server.port();
    }

    @BeforeAll
    static void beforeAll() throws Exception {
        if (!KEYCLOAK.isRunning()) {
            KEYCLOAK.start();
        }
        accessToken = accessToken();
    }

    @SetUpServer
    static void server(WebServerConfig.Builder builder) {
        Config config = Config.builder()
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .sources(ConfigSources.create(Map.of(
                                 "security.providers.0.oidc.identity-uri",
                                 KEYCLOAK.getAuthServerUrl() + "/realms/" + REALM)),
                         ConfigSources.classpath("application.yaml"))
                .build();
        builder.config(config.get("server"))
                .host("localhost")
                .port(0)
                .shutdownHook(false);
    }

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        Config config = Config.builder()
                .disableEnvironmentVariablesSource()
                .disableSystemPropertiesSource()
                .sources(ConfigSources.create(Map.of(
                                 "security.providers.0.oidc.identity-uri",
                                 KEYCLOAK.getAuthServerUrl() + "/realms/" + REALM)),
                         ConfigSources.classpath("application.yaml"))
                .build();
        builder.addFeature(McpServerConfig.builder()
                                   .config(config.get("mcp.server"))
                                   .addTool(new SecuredTool()))
                .addFeature(OidcFeature.create(config));
    }

    @Test
    void callSecuredToolWithKeycloakToken() throws Exception {
        try (McpClient client = langchain4jClient()) {
            var tools = client.listTools();
            assertThat(tools.size(), is(1));
            assertThat(tools.getFirst().name(), is(TOOL_NAME));

            var result = client.executeTool(ToolExecutionRequest.builder()
                                                    .name(TOOL_NAME)
                                                    .build());

            assertThat(result.resultText(), is("Username: " + USERNAME));
        }
    }

    private McpClient langchain4jClient() {
        McpTransport transport = new StreamableHttpMcpTransport.Builder()
                .url("http://localhost:" + port + "/secured")
                .customHeaders(Map.of("Authorization", "Bearer " + accessToken))
                .build();

        return new DefaultMcpClient.Builder()
                .protocolVersion("2025-06-18")
                .transport(transport)
                .build();
    }

    private static String accessToken() throws Exception {
        String issuerUri = KEYCLOAK.getAuthServerUrl() + "/realms/" + REALM;
        String form = Map.of(
                        "grant_type", "password",
                        "client_id", CLIENT_ID,
                        "username", USERNAME,
                        "password", PASSWORD,
                        "scope", SCOPE)
                .entrySet()
                .stream()
                .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
                        + "="
                        + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        HttpRequest request = HttpRequest.newBuilder(URI.create(issuerUri + "/protocol/openid-connect/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.body(), response.statusCode(), is(200));

        return JsonParser.create(response.body())
                .readJsonObject()
                .stringValue("access_token")
                .orElseThrow();
    }
}
