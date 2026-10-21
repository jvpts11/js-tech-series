/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.robot;

import dev.jstech.core.event.ICoreEvent;

/**
 * A robot finished a task, done or given up, on the server: posted on the Core's events, so what commands robots from
 * outside, a computer's program, hears how its orders went.
 *
 * @param robot  the robot
 * @param task   the task it finished
 * @param status how it finished: {@code DONE} or {@code FAILED}
 */
public record RobotTaskEvent(RobotEntity robot, IRobotTask task, IRobotTask.Status status) implements ICoreEvent {

    @Override
    public String eventId() {
        return "robot.task_finished";
    }
}
