/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.robot;

/**
 * Something a robot senses of itself or of the world around it, true or false: whether it carries something, whether a
 * block is near, what time of day it is. Its tasks with logic ({@link CoreRobotTasks.If}, {@link
 * CoreRobotTasks.WaitUntil}) act on it, and it is saved with them by the codec of its {@link RobotConditionType}.
 */
public interface IRobotCondition {

    /** The kind of condition, which saves and reads it. */
    RobotConditionType<?> type();

    /** Whether it holds for {@code robot} now, on the server. */
    boolean test(RobotEntity robot);
}
