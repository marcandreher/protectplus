package io.osuprotectplus.detection;

import lombok.Data;

@Data
public class Flag {
    private Flags flag;
    private String reason;
}
