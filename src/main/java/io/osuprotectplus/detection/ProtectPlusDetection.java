package io.osuprotectplus.detection;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.osuprotectplus.ProtectPlus;
import io.osuprotectplus.modules.FileStorage;
import io.osuprotectplus.replay.OsrReplay;

public class ProtectPlusDetection {
    private static Logger logger = LoggerFactory.getLogger(ProtectPlusDetection.class);
    private OsrReplay replay;
    public static final double SPINBOT_THRESHOLD_SPM = 700.0; // spins per minute

    public ProtectPlusDetection(OsrReplay replay) {
        this.replay = replay;
    }

    public List<Flag> analyze() {
        long startTime = System.currentTimeMillis();
        ArrayList<Flag> flags = new ArrayList<>();
        int gameMode = replay.getGameMode();

        if (ProtectPlus.isScoreSuspiciouslyHigh(replay)) {
            Flag flag = new Flag();
            flag.setFlag(Flags.TOO_HIGH_SCORE);
            flag.setReason("Score is suspiciously high (Score Multiplier)");
            flags.add(flag);
        }

        if (gameMode == 0 || gameMode == 2) {
            if (ProtectPlus.isSpinbot(replay)) {
                Flag flag = new Flag();
                flag.setFlag(Flags.SPINBOT_DETECTED);
                flag.setReason("Spinbot detected (Spins per minute > " + SPINBOT_THRESHOLD_SPM + ")");
                flags.add(flag);
            }
        }

        if (gameMode == 0)
            if (ProtectPlus.isFullAutoplay(replay)) {
                Flag flag = new Flag();
                flag.setFlag(Flags.AUTOPLAY);
                flag.setReason("Full Autoplay detected (Many identical delays 'i')");
                flags.add(flag);
            }

        if (gameMode == 0)

        {
            if (ProtectPlus.isTimewarpOrSpeedhack(replay)) {
                Flag flag = new Flag();
                flag.setFlag(Flags.TIMEWARP_OR_SPEEDHACK);
                flag.setReason("Timewarp or Speedhack detected (Unnatural time intervals between actions)");
                flags.add(flag);
            }
        }

        if (gameMode == 0 || gameMode == 2) {
            for (OsrReplay replay : FileStorage.loadReplaysForBeatmap(replay, replay.getBeatmapMD5())) {
                double similarity = ProtectPlus.computeCursorSimilarity(this.replay, replay);
                if (similarity > 0.8) {
                    Flag flag = new Flag();
                    flag.setFlag(Flags.REPLAY_STOLEN);
                    flag.setReason(String.format("Replay is very similar to another replay (%.2f%% similarity) (%s)",
                            similarity * 100, replay.getReplayMD5()));
                    flags.add(flag);
                }
            }
        }

        FileStorage.storeDetection(replay.getReplayMD5(), flags);

        logger.info("Analysis completed in {} ms", System.currentTimeMillis() - startTime);

        // Implement your analysis logic here
        return flags;
    }

}
