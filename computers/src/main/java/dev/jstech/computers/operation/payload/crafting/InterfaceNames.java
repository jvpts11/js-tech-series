/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload.crafting;

import dev.jstech.computers.block.part.CraftingInterfacePart;
import dev.jstech.computers.chain.machine.ChainMachineBlockEntity;
import dev.jstech.computers.crafting.CraftingFloor;
import dev.jstech.core.content.IMenuTitle;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.util.Loaded;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * What a Crafting Interface is called in every window: the name a player gave it; else the machine it feeds, a formed
 * machine by its own name and not by the port the interface faces, with the machine's place when another interface on
 * the same crafting cable feeds a different machine of the same kind; else, facing no machine, its kind and its place.
 * Made once for a window's worth of names, since telling two of a kind apart looks at every interface on the cable.
 */
public final class InterfaceNames {

    private final ServerLevel level;
    @Nullable
    private final CraftingFloor floor;
    /* The machines the cable's interfaces feed, by the block their terminal is, to tell two of a kind apart. */
    private final Map<Block, Set<BlockPos>> machinesOfKind = new HashMap<>();

    private InterfaceNames(final ServerLevel level, @Nullable final CraftingFloor floor) {
        this.level = level;
        this.floor = floor;
        if (floor != null) {
            for (final CraftingFloor.Site site : floor.interfaces()) {
                final BlockPos machine = floor.machineFed(site);
                if (machine != null) {
                    machinesOfKind.computeIfAbsent(level.getBlockState(machine).getBlock(), kind -> new HashSet<>())
                            .add(machine);
                }
            }
        }
    }

    /** The names of the interfaces on {@code floor}; with none, each is named by its kind and place alone. */
    public static InterfaceNames of(final ServerLevel level, @Nullable final CraftingFloor floor) {
        return new InterfaceNames(level, floor);
    }

    /** What the interface {@code part} on {@code site} is called. */
    public Text title(final CraftingInterfacePart part, @Nullable final CraftingFloor.Site site) {
        if (!part.name().isEmpty()) {
            return Text.literal(part.name());
        }
        final BlockPos machine = floor == null || site == null ? null : floor.machineFed(site);
        if (machine == null) {
            final BlockPos at = site == null ? BlockPos.ZERO : site.cable();
            return CraftTexts.INTERFACE_AT.with(GameText.of(part.partItem().getHoverName()), at.getX(), at.getY(),
                    at.getZ());
        }
        final Text name = machineName(machine);
        final Set<BlockPos> ofKind = machinesOfKind.get(level.getBlockState(machine).getBlock());
        return ofKind != null && ofKind.size() > 1
                ? CraftTexts.INTERFACE_AT.with(name, machine.getX(), machine.getY(), machine.getZ()) : name;
    }

    /*
     * A machine's own name: what its menu opens under when it says, a chain machine's name rather than its terminal
     * block's ("Plasma Etcher", not "Plasma Etcher Terminal"), else its block's.
     */
    private Text machineName(final BlockPos machine) {
        final BlockEntity entity = Loaded.blockEntity(level, machine);
        if (entity instanceof IMenuTitle titled) {
            return GameText.of(titled.menuTitle());
        }
        if (entity instanceof ChainMachineBlockEntity chain) {
            return chain.machine().nameKey().text();
        }
        return GameText.of(level.getBlockState(machine).getBlock().getName());
    }
}
