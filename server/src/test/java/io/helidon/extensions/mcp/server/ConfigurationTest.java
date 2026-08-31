/*
 * Copyright (c) 2025, 2026 Oracle and/or its affiliates.
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

import java.time.Duration;
import java.util.List;
import java.util.Map;

import io.helidon.common.media.type.MediaTypes;
import io.helidon.config.Config;
import io.helidon.config.ConfigSources;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static io.helidon.extensions.mcp.server.McpPagination.DEFAULT_PAGE_SIZE;
import static io.helidon.extensions.mcp.server.McpSampling.DEFAULT_MAX_TOOL_ITERATIONS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

class ConfigurationTest {

    @Test
    void testConfiguration() {
        McpServerConfig config = McpServerConfig.create(Config.just(ConfigSources.classpath("application-server.yaml"))
                                                                .get(McpServerConfigBlueprint.CONFIG_ROOT));

        assertThat(config.path(), is("/path"));
        assertThat(config.version(), is("1.0.0"));
        assertThat(config.icons().size(), is(2));
        McpIcon icon = config.icons().getFirst();
        assertThat(icon.source(), is("https://example.com/server.svg"));
        assertThat(icon.mediaType().orElseThrow(), is(MediaTypes.create("image/svg+xml")));
        assertThat(icon.sizes(), is(List.of("48x48", "96x96")));
        assertThat(icon.theme().orElseThrow(), is(McpIconTheme.DARK));
        assertThat(config.icons().get(1).source(), is("data:image/png;base64,iVBORw0KGgo="));
        assertThat(config.name(), is("helidon-mcp-server"));
        assertThat(config.description().orElseThrow(), is("Helidon MCP server"));
        assertThat(config.websiteUrl().orElseThrow(), is("https://example.com/mcp"));
        assertThat(config.toolsPageSize(), is(10));
        assertThat(config.promptsPageSize(), is(10));
        assertThat(config.resourcesPageSize(), is(10));
        assertThat(config.resourceTemplatesPageSize(), is(10));
        assertThat(config.rootListTimeout(), is(Duration.ofSeconds(1)));
        assertThat(config.instructions().orElse(""), is("instructions"));
        assertThat(config.subscriptionTimeout(), is(Duration.ofSeconds(1)));
        assertThat(config.stateless(), is(true));
        assertThat(config.maxSamplingToolIterations(), is(3));
        assertThat(config.maxSessionCount(), is(1));
        assertThat(config.maxRequestsPerSession(), is(1));
    }

    @Test
    void testConfigurationDefaultValues() {
        McpServerConfig config = McpServerConfig.create(Config.just(ConfigSources.classpath("application-empty.yaml"))
                                                                .get(McpServerConfigBlueprint.CONFIG_ROOT));

        assertThat(config.path(), is("/mcp"));
        assertThat(config.version(), is("0.0.1"));
        assertThat(config.icons(), is(List.of()));
        assertThat(config.name(), is("mcp-server"));
        assertThat(config.websiteUrl().isEmpty(), is(true));
        assertThat(config.description().isEmpty(), is(true));
        assertThat(config.instructions().isEmpty(), is(true));
        assertThat(config.toolsPageSize(), is(DEFAULT_PAGE_SIZE));
        assertThat(config.promptsPageSize(), is(DEFAULT_PAGE_SIZE));
        assertThat(config.resourcesPageSize(), is(DEFAULT_PAGE_SIZE));
        assertThat(config.resourceTemplatesPageSize(), is(DEFAULT_PAGE_SIZE));
        assertThat(config.rootListTimeout(), is(Duration.ofSeconds(5)));
        assertThat(config.subscriptionTimeout(), is(Duration.ofMinutes(2)));
        assertThat(config.stateless(), is(false));
        assertThat(config.maxSamplingToolIterations(), is(DEFAULT_MAX_TOOL_ITERATIONS));
        assertThat(config.maxSessionCount(), is(1000));
        assertThat(config.maxRequestsPerSession(), is(1000));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "icon.svg",
            "not a uri",
            "data:image/png,not-base64",
            "data:image/png;base64,@@@@",
            "data:image/;base64,AAAA",
            "data:image/png;broken;base64,AAAA",
            "data://example.com/icon,abc",
            "data:/image/png,abc",
            "file:///tmp/icon.svg",
            "javascript:alert(1)"
    })
    void testConfigurationInvalidIconSource(String source) {
        Config config = Config.just(ConfigSources.create(Map.of("source", source)));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                                                           () -> McpIcon.create(config));

        assertThat(exception.getMessage(), is("Icon source must be a valid HTTP(S) URL or data URI"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "tools-page-size",
            "prompts-page-size",
            "resources-page-size",
            "resource-templates-page-size"
    })
    void testConfigurationNegativePageSizeValues(String key) {
        try {
            var configSource = ConfigSources.create(Map.of(key, "-1"));
            McpServerConfig.create(Config.just(configSource));
            fail("Page size with negative value are not allowed and must be checked.");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Page size must be greater than zero"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"max-session-count", "max-requests-per-session"})
    void testConfigurationNegativeSessionPoolSize(String key) {
        try {
            var configSource = ConfigSources.create(Map.of(key, "-1"));
            McpServerConfig.create(Config.just(configSource));
            fail("negative value are not allowed and must be checked.");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("value must be greater than zero"));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0})
    void testConfigurationInvalidSamplingToolIterations(int value) {
        try {
            var configSource = ConfigSources.create(Map.of("max-sampling-tool-iterations", Integer.toString(value)));
            McpServerConfig.create(Config.just(configSource));
            fail("Sampling tool iterations must be greater than zero.");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Maximum sampling tool iterations must be greater than zero"));
        }
    }
}
