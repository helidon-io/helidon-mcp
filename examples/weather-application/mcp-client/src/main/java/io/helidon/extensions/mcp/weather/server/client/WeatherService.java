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

package io.helidon.extensions.mcp.weather.server.client;

import java.time.Duration;
import java.util.List;

import io.helidon.service.registry.Service;
import io.helidon.webserver.http.HttpRules;
import io.helidon.webserver.http.HttpService;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolProvider;

@Service.Singleton
class WeatherService implements HttpService {

    private final WeatherAiChat weather;

    WeatherService() {
        ChatModel model = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("llama3.1")
                .timeout(Duration.ofMinutes(3))
                .build();

        McpTransport transport = new HttpMcpTransport.Builder()
                .timeout(Duration.ofMinutes(10))
                .sseUrl("http://localhost:8081/mcp")
                .logRequests(true)
                .logResponses(true)
                .build();

        McpClient mcpClient = new DefaultMcpClient.Builder()
                .transport(transport)
                .build();

        ToolProvider toolProvider = McpToolProvider.builder()
                .mcpClients(List.of(mcpClient))
                .build();

        this.weather = AiServices.builder(WeatherAiChat.class)
                .chatModel(model)
                .toolProvider(toolProvider)
                .build();
    }

    @Override
    public void routing(HttpRules rules) {
        rules.get(this::weatherChat);
    }

    private void weatherChat(ServerRequest request, ServerResponse response) {
        String question = request.query().get("question");
        String answer = weather.weather(question);
        response.send(answer);
    }
}
