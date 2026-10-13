/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.item;

import dev.jstech.computers.hardware.ExpansionCardKind;
import dev.jstech.computers.hardware.IExpansionCardSpec;
import dev.jstech.computers.hardware.StorageCardSpec;
import dev.jstech.computers.rack.RaidMode;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

/**
 * A storage card, seated in a server's expansion slot: the Vaultis Array joins the server's drives into one volume, in
 * the RAID mode set in the server's firmware and kept on the card itself, so the card carries its array's settings
 * wherever it goes; the Vaultis Cache cuts the read latency of the server's drives.
 */
@TextHolder
public class StorageCardItem extends SpecItem<StorageCardSpec> implements IExpansionCardItem {

    private static final TextKey ARRAY = TextKey.of("jsc.item.storage_card.array",
            "RAID controller: joins its server's drives into one volume");
    private static final TextKey CACHE = TextKey.of("jsc.item.storage_card.cache",
            "Read cache: answers its server's drives faster");
    private static final TextKey UNCONFIGURED = TextKey.of("jsc.item.storage_card.unconfigured",
            "No array set: the drives stay separate (set one in the server's firmware)");
    private static final TextKey NEEDS = TextKey.of("jsc.item.storage_card.needs", "%s  -  needs %s drives or more");
    private static final TextKey CUT = TextKey.of("jsc.item.storage_card.cut", "-%s%% read latency on its drives");
    private static final TextKey SLOT = TextKey.of("jsc.item.storage_card.slot", "%s slot  -  %s W");

    public StorageCardItem(final Properties properties, final StorageCardSpec spec) {
        super(properties.stacksTo(1), spec);
    }

    /** The kind of storage card in {@code stack}, or null when it holds none. */
    @Nullable
    public static ExpansionCardKind kindOf(final ItemStack stack) {
        return stack.getItem() instanceof StorageCardItem card ? card.spec().kind() : null;
    }

    /** How much of its drives' read latency the card in {@code stack} takes away, 0 for anything but a cache. */
    public static int readCutPercent(final ItemStack stack) {
        return stack.getItem() instanceof StorageCardItem card ? card.spec().readCutPercent() : 0;
    }

    /** The RAID mode set on the controller in {@code stack}, {@link RaidMode#NONE} when unset or not a controller. */
    public static RaidMode raidMode(final ItemStack stack) {
        if (kindOf(stack) != ExpansionCardKind.RAID_CONTROLLER) {
            return RaidMode.NONE;
        }
        // An unset or unknown mode degrades to independent volumes.
        return RaidMode.byName(stack.get(ComputingComponents.RAID_MODE.get()));
    }

    /** Writes a RAID mode onto the controller in {@code stack}; nothing for any other item. */
    public static void setRaidMode(final ItemStack stack, final RaidMode mode) {
        if (kindOf(stack) == ExpansionCardKind.RAID_CONTROLLER) {
            stack.set(ComputingComponents.RAID_MODE.get(), mode.serializedName());
        }
    }

    @Override
    public IExpansionCardSpec cardSpec() {
        return spec();
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        final StorageCardSpec spec = spec();
        if (spec.kind() == ExpansionCardKind.RAID_CONTROLLER) {
            tooltip.add(GameText.component(ARRAY).withStyle(ChatFormatting.GRAY));
            final RaidMode mode = raidMode(stack);
            tooltip.add(GameText.component(mode == RaidMode.NONE ? UNCONFIGURED.text()
                            : NEEDS.with(Text.literal(mode.name()), mode.minDrives()))
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(GameText.component(CACHE).withStyle(ChatFormatting.GRAY));
            tooltip.add(GameText.component(CUT.with(spec.readCutPercent())).withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(GameText.component(SLOT.with(spec.bus().label(), spec.tdpWatts()))
                .withStyle(ChatFormatting.DARK_GRAY));
        HardwareTooltip.appendEra(tooltip, spec.era());
    }
}
