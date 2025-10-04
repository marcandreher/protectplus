package io.osuprotectplus.replay;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

// LZMA decompression: use SevenZip LZMA SDK (add dependency to your project)
import org.tukaani.xz.LZMAInputStream;

public class OsrReplayParser {

    public static void parseReplayFrames(OsrReplay replay) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(replay.getCompressedReplayData());
        try (LZMAInputStream lzmaIn = new LZMAInputStream(bais)) {
            StringBuilder sb = new StringBuilder();
            byte[] buf = new byte[4096];
            int n;
            while ((n = lzmaIn.read(buf)) != -1) {
                sb.append(new String(buf, 0, n, StandardCharsets.UTF_8));
            }
            String[] actions = sb.toString().split(",");
            for (String action : actions) {
                if (action.isEmpty() || action.startsWith("-")) continue; // skip RNG seed or empty
                String[] parts = action.split("\\|");
                if (parts.length != 4) continue;
                try {
                    long w = Long.parseLong(parts[0]);
                    float x = Float.parseFloat(parts[1]);
                    float y = Float.parseFloat(parts[2]);
                    int z = Integer.parseInt(parts[3]);
                    replay.getFrames().add(new ReplayFrame(w, x, y, z));
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    // Reads osu! string format (see spec)
    public static String readOsuString(DataInputStream in) throws IOException {
        byte b = in.readByte();
        if (b == 0x00) return "";
        if (b != 0x0b) throw new IOException("Invalid string prefix: " + b);
        int len = (int) readULEB128(in);
        byte[] strBytes = new byte[len];
        in.readFully(strBytes);
        return new String(strBytes, StandardCharsets.UTF_8);
    }

    // Reads ULEB128 (unsigned little-endian base 128)
    public static long readULEB128(DataInputStream in) throws IOException {
        long result = 0;
        int shift = 0;
        while (true) {
            byte b = in.readByte();
            result |= (long)(b & 0x7F) << shift;
            if ((b & 0x80) == 0) break;
            shift += 7;
        }
        return result;
    }

    
}