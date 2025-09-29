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

package io.helidon.extensions.mcp.tests;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.helidon.webserver.http.HttpRouting;
import io.helidon.webserver.testing.junit5.SetUpRoute;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.service.tool.ToolExecutionResult;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

abstract class AbstractLangchain4JCancelableToolsTest {
    protected McpClient client;
    protected static final CountDownLatch CANCELLATION_LATCH = new CountDownLatch(2);
    protected static final CountDownLatch CANCELLATION_HOOK_LATCH = new CountDownLatch(1);

    @SetUpRoute
    static void routing(HttpRouting.Builder builder) {
        CancelableTools.setUpRoute(builder, CANCELLATION_LATCH, CANCELLATION_HOOK_LATCH);
    }

    @Test
    void testCancellation() throws InterruptedException {
        ToolExecutionResult result = client.executeTool(ToolExecutionRequest.builder()
                                   .name("cancellation-tool")
                                   .arguments("")
                                   .build());

        assertThat(result.resultText(), containsString("There was a timeout executing the tool"));
        assertThat(CANCELLATION_LATCH.await(20, TimeUnit.SECONDS), is(true));
    }

    @Test
    void testCancellationHook() throws InterruptedException {
        ToolExecutionResult result = client.executeTool(ToolExecutionRequest.builder()
                                   .name("cancellation-hook-tool")
                                   .arguments("")
                                   .build());

        assertThat(result.resultText(), containsString("There was a timeout executing the tool"));
        assertThat(CANCELLATION_HOOK_LATCH.await(20, TimeUnit.SECONDS), is(true));
    }
}
