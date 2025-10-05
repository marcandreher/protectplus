package io.osuprotectplus.modules.base;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.osuprotectplus.detection.Flags;
import io.osuprotectplus.replay.OsrReplay;

public class AcModuleLogger {
private final Logger logger;
private final Flags flag;
private final OsrReplay replay;

static {
    File dataDir = new File("data");
    if (!dataDir.exists()) {
        dataDir.mkdirs();
    }

    File taskLogDir = new File(dataDir, "task_logs");
    if (!taskLogDir.exists()) {
        taskLogDir.mkdirs();
    }
}

private AcModuleLogger(Class<?> clazz, Flags flag, OsrReplay replay) {
    this.logger = LoggerFactory.getLogger(clazz);
    this.flag = flag;
    this.replay = replay;
}

public static AcModuleLogger getLogger(Class<?> clazz, Flags flag, OsrReplay replay) {
    return new AcModuleLogger(clazz, flag, replay);
}

public void info(String msg) {
    logger.info("[{}] {}", flag, msg);
    append(msg);
}

public void info(String format, Object... args) {
    Object[] allArgs = prependFlag(args);
    logger.info("[{}] " + format, allArgs);

    String formattedMsg = org.slf4j.helpers.MessageFormatter.arrayFormat(format, args).getMessage();
    append(formattedMsg);
}

public void warn(String msg) {
    logger.warn("[{}] {}", flag, msg);
    append(msg);
}

public void warn(String format, Object... args) {
    Object[] allArgs = prependFlag(args);
    logger.warn("[{}] " + format, allArgs);

    String formattedMsg = org.slf4j.helpers.MessageFormatter.arrayFormat(format, args).getMessage();
    append(formattedMsg);
}

public void error(String msg, Throwable t) {
    logger.error("[{}] {}", flag, msg, t);
    append(msg);
}

public void error(String format, Object... args) {
    Object[] allArgs = prependFlag(args);
    logger.error("[{}] " + format, allArgs);

    String formattedMsg = org.slf4j.helpers.MessageFormatter.arrayFormat(format, args).getMessage();
    append(formattedMsg);
}

public void error(String format, Throwable t, Object... args) {
    Object[] allArgs = prependFlag(args);
    logger.error("[{}] " + format, allArgs, t);

    String formattedMsg = org.slf4j.helpers.MessageFormatter.arrayFormat(format, args).getMessage();
    append(formattedMsg);
}

public void debug(String msg) {
    logger.debug("[{}] {}", flag, msg);
    append(msg);
}

public void debug(String format, Object... args) {
    Object[] allArgs = prependFlag(args);
    logger.debug("[{}] " + format, allArgs);

    String formattedMsg = org.slf4j.helpers.MessageFormatter.arrayFormat(format, args).getMessage();
    append(formattedMsg);
}

private Object[] prependFlag(Object... args) {
    Object[] allArgs = new Object[args.length + 1];
    allArgs[0] = flag;
    System.arraycopy(args, 0, allArgs, 1, args.length);
    return allArgs;
}

public void append(String msg) {
    File logFile = new File(new File("data/task_logs"), replay.getReplayMD5() + ".log");

    try {
        java.nio.file.Files.writeString(
            logFile.toPath(),
            msg + System.lineSeparator(),
            java.nio.file.StandardOpenOption.CREATE,
            java.nio.file.StandardOpenOption.APPEND
        );
    } catch (Exception e) {
        logger.error("Failed to write to log file: " + logFile.getAbsolutePath(), e);
    }
}


}
