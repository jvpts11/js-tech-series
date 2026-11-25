/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.chain.machine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The machines a computer's lithography program works for, the ones whose peripheral port is linked to it
 * (computers/docs/design/survival.md): the Lithography Stations from the Legacy on, which it writes masks for, and the
 * Injection Moulders, which it cuts moulds for. The machines in the world are listed as they load and leave, so a
 * computer asks only them, never the blocks round it.
 */
public final class LithoStations {

    private static final Set<ChainMachineBlockEntity> LOADED = Collections.newSetFromMap(new WeakHashMap<>());

    private LithoStations() {
    }

    /** A machine that works through a computer came into the world; it is listed while it stays. */
    public static void loaded(final ChainMachineBlockEntity machine) {
        LOADED.add(machine);
    }

    /** A machine left the world. */
    public static void gone(final ChainMachineBlockEntity machine) {
        LOADED.remove(machine);
    }

    /** The formed machines in {@code level} whose peripheral port is linked to {@code computer}, by position. */
    public static List<ChainMachineBlockEntity> of(final ServerLevel level, final BlockEntity computer) {
        final List<ChainMachineBlockEntity> out = new ArrayList<>();
        for (final ChainMachineBlockEntity machine : List.copyOf(LOADED)) {
            if (machine.isRemoved() || machine.getLevel() != level || !machine.formed()) {
                continue;
            }
            if (LithoLink.computer(level, machine).filter(linked -> linked == computer).isPresent()) {
                out.add(machine);
            }
        }
        out.sort(Comparator.comparingLong(machine -> machine.getBlockPos().asLong()));
        return out;
    }
}
