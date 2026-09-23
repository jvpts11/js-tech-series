/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.advancement;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.RackUnitHost;
import dev.jstech.computers.os.IOsHost;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jetbrains.annotations.Nullable;

/**
 * The events the mod's advancements are earned by, and the one way the code reports one.
 *
 * <p>Each id is stable: an advancement names it in its data, so renaming one silently unhooks the advancement. The
 * datagen reads these same constants, which is what keeps the two from drifting apart.
 *
 * <p>An event is credited to the player who caused it. When the code that sees the event holds that player (a
 * payload handler, a block being used) it reports them directly; when it does not (a scheduler, a tick, a process)
 * it reports the machine, and the machine's operator is credited, see {@link MachineOperators}.
 */
public final class JscEvents {

    // Hardware
    public static final String POST_PASSED = "post_passed";
    public static final String POWER_CYCLED = "power_cycled";
    public static final String RAM_FILLED = "ram_filled";
    public static final String FLOPPY_SAVED = "floppy_saved";
    public static final String BATTLESTATION = "battlestation";
    /** Detail: the era's name in lower case. */
    public static final String ERA_BUILT = "era_built";
    public static final String RACK_FILLED = "rack_filled";
    public static final String DATACENTER_FORMED = "datacenter_formed";
    public static final String CLUSTER_RUN = "cluster_run";
    public static final String SUPERCOMPUTER_ONLINE = "supercomputer_online";

    // Operating systems
    public static final String CLOCKWORK = "clockwork";
    public static final String FIRMWARE_SETUP = "firmware_setup";
    public static final String MAN_PAGE = "man_page";
    public static final String MIRROR_INSTALL = "mirror_install";
    public static final String TASK_ENDED = "task_ended";
    public static final String REMOTE_CONTROL = "remote_control";
    public static final String DUAL_BOOT = "dual_boot";
    public static final String SYSTEM_ERASED = "system_erased";
    public static final String MINESWEEPER_EXPERT = "minesweeper_expert";
    /** Detail: the path of the id of the system it ran on, such as {@code ubuntu}. */
    public static final String SCREENFETCH = "screenfetch";

    // Networks and Operations
    public static final String MAINFRAME_NETWORK = "mainframe_network";
    public static final String COMPUTER_JOINED = "computer_joined";
    public static final String SELECT_DONE = "select_done";
    public static final String IQL_QUERY = "iql_query";
    public static final String IQL_JOB = "iql_job";
    public static final String PATTERN_ENCODED = "pattern_encoded";
    public static final String AUTOCRAFT_DONE = "autocraft_done";
    public static final String MACHINE_AUTOCRAFT = "machine_autocraft";
    public static final String BUSES_PAIRED = "buses_paired";
    public static final String LONG_AUTOCRAFT = "long_autocraft";
    public static final String OPERATION_PARTIAL = "operation_partial";
    public static final String OPERATION_LOCKED = "operation_locked";
    public static final String MAINFRAME_CONFLICT = "mainframe_conflict";
    public static final String MAINFRAME_CUT = "mainframe_cut";
    public static final String COMPUTERCRAFT_MESSAGE = "computercraft_message";

    // Sigma
    /** Detail: the installed program's id. */
    public static final String PROGRAM_INSTALLED = "program_installed";
    public static final String SIGMA_RUN = "sigma_run";
    public static final String SIGMA_COMPILE_ERROR = "sigma_compile_error";
    /** Detail: {@link #HALT_STACK} or {@link #HALT_DIVIDE}. */
    public static final String SIGMA_HALTED = "sigma_halted";
    public static final String SIGMA_THREAD = "sigma_thread";
    public static final String SIGMA_WINDOW = "sigma_window";
    public static final String SIGMA_WATCH = "sigma_watch";
    public static final String SIGMA_TWO_MACHINES = "sigma_two_machines";
    public static final String SIGMA_OPERATION = "sigma_operation";
    public static final String SIGMA_PUBLISHED = "sigma_published";
    public static final String SIGMA_ON_DOS = "sigma_on_dos";
    public static final String SIGMA_UPTIME = "sigma_uptime";

    public static final String HALT_STACK = "stack";
    public static final String HALT_DIVIDE = "divide";

    private JscEvents() {
    }

    /** Reports that {@code player} caused {@code event}; nothing happens for nobody, or for a machine acting as one. */
    public static void award(@Nullable final Player player, final String event) {
        award(player, event, "");
    }

    /** The same, with the detail that tells apart the criteria of an advancement asking for all of something. */
    public static void award(@Nullable final Player player, final String event, @Nullable final String detail) {
        if (player instanceof ServerPlayer server && !(player instanceof FakePlayer)) {
            ComputingModule.EVENT.get().trigger(server, event, detail == null ? "" : detail);
        }
    }

    /** Reports an event a machine caused on its own, crediting whoever works it. */
    public static void awardOperator(@Nullable final BlockEntity machine, final String event) {
        awardOperator(machine, event, "");
    }

    /** The same, with a detail. */
    public static void awardOperator(@Nullable final BlockEntity machine, final String event,
                                     @Nullable final String detail) {
        MachineOperators.of(machine).ifPresent(player -> award(player, event, detail));
    }

    /** Reports an event caused by whoever is acting in the current {@link Acting} scope, if anybody is. */
    public static void awardActing(@Nullable final Level level, final String event, @Nullable final String detail) {
        final MinecraftServer server = level == null ? null : level.getServer();
        if (server != null) {
            Acting.current().map(id -> server.getPlayerList().getPlayer(id))
                    .ifPresent(player -> award(player, event, detail));
        }
    }

    /** Reports an event of a machine seen through its operating system, whether a computer or a rack's unit. */
    public static void awardHost(@Nullable final IOsHost host, final String event, @Nullable final String detail) {
        if (host instanceof BlockEntity machine) {
            awardOperator(machine, event, detail);
        } else if (host instanceof RackUnitHost unit) {
            awardOperator(unit.rack(), event, detail);
        }
    }

    /** Reports an event of the machine standing at {@code pos}, crediting whoever works it. */
    public static void awardOperatorAt(final Level level, final BlockPos pos, final String event) {
        awardOperator(level.getBlockEntity(pos), event, "");
    }

    /** The same, with a detail. */
    public static void awardOperatorAt(final Level level, final BlockPos pos, final String event,
                                       @Nullable final String detail) {
        awardOperator(level.getBlockEntity(pos), event, detail);
    }
}
