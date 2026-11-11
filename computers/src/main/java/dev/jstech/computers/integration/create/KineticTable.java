/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.integration.create;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.machine.IqlStateTables;
import dev.jstech.core.jstechseries.network.NetworkSystem;
import dev.jstech.core.jstechseries.network.StateRow;
import dev.jstech.core.jstechseries.network.StateTable;
import dev.jstech.core.jstechseries.network.StateTables;
import dev.jstech.core.machine.IMachineParameters;
import dev.jstech.core.util.Loaded;
import dev.jstech.core.uuid.NetworkUuid;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * Create's kinetic networks as a table of the network's language (computers/docs/design/implementation.md, "Create"):
 * a row for every Create block a computer of the network reads through the industrial control cable, with its speed,
 * the stress its kinetic network carries and the capacity it has, and, on a Rotation Speed Controller, the speed it
 * aims at, which an {@code UPDATE create_kinetics SET target_speed = ...} sets. It names no class of Create's: the
 * blocks are read through the machine parameters the Create side gives them.
 */
public final class KineticTable {

    /** The table's id's path, how a query names it. */
    public static final String NAME = "create_kinetics";
    public static final String BLOCK = "block";
    public static final String SPEED = "speed";
    public static final String STRESS = "stress";
    public static final String CAPACITY = "capacity";
    public static final String OVERSTRESSED = "overstressed";
    public static final String TARGET_SPEED = "target_speed";

    private static volatile @Nullable StateTable table;

    private KineticTable() {
    }

    /** Declares the table and where its rows come from, while the game loads, when Create is met. */
    static void declare() {
        final StateTable declared = StateTables.declare(StateTable.builder(
                        ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, NAME), CreateTexts.KINETICS)
                .text(BLOCK, CreateTexts.COLUMN_BLOCK)
                .number(SPEED, CreateTexts.SPEED, "RPM")
                .number(STRESS, CreateTexts.STRESS, "su")
                .number(CAPACITY, CreateTexts.CAPACITY, "su")
                .flag(OVERSTRESSED, CreateTexts.COLUMN_OVERSTRESSED)
                .number(TARGET_SPEED, CreateTexts.TARGET_SPEED, "RPM")
                .build());
        table = declared;
        IqlStateTables.rowsFrom(declared, KineticTable::rows);
    }

    /** Whether the table is there: Create is met, of a version J's Computers is tested against. */
    public static boolean declared() {
        return table != null;
    }

    /**
     * Every Create block a computer of {@code network} reads through the industrial control cable, with the values it
     * gives; none without Create.
     */
    public static List<Read> read(final ServerLevel level, final NetworkUuid network) {
        final List<Read> out = new ArrayList<>();
        if (table == null) {
            return out;
        }
        for (final AbstractComputerBlockEntity computer : computersOf(level, network)) {
            for (final long at : computer.linkedEndpoints()) {
                final IMachineParameters values = level.getCapability(IMachineParameters.CAPABILITY, BlockPos.of(at),
                        null);
                if (values != null && values.parameter(CAPACITY).isPresent()) {
                    out.add(new Read(computer, at, values));
                }
            }
        }
        return out;
    }

    /** The row of the table a block read has, as a query sees it; only while the table is there. */
    public static StateRow row(final ServerLevel level, final Read read) {
        final StateTable declared = table;
        if (declared == null) {
            throw new IllegalStateException("the create_kinetics table is only there with Create");
        }
        final IMachineParameters values = read.values();
        final double stress = values.read(STRESS).orElse(0);
        final double capacity = values.read(CAPACITY).orElse(0);
        final StateRow.Writer row = new StateRow.Writer(declared)
                .set(BLOCK, level.getBlockState(BlockPos.of(read.at())).getBlock().getName().getString())
                .set(SPEED, values.read(SPEED).orElse(0))
                .set(STRESS, stress)
                .set(CAPACITY, capacity)
                .set(OVERSTRESSED, stress > capacity);
        final OptionalDouble target = values.read(TARGET_SPEED);
        if (target.isPresent()) {
            row.set(TARGET_SPEED, target.getAsDouble());
        }
        return row.done(read.computer().nodeUuid(), read.at());
    }

    /* The table's rows: each block read, by the computer that reads it. */
    private static List<StateRow> rows(final ServerLevel level, final NetworkUuid network) {
        final List<StateRow> rows = new ArrayList<>();
        for (final Read read : read(level, network)) {
            rows.add(row(level, read));
        }
        return rows;
    }

    /* The network's computers that may read machines: its Mainframe, its Personal and its Crafting Computers. */
    private static List<AbstractComputerBlockEntity> computersOf(final ServerLevel level, final NetworkUuid network) {
        final NetworkSystem system = NetworkSystem.get(level);
        final List<Long> places = new ArrayList<>();
        system.mainframePositionOf(network).ifPresent(places::add);
        system.personalComputersOf(network).forEach(pc -> places.add(pc.pos()));
        system.craftingComputersOf(network).forEach(cc -> places.add(cc.pos()));
        final List<AbstractComputerBlockEntity> out = new ArrayList<>();
        for (final long at : places) {
            if (Loaded.blockEntity(level, BlockPos.of(at)) instanceof AbstractComputerBlockEntity computer) {
                out.add(computer);
            }
        }
        return out;
    }

    /** A Create block a computer reads: the computer, where the block stands and its values. */
    public record Read(AbstractComputerBlockEntity computer, long at, IMachineParameters values) {
    }
}
