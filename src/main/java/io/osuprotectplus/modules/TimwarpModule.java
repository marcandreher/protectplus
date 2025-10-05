package io.osuprotectplus.modules;

import io.osuprotectplus.detection.Flags;
import io.osuprotectplus.modules.base.AcModule;
import io.osuprotectplus.replay.OsrReplay;
import io.osuprotectplus.replay.ReplayFrame;

public class TimwarpModule extends AcModule {

    public TimwarpModule(OsrReplay replay) {
        super(replay);
    }

    @Override
    public boolean run() {
        long startTime = System.currentTimeMillis();
        if (replay.getFrames() == null || replay.getFrames().size() < 10) return false;
        
        // 1. Check for unnaturally consistent frame rates (e.g., >95% at exactly same interval)
        java.util.Map<Long, Integer> deltaFreq = new java.util.HashMap<>();
        double totalVelocity = 0;
        int velocityCount = 0;
        
        for (int i = 1; i < replay.getFrames().size(); i++) {
            ReplayFrame prev = replay.getFrames().get(i-1);
            ReplayFrame curr = replay.getFrames().get(i);
            long delta = curr.getTimeDelta();
            
            // Count frame rate consistency
            deltaFreq.put(delta, deltaFreq.getOrDefault(delta, 0) + 1);
            
            // 2. Check for impossibly fast cursor movements
            if (delta > 0) {
                double dx = curr.getX() - prev.getX();
                double dy = curr.getY() - prev.getY();
                double distance = Math.sqrt(dx * dx + dy * dy);
                double velocity = distance / delta; // pixels per ms
                
                if (velocity > 3.0) { // >3 pixels/ms is superhuman (>10800 pixels/second)
                    totalVelocity += velocity;
                    velocityCount++;
                }
            }
        }
        
        // Find most common delta and check consistency
        long mostCommonDelta = deltaFreq.entrySet().stream()
            .max(java.util.Map.Entry.comparingByValue())
            .map(java.util.Map.Entry::getKey)
            .orElse(0L);
        
        int mostCommonCount = deltaFreq.getOrDefault(mostCommonDelta, 0);
        double consistency = (double) mostCommonCount / replay.getFrames().size();
        
        // 3. Check average superhuman velocity
        double avgSuperhumanVelocity = velocityCount > 0 ? totalVelocity / velocityCount : 0;

        logger.debug("Timewarp analysis - Frame consistency: {}, Superhuman movements: {}, Avg velocity: {} px/ms in {} ms",
                consistency * 100, velocityCount, avgSuperhumanVelocity, System.currentTimeMillis() - startTime);

        // Flag if: >95% consistent frame rate OR many superhuman cursor movements
        return (consistency > 0.95 && mostCommonDelta < 20) || 
               (velocityCount > replay.getFrames().size() * 0.1 && avgSuperhumanVelocity > 5.0);
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
        return Flags.TIMEWARP_OR_SPEEDHACK;
    }
    
}
