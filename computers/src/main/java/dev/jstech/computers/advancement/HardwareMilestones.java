/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.advancement;

import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.core.tier.HardwareEra;

/**
 * What a computer's parts can earn once they are in: a board with every RAM slot taken, and a working build of
 * an era. Read each time the installed parts change, which is a player's hand in the assembly, never a tick.
 */
public final class HardwareMilestones {

    private HardwareMilestones() {
    }

    /** Credits the machine's operator with whatever its parts now amount to. */
    public static void report(final AbstractComputerBlockEntity machine) {
        if (machine.getLevel() == null || machine.getLevel().isClientSide()) {
            return;
        }
        final int slots = machine.boardRamSlots();
        if (slots > 0 && machine.installedRam() >= slots) {
            JscEvents.awardOperator(machine, JscEvents.RAM_FILLED);
        }
        final HardwareEra era = machine.installedEra();
        if (era != null && machine.buildValid()) {
            JscEvents.awardOperator(machine, JscEvents.ERA_BUILT, eraDetail(era));
        }
    }

    /** The detail {@link JscEvents#ERA_BUILT} carries for an era. */
    public static String eraDetail(final HardwareEra era) {
        return era.serializedName();
    }
}
