/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.testkit;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.hardware.CpuSpec;
import dev.jstech.computers.hardware.MotherboardSpec;
import dev.jstech.computers.item.CpuItem;
import dev.jstech.computers.item.DiskItem;
import dev.jstech.computers.item.IExpansionCardItem;
import dev.jstech.computers.item.MotherboardItem;
import dev.jstech.computers.item.PsuItem;
import dev.jstech.computers.item.RamItem;
import dev.jstech.computers.registry.ComputingContent;
import dev.jstech.core.tier.HardwareEra;
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
 * so a part renamed or added is what the journeys build with.
 */
public final class EraParts {

    private EraParts() {
    }

    /** The board in {@code board}'s spec. */
    public static MotherboardSpec board(final ItemLike board) {
        if (!(board.asItem() instanceof MotherboardItem item)) {
            throw new IllegalArgumentException(BuiltInRegistries.ITEM.getKey(board.asItem()) + " is no board");
        }
        return item.spec();
    }

    /** The strongest processor that seats in {@code board}: its socket, its era, and no alien design. */
    public static Item cpuFor(final ItemLike board) {
        final MotherboardSpec spec = board(board);
        return best(item -> item instanceof CpuItem cpu && !cpu.spec().alien()
                        && cpu.spec().socket().equals(spec.socket()) && cpu.spec().era() == spec.era(),
                item -> {
                    final CpuSpec cpu = ((CpuItem) item).spec();
                    return (long) cpu.cores() * cpu.freqMhz();
                }, "a processor for " + name(board));
    }

    /** The largest memory module {@code board} takes: a generation it accepts, of its era. */
    public static Item ramFor(final ItemLike board) {
        final MotherboardSpec spec = board(board);
        return best(item -> item instanceof RamItem ram && spec.acceptedRam().contains(ram.spec().generation())
                        && ram.spec().era() == spec.era(),
                item -> ((RamItem) item).spec().bufferItems(), "memory for " + name(board));
    }

    /** The first card of {@code kind} of {@code board}'s era whose bus fits its slots. */
    public static Item cardFor(final ItemLike board, final Class<? extends IExpansionCardItem> kind) {
        return cardFor(board, kind, item -> true);
    }

    /** The first card of {@code kind} of {@code board}'s era whose bus fits its slots and that passes {@code also}. */
    public static Item cardFor(final ItemLike board, final Class<? extends IExpansionCardItem> kind,
                               final Predicate<Item> also) {
        final MotherboardSpec spec = board(board);
        final Optional<Item> card = items().filter(item -> kind.isInstance(item) && also.test(item)
                && inEra(item, spec.era()) && ((IExpansionCardItem) item).cardSpec().fits(spec.pcieGeneration()))
                .findFirst();
        return card.orElseThrow(() -> new IllegalStateException("no " + kind.getSimpleName() + " fits "
                + name(board)));
    }

    /** The strongest power supply on {@code era}'s creative tab. */
    public static Item psuFor(final HardwareEra era) {
        return best(item -> item instanceof PsuItem && inEra(item, era), item -> ((PsuItem) item).spec().wattage(),
                "a supply of the " + era.serializedName() + " era");
    }

    /** The largest disk made for {@code era}. */
    public static Item diskFor(final HardwareEra era) {
        return best(item -> item instanceof DiskItem disk && disk.spec().era() == era,
                item -> ((DiskItem) item).spec().capacityItems(), "a disk of the " + era.serializedName() + " era");
    }

    /** A stack of one of {@code item}. */
    public static ItemStack one(final ItemLike item) {
        return new ItemStack(item);
    }

    /** Whether {@code item} sits on {@code era}'s creative tab, which is what tags it with the era. */
    public static boolean inEra(final Item item, final HardwareEra era) {
        final TagKey<Item> tag = TagKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, ComputingContent.eraTag(era)));
        return new ItemStack(item).is(tag);
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
