package io.osuprotectplus;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.osuprotectplus.modules.FileStorage;
import io.osuprotectplus.replay.OsrReplay;
import io.osuprotectplus.replay.OsrReplayParser;
import io.osuprotectplus.replay.OsuUtils;
import io.osuprotectplus.replay.ReplayFrame;

public class ProtectPlus {

    private static Logger logger = LoggerFactory.getLogger(ProtectPlus.class);

    public static OsrReplay parse(File file) throws IOException {
        long startTime = System.currentTimeMillis();
        OsrReplay replay = new OsrReplay();
        // Read all bytes once and set in replay
        byte[] allBytes;
        try (FileInputStream fis = new FileInputStream(file)) {
            allBytes = fis.readAllBytes();
        }
        replay.setFile(allBytes);
        // Now parse fields from the byte array
        try (DataInputStream in = new DataInputStream(new java.io.ByteArrayInputStream(allBytes))) {
            replay.setFileName(file.getName());
            replay.setGameMode(in.readUnsignedByte());
            replay.setVersion(Integer.reverseBytes(in.readInt()));
            replay.setBeatmapMD5(OsrReplayParser.readOsuString(in));
            replay.setPlayerName(OsrReplayParser.readOsuString(in));
            replay.setReplayMD5(OsrReplayParser.readOsuString(in));
            replay.setCount300(Short.reverseBytes(in.readShort()));
            replay.setCount100(Short.reverseBytes(in.readShort()));
            replay.setCount50(Short.reverseBytes(in.readShort()));
            replay.setCountGeki(Short.reverseBytes(in.readShort()));
            replay.setCountKatu(Short.reverseBytes(in.readShort()));
            replay.setCountMiss(Short.reverseBytes(in.readShort()));
            replay.setTotalScore(Integer.reverseBytes(in.readInt()));
            replay.setMaxCombo(Short.reverseBytes(in.readShort()));
            replay.setPerfect(in.readByte());
            replay.setMods(Integer.reverseBytes(in.readInt()));
            replay.setLifeBarGraph(OsrReplayParser.readOsuString(in));
            replay.setTimestamp(Long.reverseBytes(in.readLong()));
            replay.setCompressedReplayLength(Integer.reverseBytes(in.readInt()));
            replay.setCompressedReplayData(new byte[replay.getCompressedReplayLength()]);
            in.readFully(replay.getCompressedReplayData());
            replay.setOnlineScoreID(Long.reverseBytes(in.readLong()));
            // decompress and parse frames
            OsrReplayParser.parseReplayFrames(replay);
        }
        FileStorage.storeReplay(replay);
        // Only log if debug enabled
        if (logger.isDebugEnabled()) {
            logger.debug("Parsed replay: {} in {} ms", replay.getFileName(), System.currentTimeMillis() - startTime);
        }
        return replay;
    }

    /**
     * Computes a similarity score between two replays based on cursor positions.
     * Optimized: uses squared distance for speed, only sqrt at end.
     * @param a First replay
     * @param b Second replay
     * @return similarity score in [0, 1]
     */
    public static double computeCursorSimilarity(OsrReplay a, OsrReplay b) {
        int n = Math.min(a.getFrames().size(), b.getFrames().size());
        if (n == 0) return 0.0;
        double sumSqDist = 0.0;
        for (int i = 0; i < n; i++) {
            ReplayFrame fa = a.getFrames().get(i);
            ReplayFrame fb = b.getFrames().get(i);
            double dx = fa.getX() - fb.getX();
            double dy = fa.getY() - fb.getY();
            sumSqDist += dx * dx + dy * dy;
        }
        double avgSqDist = sumSqDist / n;
        // Maximum possible squared distance in osu! is (512^2 + 384^2)
        double maxSqDist = 512 * 512 + 384 * 384;
        double similarity = 1.0 - (Math.sqrt(avgSqDist) / Math.sqrt(maxSqDist));
        if (similarity < 0) similarity = 0;
        return similarity;
    }

    /**
     * Checks if a replay's score is suspiciously high (possible score multiplier hack) using OsrReplay.
     * This is a simple check for osu!standard mode.
     * @param replay OsrReplay object
     * @return true if score is suspiciously high
     */
    public static boolean isScoreSuspiciouslyHigh(OsrReplay replay) {
        // Calculate expected score (approximate, osu!standard formula)
        long startTime = System.currentTimeMillis();
        int totalHits = replay.getCount300() + replay.getCount100() + replay.getCount50() + replay.getCountMiss();
        if (totalHits == 0) return false;
        double baseScore = replay.getCount50() * 50 + replay.getCount100() * 100 + replay.getCount300() * 300;
        double multiplier = OsuUtils.getOfficialScoreMultiplier(replay.getMods());
        // Estimate max possible score (very rough, real formula is more complex)
        double maxScore = baseScore * replay.getMaxCombo() * multiplier;
        // Allow some tolerance for calculation error
        logger.debug("Score check {} > {} in {} ms", replay.getTotalScore(), (int)maxScore, System.currentTimeMillis() - startTime);
        if (replay.getTotalScore() > maxScore * 1.05) {
            return true;
        }
        return false;
    }

    /**
     * Detects spinbot (inhuman spinner speeds) in a replay.
     * Returns true if any spinner is completed at a speed above the threshold (e.g., >700 SPM).
     * Ignores spinner segments shorter than 300 ms and segments with non-positive duration.
     * Uses accumulated time for robust detection.
     * @param replay OsrReplay object
     * @return true if spinbot detected
     */
    public static boolean isSpinbot(OsrReplay replay) {
        final long startTime = System.currentTimeMillis();
        final double SPINBOT_THRESHOLD_SPM = 500.0;
        final long MIN_SPINNER_DURATION_MS = 300; // Ignore segments shorter than 300 ms
        if (replay.getFrames() == null || replay.getFrames().isEmpty()) return false;
        boolean inSpinner = false;
        double lastAngle = 0;
        double totalAngle = 0;
        long spinnerStartTime = 0;
        long spinnerEndTime = 0;
        long absTime = 0;
        for (int i = 1; i < replay.getFrames().size(); i++) {
            ReplayFrame curr = replay.getFrames().get(i);
            absTime += curr.getTimeDelta();
            double cx = curr.getX() - 256.0;
            double cy = curr.getY() - 192.0;
            double r = Math.sqrt(cx * cx + cy * cy);
            if (r > 40) { // Lowered threshold for spinner detection
                double angle = Math.atan2(cy, cx);
                if (!inSpinner) {
                    inSpinner = true;
                    spinnerStartTime = absTime;
                    totalAngle = 0;
                    lastAngle = angle;
                } else {
                    double delta = angle - lastAngle;
                    while (delta > Math.PI) delta -= 2 * Math.PI;
                    while (delta < -Math.PI) delta += 2 * Math.PI;
                    totalAngle += Math.abs(delta);
                    lastAngle = angle;
                    spinnerEndTime = absTime;
                }
            } else if (inSpinner) {
                long duration = spinnerEndTime - spinnerStartTime;
                if (duration > 0 && duration >= MIN_SPINNER_DURATION_MS) {
                    double spins = totalAngle / (2 * Math.PI);
                    double minutes = duration / 60000.0;
                    double spm = spins / minutes;
                    if (spm > SPINBOT_THRESHOLD_SPM) {
                        logger.debug("Spinbot detected: {} spins in {} ms ({} SPM) [Detection time: {} ms]", 
                                String.format("%.2f", spins), duration, String.format("%.2f", spm), System.currentTimeMillis() - startTime);
                        return true;
                    }
                }
                inSpinner = false;
            }
        }
        logger.debug("No spinbot detected in {} ms", System.currentTimeMillis() - startTime);
        return false;
    }

    /**
     * Detects full autoplay/auto replays: very high accuracy, no misses, low delay variation, and optionally superhuman flicks.
     * More robust for imperfect cheat autos.
     * @param replay OsrReplay object
     * @return true if likely full autoplay detected
     */
    public static boolean isFullAutoplay(OsrReplay replay) {
        // 1. Very high accuracy, no misses
        long startTime = System.currentTimeMillis();
        int totalHits = replay.getCount300() + replay.getCount100() + replay.getCount50() + replay.getCountMiss();
        if (totalHits == 0) return false;
        double acc = (replay.getCount50() * 50 + replay.getCount100() * 100 + replay.getCount300() * 300) / (totalHits * 300.0);
        if (replay.getCountMiss() > 0) return false;
        if (acc < 0.998) return false; // Require >99.8% accuracy
        // 2. All timeDeltas nearly identical (no human delay variation)
        if (replay.getFrames() == null || replay.getFrames().size() < 2) return false;
        long prevDelta = replay.getFrames().get(1).getTimeDelta();
        int identical = 0;
        int total = 0;
        double sum = 0, sumSq = 0;
        for (int i = 2; i < replay.getFrames().size(); i++) {
            long delta = replay.getFrames().get(i).getTimeDelta();
            if (Math.abs(delta - prevDelta) <= 1) identical++;
            sum += delta;
            sumSq += delta * delta;
            prevDelta = delta;
            total++;
        }
        double mean = sum / total;
        double stddev = Math.sqrt((sumSq / total) - (mean * mean));
        
        // More lenient: require 15% identical delays and stddev < 12
        if (total > 0 && ((double)identical / total) > 0.15 && stddev < 12.0) {
            // 3. Optionally: check for superhuman flicks (large cursor jumps)
            boolean superhumanFlick = false;
            for (int i = 1; i < replay.getFrames().size(); i++) {
                ReplayFrame a = replay.getFrames().get(i-1);
                ReplayFrame b = replay.getFrames().get(i);
                double dx = a.getX() - b.getX();
                double dy = a.getY() - b.getY();
                double dist = Math.sqrt(dx*dx + dy*dy);
                if (dist > 400) { // osu! playfield is 512x384, so >400 is a huge jump
                    superhumanFlick = true;
                    break;
                }
            }
            logger.debug("Detected likely full autoplay in {} ms (identical delays: {}/{}, stddev: {})", 
                    System.currentTimeMillis() - startTime, String.format("%.5f", acc), identical, total, String.format("%.2f", stddev), superhumanFlick);
            return superhumanFlick || true; // If all other conditions met, likely auto
        }

        logger.debug("No full autoplay detected in {} ms", System.currentTimeMillis() - startTime);
        return false;
    }

    /**
     * Detects timewarp/speedhack: improved detection for osu! replays.
     * Checks for unnaturally consistent frame rates, impossibly fast cursor movements, and perfect timing.
     * @param replay OsrReplay object
     * @return true if likely timewarp/speedhack detected
     */
    public static boolean isTimewarpOrSpeedhack(OsrReplay replay) {
        long startTime = System.currentTimeMillis();
        if (replay.getFrames() == null || replay.getFrames().size() < 10) return false;
        
        // 1. Check for unnaturally consistent frame rates (e.g., >95% at exactly same interval)
        java.util.Map<Long, Integer> deltaFreq = new java.util.HashMap<>();
        double totalVelocity = 0;
        int velocityCount = 0;
        
        for (int i = 1; i < replay.getFrames().size(); i++) {
            ReplayFrame prev = replay.getFrames().get(i-1);
            ReplayFrame curr = replay.getFrames().get(i);
            long delta = curr.getTimeDelta();
            
            // Count frame rate consistency
            deltaFreq.put(delta, deltaFreq.getOrDefault(delta, 0) + 1);
            
            // 2. Check for impossibly fast cursor movements
            if (delta > 0) {
                double dx = curr.getX() - prev.getX();
                double dy = curr.getY() - prev.getY();
                double distance = Math.sqrt(dx * dx + dy * dy);
                double velocity = distance / delta; // pixels per ms
                
                if (velocity > 3.0) { // >3 pixels/ms is superhuman (>10800 pixels/second)
                    totalVelocity += velocity;
                    velocityCount++;
                }
            }
        }
        
        // Find most common delta and check consistency
        long mostCommonDelta = deltaFreq.entrySet().stream()
            .max(java.util.Map.Entry.comparingByValue())
            .map(java.util.Map.Entry::getKey)
            .orElse(0L);
        
        int mostCommonCount = deltaFreq.getOrDefault(mostCommonDelta, 0);
        double consistency = (double) mostCommonCount / replay.getFrames().size();
        
        // 3. Check average superhuman velocity
        double avgSuperhumanVelocity = velocityCount > 0 ? totalVelocity / velocityCount : 0;

        logger.debug("Timewarp analysis - Frame consistency: {}, Superhuman movements: {}, Avg velocity: {} px/ms in {} ms",
                consistency * 100, velocityCount, avgSuperhumanVelocity, System.currentTimeMillis() - startTime);

        // Flag if: >95% consistent frame rate OR many superhuman cursor movements
        return (consistency > 0.95 && mostCommonDelta < 20) || 
               (velocityCount > replay.getFrames().size() * 0.1 && avgSuperhumanVelocity > 5.0);
    }
}
