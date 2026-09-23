/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.advancement;

import dev.jstech.computers.operation.ComputingOperations;
import dev.jstech.computers.operation.payload.OperationRecord;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * What a finished Operation earns, and for whom: the player who asked for it, or, when the scheduler took it on with
 * nobody acting (a job), whoever works the Mainframe that ran it.
 *
 * <p>Who asked is saved with the Operation, so one resumed after a restart is still theirs. An Operation can take far
 * longer than a player stays, so what it earns someone who has logged off waits for their next login.
 */
public final class OperationMilestones {

    private static final int LONG_AUTOCRAFT_STAGES = 10;

    private OperationMilestones() {
    }

    /** Reports one Operation the scheduler has just written down as finished. */
    public static void report(final BlockEntity mainframe, @Nullable final UUID askedBy, final String type,
                              final OperationRecord record) {
        final List<String> earned = earned(type, record);
        if (earned.isEmpty() || !(mainframe.getLevel() instanceof ServerLevel level)) {
            return;
        }
        final Optional<UUID> who = askedBy != null ? Optional.of(askedBy) : MachineOperators.idOf(mainframe);
        who.ifPresent(id -> earned.forEach(event -> JscEvents.award(level.getServer(), id, event, "")));
    }

    /** The events a settled Operation of that type reports, in the order they are earned. */
    private static List<String> earned(final String type, final OperationRecord record) {
        final List<String> out = new ArrayList<>();
        if (record.status() == OperationRecord.STATUS_PARTIAL) {
            out.add(JscEvents.OPERATION_PARTIAL);
        } else if (record.status() == OperationRecord.STATUS_RESOURCE_LOCKED) {
            out.add(JscEvents.OPERATION_LOCKED);
        }
        if (!record.completed()) {
            return out;
        }
        if (ComputingOperations.SELECT.equals(type)) {
            out.add(JscEvents.SELECT_DONE);
        }
        final boolean crafted = ComputingOperations.CRAFT.equals(type);
        final boolean processed = ComputingOperations.PROCESSING.equals(type);
        if (crafted || processed || ComputingOperations.MULTI_STAGE.equals(type)) {
            out.add(JscEvents.AUTOCRAFT_DONE);
        }
        if (processed) {
            out.add(JscEvents.MACHINE_AUTOCRAFT);
        }
        if (crafted && record.subs().size() >= LONG_AUTOCRAFT_STAGES) {
            out.add(JscEvents.LONG_AUTOCRAFT);
        }
        return out;
    }
}
