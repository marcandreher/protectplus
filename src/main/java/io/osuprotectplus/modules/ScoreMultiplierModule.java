package io.osuprotectplus.modules;

import io.osuprotectplus.detection.Flags;
import io.osuprotectplus.modules.base.AcModule;
import io.osuprotectplus.replay.OsrReplay;
import io.osuprotectplus.replay.OsuUtils;

/**
 * Score multiplier cheat detection module.
 * 
 * This module detects impossibly high scores by calculating the theoretical
 * maximum possible score and comparing it against the actual score.
 * 
 * Detects:
 * - Score multiplier hacks
 * - Modified hit values
 * - Score injection
 */
public class ScoreMultiplierModule extends AcModule {

    public ScoreMultiplierModule(OsrReplay replay) {
        super(replay);
    }

    @Override
    public boolean run() {
         // Calculate expected score (approximate, osu!standard formula)
        if(replay.getTotalScore() < 0) {
            logger.warn("Replay {} has invalid total score: {}", replay.getReplayMD5(), replay.getTotalScore());
            return true;
        }

        long startTime = System.currentTimeMillis();
        int totalHits = replay.getCount300() + replay.getCount100() + replay.getCount50() + replay.getCountMiss();
        if (totalHits == 0) return false;
        double baseScore = replay.getCount50() * 50 + replay.getCount100() * 100 + replay.getCount300() * 300;
        double multiplier = OsuUtils.getOfficialScoreMultiplier(replay.getMods());
        // Estimate max possible score (very rough, real formula is more complex)
        double maxScore = baseScore * replay.getMaxCombo() * multiplier;
        // Allow some tolerance for calculation error
        if(maxScore == 0) return false;
        logger.debug("Score check {} > {} in {} ms", replay.getTotalScore(), (int)maxScore, System.currentTimeMillis() - startTime);
        if (replay.getTotalScore() > maxScore * 1.05) {
            return true;
        }
        return false;
    }

    @Override
    public boolean canRun() {
        if(replay.getGameMode() == 0) {
            return true;
        }

        return false;
    }




    @Override
    public Flags getDetection() {
        return Flags.TOO_HIGH_SCORE;
    }
}
