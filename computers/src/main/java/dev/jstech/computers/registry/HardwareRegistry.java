/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.registry;

import dev.jstech.computers.hardware.CpuSpec;
import dev.jstech.computers.hardware.DiskSpec;
import dev.jstech.computers.hardware.GpuSpec;
import dev.jstech.computers.hardware.IExpansionCardSpec;
import dev.jstech.computers.hardware.MotherboardSpec;
import dev.jstech.computers.hardware.PsuSpec;
import dev.jstech.computers.hardware.RamSpec;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.util.DeclarationError;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

/**
 * The hardware every mod brings, by the item that is the part: an item is a processor, a graphics card, a memory
 * module, a disk, a motherboard, a power supply or an expansion card because an entry here says so, with the sheet of
 * what it is. This mod's own parts are declared through the register event as an add-on's are, so the way in is the
 * one that always runs; the entries close once the game has loaded, and a lookup is one read of a map by the item.
 */
public final class HardwareRegistry {

    private static final Map<Item, Part> DECLARED = new IdentityHashMap<>();
    /* Settled once the game has loaded: the same entries, safe to read from any thread. */
    private static volatile @Nullable Map<Item, Part> settled;

    private HardwareRegistry() {
    }

    /** Declares {@code item} a processor, while the game loads. */
    public static void processor(final ItemLike item, final CpuSpec spec) {
        put(item, new Part(Kind.PROCESSOR, spec, spec.era()));
    }

    /** Declares {@code item} a graphics card, while the game loads. */
    public static void graphicsCard(final ItemLike item, final GpuSpec spec) {
        put(item, new Part(Kind.GRAPHICS_CARD, spec, spec.era()));
    }

    /** Declares {@code item} a memory module, while the game loads. */
    public static void memory(final ItemLike item, final RamSpec spec) {
        put(item, new Part(Kind.MEMORY, spec, spec.era()));
    }

    /** Declares {@code item} a disk, while the game loads. */
    public static void disk(final ItemLike item, final DiskSpec spec) {
        put(item, new Part(Kind.DISK, spec, spec.era()));
    }

    /** Declares {@code item} a motherboard, while the game loads. */
    public static void motherboard(final ItemLike item, final MotherboardSpec spec) {
        put(item, new Part(Kind.MOTHERBOARD, spec, spec.era()));
    }

    /**
     * Declares {@code item} a power supply, while the game loads. A supply has no era of its own, since any supply
     * goes in any computer; {@code soldIn} is the era it is shown with.
     */
    public static void powerSupply(final ItemLike item, final PsuSpec spec, final Era soldIn) {
        put(item, new Part(Kind.POWER_SUPPLY, spec, soldIn));
    }

    /**
     * Declares {@code item} an expansion card, while the game loads: {@code era} is the era it is shown with, or null
     * for a card every era shares.
     */
    public static void expansionCard(final ItemLike item, final IExpansionCardSpec spec, final @Nullable Era era) {
        if (spec instanceof GpuSpec gpu) {
            graphicsCard(item, gpu);
            return;
        }
        put(item, new Part(Kind.EXPANSION_CARD, spec, era));
    }

    /** The processor {@code stack} is, or null when it is none. */
    public static @Nullable CpuSpec cpu(final ItemStack stack) {
        return of(stack, Kind.PROCESSOR, CpuSpec.class);
    }

    /** The graphics card {@code stack} is, or null when it is none. */
    public static @Nullable GpuSpec gpu(final ItemStack stack) {
        return of(stack, Kind.GRAPHICS_CARD, GpuSpec.class);
    }

    /** The memory module {@code stack} is, or null when it is none. */
    public static @Nullable RamSpec ram(final ItemStack stack) {
        return of(stack, Kind.MEMORY, RamSpec.class);
    }

    /** The disk {@code stack} is, or null when it is none. */
    public static @Nullable DiskSpec disk(final ItemStack stack) {
        return of(stack, Kind.DISK, DiskSpec.class);
    }

    /** The disk {@code item} is, or null when it is none. */
    public static @Nullable DiskSpec disk(final Item item) {
        return of(item, Kind.DISK, DiskSpec.class);
    }

    /** The motherboard {@code stack} is, or null when it is none. */
    public static @Nullable MotherboardSpec board(final ItemStack stack) {
        return of(stack, Kind.MOTHERBOARD, MotherboardSpec.class);
    }

    /** The power supply {@code stack} is, or null when it is none. */
    public static @Nullable PsuSpec psu(final ItemStack stack) {
        return of(stack, Kind.POWER_SUPPLY, PsuSpec.class);
    }

    /** The expansion card {@code stack} is, a graphics card among them, or null when it is none. */
    public static @Nullable IExpansionCardSpec card(final ItemStack stack) {
        final Part part = part(stack.isEmpty() ? null : stack.getItem());
        return part != null && part.kind().card() ? (IExpansionCardSpec) part.spec() : null;
    }

    /** What part {@code item} is, or null when it is no part. */
    public static @Nullable Part part(final @Nullable Item item) {
        if (item == null) {
            return null;
        }
        final Map<Item, Part> entries = settled;
        if (entries != null) {
            return entries.get(item);
        }
        synchronized (DECLARED) {
            return DECLARED.get(item);
        }
    }

    /** Every item that is a part. */
    public static Set<Item> parts() {
        final Map<Item, Part> entries = settled;
        if (entries != null) {
            return entries.keySet();
        }
        synchronized (DECLARED) {
            return Set.copyOf(DECLARED.keySet());
        }
    }

    /** Closes the entries once the game has loaded; from the mod. */
    public static void freeze() {
        synchronized (DECLARED) {
            settled = Map.copyOf(DECLARED);
        }
    }

    private static void put(final ItemLike like, final Part part) {
        Objects.requireNonNull(like, "item");
        Objects.requireNonNull(part.spec(), "spec");
        final Item item = like.asItem();
        final String id = BuiltInRegistries.ITEM.getKey(item).toString();
        final String mod = id.substring(0, id.indexOf(':'));
        synchronized (DECLARED) {
            if (settled != null) {
                throw DeclarationError.of(mod, "the part " + id, "is declared after the game has loaded",
                        "declare it from the register event, while the game loads");
            }
            final Part before = DECLARED.putIfAbsent(item, part);
            if (before != null) {
                throw DeclarationError.of(mod, "the part " + id, "is declared twice, as a " + before.kind().word()
                        + " and as a " + part.kind().word(), "declare each item as one part");
            }
        }
    }

    private static <S> @Nullable S of(final ItemStack stack, final Kind kind, final Class<S> type) {
        return stack.isEmpty() ? null : of(stack.getItem(), kind, type);
    }

    private static <S> @Nullable S of(final Item item, final Kind kind, final Class<S> type) {
        final Part part = part(item);
        return part != null && part.kind() == kind ? type.cast(part.spec()) : null;
    }

    /** What kind of part an item is. */
    public enum Kind {
        PROCESSOR("processor"),
        GRAPHICS_CARD("graphics card"),
        MEMORY("memory module"),
        DISK("disk"),
        MOTHERBOARD("motherboard"),
        POWER_SUPPLY("power supply"),
        EXPANSION_CARD("expansion card");

        private final String word;

        Kind(final String word) {
            this.word = word;
        }

        /** Whether a part of this kind sits in an expansion slot. */
        public boolean card() {
            return this == GRAPHICS_CARD || this == EXPANSION_CARD;
        }

        /** The kind as a sentence names it. */
        public String word() {
            return word;
        }
    }

    /**
     * One part: its kind, its sheet, and the era it is shown with.
     *
     * @param kind what kind of part it is
     * @param spec its sheet: a {@link CpuSpec} for a processor, and so on
     * @param era  the era it belongs to, or, for a supply, the era it was sold in; null for a card every era shares
     */
    public record Part(Kind kind, Object spec, @Nullable Era era) {
    }
}
