package io.osuprotectplus.handlers;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import io.javalin.http.Context;
import io.javalin.http.Handler;

public class HomeHandler implements Handler {

    @Override
    public void handle(Context ctx) throws Exception {
        // Load web/index.html from resources
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("web/index.html")) {
            if (inputStream == null) {
                ctx.status(404).result("index.html not found in resources");
                return;
            }
            
            String html = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            ctx.html(html);
        }
    }
}
