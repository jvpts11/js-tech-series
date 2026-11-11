/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.advancement;

import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.os.QuinceStart;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.SeriesAxes;
import dev.jstech.core.progression.PlayerProgress;
import net.minecraft.server.level.ServerLevel;

/**
 * What a computer's parts can earn once they are in: a board with every RAM slot taken, and a working build of
 * an era. Read each time the installed parts change, which is a player's hand in the assembly, never a tick.
 */
public final class HardwareMilestones {

    private HardwareMilestones() {
    }

    /**
     * Credits the machine's operator with whatever its parts now amount to. A working build of an era also moves the
     * operator, and their team, to that era along J's Core's axis of hardware eras, forward only: assembling counts,
     * owning the parts does not, so other mods can gate what they offer on the era a player has built.
     */
    public static void report(final AbstractComputerBlockEntity machine) {
        if (machine.getLevel() == null || machine.getLevel().isClientSide()) {
            return;
        }
        final int slots = machine.boardRamSlots();
        if (slots > 0 && machine.installedRam() >= slots) {
            JscEvents.awardOperator(machine, JscEvents.RAM_FILLED);
        }
        final Era era = machine.installedEra();
        if (era != null && machine.buildValid()) {
            JscEvents.awardOperator(machine, JscEvents.ERA_BUILT, eraDetail(era));
            if (machine.quinceStart() != QuinceStart.NONE) {
                JscEvents.awardOperator(machine, JscEvents.QUINCE_BUILT);
            }
            if (machine.getLevel() instanceof ServerLevel level) {
                MachineOperators.idOf(machine).ifPresent(operator ->
                        PlayerProgress.reach(level.getServer(), operator, SeriesAxes.HARDWARE_ERA, era));
            }
        }
    }

    /** The detail {@link JscEvents#ERA_BUILT} carries for an era: its id. */
    public static String eraDetail(final Era era) {
        return era.id();
    }
}
