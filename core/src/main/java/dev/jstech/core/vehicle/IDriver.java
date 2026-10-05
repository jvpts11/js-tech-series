/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.vehicle;

/**
 * Something other than a player that drives a vehicle it sits in the front seat of: a robot, a program's driver. The
 * vehicle asks it every tick, on the server.
 */
@FunctionalInterface
public interface IDriver {

    /** What it asks of the vehicle this tick. */
    DriverInput driverInput();
}
