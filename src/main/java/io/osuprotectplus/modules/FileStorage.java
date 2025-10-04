package io.osuprotectplus.modules;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.osuprotectplus.ProtectPlus;
import io.osuprotectplus.detection.Flag;
import io.osuprotectplus.replay.OsrReplay;

public class FileStorage {
    private static Logger logger = LoggerFactory.getLogger(FileStorage.class);

    static {
        File storageDir = new File("storage");
        if (!storageDir.exists()) {
            storageDir.mkdir();
        }

        File detectionDir = new File("detection");
        if (!detectionDir.exists()) {
            detectionDir.mkdir();
        }
    }

    public static void storeDetection(String replayMD5, ArrayList<Flag> flags) {
        File detectionFile = new File("detection/" + replayMD5 + ".json");
        if(detectionFile.exists()) {
            return;
        }

        if(flags.isEmpty()) {
            return;
        }

        try {
            String json = new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(flags);
            java.nio.file.Files.write(detectionFile.toPath(), json.getBytes());
            logger.debug("Stored detection: {}", detectionFile.getCanonicalPath());
        } catch (Exception e) {
            logger.error("Failed to store detection: {}", e.getMessage());
        }
    }

    public static void storeReplay(OsrReplay replay) {
        File beatmapFolder = new File("storage/" + replay.getBeatmapMD5());
        if (!beatmapFolder.exists()) {
            beatmapFolder.mkdir();
        }

        File replayFile = new File(beatmapFolder, replay.getReplayMD5() + ".osr");
        if (replayFile.exists()) {
            return;
        }

        try {
            java.nio.file.Files.write(replayFile.toPath(), replay.getFile());
            logger.debug("Stored replay: {}", replayFile.getCanonicalPath());
        } catch (Exception e) {
            logger.error("Failed to store replay: {}", e.getMessage());
        }
    }

    public static List<OsrReplay> loadReplaysForBeatmap(OsrReplay orig, String beatmapMD5) {
        List<OsrReplay> replays = new ArrayList<>();
        File beatmapFolder = new File("storage/" + beatmapMD5);
        if (!beatmapFolder.exists() || !beatmapFolder.isDirectory()) {
            return replays;
        }

        File[] files = beatmapFolder.listFiles((dir, name) -> name.endsWith(".osr"));
        if (files == null) {
            return replays;
        }

        for (File file : files) {
            try {
                OsrReplay replay = ProtectPlus.parse(file);
                if(replay.getReplayMD5().equals(orig.getReplayMD5())) continue;
                replays.add(replay);
            } catch (Exception e) {
                logger.error("Failed to load replay from file {}: {}", file.getName(), e.getMessage());
            }
        }

        return replays;
    }


}
