/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.robot;

/**
 * One thing a robot is told to do: go somewhere, break a block, pick up what lies around, wait, drive. A robot works
 * its tasks one after the other, a tick at a time, and keeps them with the world, each saved by the codec of its
 * {@link RobotTaskType}; what a task has done so far is its own and starts over when the world loads.
 *
 * <p>A robot is a worker. No task the Core gives it harms anything living, and a mod's tasks should keep it so: in the
 * series, fighting is a player's to do.
 */
public interface IRobotTask {

    /** The kind of task, which saves and reads it. */
    RobotTaskType<?> type();

    /** One tick of the task, done by {@code robot} on the server. */
    Status tick(RobotEntity robot);

    /** Where a task stands after a tick. */
    enum Status {
        /** Still at it. */
        RUNNING,
        /** Done; the robot goes on to its next task. */
        DONE,
        /** It cannot be done; the robot gives it up and goes on to its next task. */
        FAILED
    }
}
