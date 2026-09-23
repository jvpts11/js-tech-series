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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * What a finished Operation earns, and for whom: the player who asked for it, or, when the scheduler took it on with
 * nobody acting (a job, a resume after a restart), whoever works the Mainframe that ran it.
 */
public final class OperationMilestones {

    private static final int LONG_AUTOCRAFT_STAGES = 10;

    private OperationMilestones() {
    }

    /** Reports one Operation the scheduler has just written down as finished. */
    public static void report(final BlockEntity mainframe, @Nullable final UUID askedBy, final String type,
                              final OperationRecord record) {
        final Optional<ServerPlayer> player = askedBy != null && mainframe.getLevel() instanceof ServerLevel level
                ? Optional.ofNullable(level.getServer().getPlayerList().getPlayer(askedBy))
                : MachineOperators.of(mainframe);
        player.ifPresent(who -> report(who, type, record));
    }

    private static void report(final ServerPlayer player, final String type, final OperationRecord record) {
        if (record.status() == OperationRecord.STATUS_PARTIAL) {
            JscEvents.award(player, JscEvents.OPERATION_PARTIAL);
        } else if (record.status() == OperationRecord.STATUS_RESOURCE_LOCKED) {
            JscEvents.award(player, JscEvents.OPERATION_LOCKED);
        }
        if (!record.completed()) {
            return;
        }
        if (ComputingOperations.SELECT.equals(type)) {
            JscEvents.award(player, JscEvents.SELECT_DONE);
        }
        final boolean crafted = ComputingOperations.CRAFT.equals(type);
        final boolean processed = ComputingOperations.PROCESSING.equals(type);
        if (crafted || processed || ComputingOperations.MULTI_STAGE.equals(type)) {
            JscEvents.award(player, JscEvents.AUTOCRAFT_DONE);
        }
        if (processed) {
            JscEvents.award(player, JscEvents.MACHINE_AUTOCRAFT);
        }
        if (crafted && record.subs().size() >= LONG_AUTOCRAFT_STAGES) {
            JscEvents.award(player, JscEvents.LONG_AUTOCRAFT);
        }
    }
}
