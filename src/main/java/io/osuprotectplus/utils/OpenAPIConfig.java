package io.osuprotectplus.utils;

import java.util.function.Consumer;

import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.OpenApiPluginConfiguration;
import io.osuprotectplus.App;

public class OpenAPIConfig extends OpenApiPlugin {

    public OpenAPIConfig(Consumer<OpenApiPluginConfiguration> userConfig) {
        super(userConfig.andThen(config -> {
            config.withDefinitionConfiguration((version, definition) -> {
                definition.withInfo(info -> {
                    info.title("ProtectPlus AntiCheat API")
                            .version("1.0.5")
                            .description("The next level API for ProtectPlus AntiCheat");
                });
                definition.withServer(server -> {
                    server.url(App.apiUrl)
                            .description(App.runtime + " SERVER");
                });
            });
        }));
    }
}