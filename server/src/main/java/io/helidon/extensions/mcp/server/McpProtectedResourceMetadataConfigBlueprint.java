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

import java.net.URI;
import java.util.List;
import java.util.Optional;

import io.helidon.builder.api.Option;
import io.helidon.builder.api.Prototype;

/**
 * OAuth 2.0 Protected Resource Metadata configuration.
 * Resource and authorization server URIs must use HTTPS. An HTTP resource URI is accepted for localhost or loopback
 * development. An HTTP authorization server URI is accepted only when both it and the protected resource use localhost
 * or loopback literal hosts.
 */
@Prototype.Configured
@Prototype.IncludeDefaultMethods("metadataPath")
@Prototype.Blueprint(decorator = McpProtectedResourceMetadataConfigSupport.class)
interface McpProtectedResourceMetadataConfigBlueprint {
    /**
     * Canonical, externally visible URI of the MCP server.
     *
     * @return resource URI
     */
    @Option.Configured
    @Option.Required
    URI resource();

    /**
     * Exact local HTTP path where Helidon serves the protected resource metadata.
     * This is intended for reverse proxies that map canonical metadata URLs to distinct local routes.
     * If not configured, the path is derived from {@link #resource()}.
     * It must not match the MCP server path or the legacy OpenID discovery path.
     *
     * @return local metadata path
     */
    @Option.Configured
    default Optional<String> metadataPath() {
        return Optional.empty();
    }

    /**
     * Authorization server issuer identifiers that can be used with this MCP server.
     * Each value must exactly match the issuer in the authorization server metadata.
     * At least one authorization server is required.
     *
     * @return authorization server issuer identifiers
     */
    @Option.Configured
    @Option.Required
    @Option.Singular("authorizationServer")
    List<URI> authorizationServers();

    /**
     * OAuth scopes supported by this MCP server.
     *
     * @return supported scopes
     */
    @Option.Configured
    @Option.Singular("scope")
    List<String> scopesSupported();
}
