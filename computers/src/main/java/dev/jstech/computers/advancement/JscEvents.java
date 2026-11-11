/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.advancement;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.RackUnitHost;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.core.advancement.Advancements;
import net.minecraft.core.BlockPos;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
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
    public static final String PSU_FAILED = "psu_failed";
    public static final String THERMAL_TRIP = "thermal_trip";
    /** A running machine carried through a power loss on a UPS's battery. */
    public static final String UPS_CARRIED = "ups_carried";
    /** A cooler made with thermal paste fitted over a processor. */
    public static final String PASTE_FITTED = "paste_fitted";
    /** Every fan seat of a case holding an Advanced case fan. */
    public static final String ADVANCED_FANS = "advanced_fans";
    /** One unit of a redundant power supply failing while its machine stays on. */
    public static final String PSU_UNIT_LOST = "psu_unit_lost";
    public static final String QUINCE_BUILT = "quince_built";
    public static final String BOOK_PRINTED = "book_printed";

    // The chain
    /** Detail: the machine's family, its id or the one its kind shares ({@code ChainMachines.familyOf}). */
    public static final String CHAIN_FORMED = "chain_formed";
    /** Detail: the machine's family, as for {@link #CHAIN_FORMED}. */
    public static final String CHAIN_MADE = "chain_made";
    /** Detail: the era's name in lower case, of the layer a Lithography Station exposed. */
    public static final String LAYER_EXPOSED = "layer_exposed";
    public static final String MASK_WRITTEN = "mask_written";
    public static final String FIRMWARE_FLASHED = "firmware_flashed";

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
    /** Detail: the path of the id of the program the port installed. */
    public static final String BUILT_FROM_PORTS = "built_from_ports";
    public static final String PORTABLE_UNLOCKED = "portable_unlocked";
    /** A system checking its disk as it starts, after a sudden shutdown. */
    public static final String DISK_CHECKED = "disk_checked";
    /** A system installed from a medium found in an abandoned building. */
    public static final String FOUND_SYSTEM = "found_system";
    public static final String PROGRAMMER_TRADE = "programmer_trade";
    /** An installer disc copied with Easy Disc Creator. */
    public static final String INSTALLER_COPIED = "installer_copied";

    // Networks and Operations
    public static final String MAINFRAME_NETWORK = "mainframe_network";
    public static final String COMPUTER_JOINED = "computer_joined";
    /** Two networks federated through their WAN Gateway Computers. */
    public static final String WAN_JOINED = "wan_joined";
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
    /** A computer or a portable linked through a wireless access point. */
    public static final String ACCESS_POINT_JOINED = "access_point_joined";
    public static final String PORTABLE_REQUEST = "portable_request";
    public static final String PHOTO_SENT = "photo_sent";
    public static final String VEIN_FOUND = "vein_found";
    public static final String MESSAGE_SENT = "message_sent";
    public static final String NETWORK_BOOT = "network_boot";

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
    public static final String BREAKPOINT = "breakpoint";

    // Teracoin
    public static final String WALLET_MADE = "wallet_made";
    public static final String BLOCK_FOUND = "block_found";
    public static final String POOL_JOINED = "pool_joined";
    /** Every riser of a Mining Computer holding a card. */
    public static final String RISERS_FILLED = "risers_filled";
    public static final String ASIC_MINING = "asic_mining";
    /** TRC sent to an address another player made. */
    public static final String TRC_SENT = "trc_sent";
    /** 1,000 TRC held in one wallet for seven game days with nothing sent from it. */
    public static final String HODL = "hodl";
    public static final String PRINTED_3D = "printed_3d";
    /** The block reward halving while the player is on the server. */
    public static final String HALVING = "halving";

    public static final String HALT_STACK = "stack";
    public static final String HALT_DIVIDE = "divide";

    private JscEvents() {
    }

    /** Reports that {@code player} caused {@code event}; nothing happens for nobody, or for a machine acting as one. */
    public static void award(@Nullable final Player player, final String event) {
        award(player, event, "");
    }

    /**
     * The same, with the detail that tells apart the criteria of an advancement asking for all of something. The
     * event is reported to the Core's trigger under this mod's namespace, {@code jsc:<event>}.
     */
    public static void award(@Nullable final Player player, final String event, @Nullable final String detail) {
        Advancements.award(player, ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, event), detail);
    }

    /** Reports an event a machine caused on its own, crediting whoever works it. */
    public static void awardOperator(@Nullable final BlockEntity machine, final String event) {
        awardOperator(machine, event, "");
    }

    /** The same, with a detail. */
    public static void awardOperator(@Nullable final BlockEntity machine, final String event,
                                     @Nullable final String detail) {
        if (machine != null && machine.getLevel() instanceof ServerLevel level) {
            MachineOperators.idOf(machine).ifPresent(id -> award(level.getServer(), id, event, detail));
        }
    }

    /** Reports an event caused by whoever is acting in the current {@link Acting} scope, if anybody is. */
    public static void awardActing(@Nullable final Level level, final String event, @Nullable final String detail) {
        final MinecraftServer server = level == null ? null : level.getServer();
        if (server != null) {
            Acting.current().ifPresent(id -> award(server, id, event, detail));
        }
    }

    /**
     * Reports that the player with that id caused an event: given at once when they are here, and kept for their
     * next login when they are not, since what a machine finishes on its own often finishes while they are away.
     */
    public static void award(final MinecraftServer server, final UUID player, final String event,
                             @Nullable final String detail) {
        final ServerPlayer here = server.getPlayerList().getPlayer(player);
        if (here != null) {
            award(here, event, detail);
        } else {
            PendingAwards.keep(server, player, event, detail == null ? "" : detail);
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
