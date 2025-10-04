package io.osuprotectplus.models;

import java.util.Map;

import io.osuprotectplus.detection.Flag;
import lombok.Data;

@Data
public class Beatmap {
    private String hash;
    private long replayCount;

    private Map<String, Flag[]> flaggedReplays;
}
