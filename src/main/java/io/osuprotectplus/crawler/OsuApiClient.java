package io.osuprotectplus.crawler;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Handles API communication with osu! servers
 */
public class OsuApiClient {
    private static final Logger logger = LoggerFactory.getLogger(OsuApiClient.class);
    private static final Gson gson = new Gson();

    public static final OkHttpClient httpClient = new OkHttpClient();
    private final String apiKey;
    
    public OsuApiClient(String apiKey) {
        this.apiKey = apiKey;
    }
    
    /**
     * Fetches beatmaps from the osu! API
     * @param since The timestamp to fetch beatmaps since
     * @param limit Maximum number of beatmaps to fetch
     * @return Array of beatmaps, or null if request failed
     */
    public Beatmap[] fetchBeatmaps(String since, int limit) throws IOException, InterruptedException {
        HttpUrl url = new HttpUrl.Builder()
                .scheme("https")
                .host("osu.ppy.sh")
                .addPathSegments("api/get_beatmaps")
                .addQueryParameter("k", apiKey)
                .addQueryParameter("since", since)
                .addQueryParameter("s", "1") // ranked only
                .addQueryParameter("limit", String.valueOf(limit))
                .build();

        Request request = new Request.Builder().url(url).build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.error("API request failed with status: {}", response.code());
                if (response.code() == 429) { // Rate limited
                    logger.warn("Rate limited, waiting 10 seconds...");
                    Thread.sleep(10000);
                }
                throw new IOException("API request failed: " + response.code());
            }

            String body = response.body().string();
            logger.debug("Received response with {} characters", body.length());
            
            return gson.fromJson(body, Beatmap[].class);
        }
    }
    
    /**
     * Filters beatmaps to only include those from a specific date
     */
    public List<Beatmap> filterByDate(Beatmap[] beatmaps, String targetDate) {
        if (beatmaps == null) {
            return List.of();
        }
        
        return Arrays.stream(beatmaps)
            .filter(beatmap -> targetDate.equals(beatmap.getEffectiveDate()))
            .collect(Collectors.toList());
    }
    
    /**
     * Extracts beatmap IDs from a list of beatmaps
     */
    public List<String> extractBeatmapIds(List<Beatmap> beatmaps) {
        return beatmaps.stream()
            .map(beatmap -> beatmap.beatmap_id)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets the latest timestamp from a list of beatmaps for pagination
     */
    public String getLatestTimestamp(List<Beatmap> beatmaps) {
        return beatmaps.stream()
            .map(Beatmap::getFullTimestamp)
            .filter(timestamp -> timestamp != null && !timestamp.isEmpty())
            .max(String::compareTo)
            .orElse(null);
    }
    
    /**
     * Fetches scores for a specific beatmap from the osu! API
     * @param beatmapId The beatmap ID to fetch scores for
     * @param limit Maximum number of scores to fetch (default 50, max 100)
     * @return Array of scores, or null if request failed
     */
    public Score[] fetchScores(String beatmapId, int limit) throws IOException, InterruptedException {
        HttpUrl url = new HttpUrl.Builder()
                .scheme("https")
                .host("osu.ppy.sh")
                .addPathSegments("api/get_scores")
                .addQueryParameter("k", apiKey)
                .addQueryParameter("b", beatmapId)
                .addQueryParameter("limit", String.valueOf(Math.min(limit, 100))) // API max is 100
                .build();

        Request request = new Request.Builder().url(url).build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.error("Scores API request failed with status: {} for beatmap {}", response.code(), beatmapId);
                if (response.code() == 429) { // Rate limited
                    logger.warn("Rate limited, waiting 10 seconds...");
                    Thread.sleep(10000);
                }
                throw new IOException("Scores API request failed: " + response.code());
            }

            String body = response.body().string();
            logger.debug("Received scores response with {} characters for beatmap {}", body.length(), beatmapId);
            
            return gson.fromJson(body, Score[].class);
        }
    }
    
    /**
     * Fetches scores for a specific beatmap with default limit of 50
     */
    public Score[] fetchScores(String beatmapId) throws IOException, InterruptedException {
        return fetchScores(beatmapId, 50);
    }
    
    /**
     * Filters scores to only include those with replays available
     */
    public List<Score> filterScoresWithReplays(Score[] scores) {
        if (scores == null) {
            return List.of();
        }
        
        return Arrays.stream(scores)
            .filter(Score::hasReplayAvailable)
            .collect(Collectors.toList());
    }
    
    /**
     * Extracts score IDs from a list of scores
     */
    public List<String> extractScoreIds(List<Score> scores) {
        return scores.stream()
            .map(score -> score.score_id)
            .collect(Collectors.toList());
    }
    
    /**
     * Extracts user IDs from a list of scores
     */
    public List<String> extractUserIds(List<Score> scores) {
        return scores.stream()
            .map(score -> score.user_id)
            .distinct() // Remove duplicates
            .collect(Collectors.toList());
    }
    
    /**
     * Downloads a replay file for a specific score from the osu! API
     * @param scoreId The score ID to download the replay for
     * @param gameMode The game mode (0 = osu!, 1 = Taiko, 2 = Catch the Beat, 3 = osu!mania)
     * @return Raw replay data as byte array, or null if request failed
     */
    public byte[] fetchReplay(String scoreId, int gameMode) throws IOException, InterruptedException {
        HttpUrl url = new HttpUrl.Builder()
                .scheme("https")
                .host("osu.ppy.sh")
                .addPathSegments("api/get_replay")
                .addQueryParameter("k", apiKey)
                .addQueryParameter("s", scoreId)
                .addQueryParameter("m", String.valueOf(gameMode))
                .build();

        Request request = new Request.Builder().url(url).build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.error("Replay API request failed with status: {} for score {}", response.code(), scoreId);
                if (response.code() == 429) { // Rate limited
                    logger.warn("Rate limited, waiting 10 seconds...");
                    Thread.sleep(10000);
                }
                throw new IOException("Replay API request failed: " + response.code());
            }

            byte[] replayData = response.body().bytes();
            logger.debug("Downloaded replay with {} bytes for score {}", replayData.length, scoreId);
            
            return replayData;
        }
    }
    
    /**
     * Downloads a replay file for a specific score with default game mode (osu! standard)
     */
    public byte[] fetchReplay(String scoreId) throws IOException, InterruptedException {
        return fetchReplay(scoreId, 0); // Default to osu! standard mode
    }
}