package io.osuprotectplus;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.osuprotectplus.replay.OsrReplay;
import io.osuprotectplus.replay.OsrReplayParser;
import io.osuprotectplus.utils.FileStorage;

public class ProtectPlus {

    public static OsrReplay parse(File file) throws IOException {
        long startTime = System.currentTimeMillis();
        OsrReplay replay = new OsrReplay();
        // Read all bytes once and set in replay
        byte[] allBytes;
        try (FileInputStream fis = new FileInputStream(file)) {
            allBytes = fis.readAllBytes();
        }
        replay.setFile(allBytes);
        // Now parse fields from the byte array
        try (DataInputStream in = new DataInputStream(new java.io.ByteArrayInputStream(allBytes))) {
            replay.setFileName(file.getName());
            replay.setGameMode(in.readUnsignedByte());
            replay.setVersion(Integer.reverseBytes(in.readInt()));
            replay.setBeatmapMD5(OsrReplayParser.readOsuString(in));
            replay.setPlayerName(OsrReplayParser.readOsuString(in));
            replay.setReplayMD5(OsrReplayParser.readOsuString(in));
            replay.setCount300(Short.reverseBytes(in.readShort()));
            replay.setCount100(Short.reverseBytes(in.readShort()));
            replay.setCount50(Short.reverseBytes(in.readShort()));
            replay.setCountGeki(Short.reverseBytes(in.readShort()));
            replay.setCountKatu(Short.reverseBytes(in.readShort()));
            replay.setCountMiss(Short.reverseBytes(in.readShort()));
            replay.setTotalScore(Integer.reverseBytes(in.readInt()));
            replay.setMaxCombo(Short.reverseBytes(in.readShort()));
            replay.setPerfect(in.readByte());
            replay.setMods(Integer.reverseBytes(in.readInt()));
            replay.setLifeBarGraph(OsrReplayParser.readOsuString(in));
            replay.setTimestamp(Long.reverseBytes(in.readLong()));
            replay.setCompressedReplayLength(Integer.reverseBytes(in.readInt()));
            replay.setCompressedReplayData(new byte[replay.getCompressedReplayLength()]);
            in.readFully(replay.getCompressedReplayData());
            replay.setOnlineScoreID(Long.reverseBytes(in.readLong()));
            // decompress and parse frames
            OsrReplayParser.parseReplayFrames(replay);
        }
        FileStorage.storeReplay(replay);

        return replay;
    }

}
