package io.osuprotectplus.crawler;

/**
 * Represents a score from the osu! API
 */
public class Score {
    public String score_id;
    public String score;
    public String username;
    public String maxcombo;
    public String count50;
    public String count100;
    public String count300;
    public String countmiss;
    public String countkatu;
    public String countgeki;
    public String perfect;
    public String enabled_mods;
    public String user_id;
    public String date;
    public String rank;
    public String pp;
    public String replay_available;
    
    /**
     * Checks if this score has a replay available for download
     */
    public boolean hasReplayAvailable() {
        return "1".equals(replay_available);
    }
    
    /**
     * Gets the total hit count for this score
     */
    public int getTotalHits() {
        int count50 = parseIntSafe(this.count50);
        int count100 = parseIntSafe(this.count100);
        int count300 = parseIntSafe(this.count300);
        int countmiss = parseIntSafe(this.countmiss);
        return count50 + count100 + count300 + countmiss;
    }
    
    /**
     * Gets the accuracy percentage for this score
     */
    public double getAccuracy() {
        int count50 = parseIntSafe(this.count50);
        int count100 = parseIntSafe(this.count100);
        int count300 = parseIntSafe(this.count300);
        int totalHits = getTotalHits();
        
        if (totalHits == 0) return 0.0;
        
        // Standard osu! accuracy calculation
        double points = (count50 * 50 + count100 * 100 + count300 * 300);
        double maxPoints = totalHits * 300;
        return (points / maxPoints) * 100.0;
    }
    
    /**
     * Checks if this is a perfect play (no misses, 100% accuracy)
     */
    public boolean isPerfectPlay() {
        return "1".equals(perfect);
    }
    
    private int parseIntSafe(String value) {
        try {
            return value != null ? Integer.parseInt(value) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    @Override
    public String toString() {
        return String.format("Score{id=%s, user=%s, score=%s, combo=%s, acc=%.2f%%, rank=%s, replay=%s}", 
            score_id, username, score, maxcombo, getAccuracy(), rank, hasReplayAvailable());
    }
}