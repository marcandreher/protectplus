package io.osuprotectplus.detection;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.osuprotectplus.ProtectPlus;
import io.osuprotectplus.replay.OsrReplay;

public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) throws IOException {
        logger.info("Persisting detection data...");

        // foreach file in storage/

        File storageFolder = new File("storage"); // directory path
        File[] files = storageFolder.listFiles();

        for (File file : files) {
            File[] osrFiles = file.listFiles((dir, name) -> name.endsWith(".osr"));
            if (osrFiles != null) {
                for (File replayFile : osrFiles) {
                    OsrReplay replay = ProtectPlus.parse(replayFile);

                    ProtectPlusDetection detection = new ProtectPlusDetection(replay);
                    detection.analyze();

                }
            } else {
                logger.warn("No .osr files found in the storage directory.");
            }
        }

    }
}
