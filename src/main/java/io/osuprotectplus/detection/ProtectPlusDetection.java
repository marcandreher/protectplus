package io.osuprotectplus.detection;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.osuprotectplus.modules.base.AcModule;
import io.osuprotectplus.replay.OsrReplay;
import io.osuprotectplus.utils.FileStorage;

public class ProtectPlusDetection {
    private static Logger logger = LoggerFactory.getLogger(ProtectPlusDetection.class);
    private OsrReplay replay;
    public static final double SPINBOT_THRESHOLD_SPM = 700.0; // spins per minute
    public static final ArrayList<Class<? extends AcModule>> modules = new ArrayList<>();

    static {
        modules.add(io.osuprotectplus.modules.ScoreMultiplierModule.class);
        modules.add(io.osuprotectplus.modules.SpinbotCheckModule.class);
        modules.add(io.osuprotectplus.modules.FullAutoplayModule.class);
        modules.add(io.osuprotectplus.modules.TimwarpModule.class);
        modules.add(io.osuprotectplus.modules.ReplayStealModule.class);
    }

    public ProtectPlusDetection(OsrReplay replay) {
        this.replay = replay;

    }

    public List<Flag> analyze() {
        long startTime = System.currentTimeMillis();
        ArrayList<Flag> flags = new ArrayList<>();

        for (Class<? extends AcModule> moduleClass : modules) {
            try {
                AcModule module = moduleClass.getConstructor(OsrReplay.class).newInstance(replay);
                if (module.canRun()) {
                    boolean result = module.run();
                    if (result) {
                        Flag flag = new Flag();
                        flag.setFlag(module.getDetection());;
                        flags.add(flag);
                    }
                }
            } catch (Exception e) {
                logger.error("Error running module {}: {}", moduleClass.getSimpleName(), e.getMessage());
            }
        }
        if (flags.isEmpty()) {
            File taskLogFile = new File("data/task_logs", replay.getReplayMD5() + ".log");
            if (taskLogFile.exists()) {
                taskLogFile.delete();
            }
        }

        FileStorage.storeDetection(replay.getReplayMD5(), flags);

        logger.info("Analysis completed in {} ms", System.currentTimeMillis() - startTime);

        // Implement your analysis logic here
        return flags;
    }

}
