package io.osuprotectplus.detection;

import java.util.List;

import lombok.Data;

@Data
public class Detection {
    private String beatmapHash;
    private String replayHash;
    private List<Flag> flags;
    private String details;
}
