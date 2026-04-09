/*
 * Copyright (c) 2024, 2026 Oracle and/or its affiliates.
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

package io.helidon.extensions.mcp.codegen;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import io.helidon.common.media.type.MediaType;
import io.helidon.common.media.type.MediaTypes;
import io.helidon.common.types.TypeName;
import io.helidon.common.uri.UriPath;
import io.helidon.extensions.mcp.server.Mcp;
import io.helidon.extensions.mcp.server.McpCancellation;
import io.helidon.extensions.mcp.server.McpCompletion;
import io.helidon.extensions.mcp.server.McpCompletionRequest;
import io.helidon.extensions.mcp.server.McpCompletionResult;
import io.helidon.extensions.mcp.server.McpCompletionType;
import io.helidon.extensions.mcp.server.McpElicitation;
import io.helidon.extensions.mcp.server.McpFeatures;
import io.helidon.extensions.mcp.server.McpLogger;
import io.helidon.extensions.mcp.server.McpParameters;
import io.helidon.extensions.mcp.server.McpProgress;
import io.helidon.extensions.mcp.server.McpPrompt;
import io.helidon.extensions.mcp.server.McpPromptArgument;
import io.helidon.extensions.mcp.server.McpPromptRequest;
import io.helidon.extensions.mcp.server.McpPromptResult;
import io.helidon.extensions.mcp.server.McpRequest;
import io.helidon.extensions.mcp.server.McpResource;
import io.helidon.extensions.mcp.server.McpResourceRequest;
import io.helidon.extensions.mcp.server.McpResourceResult;
import io.helidon.extensions.mcp.server.McpResourceSubscriber;
import io.helidon.extensions.mcp.server.McpResourceUnsubscriber;
import io.helidon.extensions.mcp.server.McpRole;
import io.helidon.extensions.mcp.server.McpRoots;
import io.helidon.extensions.mcp.server.McpSampling;
import io.helidon.extensions.mcp.server.McpServerConfig;
import io.helidon.extensions.mcp.server.McpSubscribeRequest;
import io.helidon.extensions.mcp.server.McpTool;
import io.helidon.extensions.mcp.server.McpToolAnnotations;
import io.helidon.extensions.mcp.server.McpToolRequest;
import io.helidon.extensions.mcp.server.McpToolResult;
import io.helidon.extensions.mcp.server.McpUnsubscribeRequest;
import io.helidon.service.registry.Service;
import io.helidon.service.registry.Services;
import io.helidon.webserver.http.HttpFeature;
import io.helidon.webserver.http.HttpRouting;

import org.hamcrest.CoreMatchers;
import org.hamcrest.collection.IsEmptyCollection;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

// this test must be in the package of McpTypes, so it sees it and its fields
class McpTypesTest {
    @Test
    void testTypes() {
        // it is really important to test ALL constants on the class, so let's use reflection
        Field[] declaredFields = McpTypes.class.getDeclaredFields();

        Set<String> toCheck = new HashSet<>();
        Set<String> checked = new HashSet<>();
        Map<String, Field> fields = new HashMap<>();

        for (Field declaredField : declaredFields) {
            String name = declaredField.getName();

            assertThat(name + " must be a TypeName", declaredField.getType(), CoreMatchers.sameInstance(TypeName.class));
            assertThat(name + " must be static", Modifier.isStatic(declaredField.getModifiers()), is(true));
            assertThat(name + " must be package local, not public",
                       Modifier.isPublic(declaredField.getModifiers()),
                       is(false));
            assertThat(name + " must be package local, not private",
                       Modifier.isPrivate(declaredField.getModifiers()),
                       is(false));
            assertThat(name + " must be package local, not protected",
                       Modifier.isProtected(declaredField.getModifiers()),
                       is(false));
            assertThat(name + " must be final", Modifier.isFinal(declaredField.getModifiers()), is(true));

            toCheck.add(name);
            fields.put(name, declaredField);
        }

        checkField(toCheck, checked, fields, "CONSUMER", Consumer.class);
        checkField(toCheck, checked, fields, "FUNCTION", Function.class);
        checkField(toCheck, checked, fields, "URI_PATH", UriPath.class);
        checkField(toCheck, checked, fields, "MCP_NAME", Mcp.Name.class);
        checkField(toCheck, checked, fields, "MCP_PATH", Mcp.Path.class);
        checkField(toCheck, checked, fields, "MCP_TOOL", Mcp.Tool.class);
        checkField(toCheck, checked, fields, "MCP_ROLE", Mcp.Role.class);
        checkField(toCheck, checked, fields, "MCP_SERVER", Mcp.Server.class);
        checkField(toCheck, checked, fields, "MCP_PROMPT", Mcp.Prompt.class);
        checkField(toCheck, checked, fields, "MCP_VERSION", Mcp.Version.class);
        checkField(toCheck, checked, fields, "MCP_RESOURCE", Mcp.Resource.class);
        checkField(toCheck, checked, fields, "MCP_STATELESS", Mcp.Stateless.class);
        checkField(toCheck, checked, fields, "MCP_TOOL_OUTPUT_SCHEMA", Mcp.ToolOutputSchema.class);
        checkField(toCheck, checked, fields, "MCP_TOOL_OUTPUT_SCHEMA_TEXT", Mcp.ToolOutputSchemaText.class);
        checkField(toCheck, checked, fields, "MCP_TOOLS_PAGE_SIZE", Mcp.ToolsPageSize.class);
        checkField(toCheck, checked, fields, "MCP_PROMPTS_PAGE_SIZE", Mcp.PromptsPageSize.class);
        checkField(toCheck, checked, fields, "MCP_RESOURCES_PAGE_SIZE", Mcp.ResourcesPageSize.class);
        checkField(toCheck, checked, fields, "MCP_RESOURCE_TEMPLATES_PAGE_SIZE", Mcp.ResourceTemplatesPageSize.class);
        checkField(toCheck, checked, fields, "MCP_COMPLETION", Mcp.Completion.class);
        checkField(toCheck, checked, fields, "MCP_DESCRIPTION", Mcp.Description.class);
        checkField(toCheck, checked, fields, "MCP_ROOTS", McpRoots.class);
        checkField(toCheck, checked, fields, "MCP_LOGGER", McpLogger.class);
        checkField(toCheck, checked, fields, "MCP_ROLE_ENUM", McpRole.class);
        checkField(toCheck, checked, fields, "MCP_REQUEST", McpRequest.class);
        checkField(toCheck, checked, fields, "MCP_FEATURES", McpFeatures.class);
        checkField(toCheck, checked, fields, "MCP_PROGRESS", McpProgress.class);
        checkField(toCheck, checked, fields, "MCP_SAMPLING", McpSampling.class);
        checkField(toCheck, checked, fields, "MCP_TOOL_INTERFACE", McpTool.class);
        checkField(toCheck, checked, fields, "MCP_PARAMETERS", McpParameters.class);
        checkField(toCheck, checked, fields, "MCP_TOOL_RESULT", McpToolResult.class);
        checkField(toCheck, checked, fields, "MCP_PROMPT_INTERFACE", McpPrompt.class);
        checkField(toCheck, checked, fields, "MCP_CANCELLATION", McpCancellation.class);
        checkField(toCheck, checked, fields, "MCP_SERVER_CONFIG", McpServerConfig.class);
        checkField(toCheck, checked, fields, "MCP_RESOURCE_INTERFACE", McpResource.class);
        checkField(toCheck, checked, fields, "MCP_PROMPT_ARGUMENT", McpPromptArgument.class);
        checkField(toCheck, checked, fields, "MCP_COMPLETION_INTERFACE", McpCompletion.class);
        checkField(toCheck, checked, fields, "MCP_COMPLETION_TYPE", McpCompletionType.class);
        checkField(toCheck, checked, fields, "HTTP_FEATURE", HttpFeature.class);
        checkField(toCheck, checked, fields, "HELIDON_MEDIA_TYPE", MediaType.class);
        checkField(toCheck, checked, fields, "HELIDON_MEDIA_TYPES", MediaTypes.class);
        checkField(toCheck, checked, fields, "MCP_ELICITATION", McpElicitation.class);
        checkField(toCheck, checked, fields, "HTTP_ROUTING_BUILDER", HttpRouting.Builder.class);
        checkField(toCheck, checked, fields, "MCP_TOOL_ANNOTATIONS", McpToolAnnotations.class);
        checkField(toCheck, checked, fields, "MCP_COMPLETION_REQUEST", McpCompletionRequest.class);
        checkField(toCheck, checked, fields, "MCP_RESOURCE_SUBSCRIBER", Mcp.ResourceSubscriber.class);
        checkField(toCheck, checked, fields, "MCP_RESOURCE_UNSUBSCRIBER", Mcp.ResourceUnsubscriber.class);
        checkField(toCheck, checked, fields, "MCP_SUBSCRIBE_REQUEST", McpSubscribeRequest.class);
        checkField(toCheck, checked, fields, "MCP_UNSUBSCRIBE_REQUEST", McpUnsubscribeRequest.class);
        checkField(toCheck, checked, fields, "MCP_RESOURCE_SUBSCRIBER_INTERFACE", McpResourceSubscriber.class);
        checkField(toCheck, checked, fields, "MCP_RESOURCE_UNSUBSCRIBER_INTERFACE", McpResourceUnsubscriber.class);

        checkField(toCheck, checked, fields, "SERVICES", Services.class);
        checkField(toCheck, checked, fields, "OPTIONAL_STRING", Optional.class);
        checkField(toCheck, checked, fields, "OPTIONAL_TOOL_ANNOTATIONS", Optional.class);
        checkField(toCheck, checked, fields, "CONSUMER_REQUEST", Consumer.class);
        checkField(toCheck, checked, fields, "SCOPE_ANNOTATION", Service.Scope.class);
        checkField(toCheck, checked, fields, "SERVICE_SINGLETON", Service.Singleton.class);
        checkField(toCheck, checked, fields, "LIST_STRING", List.class);
        checkField(toCheck, checked, fields, "LIST_MCP_PROMPT_ARGUMENT", List.class);
        checkField(toCheck, checked, fields, "MCP_COMPLETION_RESULT", McpCompletionResult.class);
        checkField(toCheck, checked, fields, "MCP_TOOL_REQUEST", McpToolRequest.class);
        checkField(toCheck, checked, fields, "MCP_PROMPT_RESULT", McpPromptResult.class);
        checkField(toCheck, checked, fields, "MCP_PROMPT_REQUEST", McpPromptRequest.class);
        checkField(toCheck, checked, fields, "MCP_RESOURCE_RESULT", McpResourceResult.class);
        checkField(toCheck, checked, fields, "MCP_RESOURCE_REQUEST", McpResourceRequest.class);

        assertThat("All the types from McpTypes must be tested.", toCheck, IsEmptyCollection.empty());
    }

    private void checkField(Set<String> namesToCheck,
                            Set<String> checkedNames,
                            Map<String, Field> namesToFields,
                            String name,
                            Class<?> expectedType) {
        Field field = namesToFields.get(name);
        assertThat("Field " + name + " does not exist in the class", field, notNullValue());
        try {
            namesToCheck.remove(name);
            if (checkedNames.add(name)) {
                TypeName value = (TypeName) field.get(null);
                assertThat("Field " + name, value.fqName(), is(expectedType.getCanonicalName()));
            } else {
                fail("Field " + name + " is checked more than once.class");
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
