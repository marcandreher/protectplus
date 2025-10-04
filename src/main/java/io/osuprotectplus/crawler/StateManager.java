package io.osuprotectplus.crawler;

import java.io.File;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * Manages the crawler state (current date being processed)
 */
public class StateManager {
    private static final Logger logger = LoggerFactory.getLogger(StateManager.class);
    private static final Gson gson = new Gson();
    
    private final File file;

    public StateManager(File file) {
        this.file = file;
    }

    public String loadSince(String defaultValue) {
        if (!file.exists()) {
            saveSince(defaultValue);
            return defaultValue;
        }
        try {
            String json = Files.readString(file.toPath());
            SinceData data = gson.fromJson(json, SinceData.class);
            return (data != null && data.since != null && !data.since.isEmpty()) ? data.since : defaultValue;
        } catch (Exception e) {
            logger.warn("Failed to read state, using default: {}", e.getMessage());
            return defaultValue;
        }
    }

    public void saveSince(String since) {
        try {
            Files.writeString(file.toPath(), gson.toJson(new SinceData(since)));
            logger.debug("Saved state: {}", since);
        } catch (Exception e) {
            logger.error("Failed to save state: {}", e.getMessage());
        }
    }

    private static class SinceData {
        String since;

        SinceData(String since) {
            this.since = since;
        }
    }
}