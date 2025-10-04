package io.osuprotectplus.replay;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class OsrReplay {
    private byte[] file;
    private String fileName;
    private int gameMode;
    private int version;
    private String beatmapMD5;
    private String playerName;
    private String replayMD5;
    private short count300, count100, count50, countGeki, countKatu, countMiss;
    private int totalScore;
    private short maxCombo;
    private byte perfect;
    private int mods;
    private String lifeBarGraph;
    private long timestamp;
    private int compressedReplayLength;
    private byte[] compressedReplayData;
    private long onlineScoreID;
    private List<ReplayFrame> frames = new ArrayList<>();

    public String toString() {
        return String.format(
                "OsrReplay[fileName=%s, player=%s, beatmapMD5=%s, replayMD5=%s, score=%d, maxCombo=%d, 300=%d, 100=%d, 50=%d, miss=%d, mods=%d, frames=%d]",
                fileName, playerName, beatmapMD5, replayMD5, totalScore, maxCombo, count300, count100, count50, countMiss, mods,
                frames.size());
    }
}
