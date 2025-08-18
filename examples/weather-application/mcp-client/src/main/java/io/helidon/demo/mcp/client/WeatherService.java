package io.helidon.demo.mcp.client;

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
                .baseUrl("http://localhost:11434/")
                .modelName("llama3.1")
                .timeout(Duration.ofMinutes(3))
                .build();

        McpTransport transport = new HttpMcpTransport.Builder()
                .timeout(Duration.ofDays(1))
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
