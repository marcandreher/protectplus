package io.osuprotectplus.crawler;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.OkHttpClient;

/**
 * Main application entry point for ProtectPlus Crawler
 */
public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);

    private static final File CONFIG_FILE = new File(".config/app.toml");
    private static final File STATE_FILE = new File(".config/since.json");

    public static void main(String[] args) {
        logger.info("Starting ProtectPlus Crawler (Modular Architecture)...");

        // Create OkHttpClient with proper resource management
        OkHttpClient httpClient = new OkHttpClient();
        
        try {
            // Load configuration
            Config config = Config.load(CONFIG_FILE);
            if (!config.isValid()) {
                logger.error("Missing or invalid osu! API key in .config/app.toml");
                logger.error("Please add: [osuapi]\\nkey = \"your-api-key-here\"");
                System.exit(1);
            }

            // Initialize state manager
            StateManager stateManager = new StateManager(STATE_FILE);
            
            // Create and run crawler engine
            CrawlerEngine engine = new CrawlerEngine(httpClient, config, stateManager);
            engine.runContinuous();
            
        } catch (InterruptedException e) {
            logger.info("Crawler interrupted by user");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("Fatal error in crawler: {}", e.getMessage(), e);
        } finally {
            // Properly shutdown OkHttpClient to clean up background threads
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
            if (httpClient.cache() != null) {
                try {
                    httpClient.cache().close();
                } catch (IOException e) {
                    logger.warn("Error closing HTTP cache: {}", e.getMessage());
                }
            }
            logger.info("HTTP client resources cleaned up");
        }
    }
}
