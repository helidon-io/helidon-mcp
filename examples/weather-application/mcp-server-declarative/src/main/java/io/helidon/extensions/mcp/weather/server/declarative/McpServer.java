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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Mcp.Server("helidon-mcp-weather-server")
class McpServer {

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
            Alert alert = new ObjectMapper().readValue(response.as(String.class), new TypeReference<>() { });

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
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Alert(@JsonProperty("features") List<Feature> features) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Feature(@JsonProperty("properties") Properties properties) { }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Properties(@JsonProperty("event") String event,
                                 @JsonProperty("id") String id,
                                 @JsonProperty("areaDesc") String areaDesc,
                                 @JsonProperty("severity") String severity,
                                 @JsonProperty("description") String description,
                                 @JsonProperty("instruction") String instruction) { }
    }
}
