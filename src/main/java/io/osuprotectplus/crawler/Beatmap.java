package io.osuprotectplus.crawler;

/**
 * Represents a beatmap from the osu! API
 */
public class Beatmap {
    public String beatmapset_id;
    public String beatmap_id;
    public String approved;
    public String total_length;
    public String hit_length;
    public String version;
    public String file_md5;
    public String diff_size;
    public String diff_overall;
    public String diff_approach;
    public String diff_drain;
    public String mode;
    public String count_normal;
    public String count_slider;
    public String count_spinner;
    public String submit_date;
    public String approved_date;
    public String last_update;
    public String artist;
    public String artist_unicode;
    public String title;
    public String title_unicode;
    public String creator;
    public String creator_id;
    public String bpm;
    public String source;
    public String tags;
    public String genre_id;
    public String language_id;
    public String favourite_count;
    public String rating;
    public String storyboard;
    public String video;
    public String download_unavailable;
    public String audio_unavailable;
    public String playcount;
    public String passcount;
    public String packs;
    public String max_combo;
    public String diff_aim;
    public String diff_speed;
    public String difficultyrating;
    
    /**
     * Gets the date this beatmap should be considered for (approved_date or submit_date)
     */
    public String getEffectiveDate() {
        String date = approved_date != null ? approved_date : submit_date;
        if (date != null && date.contains(" ")) {
            return date.split(" ")[0]; // Extract just the date part
        }
        return date;
    }
    
    /**
     * Gets the full timestamp for pagination purposes
     */
    public String getFullTimestamp() {
        return approved_date != null ? approved_date : submit_date;
    }
    
    @Override
    public String toString() {
        return String.format("Beatmap{id=%s, title='%s - %s [%s]', date=%s}", 
            beatmap_id, artist, title, version, getEffectiveDate());
    }
}