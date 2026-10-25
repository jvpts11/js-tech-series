/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.registry;

import dev.jstech.computers.block.IEraChassisBlock;
import dev.jstech.computers.block.SmallComputerBlock;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.hardware.CaseParts;
import dev.jstech.computers.hardware.ComputerDef;
import dev.jstech.computers.rack.RackChassis;
import dev.jstech.core.util.DeclarationError;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * The computers every mod brings: a block is a computer, and an item a server, because an entry here says so, with
 * what it is. This mod's own computers are declared through the register event as an add-on's are, so the way in is
 * the one that always runs; the entries close once the game has loaded, and a lookup is one read of a map.
 */
public final class ComputerRegistry {

    private static final Map<Block, ComputerDef> COMPUTERS = new IdentityHashMap<>();
    private static final Map<Item, RackChassis> SERVERS = new IdentityHashMap<>();
    /* Settled once the game has loaded: the same entries, safe to read from any thread. */
    private static volatile @Nullable Map<Block, ComputerDef> settledComputers;
    private static volatile @Nullable Map<Item, RackChassis> settledServers;

    private ComputerRegistry() {
    }

    /**
     * Declares {@code block} a computer, while the game loads. A block that knows its own era, as every computer's
     * case does, has to be of the entry's.
     *
     * @throws DeclarationError when the block is declared already, or the game has loaded, or the eras differ
     */
    public static void computer(final Block block, final ComputerDef computer) {
        Objects.requireNonNull(block, "block");
        Objects.requireNonNull(computer, "computer");
        final ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        if (block instanceof IEraChassisBlock chassis && chassis.chassisEra() != computer.era()) {
            throw DeclarationError.of(id.getNamespace(), "the computer " + id, "is declared of the era "
                    + computer.era().id() + " while its block is of " + chassis.chassisEra().id(),
                    "declare it of its block's era");
        }
        final CaseParts parts = computer.parts();
        if (parts.processors() == CaseParts.AS_THE_BOARD || parts.memory() == CaseParts.AS_THE_BOARD
                || parts.cards() == CaseParts.AS_THE_BOARD || parts.disks() == CaseParts.AS_THE_BOARD) {
            throw DeclarationError.of(id.getNamespace(), "the computer " + id,
                    "leaves a count of its parts to the board", "count every part its case has room for");
        }
        if (block instanceof SmallComputerBlock && !fitsTheScreen(parts)) {
            throw DeclarationError.of(id.getNamespace(), "the computer " + id,
                    "counts more parts than the Personal Computer's screen it is assembled on has room for",
                    "count no more than " + PersonalComputerBlockEntity.PARTS);
        }
        synchronized (COMPUTERS) {
            requireOpen(settledComputers, id, "computer");
            if (COMPUTERS.putIfAbsent(block, computer) != null) {
                throw DeclarationError.of(id.getNamespace(), "the computer " + id, "is declared twice",
                        "declare it once");
            }
        }
    }

    /**
     * Declares {@code item} a server built in {@code chassis}, while the game loads.
     *
     * @throws DeclarationError when the item is declared already, or the game has loaded
     */
    public static void server(final ItemLike item, final RackChassis chassis) {
        Objects.requireNonNull(item, "item");
        Objects.requireNonNull(chassis, "chassis");
        final Item server = item.asItem();
        final ResourceLocation id = BuiltInRegistries.ITEM.getKey(server);
        synchronized (SERVERS) {
            requireOpen(settledServers, id, "server");
            if (SERVERS.putIfAbsent(server, chassis) != null) {
                throw DeclarationError.of(id.getNamespace(), "the server " + id, "is declared twice",
                        "declare it once");
            }
        }
    }

    /** What computer {@code block} is, or null when it is none. */
    public static @Nullable ComputerDef computer(final @Nullable Block block) {
        if (block == null) {
            return null;
        }
        final Map<Block, ComputerDef> entries = settledComputers;
        if (entries != null) {
            return entries.get(block);
        }
        synchronized (COMPUTERS) {
            return COMPUTERS.get(block);
        }
    }

    /**
     * What computer {@code block} is, for a block entity that is one.
     *
     * @throws IllegalStateException when the block was never declared a computer
     */
    public static ComputerDef require(final Block block) {
        final ComputerDef computer = computer(block);
        if (computer == null) {
            throw new IllegalStateException("the block " + BuiltInRegistries.BLOCK.getKey(block)
                    + " runs a computer's block entity but was never declared a computer");
        }
        return computer;
    }

    /** The chassis of the server {@code item} is, or null when it is no server. */
    public static @Nullable RackChassis chassis(final @Nullable Item item) {
        if (item == null) {
            return null;
        }
        final Map<Item, RackChassis> entries = settledServers;
        if (entries != null) {
            return entries.get(item);
        }
        synchronized (SERVERS) {
            return SERVERS.get(item);
        }
    }

    /** Every block that is a computer. */
    public static Set<Block> computers() {
        final Map<Block, ComputerDef> entries = settledComputers;
        if (entries != null) {
            return entries.keySet();
        }
        synchronized (COMPUTERS) {
            return Set.copyOf(COMPUTERS.keySet());
        }
    }

    /** Every item that is a server. */
    public static Set<Item> servers() {
        final Map<Item, RackChassis> entries = settledServers;
        if (entries != null) {
            return entries.keySet();
        }
        synchronized (SERVERS) {
            return Set.copyOf(SERVERS.keySet());
        }
    }

    /** Closes the entries once the game has loaded; from the mod. */
    public static void freeze() {
        synchronized (COMPUTERS) {
            settledComputers = Map.copyOf(COMPUTERS);
        }
        synchronized (SERVERS) {
            settledServers = Map.copyOf(SERVERS);
        }
    }

    /* Whether a small computer's case counts no more than the screen it is assembled on has a place for. */
    private static boolean fitsTheScreen(final CaseParts parts) {
        final CaseParts room = PersonalComputerBlockEntity.PARTS;
        return parts.processors() <= room.processors() && parts.memory() <= room.memory()
                && parts.cards() <= room.cards() && parts.disks() <= room.disks() && parts.bays() <= room.bays()
                && parts.storage() <= room.storage();
    }

    private static void requireOpen(final @Nullable Map<?, ?> settled, final ResourceLocation id, final String what) {
        if (settled != null) {
            throw DeclarationError.of(id.getNamespace(), "the " + what + " " + id,
                    "is declared after the game has loaded",
                    "declare it from the register event, while the game loads");
        }
    }
}
