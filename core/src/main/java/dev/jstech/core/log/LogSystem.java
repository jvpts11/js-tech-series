/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One system of a mod that writes to the log under a name of its own, {@code mod/system} (the Core's recordings write
 * as {@code jscore/media}), so whoever reads the log knows which part of which mod said it, and its level can be
 * turned up to follow a fault in that part alone while the game runs. Declared once, with {@link LogSystems#declare}.
 */
public final class LogSystem {

    private final String mod;
    private final String name;
    /* Made the first time the system writes, so a system that never writes holds no logger. */
    private volatile Logger logger;

    LogSystem(final String mod, final String name) {
        this.mod = mod;
        this.name = name;
    }

    /** The system as commands name it, the way the game names things: {@code jscore:media}. */
    public String id() {
        return mod + ":" + name;
    }

    /** The system's name in the log: {@code jscore/media}. */
    public String loggerName() {
        return mod + "/" + name;
    }

    /** The mod the system belongs to. */
    public String mod() {
        return mod;
    }

    /** The system's own name within its mod. */
    public String name() {
        return name;
    }

    /** What the system writes to the log with. */
    public Logger logger() {
        Logger made = logger;
        if (made == null) {
            // The log hands back the same logger for the same name, so two threads making it at once agree.
            made = LoggerFactory.getLogger(loggerName());
            logger = made;
        }
        return made;
    }

    @Override
    public String toString() {
        return id();
    }
}
