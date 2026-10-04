/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.item;

import dev.jstech.computers.crafting.CraftingPattern;
import dev.jstech.computers.hardware.CraftingCardSpec;
import dev.jstech.computers.hardware.IExpansionCardSpec;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.List;

/**
 * A Crafting Card component item: the expansion card a Crafting Computer needs to execute recipes. It drives a number
 * of Crafting Interfaces by its era, and its own ROM keeps the bench recipes the computer crafts, which travel with
 * the card: a card moved to another computer brings them along.
 */
@TextHolder
public class CraftingCardItem extends SpecItem<CraftingCardSpec> implements IExpansionCardItem {

    private static final TextKey PURPOSE = TextKey.of("jsc.item.crafting_card.purpose", "Crafting accelerator (FPGA)");
    private static final TextKey THREADS_ONE =
            TextKey.of("jsc.item.crafting_card.threads_one", "Threads  -  %s thread");
    private static final TextKey THREADS_MANY =
            TextKey.of("jsc.item.crafting_card.threads_many", "Threads  -  %s threads");
    private static final TextKey THROUGHPUT = TextKey.of("jsc.item.crafting_card.throughput", "Throughput  -  %sx CPU");
    private static final TextKey DRIVES = TextKey.of("jsc.item.crafting_card.drives",
            "Drives %s Crafting Interfaces");
    private static final TextKey ROM = TextKey.of("jsc.item.crafting_card.rom", "ROM  -  %s of %s bench recipes");
    private static final TextKey ROM_ENTRY = TextKey.of("jsc.item.crafting_card.rom_entry", "  - %s");
    /** The most recipes the tooltip names before it says how many more there are. */
    private static final int LISTED = 8;
    private static final TextKey MORE = TextKey.of("jsc.item.crafting_card.more", "  and %s more");

    public CraftingCardItem(final Properties properties, final CraftingCardSpec spec) {
        super(properties, spec);
    }

    /** The bench recipes the ROM of the card in {@code stack} keeps, in the order they were loaded. */
    public static List<CraftingPattern> rom(final ItemStack stack) {
        final List<CraftingPattern> held = stack.get(ComputingComponents.RECIPE_ROM.get());
        return held == null ? List.of() : held;
    }

    /** How many bench recipes the card in {@code stack} keeps at most, 0 for anything that is not a card. */
    public static int romSize(final ItemStack stack) {
        return stack.getItem() instanceof CraftingCardItem card ? card.spec().romSize() : 0;
    }

    /**
     * Writes {@code pattern} into the ROM of the card in {@code stack}: false when it is full or keeps the same
     * recipe already. The stack is changed in place.
     */
    public static boolean load(final ItemStack stack, final CraftingPattern pattern) {
        final List<CraftingPattern> held = rom(stack);
        if (held.size() >= romSize(stack)) {
            return false;
        }
        for (final CraftingPattern existing : held) {
            if (existing.sameRecipe(pattern)) {
                return false;
            }
        }
        final List<CraftingPattern> next = new ArrayList<>(held);
        next.add(pattern);
        stack.set(ComputingComponents.RECIPE_ROM.get(), List.copyOf(next));
        return true;
    }

    /** Erases the recipe at {@code index} from the ROM of the card in {@code stack}; false when there is none. */
    public static boolean remove(final ItemStack stack, final int index) {
        final List<CraftingPattern> held = rom(stack);
        if (index < 0 || index >= held.size()) {
            return false;
        }
        final List<CraftingPattern> next = new ArrayList<>(held);
        next.remove(index);
        if (next.isEmpty()) {
            stack.remove(ComputingComponents.RECIPE_ROM.get());
        } else {
            stack.set(ComputingComponents.RECIPE_ROM.get(), List.copyOf(next));
        }
        return true;
    }

    @Override
    public IExpansionCardSpec cardSpec() {
        return spec();
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        final CraftingCardSpec spec = spec();
        tooltip.add(GameText.component(PURPOSE).withStyle(ChatFormatting.GRAY));
        tooltip.add(GameText.component((spec.threads() == 1 ? THREADS_ONE : THREADS_MANY).with(spec.threads()))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(GameText.component(THROUGHPUT.with(spec.cpuFactor())).withStyle(ChatFormatting.GRAY));
        tooltip.add(GameText.component(DRIVES.with(spec.interfaces())).withStyle(ChatFormatting.GRAY));
        final List<CraftingPattern> held = rom(stack);
        tooltip.add(GameText.component(ROM.with(held.size(), spec.romSize())).withStyle(ChatFormatting.GRAY));
        for (int i = 0; i < Math.min(LISTED, held.size()); i++) {
            tooltip.add(GameText.component(ROM_ENTRY.with(held.get(i).displayText()))
                    .withStyle(ChatFormatting.GOLD));
        }
        if (held.size() > LISTED) {
            tooltip.add(GameText.component(MORE.with(held.size() - LISTED)).withStyle(ChatFormatting.GOLD));
        }
        tooltip.add(GameText.component(HardwareTooltip.TIER_POWER_BUS.with(spec.tier(), spec.tdpWatts(), spec.bus()))
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
