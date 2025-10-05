package io.osuprotectplus.replay;

/**
 * Utility class for osu! game calculations and mod handling.
 * Contains official scoring multipliers and game-specific calculations.
 */
public class OsuUtils {

    // Mod bit flags (as per osu! source code)
    public static final int MOD_NONE = 0;
    public static final int MOD_NO_FAIL = 1;
    public static final int MOD_EASY = 2;
    public static final int MOD_TOUCH_DEVICE = 4;
    public static final int MOD_HIDDEN = 8;
    public static final int MOD_HARD_ROCK = 16;
    public static final int MOD_SUDDEN_DEATH = 32;
    public static final int MOD_DOUBLE_TIME = 64;
    public static final int MOD_RELAX = 128;
    public static final int MOD_HALF_TIME = 256;
    public static final int MOD_NIGHTCORE = 512;
    public static final int MOD_FLASHLIGHT = 1024;
    public static final int MOD_AUTOPLAY = 2048;
    public static final int MOD_SPUN_OUT = 4096;
    public static final int MOD_AUTOPILOT = 8192;
    public static final int MOD_PERFECT = 16384;
    public static final int MOD_CINEMA = 4194304;

    /**
     * Returns the official osu! score multiplier for a given mods bitmask.
     * Implements all mods as per osu! wiki (updated 2025).
     * 
     * @param mods mods bitmask
     * @return score multiplier (0.0 for unranked mods)
     */
    public static double getOfficialScoreMultiplier(int mods) {
        // Check for unranked mods first (return 0.0 immediately)
        if (hasUnrankedMods(mods)) {
            return 0.0;
        }

        double multiplier = 1.0;
        
        // Difficulty reduction mods
        if ((mods & MOD_NO_FAIL) != 0) multiplier *= 0.5;
        if ((mods & MOD_EASY) != 0) multiplier *= 0.5;
        if ((mods & MOD_HALF_TIME) != 0) multiplier *= 0.3;
        
        // Difficulty increase mods
        if ((mods & MOD_HIDDEN) != 0) multiplier *= 1.06;
        if ((mods & MOD_HARD_ROCK) != 0) multiplier *= 1.06;
        if ((mods & MOD_DOUBLE_TIME) != 0) multiplier *= 1.12;
        if ((mods & MOD_FLASHLIGHT) != 0) multiplier *= 1.12;
        
        // Special mods with multipliers
        if ((mods & MOD_SPUN_OUT) != 0) multiplier *= 0.9;
        
        // Note: Nightcore is automatically enabled with DoubleTime, no extra effect
        // Note: SuddenDeath and Perfect have no score effect
        // Note: TouchDevice has no score effect

        return multiplier;
    }

    /**
     * Checks if the mod combination contains any unranked mods.
     * Unranked mods make the score unsubmittable/invalid.
     * 
     * @param mods mods bitmask
     * @return true if contains unranked mods
     */
    public static boolean hasUnrankedMods(int mods) {
        return (mods & MOD_RELAX) != 0 ||      // Relax
               (mods & MOD_AUTOPLAY) != 0 ||   // Autoplay
               (mods & MOD_AUTOPILOT) != 0 ||  // Autopilot
               (mods & MOD_CINEMA) != 0;       // Cinema
    }

    /**
     * Gets a human-readable string representation of the enabled mods.
     * 
     * @param mods mods bitmask
     * @return comma-separated mod names
     */
    public static String getModsString(int mods) {
        if (mods == 0) return "None";
        
        StringBuilder sb = new StringBuilder();
        
        if ((mods & MOD_NO_FAIL) != 0) sb.append("NF,");
        if ((mods & MOD_EASY) != 0) sb.append("EZ,");
        if ((mods & MOD_TOUCH_DEVICE) != 0) sb.append("TD,");
        if ((mods & MOD_HIDDEN) != 0) sb.append("HD,");
        if ((mods & MOD_HARD_ROCK) != 0) sb.append("HR,");
        if ((mods & MOD_SUDDEN_DEATH) != 0) sb.append("SD,");
        if ((mods & MOD_DOUBLE_TIME) != 0) sb.append("DT,");
        if ((mods & MOD_RELAX) != 0) sb.append("RX,");
        if ((mods & MOD_HALF_TIME) != 0) sb.append("HT,");
        if ((mods & MOD_NIGHTCORE) != 0) sb.append("NC,");
        if ((mods & MOD_FLASHLIGHT) != 0) sb.append("FL,");
        if ((mods & MOD_AUTOPLAY) != 0) sb.append("AT,");
        if ((mods & MOD_SPUN_OUT) != 0) sb.append("SO,");
        if ((mods & MOD_AUTOPILOT) != 0) sb.append("AP,");
        if ((mods & MOD_PERFECT) != 0) sb.append("PF,");
        if ((mods & MOD_CINEMA) != 0) sb.append("CN,");
        
        // Remove trailing comma
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        
        return sb.toString();
    }

    /**
     * Calculates accuracy percentage for osu!standard mode.
     * 
     * @param count300 number of 300s
     * @param count100 number of 100s
     * @param count50 number of 50s
     * @param countMiss number of misses
     * @return accuracy as decimal (0.0 to 1.0)
     */
    public static double calculateStandardAccuracy(int count300, int count100, int count50, int countMiss) {
        int totalHits = count300 + count100 + count50 + countMiss;
        if (totalHits == 0) return 0.0;
        
        double hitValue = count300 * 300 + count100 * 100 + count50 * 50;
        return hitValue / (totalHits * 300.0);
    }

    /**
     * Calculates accuracy percentage for osu!taiko mode.
     * 
     * @param count300 number of GREATs
     * @param count100 number of GOODs
     * @param countMiss number of misses
     * @return accuracy as decimal (0.0 to 1.0)
     */
    public static double calculateTaikoAccuracy(int count300, int count100, int countMiss) {
        int totalHits = count300 + count100 + countMiss;
        if (totalHits == 0) return 0.0;
        
        double hitValue = count300 * 300 + count100 * 150;
        return hitValue / (totalHits * 300.0);
    }

    /**
     * Calculates accuracy percentage for osu!catch mode.
     * 
     * @param count300 number of fruits caught
     * @param count100 number of drops caught
     * @param count50 number of droplets caught
     * @param countMiss number of fruits/drops missed
     * @return accuracy as decimal (0.0 to 1.0)
     */
    public static double calculateCatchAccuracy(int count300, int count100, int count50, int countMiss) {
        int totalHits = count300 + count100 + count50 + countMiss;
        if (totalHits == 0) return 0.0;
        
        return (double)(count300 + count100 + count50) / totalHits;
    }

    /**
     * Calculates accuracy percentage for osu!mania mode.
     * 
     * @param countGeki number of MAX/Rainbow 300s
     * @param count300 number of 300s
     * @param countKatu number of 200s
     * @param count100 number of 100s
     * @param count50 number of 50s
     * @param countMiss number of misses
     * @return accuracy as decimal (0.0 to 1.0)
     */
    public static double calculateManiaAccuracy(int countGeki, int count300, int countKatu, 
                                              int count100, int count50, int countMiss) {
        int totalHits = countGeki + count300 + countKatu + count100 + count50 + countMiss;
        if (totalHits == 0) return 0.0;
        
        double hitValue = countGeki * 300 + count300 * 300 + countKatu * 200 + 
                         count100 * 100 + count50 * 50;
        return hitValue / (totalHits * 300.0);
    }

    /**
     * Checks if a mod combination is considered "difficulty increasing".
     * 
     * @param mods mods bitmask
     * @return true if mods increase difficulty
     */
    public static boolean isDifficultyIncreasing(int mods) {
        return (mods & (MOD_HIDDEN | MOD_HARD_ROCK | MOD_DOUBLE_TIME | 
                       MOD_FLASHLIGHT | MOD_SUDDEN_DEATH | MOD_PERFECT)) != 0;
    }

    /**
     * Checks if a mod combination is considered "difficulty reducing".
     * 
     * @param mods mods bitmask
     * @return true if mods reduce difficulty
     */
    public static boolean isDifficultyReducing(int mods) {
        return (mods & (MOD_NO_FAIL | MOD_EASY | MOD_HALF_TIME | MOD_SPUN_OUT)) != 0;
    }
}
