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

package io.helidon.extensions.mcp.weather.server.declarative;

import java.util.List;
import java.util.stream.Collectors;

import io.helidon.extensions.mcp.server.Mcp;
import io.helidon.extensions.mcp.server.McpToolContent;
import io.helidon.extensions.mcp.server.McpToolContents;
import io.helidon.webclient.api.HttpClientResponse;
import io.helidon.webclient.api.WebClient;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.spi.JsonbProvider;

@Mcp.Server("helidon-mcp-weather-server")
class McpServer {
    private static final Jsonb JSON = JsonbProvider.provider().create().build();
    private static final WebClient WEBCLIENT = WebClient.builder()
                                                        .baseUri("https://api.weather.gov")
                                                        .addHeader("Accept", "application/geo+json")
                                                        .addHeader("User-Agent", "WeatherApiClient/1.0 (your@email.com)")
                                                        .build();

    @Mcp.Tool("Get weather alert per US state")
    List<McpToolContent> getWeatherAlertFromState(String state) {
        try (HttpClientResponse response = WEBCLIENT.get()
                .path("/alerts/active/area/" + state)
                .request()) {

            Alert alert = JSON.fromJson(response.as(String.class), Alert.class);
            String content = alert.features()
                    .stream()
                    .map(f -> String.format("""
                                                    Event: %s
                                                    Area: %s
                                                    Severity: %s
                                                    Description: %s
                                                    Instructions: %s
                                                    """, f.properties().event(), f.properties.areaDesc(), f.properties.severity(),
                                            f.properties.description(), f.properties.instruction()))
                    .collect(Collectors.joining("\n"));

            if (content.isEmpty()) {
                return List.of(McpToolContents.textContent("There is no alert for this state"));
            }
            return List.of(McpToolContents.textContent(content));
        }
    }

    public record Alert(@JsonbProperty("features") List<Feature> features) {

        public record Feature(@JsonbProperty("properties") Properties properties) {
        }

        public record Properties(@JsonbProperty("event") String event,
                                 @JsonbProperty("id") String id,
                                 @JsonbProperty("areaDesc") String areaDesc,
                                 @JsonbProperty("severity") String severity,
                                 @JsonbProperty("description") String description,
                                 @JsonbProperty("instruction") String instruction) {
        }
    }
}
