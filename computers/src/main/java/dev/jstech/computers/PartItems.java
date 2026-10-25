/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers;

import dev.jstech.computers.hardware.CpuSpec;
import dev.jstech.computers.hardware.DiskSpec;
import dev.jstech.computers.hardware.GpuSpec;
import dev.jstech.computers.hardware.IExpansionCardSpec;
import dev.jstech.computers.hardware.MotherboardSpec;
import dev.jstech.computers.hardware.PsuSpec;
import dev.jstech.computers.hardware.RamSpec;
import dev.jstech.computers.registry.ComputingContent;
import dev.jstech.computers.registry.ComputingContent.Shelf;
import dev.jstech.computers.registry.OwnHardware;
import dev.jstech.core.content.ItemBuilder;
import dev.jstech.core.jstechseries.tier.Era;
import java.util.function.Function;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

/**
 * How this mod's parts are declared onto its content: each a plain item on the components shelf of its era's tab,
 * which the register event then makes the part its sheet says, as an add-on's item is made one.
 */
final class PartItems {

    private PartItems() {
    }

    static ItemBuilder<Item> cpu(final String id, final CpuSpec spec) {
        OwnHardware.add(id, (event, item) -> event.processor(item, spec));
        return onShelf(id, spec.era(), Item::new);
    }

    static ItemBuilder<Item> ram(final String id, final RamSpec spec) {
        OwnHardware.add(id, (event, item) -> event.memory(item, spec));
        return onShelf(id, spec.era(), Item::new);
    }

    static ItemBuilder<Item> gpu(final String id, final GpuSpec spec) {
        OwnHardware.add(id, (event, item) -> event.graphicsCard(item, spec));
        return onShelf(id, spec.era(), Item::new);
    }

    /* An expansion card of {@code era}, or of every era for null. */
    static ItemBuilder<Item> card(final String id, final IExpansionCardSpec spec, final @Nullable Era era) {
        return card(id, spec, era, Item.DEFAULT_MAX_STACK_SIZE);
    }

    /* An expansion card that stacks only to {@code maxStack}. */
    static ItemBuilder<Item> card(final String id, final IExpansionCardSpec spec, final @Nullable Era era,
                                  final int maxStack) {
        OwnHardware.add(id, (event, item) -> event.expansionCard(item, spec, era));
        return onShelf(id, era, properties -> new Item(properties.stacksTo(maxStack)));
    }

    /* A supply, which carries no era of its own: it is shown with the machines of the age it sold in. */
    static ItemBuilder<Item> psu(final String id, final Era soldIn, final PsuSpec spec) {
        OwnHardware.add(id, (event, item) -> event.powerSupply(item, spec, soldIn));
        return onShelf(id, soldIn, Item::new);
    }

    static ItemBuilder<Item> board(final String id, final MotherboardSpec spec) {
        OwnHardware.add(id, (event, item) -> event.motherboard(item, spec));
        return onShelf(id, spec.era(), Item::new);
    }

    static ItemBuilder<Item> disk(final String id, final DiskSpec spec) {
        OwnHardware.add(id, (event, item) -> event.disk(item, spec));
        return onShelf(id, spec.era(), Item::new);
    }

    /* A thing of {@code era}, or of every era for null, on the components shelf of that era's tab. */
    static <T extends Item> ItemBuilder<T> onShelf(final String id, final @Nullable Era era,
                                                   final Function<Item.Properties, T> factory) {
        return ComputingContent.CONTENT.item(id, factory).tab(ComputingContent.shelf(Shelf.COMPONENTS, era));
    }
}
