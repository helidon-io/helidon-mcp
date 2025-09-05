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

package io.helidon.extensions.mcp.tests.declarative;

import java.util.List;

import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.McpResourceContents;
import dev.langchain4j.mcp.client.McpTextResourceContents;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static io.helidon.extensions.mcp.tests.declarative.McpResourcesServer.RESOURCE_CONTENT;
import static io.helidon.extensions.mcp.tests.declarative.McpResourcesServer.RESOURCE_MEDIA_TYPE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

abstract class AbstractLangchain4jResourcesServerTest {
    protected static McpClient client;

    @AfterAll
    static void afterAll() throws Exception {
        if (client != null) {
            client.close();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "resource", "resource1", "resource2", "resource3",
            "resource4", "resource5"
    })
    void readResource(String uri) {
        var result = client.readResource(uri);
        List<McpResourceContents> contents = result.contents();
        assertThat(contents.size(), is(1));

        McpTextResourceContents text = (McpTextResourceContents) contents.getFirst();
        assertThat(text.uri(), is(uri));
        assertThat(text.text(), is(RESOURCE_CONTENT));
        assertThat(text.mimeType(), is(RESOURCE_MEDIA_TYPE));
        assertThat(text.type(), is(McpResourceContents.Type.TEXT));
    }
}
