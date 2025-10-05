package io.osuprotectplus.modules;

import io.osuprotectplus.detection.Flags;
import io.osuprotectplus.modules.base.AcModule;
import io.osuprotectplus.replay.OsrReplay;
import io.osuprotectplus.replay.ReplayFrame;

public class SpinbotCheckModule extends AcModule {

    private static final double SPINBOT_THRESHOLD_SPM = 500.0;
    private static final long MIN_SPINNER_DURATION_MS = 300; // Ignore segments shorter than 300 ms

    public SpinbotCheckModule(OsrReplay replay) {
        super(replay);
    }

    @Override
    public boolean run() {
        final long startTime = System.currentTimeMillis();

        if (replay.getFrames() == null || replay.getFrames().isEmpty()) {
            return false;
        }
        
        boolean inSpinner = false;
        double lastAngle = 0;
        double totalAngle = 0;
        long spinnerStartTime = 0;
        long spinnerEndTime = 0;
        long absTime = 0;
        
        int spinnersAnalyzed = 0;
        double highestSPM = 0.0;
        
        for (int i = 1; i < replay.getFrames().size(); i++) {
            ReplayFrame curr = replay.getFrames().get(i);
            absTime += curr.getTimeDelta();
            double cx = curr.getX() - 256.0;
            double cy = curr.getY() - 192.0;
            double r = Math.sqrt(cx * cx + cy * cy);
            
            if (r > 40) { // Lowered threshold for spinner detection
                double angle = Math.atan2(cy, cx);
                if (!inSpinner) {
                    inSpinner = true;
                    spinnerStartTime = absTime;
                    totalAngle = 0;
                    lastAngle = angle;
                } else {
                    double delta = angle - lastAngle;
                    while (delta > Math.PI) delta -= 2 * Math.PI;
                    while (delta < -Math.PI) delta += 2 * Math.PI;
                    totalAngle += Math.abs(delta);
                    lastAngle = angle;
                    spinnerEndTime = absTime;
                }
            } else if (inSpinner) {
                long duration = spinnerEndTime - spinnerStartTime;
                if (duration > 0 && duration >= MIN_SPINNER_DURATION_MS) {
                    spinnersAnalyzed++;
                    double spins = totalAngle / (2 * Math.PI);
                    double minutes = duration / 60000.0;
                    double spm = spins / minutes;
                    
                    // Track highest SPM for logging
                    if (spm > highestSPM) {
                        highestSPM = spm;
                    }
                    
                    if (spm > SPINBOT_THRESHOLD_SPM) {
                        long analysisTime = System.currentTimeMillis() - startTime;
                        logger.info("Spinbot detected: {} SPM (threshold: {}) in {}ms", 
                                   Math.round(spm), Math.round(SPINBOT_THRESHOLD_SPM), analysisTime);
                        return true;
                    }
                }
                inSpinner = false;
            }
        }
        
        long analysisTime = System.currentTimeMillis() - startTime;
        logger.debug("No spinbot detected: {} spinners, max {} SPM in {}ms", 
                    spinnersAnalyzed, Math.round(highestSPM), analysisTime);
        return false;
    }

    @Override
    public boolean canRun() {
        if(replay.getGameMode() == 0 || replay.getGameMode() == 2) {
            return true;
        }

        return false;
    }

    @Override
    public Flags getDetection() {
        return Flags.SPINBOT_DETECTED;
    }
    
}
