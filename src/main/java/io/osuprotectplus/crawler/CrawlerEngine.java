package io.osuprotectplus.crawler;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.OkHttpClient;

/**
 * Main crawler engine that orchestrates the crawling process
 */
public class CrawlerEngine {
    private static final Logger logger = LoggerFactory.getLogger(CrawlerEngine.class);
    
    public final OsuApiClient apiClient;
    public final BeatmapProcessor processor;
    public final StateManager stateManager;
    public final ScoreProcessor scoreProcessor;
    
    private static final int BATCH_SIZE = 500;
    private static final int REQUEST_DELAY_MS = 100000;
    private static final int ERROR_DELAY_MS = 5000;
    
    public static CrawlerEngine instance;

    public CrawlerEngine(OkHttpClient httpClient, Config config, StateManager stateManager) {
        this.apiClient = new OsuApiClient(config.osuApiKey);
        this.processor = new BeatmapProcessor();
        this.stateManager = stateManager;
        this.scoreProcessor = new ScoreProcessor();
        instance = this;
    }
    
    /**
     * Runs the continuous crawling process
     */
    public void runContinuous() throws InterruptedException {
        logger.info("Starting continuous crawling mode...");
        
        while (true) {
            String currentDate = stateManager.loadSince("2015-01-01");
            logger.info("\n=== Crawling beatmaps for date: {} ===" , currentDate);
            
            try {
                int totalBeatmaps = crawlDate(currentDate);
                
                // Move to next date
                LocalDate nextDate = LocalDate.parse(currentDate).plusDays(1);
                String nextDateStr = nextDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
                
                stateManager.saveSince(nextDateStr);
                logger.info("=== Completed date: {} | Total beatmaps: {} | Moving to: {} ===" ,
                    currentDate, totalBeatmaps, nextDateStr);
                
                // Check if we've reached current date
                if (nextDate.isAfter(LocalDate.now())) {
                    logger.info("Reached current date. Crawler completed successfully!");
                    break;
                }
                
            } catch (Exception e) {
                logger.error("Error crawling date {}: {}", currentDate, e.getMessage(), e);
                logger.info("Waiting {} seconds before retry...", ERROR_DELAY_MS / 1000);
                Thread.sleep(ERROR_DELAY_MS);
            }
        }
    }
    
    /**
     * Crawls all beatmaps for a specific date
     * @param targetDate The date to crawl (YYYY-MM-DD format)
     * @return Total number of beatmaps found for this date
     */
    private int crawlDate(String targetDate) throws IOException, InterruptedException {
        boolean hasMoreBeatmaps = true;
        int totalBeatmapsForDate = 0;
        String since = targetDate; // Working variable for pagination
        
        while (hasMoreBeatmaps) {
            logger.debug("Fetching batch with since={}", since);
            
            try {
                // Fetch beatmaps from API
                Beatmap[] allBeatmaps = apiClient.fetchBeatmaps(since, BATCH_SIZE);
                
                if (allBeatmaps == null || allBeatmaps.length == 0) {
                    logger.debug("No more beatmaps returned from API");
                    break;
                }
                
                // Filter to only current date
                List<Beatmap> currentDateBeatmaps = apiClient.filterByDate(allBeatmaps, targetDate);
                
                if (currentDateBeatmaps.isEmpty()) {
                    logger.debug("No beatmaps found for target date in this batch");
                    break;
                }
                
                // Process the beatmaps
                int processed = processor.processBeatmaps(currentDateBeatmaps, targetDate);
                totalBeatmapsForDate += processed;
                
                // Extract IDs for logging
                List<String> beatmapIds = apiClient.extractBeatmapIds(currentDateBeatmaps);
                logger.info("Found {} beatmaps for {}: {}", beatmapIds.size(), targetDate, beatmapIds);
                
                // Update pagination cursor
                String latestTimestamp = apiClient.getLatestTimestamp(currentDateBeatmaps);
                if (latestTimestamp != null) {
                    since = latestTimestamp;
                } else {
                    logger.warn("No timestamp found for pagination, stopping");
                    break;
                }
                
                // Check if we got fewer than the limit (end of data)
                if (allBeatmaps.length < BATCH_SIZE) {
                    logger.debug("Received fewer than {} beatmaps, assuming end of data", BATCH_SIZE);
                    hasMoreBeatmaps = false;
                }
                
                // Rate limiting
                Thread.sleep(REQUEST_DELAY_MS);
                
            } catch (IOException e) {
                logger.error("API error: {}", e.getMessage());
                Thread.sleep(ERROR_DELAY_MS);
                // Continue with retry
            }
        }
        
        // Log final summary for this date
        logger.info("Completed crawling date: {} with {} total beatmaps", targetDate, totalBeatmapsForDate);
        return totalBeatmapsForDate;
    }
}