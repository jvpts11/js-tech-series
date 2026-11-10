/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.villager;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.os.BoardFirmware;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.ProgramKind;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.media.InstallMedia;
import dev.jstech.computers.os.media.InstallerStacks;
import dev.jstech.computers.registry.HardwareRegistry;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.MaterialUses;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import org.jetbrains.annotations.Nullable;

/**
 * What the Programmer and the IT Technician trade (computers/docs/design/survival.md, "Villagers"). A villager's level
 * is the era of what it sells, Novice the Vintage to Master the Advanced, and it keeps the trades of the levels below.
 * The Programmer sells the era's systems and programs on its media and buys paper, books and blank media; the IT
 * Technician sells the era's parts (a board with its firmware written), its network cables in eights and its monitor,
 * and buys copper, tin and the parts of older eras. A price is the era's base times what is sold's factor, at most 64
 * emeralds; every trade has vanilla's twelve uses but a system, the rare one, has three. Each level offers two of its
 * trades, drawn as vanilla draws them, each a thing of the era's catalogue picked as the offer is made.
 */
public final class ComputerTrades {

    /* Each level's era's base price, Novice to Master. */
    private static final int[] BASE = {4, 8, 12, 20, 32};
    /* The experience a trade of each level gives the villager, as vanilla's do. */
    private static final int[] XP = {2, 10, 20, 30, 30};
    private static final int MOST = 64;
    private static final int USES = 12;
    private static final int SYSTEM_USES = 3;
    private static final float MULTIPLIER = 0.05F;
    private static final double PROGRAM = 1.0;
    private static final double SYSTEM = 2.0;
    private static final double PART = 1.0;
    private static final double CHIP = 1.5;
    private static final double CABLES = 0.25;
    private static final int CABLE_PACK = 8;
    private static final int PAPER = 24;
    private static final int BOOKS = 4;
    private static final int MEDIA = 4;
    private static final int COPPER = 10;
    private static final int TIN = 8;
    private static final int LEVELS = 5;
    private static final Set<HardwareRegistry.Kind> SOLD = Set.of(HardwareRegistry.Kind.PROCESSOR,
            HardwareRegistry.Kind.GRAPHICS_CARD, HardwareRegistry.Kind.MEMORY, HardwareRegistry.Kind.DISK,
            HardwareRegistry.Kind.MOTHERBOARD, HardwareRegistry.Kind.POWER_SUPPLY, HardwareRegistry.Kind.COOLER);
    private static final Map<Era, String> NETWORK_CABLE = Map.of(HardwareEra.VINTAGE, "thin_coax_cable",
            HardwareEra.LEGACY, "ethernet_cable", HardwareEra.TRANSITION, "cat5e_cable", HardwareEra.STANDARD,
            "gigabit_cable", HardwareEra.ADVANCED, "cat6a_cable");
    private static final Map<Era, String> MONITOR = Map.of(HardwareEra.VINTAGE, "vintage_monitor",
            HardwareEra.LEGACY, "legacy_monitor", HardwareEra.TRANSITION, "transition_monitor", HardwareEra.STANDARD,
            "monitor", HardwareEra.ADVANCED, "color_monitor");

    private ComputerTrades() {
    }

    /** Gives the two professions their trades, level by level, when the game asks every profession for its own. */
    public static void onTrades(final VillagerTradesEvent event) {
        if (event.getType() == ComputerVillagers.PROGRAMMER.get()) {
            programmer(event.getTrades());
        } else if (event.getType() == ComputerVillagers.IT_TECHNICIAN.get()) {
            technician(event.getTrades());
        }
    }

    /** The era of what a villager of {@code level} (1 Novice to 5 Master) sells. */
    public static Era eraOf(final int level) {
        return HardwareEra.WITH_PARTS.get(Math.clamp(level, 1, LEVELS) - 1);
    }

    /** What a thing of {@code factor} costs at {@code level}: the era's base times it, at least one, at most 64. */
    public static int price(final int level, final double factor) {
        return Math.clamp((int) Math.ceil(BASE[level - 1] * factor), 1, MOST);
    }

    private static void programmer(final Int2ObjectMap<List<VillagerTrades.ItemListing>> trades) {
        for (int level = 1; level <= LEVELS; level++) {
            final Era era = eraOf(level);
            final List<VillagerTrades.ItemListing> list = trades.get(level);
            list.add(sell(level, random -> pick(random, programs(era), InstallerStacks::forProgram), PROGRAM, USES));
            list.add(sell(level, random -> pick(random, systems(era), InstallerStacks::forSystem), SYSTEM,
                    SYSTEM_USES));
            list.add(buy(level, InstallerStacks.blank(InstallMedia.forProgram(era, ProgramKind.APP)), MEDIA));
            if (level == 1) {
                list.add(buy(level, Items.PAPER, PAPER));
                list.add(buy(level, Items.BOOK, BOOKS));
            }
        }
    }

    private static void technician(final Int2ObjectMap<List<VillagerTrades.ItemListing>> trades) {
        for (int level = 1; level <= LEVELS; level++) {
            final Era era = eraOf(level);
            final List<VillagerTrades.ItemListing> list = trades.get(level);
            final int at = level;
            list.add((trader, random) -> {
                final Item part = pickItem(random, parts(era));
                if (part == null) {
                    return null;
                }
                final double factor = chip(part) ? CHIP : PART;
                return offer(new ItemCost(Items.EMERALD, price(at, factor)), sold(part), USES, at);
            });
            list.add(sell(level, random -> new ItemStack(item(NETWORK_CABLE.get(era)), CABLE_PACK), CABLES, USES));
            list.add(sell(level, random -> new ItemStack(item(MONITOR.get(era))), PART, USES));
            if (level == 1) {
                list.add(buy(level, MaterialUses.item(Elements.of(Element.COPPER), Forms.INGOT).get(), COPPER));
                list.add(buy(level, MaterialUses.item(Elements.of(Element.TIN), Forms.INGOT).get(), TIN));
            } else {
                // Half its era's base for a part of an older era.
                list.add((trader, random) -> {
                    final Item old = pickItem(random, parts(eraOf(1 + random.nextInt(at - 1))));
                    return old == null ? null : offer(new ItemCost(old, 1), new ItemStack(Items.EMERALD,
                            Math.max(1, BASE[at - 1] / 2)), USES, at);
                });
            }
        }
    }

    /* A listing that sells what {@code made} picks, for its price at {@code level}. */
    private static VillagerTrades.ItemListing sell(final int level, final Function<RandomSource, ItemStack> made,
                                                   final double factor, final int uses) {
        return (trader, random) -> {
            final ItemStack sold = made.apply(random);
            return sold.isEmpty() ? null : offer(new ItemCost(Items.EMERALD, price(level, factor)), sold, uses, level);
        };
    }

    /* A listing that pays an emerald for {@code count} of {@code item}. */
    private static VillagerTrades.ItemListing buy(final int level, final ItemLike item, final int count) {
        return (trader, random) -> offer(new ItemCost(item, count), new ItemStack(Items.EMERALD), USES, level);
    }

    private static MerchantOffer offer(final ItemCost cost, final ItemStack result, final int uses, final int level) {
        return new MerchantOffer(cost, result, uses, XP[level - 1], MULTIPLIER);
    }

    /* The installable programs of {@code era}, by id. */
    private static List<ProgramSpec> programs(final Era era) {
        return OsRegistry.programs().stream().filter(program -> program.installable() && program.era() == era
                        && program.kind() != ProgramKind.DESKTOP_ENVIRONMENT)
                .sorted(Comparator.comparing(program -> program.id().toString())).toList();
    }

    /* The systems of {@code era}, by id. */
    private static List<OsDef> systems(final Era era) {
        return OsRegistry.oses().stream().filter(os -> HardwareEra.withParts(os.minEra()) == era)
                .sorted(Comparator.comparing(os -> os.id().toString())).toList();
    }

    /* The parts of {@code era} a technician sells or buys, by id. */
    private static List<Item> parts(final Era era) {
        return HardwareRegistry.parts().stream().filter(item -> HardwareRegistry.part(item)
                        instanceof HardwareRegistry.Part part && part.era() == era && SOLD.contains(part.kind()))
                .sorted(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString())).toList();
    }

    private static <T> ItemStack pick(final RandomSource random, final List<T> from,
                                      final Function<T, ItemStack> stack) {
        return from.isEmpty() ? ItemStack.EMPTY : stack.apply(from.get(random.nextInt(from.size())));
    }

    private static @Nullable Item pickItem(final RandomSource random, final List<Item> from) {
        return from.isEmpty() ? null : from.get(random.nextInt(from.size()));
    }

    /* A part as sold: a board with its firmware written, as a player's Preinstaller writes it. */
    private static ItemStack sold(final Item part) {
        final ItemStack stack = new ItemStack(part);
        return HardwareRegistry.board(stack) != null && BoardFirmware.blank(stack) ? BoardFirmware.written(stack)
                : stack;
    }

    /* Whether {@code part} is a processor or a graphics card, the dearer parts. */
    private static boolean chip(final Item part) {
        final HardwareRegistry.Part found = HardwareRegistry.part(part);
        return found != null && (found.kind() == HardwareRegistry.Kind.PROCESSOR
                || found.kind() == HardwareRegistry.Kind.GRAPHICS_CARD);
    }

    private static Item item(final String path) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, path));
    }
}
