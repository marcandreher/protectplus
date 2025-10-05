package io.osuprotectplus.modules;

import io.osuprotectplus.detection.Flags;
import io.osuprotectplus.modules.base.AcModule;
import io.osuprotectplus.replay.OsrReplay;
import io.osuprotectplus.replay.ReplayFrame;

/**
 * Full autoplay detection module.
 * 
 * Detects automated replays by analyzing:
 * - Extremely high accuracy (>99.9%)
 * - No misses
 * - Robotic timing patterns
 * - Superhuman cursor movements
 */
public class FullAutoplayModule extends AcModule {

    // Configuration constants
    private static final double MIN_ACCURACY = 0.999; // 99.9% accuracy required
    private static final double MAX_IDENTICAL_RATIO = 0.8; // Max 80% identical delays
    private static final double MAX_TIMING_STDDEV = 5.0; // Very low timing variation
    private static final double SUPERHUMAN_DISTANCE = 450.0; // Impossible cursor jump
    private static final int MIN_FRAMES = 100; // Minimum frames for reliable analysis

    public FullAutoplayModule(OsrReplay replay) {
        super(replay);
    }

    @Override
    public boolean run() {
        long startTime = System.currentTimeMillis();
        
        // Early validation
        if (!hasValidData()) {
            return false;
        }

        // Multiple criteria must be met for autoplay detection
        boolean highAccuracy = checkHighAccuracy();
        boolean roboticTiming = checkRoboticTiming();
        boolean superhumanMovement = checkSuperhumanMovement();
        
        // Require high accuracy AND (robotic timing OR superhuman movement)
        boolean isAutoplay = highAccuracy && (roboticTiming || superhumanMovement);
        
        long analysisTime = System.currentTimeMillis() - startTime;
        
        if (isAutoplay) {
            logger.info("Autoplay detected: acc={}%, timing={}, movement={} in {}ms", 
                       Math.round(calculateAccuracy() * 100), roboticTiming, superhumanMovement, analysisTime);
        } else {
            logger.debug("No autoplay detected: acc={}%, timing={}, movement={} in {}ms", 
                        Math.round(calculateAccuracy() * 100), roboticTiming, superhumanMovement, analysisTime);
        }

        return isAutoplay;
    }

    /**
     * Validates that replay has sufficient data for analysis
     */
    private boolean hasValidData() {
        int totalHits = getTotalHits();
        
        if (totalHits == 0) {
            logger.debug("No hits found for autoplay analysis");
            return false;
        }
        
        if (replay.getFrames() == null || replay.getFrames().size() < MIN_FRAMES) {
            logger.debug("Insufficient frames for autoplay analysis: {}", 
                        replay.getFrames() != null ? replay.getFrames().size() : 0);
            return false;
        }
        
        return true;
    }

    /**
     * Checks for extremely high accuracy (typical of autoplay)
     */
    private boolean checkHighAccuracy() {
        // Must have no misses
        if (replay.getCountMiss() > 0) {
            return false;
        }
        
        // Must have extremely high accuracy
        double accuracy = calculateAccuracy();
        return accuracy >= MIN_ACCURACY;
    }

    /**
     * Checks for robotic timing patterns
     */
    private boolean checkRoboticTiming() {
        if (replay.getFrames().size() < 3) return false;
        
        long prevDelta = replay.getFrames().get(1).getTimeDelta();
        int identicalCount = 0;
        int totalFrames = 0;
        double sum = 0, sumSquares = 0;
        
        for (int i = 2; i < replay.getFrames().size(); i++) {
            long delta = replay.getFrames().get(i).getTimeDelta();
            
            // Count nearly identical deltas (within 1ms)
            if (Math.abs(delta - prevDelta) <= 1) {
                identicalCount++;
            }
            
            // Calculate statistics
            sum += delta;
            sumSquares += delta * delta;
            totalFrames++;
            prevDelta = delta;
        }
        
        if (totalFrames == 0) return false;
        
        // Calculate timing consistency metrics
        double identicalRatio = (double) identicalCount / totalFrames;
        double mean = sum / totalFrames;
        double variance = (sumSquares / totalFrames) - (mean * mean);
        double stddev = Math.sqrt(Math.max(0, variance));
        
        // Robotic if too many identical delays AND very low standard deviation
        return identicalRatio > MAX_IDENTICAL_RATIO && stddev < MAX_TIMING_STDDEV;
    }

    /**
     * Checks for superhuman cursor movements
     */
    private boolean checkSuperhumanMovement() {
        int superhumanCount = 0;

        for (int i = 1; i < replay.getFrames().size(); i++) {
            ReplayFrame prev = replay.getFrames().get(i - 1);
            ReplayFrame curr = replay.getFrames().get(i);
            
            double dx = curr.getX() - prev.getX();
            double dy = curr.getY() - prev.getY();
            double distance = Math.sqrt(dx * dx + dy * dy);

            // Check for impossible cursor teleportation
            if (distance > SUPERHUMAN_DISTANCE) {
                superhumanCount++;
            }
        }
        
        // Require multiple superhuman movements (not just one glitch)
        return superhumanCount >= 3;
    }

    /**
     * Calculates accuracy percentage
     */
    private double calculateAccuracy() {
        int totalHits = getTotalHits();
        if (totalHits == 0) return 0.0;
        
        double hitValue = replay.getCount50() * 50 + 
                         replay.getCount100() * 100 + 
                         replay.getCount300() * 300;
        
        return hitValue / (totalHits * 300.0);
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
        if(replay.getGameMode() == 0 ) {
            return true;
        }
        return false;
    }

    @Override
    public Flags getDetection() {
        return Flags.AUTOPLAY;
    }
    
}
