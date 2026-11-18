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
 * The Lithography Stations a computer works for, the ones from the Legacy on whose peripheral port is linked to it
 * (computers/docs/design/survival.md): what its lithography program shows and writes masks on. The stations in the
 * world are listed as they load and leave, so a computer asks only them, never the blocks round it.
 */
public final class LithoStations {

    private static final Set<WaferMachineBlockEntity> LOADED = Collections.newSetFromMap(new WeakHashMap<>());

    private LithoStations() {
    }

    /** A station that works through a computer came into the world; it is listed while it stays. */
    public static void loaded(final WaferMachineBlockEntity station) {
        LOADED.add(station);
    }

    /** A station left the world. */
    public static void gone(final WaferMachineBlockEntity station) {
        LOADED.remove(station);
    }

    /** The formed stations in {@code level} whose peripheral port is linked to {@code computer}, by position. */
    public static List<WaferMachineBlockEntity> of(final ServerLevel level, final BlockEntity computer) {
        final List<WaferMachineBlockEntity> out = new ArrayList<>();
        for (final WaferMachineBlockEntity station : List.copyOf(LOADED)) {
            if (station.isRemoved() || station.getLevel() != level || !station.formed()) {
                continue;
            }
            if (LithoLink.computer(level, station).filter(linked -> linked == computer).isPresent()) {
                out.add(station);
            }
        }
        out.sort(Comparator.comparingLong(station -> station.getBlockPos().asLong()));
        return out;
    }
}
