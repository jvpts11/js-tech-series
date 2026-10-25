/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.testkit.computers;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.hardware.CpuSpec;
import dev.jstech.computers.hardware.DiskSpec;
import dev.jstech.computers.hardware.IExpansionCardSpec;
import dev.jstech.computers.hardware.MotherboardSpec;
import dev.jstech.computers.hardware.PsuSpec;
import dev.jstech.computers.hardware.RamSpec;
import dev.jstech.computers.registry.ComputingContent;
import dev.jstech.computers.registry.HardwareRegistry;
import dev.jstech.core.jstechseries.tier.Era;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/**
 * The parts a machine of an era is built from, picked from the mod's own items the way a player picks them from the
 * era's creative tab: for a board, the strongest processor of its socket and era, the largest memory it takes, a card
 * of a kind that fits its slots; for an era, its strongest supply and its largest disk. No part list is written here,
 * so a part renamed or added is what the journeys build with. What a part is, is its entry in the hardware registry.
 */
public final class EraParts {

    private EraParts() {
    }

    /** The board in {@code board}'s spec. */
    public static MotherboardSpec board(final ItemLike board) {
        final MotherboardSpec spec = HardwareRegistry.board(new ItemStack(board));
        if (spec == null) {
            throw new IllegalArgumentException(BuiltInRegistries.ITEM.getKey(board.asItem()) + " is no board");
        }
        return spec;
    }

    /** The strongest processor that seats in {@code board}: its socket, its era, and no alien design. */
    public static Item cpuFor(final ItemLike board) {
        final MotherboardSpec spec = board(board);
        return best(item -> cpu(item) instanceof CpuSpec cpu && !cpu.alien()
                        && cpu.socket().equals(spec.socket()) && cpu.era() == spec.era(),
                item -> (long) cpu(item).cores() * cpu(item).freqMhz(), "a processor for " + name(board));
    }

    /** The largest memory module {@code board} takes: a generation it accepts, of its era. */
    public static Item ramFor(final ItemLike board) {
        final MotherboardSpec spec = board(board);
        return best(item -> ram(item) instanceof RamSpec ram && spec.acceptedRam().contains(ram.generation())
                        && ram.era() == spec.era(),
                item -> ram(item).bufferItems(), "memory for " + name(board));
    }

    /** The first card whose sheet is of {@code kind}, of {@code board}'s era, whose bus fits its slots. */
    public static Item cardFor(final ItemLike board, final Class<? extends IExpansionCardSpec> kind) {
        return cardFor(board, kind, item -> true);
    }

    /** The first card of {@code kind} of {@code board}'s era whose bus fits its slots and that passes {@code also}. */
    public static Item cardFor(final ItemLike board, final Class<? extends IExpansionCardSpec> kind,
                               final Predicate<Item> also) {
        final MotherboardSpec spec = board(board);
        final Optional<Item> card = items().filter(item -> kind.isInstance(card(item)) && also.test(item)
                && inEra(item, spec.era()) && spec.takes(card(item))).findFirst();
        return card.orElseThrow(() -> new IllegalStateException("no " + kind.getSimpleName() + " fits "
                + name(board)));
    }

    /** The strongest power supply on {@code era}'s creative tab. */
    public static Item psuFor(final Era era) {
        return best(item -> psu(item) != null && inEra(item, era), item -> psu(item).wattage(),
                "a supply of the " + era.serializedName() + " era");
    }

    /** The largest disk made for {@code era}. */
    public static Item diskFor(final Era era) {
        return best(item -> disk(item) instanceof DiskSpec disk && disk.era() == era,
                item -> disk(item).capacityItems(), "a disk of the " + era.serializedName() + " era");
    }

    /** The smallest disk made for {@code era}: the quickest an array of them is rebuilt on. */
    public static Item smallestDiskFor(final Era era) {
        return items().filter(item -> disk(item) instanceof DiskSpec disk && disk.era() == era)
                .min(Comparator.comparingLong(item -> disk(item).capacityItems()))
                .orElseThrow(() -> new IllegalStateException("no disk of the " + era.serializedName() + " era"));
    }

    /** A stack of one of {@code item}. */
    public static ItemStack one(final ItemLike item) {
        return new ItemStack(item);
    }

    /** Whether {@code item} sits on {@code era}'s creative tab, which is what tags it with the era. */
    public static boolean inEra(final Item item, final Era era) {
        final TagKey<Item> tag = TagKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, ComputingContent.eraTag(era)));
        return new ItemStack(item).is(tag);
    }

    private static CpuSpec cpu(final Item item) {
        return HardwareRegistry.cpu(new ItemStack(item));
    }

    private static RamSpec ram(final Item item) {
        return HardwareRegistry.ram(new ItemStack(item));
    }

    private static IExpansionCardSpec card(final Item item) {
        return HardwareRegistry.card(new ItemStack(item));
    }

    private static PsuSpec psu(final Item item) {
        return HardwareRegistry.psu(new ItemStack(item));
    }

    private static DiskSpec disk(final Item item) {
        return HardwareRegistry.disk(item);
    }

    private static Item best(final Predicate<Item> fits, final ToLongFunction<Item> score, final String what) {
        return items().filter(fits).max(Comparator.comparingLong(score))
                .orElseThrow(() -> new IllegalStateException("no " + what));
    }

    private static Stream<Item> items() {
        return StreamSupport.stream(BuiltInRegistries.ITEM.spliterator(), false)
                .filter(item -> JsComputers.MODID.equals(BuiltInRegistries.ITEM.getKey(item).getNamespace()));
    }

    private static String name(final ItemLike item) {
        return BuiltInRegistries.ITEM.getKey(item.asItem()).toString();
    }
}
