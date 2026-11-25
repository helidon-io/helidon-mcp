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
package io.helidon.extensions.mcp.tests.declarative;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import io.helidon.extensions.mcp.server.Mcp;
import io.helidon.json.schema.JsonSchema;

@Mcp.Server
@Mcp.Path("/enum-inputs")
@Mcp.Stateless
class McpEnumInputsServer {
    @Mcp.Tool("Create a project for the selected Helidon flavor")
    String createProject(String projectName, @Mcp.Description("Choose \"SE\" or \"MP\"\tby name") HelidonFlavor flavor) {
        return projectName + ":" + (flavor == null ? "unspecified" : flavor.name());
    }

    @Mcp.Tool("Select a required Helidon flavor")
    String requiredFlavor(@Mcp.Required HelidonFlavor flavor) {
        return flavor.name();
    }

    @Mcp.Tool("Select a day of the week")
    String day(DayOfWeek day) {
        return day.name();
    }

    @Mcp.Tool("Select multiple Helidon flavors")
    String flavors(@Mcp.Description("Helidon flavors") List<HelidonFlavor> flavors) {
        return flavors.stream().map(HelidonFlavor::name).collect(Collectors.joining(","));
    }

    @Mcp.Tool("Select an array of Helidon flavors")
    String flavorArray(@Mcp.Description("Helidon flavors") HelidonFlavor[] flavors) {
        return flavors == null ? "unspecified"
                : Arrays.stream(flavors)
                        .map(flavor -> flavor == null ? "unspecified" : flavor.name())
                        .collect(Collectors.joining(","));
    }

    @Mcp.Tool("Select a deployment environment")
    String deployment(Deployment deployment) {
        return deployment.name();
    }

    @Mcp.Tool("Select deployment environments")
    String deploymentList(List<Deployment> deployments) {
        return deployments.stream().map(Deployment::name).collect(Collectors.joining(","));
    }

    @Mcp.Tool("Select an array of deployment environments")
    String deploymentArray(Deployment[] deployments) {
        return Arrays.stream(deployments).map(Deployment::name).collect(Collectors.joining(","));
    }

    enum HelidonFlavor {
        SE,
        MP;

        @Override
        public String toString() {
            return "Helidon " + name();
        }
    }

    @JsonSchema.Schema
    @JsonSchema.Title("Deployment environment")
    enum Deployment {
        DEVELOPMENT,
        PRODUCTION
    }
}
