package io.helidon.demo.mcp.client;

import io.helidon.common.config.Config;
import io.helidon.logging.common.LogConfig;
import io.helidon.service.registry.Services;
import io.helidon.webserver.WebServer;

class Main {

    private Main() {
    }

    public static void main(String[] args) {
        LogConfig.configureRuntime();
        Config config = Services.get(Config.class);

        WebServer.builder()
                .config(config.get("server"))
                .routing(routing -> routing.register("/weather", Services.get(WeatherService.class)))
                .build()
                .start();
    }
}
