package io.osuprotectplus.handlers;

import java.util.Map;

import org.jetbrains.annotations.NotNull;

import io.javalin.http.Context;
import io.javalin.http.Handler;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiContentProperty;
import io.javalin.openapi.OpenApiRequestBody;
import io.javalin.openapi.OpenApiResponse;
import io.osuprotectplus.detection.Flag;

public class AnalyzeHandler implements Handler {

    @Override
    @OpenApi(
        summary = "Analyze osu! replay file",
        description = "Upload an osu! replay (.osr) file and get back a list of detected cheats/flags",
        path = "/api/v1/analyze",
        methods = HttpMethod.POST,
        tags = {"Replay Analysis"},
        requestBody = @OpenApiRequestBody(
            content = {
                @OpenApiContent(
                    type = "multipart/form-data",
                    properties = {
                       @OpenApiContentProperty(
                            name = "file",
                            type = "string",
                            format = "binary"
                       )
                    }
                )
            },
            description = "Upload .osr file",
            required = true
        ),
        responses = {
            @OpenApiResponse(
                status = "200",
                content = {@OpenApiContent(from = Flag[].class)},
                description = "Successfully analyzed replay - returns list of detected flags/cheats"
            ),
            @OpenApiResponse(
                status = "400",
                description = "Bad request - no file uploaded, invalid file format, or parsing error"
            )
        }
    )
    public void handle(@NotNull Context ctx) throws Exception {
        // Validate file upload
        var uploadedFile = ctx.uploadedFile("file");
        if (uploadedFile == null) {
            ctx.status(400).json(Map.of(
                "error", "No file uploaded",
                "message", "Please upload an osu! replay file (.osr) using multipart/form-data with field name 'file'"
            ));
            return;
        }

        // Validate file extension
        if (!uploadedFile.filename().toLowerCase().endsWith(".osr")) {
            ctx.status(400).json(Map.of(
                "error", "Invalid file format",
                "message", "Only .osr (osu! replay) files are supported"
            ));
            return;
        }

        // Create temporary file for processing
        java.io.File tempFile = java.io.File.createTempFile("replay_", ".osr");
        try (var fileOut = new java.io.FileOutputStream(tempFile);
             var uploadIn = uploadedFile.content()) {
            uploadIn.transferTo(fileOut);
        }

        try {
            // Parse and analyze the replay
            var replay = io.osuprotectplus.ProtectPlus.parse(tempFile);
            var detection = new io.osuprotectplus.detection.ProtectPlusDetection(replay);
            var flags = detection.analyze();

            // Return successful response
            ctx.json(flags);
        } catch (Exception e) {
            ctx.status(400).json(Map.of(
                "error", "Failed to parse replay",
                "message", e.getMessage()
            ));
        } finally {
            // Clean up temporary file
            tempFile.delete();
        }
    }
    
}
