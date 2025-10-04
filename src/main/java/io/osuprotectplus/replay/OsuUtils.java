package io.osuprotectplus.replay;

public class OsuUtils {

    /**
     * Returns the official osu!standard score multiplier for a given mods bitmask.
     * Implements all mods as per osu! wiki (as of 2025).
     * @param mods mods bitmask
     * @return score multiplier
     */
    public static double getOfficialScoreMultiplier(int mods) {
        double multiplier = 1.0;
        // NoFail
        if ((mods & 1) != 0) multiplier *= 0.5;
        // Easy
        if ((mods & 2) != 0) multiplier *= 0.5;
        // TouchDevice (no effect)
        // Hidden
        if ((mods & 8) != 0) multiplier *= 1.06;
        // HardRock
        if ((mods & 16) != 0) multiplier *= 1.06;
        // SuddenDeath (no effect)
        // DoubleTime
        if ((mods & 64) != 0) multiplier *= 1.12;
        // Nightcore (always with DT, no extra effect)
        // Relax (score = 0)
        if ((mods & 128) != 0) return 0.0;
        // HalfTime
        if ((mods & 256) != 0) multiplier *= 0.3;
        // Flashlight
        if ((mods & 1024) != 0) multiplier *= 1.12;
        // Autoplay (score = 0)
        if ((mods & 2048) != 0) return 0.0;
        // SpunOut
        if ((mods & 4096) != 0) multiplier *= 0.9;
        // Autopilot (score = 0)
        if ((mods & 8192) != 0) return 0.0;
        // Perfect (no effect)
        // Key mods (mania only, no effect in std)
        // FadeIn (mania only)
        // Random (mania only)
        // Cinema (score = 0)
        if ((mods & 4194304) != 0) return 0.0;
        // TargetPractice (no effect)
        // Coop, Mirror, ScoreV2 (no effect)
        return multiplier;
    }
    
}
