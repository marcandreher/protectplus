package io.osuprotectplus.modules.base;

import io.osuprotectplus.detection.Flags;
import io.osuprotectplus.replay.OsrReplay;

public class AcModule implements IModule {

    protected final OsrReplay replay;
    protected final AcModuleLogger logger;

    public AcModule(OsrReplay replay) {
        this.replay = replay;
        this.logger = AcModuleLogger.getLogger(AcModule.class, getDetection(), replay);
    }

    @Override
    public boolean canRun() {
        throw new UnsupportedOperationException("Unimplemented method 'canRun'");
    }

    @Override
    public boolean run() {
        throw new UnsupportedOperationException("Unimplemented method 'run'");
    }

    @Override
    public Flags getDetection() {
        throw new UnsupportedOperationException("Unimplemented method 'getDetection'");
    }
    
}
