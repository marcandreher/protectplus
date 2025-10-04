package io.osuprotectplus.handlers;

import org.jetbrains.annotations.NotNull;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiResponse;
import io.osuprotectplus.models.Stats;

public class StatsHandler implements Handler {

    @Override
    @OpenApi(
        summary = "Get ProtectPlus statistics",
        description = "Retrieve various statistics about ProtectPlus usage and detections",
        path = "/api/v1/stats",
        methods = HttpMethod.GET,
        tags = {"Statistics"},
        responses = {
            @OpenApiResponse(
                status = "200",
                content = {@OpenApiContent(from = Stats.class)},
                description = "Successfully retrieved statistics"
            ),
            @OpenApiResponse(
                status = "500",
                description = "Internal server error"
            )
        }
    )
    public void handle(@NotNull Context ctx) throws Exception {
        // Get count of files in detection/ directory
        var detectionDir = new java.io.File("detection");
        Stats stats = new Stats();

        if (detectionDir.exists() && detectionDir.isDirectory()) {
            stats.setDetectionCount(java.util.Arrays.stream(detectionDir.listFiles())
                .filter(file -> file.isFile() && file.getName().endsWith(".json"))
                .count());
        }
        // Get count of files in storage/ directory (recursively)
        var storageDir = new java.io.File("storage");
        
        if (storageDir.exists() && storageDir.isDirectory()) {
            stats.setReplayCount(java.util.Arrays.stream(storageDir.listFiles())
                .filter(file -> file.isDirectory())
                .flatMap(dir -> java.util.Arrays.stream(dir.listFiles()))
                .filter(file -> file.isFile() && file.getName().endsWith(".osr"))
                .count());
        }

        // Return the stats object as JSON
        ctx.json(stats);
    }
    
}
