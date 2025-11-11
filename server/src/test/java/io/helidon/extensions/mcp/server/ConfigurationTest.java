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

import java.time.Duration;
import java.util.Map;

import io.helidon.config.Config;
import io.helidon.config.ConfigSources;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static io.helidon.extensions.mcp.server.McpPagination.DEFAULT_PAGE_SIZE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;

class ConfigurationTest {

    @Test
    void testConfiguration() {
        var config = McpServerConfig.create(Config.just(ConfigSources.classpath("application-server.yaml"))
                                                             .get(McpServerConfigBlueprint.CONFIG_ROOT));

        assertThat(config.path(), is("/path"));
        assertThat(config.version(), is("1.0.0"));
        assertThat(config.name(), is("helidon-mcp-server"));
        assertThat(config.toolsPageSize(), is(10));
        assertThat(config.promptsPageSize(), is(10));
        assertThat(config.resourcesPageSize(), is(10));
        assertThat(config.resourceTemplatesPageSize(), is(10));
        assertThat(config.rootListTimeout(), is(Duration.ofSeconds(1)));
        assertThat(config.subscriptionTimeout(), is(Duration.ofSeconds(1)));
    }

    @Test
    void testConfigurationDefaultValues() {
        var config = McpServerConfig.create(Config.just(ConfigSources.classpath("application-empty.yaml"))
                                                             .get(McpServerConfigBlueprint.CONFIG_ROOT));

        assertThat(config.path(), is("/mcp"));
        assertThat(config.version(), is("0.0.1"));
        assertThat(config.name(), is("mcp-server"));
        assertThat(config.toolsPageSize(), is(DEFAULT_PAGE_SIZE));
        assertThat(config.promptsPageSize(), is(DEFAULT_PAGE_SIZE));
        assertThat(config.resourcesPageSize(), is(DEFAULT_PAGE_SIZE));
        assertThat(config.resourceTemplatesPageSize(), is(DEFAULT_PAGE_SIZE));
        assertThat(config.rootListTimeout(), is(Duration.ofSeconds(5)));
        assertThat(config.subscriptionTimeout(), is(Duration.ofMinutes(2)));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "tools-page-size",
            "prompts-page-size",
            "resources-page-size",
            "resource-templates-page-size",
    })
    void testConfigurationNegativePageSizeValues(String key) {
        try {
            var configSource = ConfigSources.create(Map.of(key, "-1"));
            var config = McpServerConfig.create(Config.just(configSource));
            fail("Page size with negative value are not allowed and must be checked.");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("Page size must be greater than zero"));
        }
    }
}
