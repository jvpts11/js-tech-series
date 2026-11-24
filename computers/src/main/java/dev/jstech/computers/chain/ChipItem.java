/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.chain;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A chip the Packaging Line cuts from a wafer: a memory, controller, processor, graphics, hashing or mobile chip. A
 * chip of a class ({@link ChipClasses}) says its class in its name, "Memory Chip (Advanced, Class A)", by a sentence of
 * its own beside its plain name, so each language words it its way.
 */
public final class ChipItem extends Item {

    /** What a classed chip's name key adds to its plain one. */
    public static final String CLASSED = ".classed";

    public ChipItem(final Properties properties) {
        super(properties);
    }

    /**
     * A classed chip's name in English, from its plain one: the class goes inside its parentheses, "Memory Chip
     * (Advanced)" becoming "Memory Chip (Advanced, Class %s)", the letter given when it is said.
     */
    public static String classedEnglish(final String english) {
        final int close = english.lastIndexOf(')');
        return close < 0 ? english + " (Class %s)"
                : english.substring(0, close) + ", Class %s" + english.substring(close);
    }

    @Override
    public Component getName(final ItemStack stack) {
        final int grade = ChipClasses.of(stack);
        return grade == 0 ? super.getName(stack)
                : Component.translatable(getDescriptionId(stack) + CLASSED, ChipClasses.letter(grade));
    }
}
