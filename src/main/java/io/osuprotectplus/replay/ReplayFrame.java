package io.osuprotectplus.replay;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReplayFrame {
    private long timeDelta;
    private float x;
    private float y;
    private int keys;

    public String toString() {
        return String.format("%d | %.2f | %.2f | %d", timeDelta, x, y, keys);
    }
}
