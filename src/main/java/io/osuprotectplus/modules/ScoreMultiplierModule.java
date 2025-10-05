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

    // Configuration constants
    private static final double SCORE_TOLERANCE = 1.05; // 5% tolerance for calculation errors
    private static final int MIN_HITS_THRESHOLD = 10; // Minimum hits required for analysis

    public ScoreMultiplierModule(OsrReplay replay) {
        super(replay);
    }

    @Override
    public boolean run() {
        long startTime = System.currentTimeMillis();
        
        try {
            // Early validation
            if (!hasValidHitData()) {
                logger.debug("Invalid hit data, skipping analysis");
                return false;
            }

            // Theoretical maximum validation for all game modes
            boolean isCheat = validateTheoreticalMaximum();

            long analysisTime = System.currentTimeMillis() - startTime;
            if (isCheat) {
                logger.info("Detected impossible score: {} (analysis took {} ms)", 
                           replay.getTotalScore(), analysisTime);
            } else {
                logger.debug("Score validation passed: {} (analysis took {} ms)", 
                            replay.getTotalScore(), analysisTime);
            }

            return isCheat;

        } catch (Exception e) {
            logger.error("Error during score analysis", e);
            return false;
        }
    }

    /**
     * Validates that the replay has sufficient hit data for analysis
     */
    private boolean hasValidHitData() {
        int totalHits = getTotalHits();
        
        // Check for minimum hits required
        if (totalHits < MIN_HITS_THRESHOLD) {
            logger.debug("Insufficient hits for analysis: {}", totalHits);
            return false;
        }

        // Check for valid score
        if (replay.getTotalScore() <= 0) {
            logger.debug("Invalid total score: {}", replay.getTotalScore());
            return false;
        }

        return true;
    }

    /**
     * Validates against theoretical maximum possible score for all game modes
     */
    private boolean validateTheoreticalMaximum() {
        double modMultiplier = OsuUtils.getOfficialScoreMultiplier(replay.getMods());
        
        // Handle unranked mods (score should be 0 or very low)
        if (modMultiplier == 0.0) {
            if (replay.getTotalScore() > 1000) { // Allow small scores for edge cases
                logger.warn("Score {} with unranked mods (multiplier: {})", 
                           replay.getTotalScore(), modMultiplier);
                return true;
            }
            return false;
        }

        // Calculate theoretical maximum based on game mode
        double maxPossibleScore = calculateMaxPossibleScore(modMultiplier);
        double maxValidScore = maxPossibleScore * SCORE_TOLERANCE;
        
        logger.debug("Theoretical validation - Actual: {}, Max possible: {}, Max valid: {}, Mode: {}", 
                    replay.getTotalScore(), (long)maxPossibleScore, (long)maxValidScore, replay.getGameMode());

        return replay.getTotalScore() > maxValidScore;
    }

    /**
     * Calculates the maximum possible score based on game mode
     */
    private double calculateMaxPossibleScore(double modMultiplier) {
        switch (replay.getGameMode()) {
            case 0: // osu!standard
                return calculateStandardMaxScore(modMultiplier);
            case 1: // osu!taiko
                return calculateTaikoMaxScore(modMultiplier);
            case 2: // osu!catch
                return calculateCatchMaxScore(modMultiplier);
            case 3: // osu!mania
                return calculateManiaMaxScore(modMultiplier);
            default:
                logger.warn("Unknown game mode: {}, using standard calculation", replay.getGameMode());
                return calculateStandardMaxScore(modMultiplier);
        }
    }

    /**
     * Calculates maximum possible score for osu!standard
     */
    private double calculateStandardMaxScore(double modMultiplier) {
        double baseScore = calculateBaseScore();
        return baseScore * Math.max(1, replay.getMaxCombo()) * modMultiplier;
    }

    /**
     * Calculates maximum possible score for osu!taiko
     */
    private double calculateTaikoMaxScore(double modMultiplier) {
        // Taiko scoring: 300 for GREAT, 150 for GOOD
        double baseScore = replay.getCount300() * 300 + replay.getCount100() * 150;
        return baseScore * Math.max(1, replay.getMaxCombo()) * modMultiplier;
    }

    /**
     * Calculates maximum possible score for osu!catch
     */
    private double calculateCatchMaxScore(double modMultiplier) {
        // Catch scoring similar to standard
        double baseScore = calculateBaseScore();
        return baseScore * Math.max(1, replay.getMaxCombo()) * modMultiplier;
    }

    /**
     * Calculates maximum possible score for osu!mania
     */
    private double calculateManiaMaxScore(double modMultiplier) {
        // Mania scoring includes different hit values and special notes
        double baseScore = replay.getCount300() * 300 + 
                          replay.getCount100() * 200 + 
                          replay.getCount50() * 50 +
                          replay.getCountGeki() * 300 +  // MAX/Rainbow 300
                          replay.getCountKatu() * 200;   // 200 or good hit
        
        // Mania doesn't use combo in the same way
        return baseScore * modMultiplier;
    }

    /**
     * Calculates base score from hit counts (for standard/catch modes)
     */
    private double calculateBaseScore() {
        return replay.getCount50() * 50 + 
               replay.getCount100() * 100 + 
               replay.getCount300() * 300;
    }

    /**
     * Gets total hit count
     */
    private int getTotalHits() {
        return replay.getCount300() + replay.getCount100() + 
               replay.getCount50() + replay.getCountMiss();
    }

    @Override
    public boolean canRun() {
        // Can run on all game modes with some basic validation
        return replay != null && 
               replay.getTotalScore() >= 0 && 
               getTotalHits() > 0;
    }

    @Override
    public Flags getDetection() {
        return Flags.TOO_HIGH_SCORE;
    }
}
