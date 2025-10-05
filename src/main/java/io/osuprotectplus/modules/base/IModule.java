package io.osuprotectplus.modules.base;

import io.osuprotectplus.detection.Flags;

public interface IModule {
    public boolean canRun();
    public boolean run();
    public Flags getDetection();
}
