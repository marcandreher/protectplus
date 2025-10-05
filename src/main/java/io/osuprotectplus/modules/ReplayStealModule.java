package io.osuprotectplus.modules;

import io.osuprotectplus.detection.Flags;
import io.osuprotectplus.modules.base.AcModule;
import io.osuprotectplus.replay.OsrReplay;
import io.osuprotectplus.replay.ReplayFrame;
import io.osuprotectplus.utils.FileStorage;

public class ReplayStealModule extends AcModule {

    public ReplayStealModule(OsrReplay replay) {
        super(replay);
    }

    @Override
    public boolean run() {
        boolean foundStolen = false;
        int analyzed = 0;
        for (OsrReplay gotReplay : FileStorage.loadReplaysForBeatmap(replay, replay.getBeatmapMD5())) {
            double similarity = computeCursorSimilarity(this.replay, gotReplay);
            analyzed++;
            if (similarity > 0.8) { // 80% similarity threshold
                logger.info("Replay stolen from replay {}: {:.2f}%", gotReplay.getBeatmapMD5(), similarity * 100);
                foundStolen = true;
            }
        }
        logger.info("Analyzed {} replays for beatmap {}", analyzed, replay.getBeatmapMD5());
        return foundStolen;
    }

    public double computeCursorSimilarity(OsrReplay a, OsrReplay b) {
        int n = Math.min(a.getFrames().size(), b.getFrames().size());
        if (n == 0)
            return 0.0;
        double sumSqDist = 0.0;
        for (int i = 0; i < n; i++) {
            ReplayFrame fa = a.getFrames().get(i);
            ReplayFrame fb = b.getFrames().get(i);
            double dx = fa.getX() - fb.getX();
            double dy = fa.getY() - fb.getY();
            sumSqDist += dx * dx + dy * dy;
        }
        double avgSqDist = sumSqDist / n;
        // Maximum possible squared distance in osu! is (512^2 + 384^2)
        double maxSqDist = 512 * 512 + 384 * 384;
        double similarity = 1.0 - (Math.sqrt(avgSqDist) / Math.sqrt(maxSqDist));
        if (similarity < 0)
            similarity = 0;
        return similarity;
    }

    @Override
    public boolean canRun() {
        return true;
    }

    @Override
    public Flags getDetection() {
        return Flags.REPLAY_STOLEN;
    }
}
