/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.item;

import dev.jstech.computers.hardware.ClusterInterfaceCardSpec;
import dev.jstech.computers.hardware.CpuSpec;
import dev.jstech.computers.hardware.CraftingCardSpec;
import dev.jstech.computers.hardware.DiskSpec;
import dev.jstech.computers.hardware.GpuSpec;
import dev.jstech.computers.hardware.IExpansionCardSpec;
import dev.jstech.computers.hardware.MotherboardSpec;
import dev.jstech.computers.hardware.NetworkCardSpec;
import dev.jstech.computers.hardware.PhiCoprocessorSpec;
import dev.jstech.computers.hardware.PsuSpec;
import dev.jstech.computers.hardware.RamSpec;
import dev.jstech.computers.hardware.SoundCardSpec;
import dev.jstech.computers.hardware.StorageCardSpec;
import dev.jstech.computers.hardware.WorkshopCardSpec;
import dev.jstech.computers.registry.HardwareRegistry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * The tooltip of every part, whichever mod's item it is: what its sheet says, read from the entry that makes the item
 * a part. The lines go right under the item's name, where an item's own lines go.
 */
public final class PartTooltips {

    private PartTooltips() {
    }

    /** Adds a part's lines to its tooltip; the mod's listener on the game's event, on both sides. */
    public static void onTooltip(final ItemTooltipEvent event) {
        final List<Component> lines = lines(event.getItemStack());
        if (!lines.isEmpty()) {
            final List<Component> tooltip = event.getToolTip();
            tooltip.addAll(Math.min(1, tooltip.size()), lines);
        }
    }

    /** The lines the tooltip of {@code stack} gets for the part it is; none for a stack that is no part. */
    public static List<Component> lines(final ItemStack stack) {
        final HardwareRegistry.Part part = HardwareRegistry.part(stack.isEmpty() ? null : stack.getItem());
        if (part == null) {
            return List.of();
        }
        final List<Component> lines = new ArrayList<>();
        switch (part.spec()) {
            case CpuSpec cpu -> ProcessorTooltip.append(cpu, lines);
            case GpuSpec gpu -> GraphicsCardTooltip.append(gpu, lines);
            case RamSpec ram -> MemoryTooltip.append(ram, lines);
            case DiskSpec disk -> DiskTooltip.append(disk, stack, lines);
            case MotherboardSpec board -> MotherboardTooltip.append(board, lines);
            case PsuSpec psu -> PowerSupplyTooltip.append(psu, lines);
            case IExpansionCardSpec card -> card(card, part, stack, lines);
            default -> {
                // A kind of sheet no tooltip is written for says nothing.
            }
        }
        return lines;
    }

    private static void card(final IExpansionCardSpec card, final HardwareRegistry.Part part, final ItemStack stack,
                             final List<Component> lines) {
        switch (card) {
            case SoundCardSpec sound -> SoundCardTooltip.append(sound, lines);
            case NetworkCardSpec network -> NetworkCardTooltip.append(network, lines);
            case StorageCardSpec storage -> StorageCardTooltip.append(storage, stack, lines);
            case CraftingCardSpec crafting -> CraftingCardTooltip.append(crafting, stack, lines);
            case WorkshopCardSpec workshop -> WorkshopCardTooltip.append(workshop, lines);
            case ClusterInterfaceCardSpec cluster -> ClusterCardTooltip.append(cluster, lines);
            case PhiCoprocessorSpec phi -> CoprocessorTooltip.append(phi, lines);
            default -> PlainCardTooltip.append(card, part.era(), lines);
        }
    }
}
