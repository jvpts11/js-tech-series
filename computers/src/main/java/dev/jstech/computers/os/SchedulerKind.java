/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import dev.jstech.core.id.IStableName;

/**
 * The task-scheduling model implemented by a kernel.
 *
 * <p>This enum is pure and carries no Minecraft dependency, so it can be used freely in JUnit tests
 * and in kernel definitions without pulling in the binding layer.
 */
public enum SchedulerKind implements IStableName {
    /** No multitasking scheduler, single-task or batch execution only. */
    NONE("none"),
    /** Cooperative multitasking, tasks yield voluntarily. */
    COOPERATIVE("cooperative"),
    /** Preemptive multitasking, the kernel forcibly context-switches tasks. */
    PREEMPTIVE("preemptive");

    private final String serializedName;

    SchedulerKind(final String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String serializedName() {
        return serializedName;
    }
}
