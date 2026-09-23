/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.item;

import dev.jstech.computers.rack.RaidMode;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A gadget that goes into one of a bay's gadget slots on the rack's front panel, changing how that
 * bay's drives behave: a RAID Controller aggregates them into one logical volume, a Cache Card cuts
 * read latency. A gadget serves the machine mounted in its row and nothing else.
 */
@TextHolder
public class RackGadgetItem extends Item {

    /** What a rack gadget does for its bay. */
    public enum Kind {
        /** Aggregates the bay's drives into a single logical volume (mode set on the gadget). */
        RAID_CONTROLLER,
        /** Cuts the read latency of queries served by the bay's drives. */
        CACHE_CARD
    }

    private final Kind kind;

    /** The read-latency cut a Cache Card gives its bay, in percent. */
    public static final int CACHE_LATENCY_CUT_PERCENT = 25;

    private static final TextKey RAID_TOOLTIP = TextKey.of("item.jsc.raid_controller.tooltip",
            "Bay gadget: joins the bay's drives into one volume");
    private static final TextKey CACHE_TOOLTIP = TextKey.of("item.jsc.cache_card.tooltip",
            "Bay gadget: cuts read latency on the bay's drives");
    private static final TextKey UNCONFIGURED = TextKey.of("item.jsc.raid_controller.unconfigured",
            "Unconfigured - the bay's drives stay independent");
    private static final TextKey RAID_NEEDS = TextKey.of("item.jsc.raid_controller.needs",
            "%s - needs %s+ drives");
    private static final TextKey CACHE_CUT = TextKey.of("item.jsc.cache_card.cut", "-%s%% read latency in its bay");

    public RackGadgetItem(final Properties properties, final Kind kind) {
        super(properties.stacksTo(1));
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    /** The gadget kind of a stack, or null when the stack is not a rack gadget. */
    @Nullable
    public static Kind kindOf(final ItemStack stack) {
        return stack.getItem() instanceof RackGadgetItem gadget ? gadget.kind() : null;
    }

    /** The RAID mode configured on a controller stack ({@link RaidMode#NONE} when unset). */
    public static RaidMode raidMode(final ItemStack stack) {
        if (kindOf(stack) != Kind.RAID_CONTROLLER) {
            return RaidMode.NONE;
        }
        // An unset or unknown mode degrades to independent volumes.
        return RaidMode.byName(stack.get(ComputingComponents.RAID_MODE.get()));
    }

    /** Writes a RAID mode onto a controller stack (a no-op for any other item). */
    public static void setRaidMode(final ItemStack stack, final RaidMode mode) {
        if (kindOf(stack) == Kind.RAID_CONTROLLER) {
            stack.set(ComputingComponents.RAID_MODE.get(), mode.serializedName());
        }
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        if (kind == Kind.RAID_CONTROLLER) {
            tooltip.add(GameText.component(RAID_TOOLTIP).withStyle(ChatFormatting.GRAY));
            final RaidMode mode = raidMode(stack);
            tooltip.add(GameText.component(mode == RaidMode.NONE ? UNCONFIGURED.text()
                            : RAID_NEEDS.with(Text.literal(mode.name()), mode.minDrives()))
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(GameText.component(CACHE_TOOLTIP).withStyle(ChatFormatting.GRAY));
            tooltip.add(GameText.component(CACHE_CUT.with(CACHE_LATENCY_CUT_PERCENT))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
