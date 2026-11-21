/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.agent;

import dev.jstech.core.moving.IStructureBody;

/**
 * What a mod tells an agent of a moving structure it flies, as a ship's flight: its throttle, its autopilot, its
 * jump, the world below; nothing of a structure that is not its own.
 */
@FunctionalInterface
public interface IStructureReading {

    /** Tells {@code report} what there is to tell of {@code structure}, as the side the agent reads from has it. */
    void read(IStructureBody structure, AgentReport report);
}
