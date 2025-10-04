package io.osuprotectplus.handlers;

import java.io.File;

import org.jetbrains.annotations.NotNull;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiResponse;
import io.osuprotectplus.models.PPSystem;

public class SystemHandler implements Handler {

    @Override
    @OpenApi(summary = "Get ProtectPlus system information", description = "Retrieve various information about the system running ProtectPlus", path = "/api/v1/system", methods = HttpMethod.GET, tags = {
            "Statistics" }, responses = {
                    @OpenApiResponse(status = "200", content = {
                            @OpenApiContent(from = PPSystem.class) }, description = "Successfully retrieved system information"),
                    @OpenApiResponse(status = "500", description = "Internal server error")
            })

    public void handle(@NotNull Context ctx) throws Exception {
        PPSystem sys = new PPSystem();
        sys.setOs(System.getProperty("os.name") + " " + System.getProperty("os.version") + " (" + System.getProperty("os.arch") + ")");
        sys.setSpace(getAvailableSpace("."));
        sys.setMemory((Runtime.getRuntime().maxMemory() / (1024 * 1024)) + " MB");
        sys.setCpu(System.getenv("PROCESSOR_IDENTIFIER"));
        sys.setJava(System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");
        ctx.json(sys);
    }

    
    public static String getAvailableSpace(String path) {
        File file = new File(path);
        long freeBytes = file.getUsableSpace();
        long freeMB = freeBytes / (1024 * 1024);
        long freeGB = freeBytes / (1024 * 1024 * 1024);
        return freeGB + " GB (" + freeMB + " MB) available";
    }

}
