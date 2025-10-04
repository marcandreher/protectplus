package io.osuprotectplus.crawler;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;



import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Processes score data for storage, analysis, and replay collection
 */
public class ScoreProcessor {
    private static final Logger logger = LoggerFactory.getLogger(ScoreProcessor.class);

    /**
     * Constructs a proper .osr file format with headers followed by LZMA data
     * Based on osu! .osr file specification
     *
     * @param score The score object containing metadata
     * @param lzmaData The LZMA-compressed replay data from osu! API
     * @return Complete .osr file as byte array
     */
    private byte[] constructOsrFile(Score score, byte[] lzmaData) {
        try (ByteArrayOutputStream osrFile = new ByteArrayOutputStream()) {
            // Game mode (1 byte) - osu! standard = 0
            osrFile.write(0);
            
            // Version (4 bytes, little-endian) - use current timestamp as version
            int version = (int) (System.currentTimeMillis() / 1000);
            osrFile.write(version & 0xFF);
            osrFile.write((version >> 8) & 0xFF);
            osrFile.write((version >> 16) & 0xFF);
            osrFile.write((version >> 24) & 0xFF);
            
            // Beatmap hash (string) - we don't have the actual MD5 hash, write empty
            writeOsuString(osrFile, "");
            
            // Player name (string)
            writeOsuString(osrFile, score.username != null ? score.username : "");
            
            // Replay hash (string) - generate from score_id and user for uniqueness
            String replayHash = score.score_id != null ? generateReplayHash(score) : "";
            if(replayHash.isEmpty() || score.score_id == null) {
                logger.warn("Score {} has no score_id, cannot generate replay hash", score.score_id);
                return null;
            }
            writeOsuString(osrFile, replayHash);
            
            // Score statistics (18 bytes total)
            writeShort(osrFile, parseIntSafe(score.count300));  // 300s count
            writeShort(osrFile, parseIntSafe(score.count100));  // 100s count
            writeShort(osrFile, parseIntSafe(score.count50));   // 50s count
            writeShort(osrFile, parseIntSafe(score.countgeki)); // Gekis count
            writeShort(osrFile, parseIntSafe(score.countkatu)); // Katus count
            writeShort(osrFile, parseIntSafe(score.countmiss)); // Misses count
            writeInt(osrFile, parseIntSafe(score.score));       // Total score
            writeShort(osrFile, parseIntSafe(score.maxcombo));  // Max combo
            
            // Perfect combo (1 byte) - 1 if perfect, 0 otherwise
            osrFile.write(parseIntSafe(score.perfect) == 1 ? 1 : 0);
            
            // Mods (4 bytes) - we have enabled_mods as integer
            writeInt(osrFile, parseIntSafe(score.enabled_mods));
            
            // Life bar graph (string) - empty for now
            writeOsuString(osrFile, "");
            
            // Timestamp (8 bytes) - use current timestamp
            long timestamp = System.currentTimeMillis() * 10000 + 621355968000000000L; // .NET ticks
            writeLong(osrFile, timestamp);
            
            // LZMA-compressed replay data length (4 bytes)
            writeInt(osrFile, lzmaData.length);
            
            // LZMA-compressed replay data
            osrFile.write(lzmaData);
            
            // Online score ID (8 bytes)
            writeLong(osrFile, parseLongSafe(score.score_id));
            
            return osrFile.toByteArray();
            
        } catch (Exception e) {
            logger.error("Failed to construct .osr file for score {}: {}", score.score_id, e.getMessage());
            return lzmaData; // Fall back to raw LZMA data
        }
    }
    
    /**
     * Writes an osu! string format: 1 byte for presence (0x00 or 0x0b), then ULEB128 length, then UTF-8 data
     */
    private void writeOsuString(ByteArrayOutputStream stream, String str) throws Exception {
        if (str == null || str.isEmpty()) {
            stream.write(0x00); // No string present
        } else {
            stream.write(0x0b); // String present
            byte[] utf8Bytes = str.getBytes(StandardCharsets.UTF_8);
            writeULEB128(stream, utf8Bytes.length);
            stream.write(utf8Bytes);
        }
    }
    
    /**
     * Writes a ULEB128 encoded integer
     */
    private void writeULEB128(ByteArrayOutputStream stream, int value) throws Exception {
        while (value >= 0x80) {
            stream.write((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        stream.write(value & 0x7F);
    }
    
    /**
     * Safely parses a String to int, returning 0 if null or invalid
     */
    private int parseIntSafe(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * Safely parses a String to long, returning 0 if null or invalid
     */
    private long parseLongSafe(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
    
    /**
     * Writes a 2-byte little-endian short
     */
    private void writeShort(ByteArrayOutputStream stream, int value) throws Exception {
        stream.write(value & 0xFF);
        stream.write((value >> 8) & 0xFF);
    }
    
    /**
     * Writes a 4-byte little-endian integer
     */
    private void writeInt(ByteArrayOutputStream stream, int value) throws Exception {
        stream.write(value & 0xFF);
        stream.write((value >> 8) & 0xFF);
        stream.write((value >> 16) & 0xFF);
        stream.write((value >> 24) & 0xFF);
    }
    
    /**
     * Writes an 8-byte little-endian long
     */
    private void writeLong(ByteArrayOutputStream stream, long value) throws Exception {
        stream.write((int)(value & 0xFF));
        stream.write((int)((value >> 8) & 0xFF));
        stream.write((int)((value >> 16) & 0xFF));
        stream.write((int)((value >> 24) & 0xFF));
        stream.write((int)((value >> 32) & 0xFF));
        stream.write((int)((value >> 40) & 0xFF));
        stream.write((int)((value >> 48) & 0xFF));
        stream.write((int)((value >> 56) & 0xFF));
    }
    

    /**
     * Generates a pseudo-hash for replay identification
     */
    private String generateReplayHash(Score score) {
        if (score.score_id == null) {
            return "";
        }
        // Create a unique identifier from score data
        String identifier = String.format("replay_%s_%s", 
            score.score_id, 
            score.user_id != null ? score.user_id : "unknown");
        
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(identifier.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            // Fallback to simple string manipulation if MD5 fails
            return identifier + "_" + identifier.hashCode();
        }
    }

    /**
     * Processes scores for a specific beatmap
     * 
     * @param scores    The scores to process
     * @param beatmapId The beatmap ID these scores belong to
     * @return Number of scores processed
     */
    public int processScores(List<Score> scores, String beatmapId) {
        if (scores == null || scores.isEmpty()) {
            logger.debug("No scores to process for beatmap {}", beatmapId);
            return 0;
        }

        logger.info("Processing {} scores for beatmap {}", scores.size(), beatmapId);

        // Process each score
        for (Score score : scores) {
            processSingleScore(score, beatmapId);
        }

        return scores.size();
    }

    /**
     * Processes a single score
     * TODO: Implement your specific score processing logic here
     */
    private void processSingleScore(Score score, String beatmapId) {

        if (!score.hasReplayAvailable()) {
            return;
        }

        logger.debug("Score {} has replay available for download", score.score_id);

        byte[] replayData = null;
        try {
            replayData = CrawlerEngine.instance.apiClient.fetchReplay(score.score_id);
        } catch (Exception e) {
            logger.warn("Failed to fetch replay for score {}: {}", score.score_id, e.getMessage());
            return; // Don't continue if replay fetch failed
        }

        if (replayData == null || replayData.length == 0) {
            logger.warn("No replay data received for score {}", score.score_id);
            return;
        }

        logger.debug("Successfully downloaded {} bytes for score {}", replayData.length, score.score_id);

        // Check what we actually received from the API
        String responseString = new String(replayData);
        logger.debug("First 100 characters of replay response for score {}: {}", score.score_id, 
                    responseString.length() > 100 ? responseString.substring(0, 100) : responseString);

        // The osu! API returns JSON with base64-encoded LZMA data in "content" field
        byte[] finalReplayData;
        try {
            // Parse JSON response to extract the "content" field
            Gson gson = new Gson();
            JsonObject jsonResponse = gson.fromJson(responseString, JsonObject.class);
            
            if (!jsonResponse.has("content")) {
                logger.warn("No 'content' field found in replay response for score {}: {}", score.score_id, responseString);
                return;
            }
            
            String base64Content = jsonResponse.get("content").getAsString();
            byte[] lzmaCompressedData = Base64.getDecoder().decode(base64Content);
            
            // Debug: Show first few bytes of compressed data
            StringBuilder hexDebug = new StringBuilder();
            for (int i = 0; i < Math.min(16, lzmaCompressedData.length); i++) {
                hexDebug.append(String.format("%02X ", lzmaCompressedData[i] & 0xFF));
            }
            logger.debug("Extracted and decoded {} characters to {} LZMA bytes for score {}, first 16 bytes: {}", 
                        base64Content.length(), lzmaCompressedData.length, score.score_id, hexDebug.toString());
            
            // Construct proper .osr file format with headers + LZMA data
            finalReplayData = constructOsrFile(score, lzmaCompressedData);
            logger.debug("Constructed .osr file ({} bytes) from LZMA data ({} bytes) for score {}", 
                        finalReplayData.length, lzmaCompressedData.length, score.score_id);
        } catch (Exception e) {
            logger.error("Failed to parse JSON or decode base64 replay data for score {}: {}", score.score_id, e.getMessage());
            logger.debug("Raw response data: {}", responseString.length() > 500 ? responseString.substring(0, 500) + "..." : responseString);
            return;
        }

        // --- Upload replay to analyzer service ---
        OkHttpClient client = OsuApiClient.httpClient;
        String url = "https://ac.osunolimits.dev/api/v1/analyze";

        // Save replay to file for debugging (optional)
        try {
            java.nio.file.Path replayPath = java.nio.file.Paths.get("debug_replay_" + score.score_id + ".osr");
            java.nio.file.Files.write(replayPath, finalReplayData);
            logger.debug("Saved replay to file: {}", replayPath);
        } catch (Exception e) {
            logger.warn("Failed to save replay file for debugging: {}", e.getMessage());
        }

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                        "file",
                        score.score_id + ".osr", // filename
                        RequestBody.create(finalReplayData, MediaType.parse("application/octet-stream")))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No error body";
                logger.warn("Analyzer request failed for score {}: {} - {}", score.score_id, response.code(), errorBody);
                return;
            }

            String responseBody = response.body().string();
            logger.info("Analyzer response for score {}: {}", score.score_id, responseBody);

        } catch (Exception e) {
            logger.error("Failed to send replay {} to analyzer: {}", score.score_id, e.getMessage(), e);
        }

    }

    /**
     * Filters scores for replay collection based on criteria
     * 
     * @param scores List of scores to filter
     * @return Filtered list of scores suitable for replay collection
     */
    public List<Score> filterScoresForReplayCollection(List<Score> scores) {
        return scores.stream()
                .filter(Score::hasReplayAvailable)
                .filter(score -> {
                    // Additional filtering criteria:

                    // 1. Only collect high-accuracy plays (>95%)
                    // if (score.getAccuracy() < 95.0) return false;

                    // 2. Only collect recent plays (within last year)
                    // if (!isRecentPlay(score.date)) return false;

                    // 3. Skip plays with certain mods
                    // if (hasUnwantedMods(score.enabled_mods)) return false;

                    return true; // For now, collect all available replays
                })
                .collect(Collectors.toList());
    }

    /**
     * Groups scores by user ID for user-based analysis
     */
    public Map<String, List<Score>> groupScoresByUser(List<Score> scores) {
        return scores.stream()
                .collect(Collectors.groupingBy(score -> score.user_id));
    }

    /**
     * Gets the top N scores by score value
     */
    public List<Score> getTopScores(List<Score> scores, int limit) {
        return scores.stream()
                .sorted((a, b) -> {
                    try {
                        long scoreA = Long.parseLong(a.score);
                        long scoreB = Long.parseLong(b.score);
                        return Long.compare(scoreB, scoreA); // Descending order
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                })
                .limit(limit)
                .collect(Collectors.toList());
    }
}