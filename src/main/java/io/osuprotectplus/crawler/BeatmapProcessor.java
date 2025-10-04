package io.osuprotectplus.crawler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes beatmap data for storage, analysis, etc.
 */
public class BeatmapProcessor {
    private static final Logger logger = LoggerFactory.getLogger(BeatmapProcessor.class);
    
    /**
     * Processes a batch of beatmaps for a specific date
     * @param beatmaps The beatmaps to process
     * @param date The date being processed
     * @return Number of beatmaps processed
     * @throws InterruptedException 
     * @throws IOException 
     */
    public int processBeatmaps(List<Beatmap> beatmaps, String date) throws IOException, InterruptedException {
        if (beatmaps == null || beatmaps.isEmpty()) {
            return 0;
        }
        
        logger.debug("Processing {} beatmaps for date {}", beatmaps.size(), date);
        
        for (Beatmap beatmap : beatmaps) {
            processSingleBeatmap(beatmap);
        }
        
        return beatmaps.size();
    }
    
    /**
     * Processes a single beatmap
     * TODO: Implement your specific processing logic here
     * @throws InterruptedException 
     * @throws IOException 
     */
    private void processSingleBeatmap(Beatmap beatmap) throws IOException, InterruptedException {
        // Example processing - you can extend this:

        Score[] scores = CrawlerEngine.instance.apiClient.fetchScores(beatmap.beatmap_id, 50);
        ArrayList<Score> scoreList = new ArrayList<>(Arrays.asList(scores));
        CrawlerEngine.instance.scoreProcessor.processScores(scoreList, String.valueOf(beatmap.beatmap_id));
        
        // 1. Save to database
        // saveBeatmapToDatabase(beatmap);
        
        // 2. Download beatmap file if needed
        // downloadBeatmapFile(beatmap);
        
        // 3. Queue for replay analysis
        // queueForReplayAnalysis(beatmap);
        
        // 4. Extract metadata
        // extractMetadata(beatmap);
        
        logger.trace("Processed beatmap: {}", beatmap);
    }
    
    /**
     * Logs summary statistics for a completed date
     */
    public void logDateSummary(String date, int totalBeatmaps, List<String> beatmapIds) {
        logger.info("=== Date {} Summary ===" , date);
        logger.info("Total beatmaps processed: {}", totalBeatmaps);
        logger.info("Beatmap IDs: {}", beatmapIds);
        
        // Additional statistics you might want:
        // - Unique creators count
        // - Game modes distribution
        // - Difficulty range
        // etc.
    }
}