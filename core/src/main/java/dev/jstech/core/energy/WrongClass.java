/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.energy;

/**
 * What befalls a port that meets a live line of another class than its own, as its energy's rule says. Energy flows
 * between a line and a port of another class only when the rule says nothing befalls it.
 */
public enum WrongClass {

    /** Nothing: the port takes any class, as if it were its own. */
    NONE,
    /** The block does not start, and says it lacks the voltage; nothing is harmed. */
    UNDERVOLTAGE,
    /** The block burns and stops, and stays damaged until it is repaired. */
    BURNS,
    /** An arc flash: the block is destroyed, sets fire around it and burns whoever is close. */
    ARC_FLASH;

    /** Whether the port works on the line as on its own class. */
    public boolean works() {
        return this == NONE;
    }
}
