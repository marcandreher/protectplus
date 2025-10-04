package io.osuprotectplus.handlers;

import java.io.File;

import com.google.gson.Gson;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiParam;
import io.osuprotectplus.detection.Flag;
import io.osuprotectplus.models.Beatmap;

public class BeatmapHandler implements Handler {

    @Override
    @OpenApi(summary = "Get beatmap information", description = "Retrieve flagged replays for a specific beatmap.", path = "/api/v1/beatmap", methods = HttpMethod.GET, tags = {
            "Beatmaps" }, queryParams = {
                    @OpenApiParam(name = "hash", description = "MD5 hash of the beatmap file", required = true, example = "a1b2c3d4e5f6789012345678901234ab")
            }, responses = {
                    @io.javalin.openapi.OpenApiResponse(status = "200", content = @OpenApiContent(from = Beatmap.class), description = "Successfully retrieved beatmap information"),
                    @io.javalin.openapi.OpenApiResponse(status = "404", description = "Beatmap not found"),
                    @io.javalin.openapi.OpenApiResponse(status = "500", description = "Internal server error")
            })
    public void handle(Context ctx) throws Exception {
        // Accept beatmap hash as query parameter
        String hash = ctx.queryParam("hash");

        if (hash == null || hash.isEmpty()) {
            ctx.status(400).json(java.util.Map.of(
                    "error", "Missing required parameter",
                    "message", "Beatmap hash is required as query parameter"));
            return;
        }

        // Search through all storage directories to find replays for this beatmap
        File storageDir = new File("storage");
        if (!storageDir.exists() || !storageDir.isDirectory()) {
            ctx.status(500).json(java.util.Map.of(
                    "error", "Storage not found",
                    "message", "Storage directory does not exist"));
            return;
        }

        File beatmapDir = new File(storageDir, hash);
        if (!beatmapDir.exists() || !beatmapDir.isDirectory()) {
            ctx.status(404).json(java.util.Map.of(
                    "error", "Beatmap not found",
                    "message", "No replays found for the specified beatmap hash"));
            return;
        }

        Beatmap beatmap = new Beatmap();
        beatmap.setHash(hash);

        Gson gson = new Gson();
        long replayCount = 0;

        // Look for .osr files in each user directory
        for (File replayFile : beatmapDir.listFiles()) {
            if (!replayFile.isFile() || !replayFile.getName().endsWith(".osr"))
                continue;

            replayCount++;
            String replayHash = replayFile.getName().substring(0, replayFile.getName().length() - 4);

            File detectionFile = new File("detection/" + replayHash + ".json");
            if (detectionFile.exists() && detectionFile.isFile()) {
                try {
                    // Detection files contain arrays of flags, not single flag objects
                    Flag[] flags = gson.fromJson(new String(java.nio.file.Files.readAllBytes(detectionFile.toPath())),
                            Flag[].class);
                    if (flags != null && flags.length > 0) {
                        if (beatmap.getFlaggedReplays() == null) {
                            beatmap.setFlaggedReplays(new java.util.HashMap<>());
                        }
                        beatmap.getFlaggedReplays().put(replayHash, flags);
                    }
                } catch (Exception e) {
                    // Log the error for debugging
                    System.err
                            .println("Error parsing detection file " + detectionFile.getName() + ": " + e.getMessage());
                }
            }
        }

        beatmap.setReplayCount(replayCount);
        ctx.json(beatmap);
    }

}
