/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block.part;

import dev.jstech.computers.bus.BusAbilities;
import dev.jstech.computers.bus.BusCondition;
import dev.jstech.computers.operation.payload.BusEditPayload;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.id.StableIds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Applies a change made in a bus's window to the bus, by hand, so it clears any program's mark on that setting. A
 * change the bus's era cannot be set to is refused by the bus itself, as it is when software asks for it.
 */
public final class BusEdits {

    /** How far a priority goes either way. */
    public static final int MOST_PRIORITY = 99;
    /** The most a keep, a max or a stock count is set to. */
    public static final int MOST_COUNT = 1_000_000;

    private BusEdits() {
    }

    /** Applies {@code edit} to {@code bus}, with {@code carried} the item on the player's cursor; whether it took. */
    public static boolean apply(final AbstractBusPart bus, final BusEditPayload edit, final ItemStack carried) {
        final int slot = edit.slot();
        final long value = edit.value();
        // A step past any count there is would wrap the sum round to below zero, so it is held to the largest one.
        final long step = Math.max(-MOST_COUNT, Math.min(MOST_COUNT, value));
        return switch (edit.op()) {
            case BusEditPayload.FILTER_SLOT -> bus.setFilterSlot(slot, carried, "");
            case BusEditPayload.ADD_ITEM -> !carried.isEmpty() && bus.firstEmptyFilterSlot() >= 0
                    && bus.setFilterSlot(bus.firstEmptyFilterSlot(), carried, "");
            case BusEditPayload.EXCLUDE -> bus.setExclude(value != 0L, "");
            case BusEditPayload.KEEP -> bus.setKeep(count(bus.keep() + step), "");
            case BusEditPayload.MAX -> bus.setMax(count(bus.max() + step), "");
            case BusEditPayload.ITEM_KEEP -> inFilter(slot)
                    && bus.setItemQuantities(slot, count(bus.itemKeep(slot) + step), bus.itemMax(slot), "");
            case BusEditPayload.ITEM_MAX -> inFilter(slot)
                    && bus.setItemQuantities(slot, bus.itemKeep(slot), count(bus.itemMax(slot) + step), "");
            case BusEditPayload.PRIORITY -> bus.setPriority((int) Math.max(-MOST_PRIORITY,
                    Math.min(MOST_PRIORITY, bus.priority() + step)), "");
            case BusEditPayload.MODE -> bus.setMode(value == 1L ? AbstractBusPart.MODE_REDSTONE
                    : AbstractBusPart.MODE_CONTINUOUS, "");
            case BusEditPayload.POWER -> bus.setPowered(value != 0L, "");
            case BusEditPayload.MATCH -> bus.setFuzzy(value != 0L, "");
            case BusEditPayload.ADD_TAG -> {
                final ResourceLocation id = tag(edit.text());
                yield id != null && bus.addTag(id, "");
            }
            case BusEditPayload.REMOVE_TAG -> bus.removeTag(slot, "");
            case BusEditPayload.ADD_CONDITION -> {
                final BusCondition condition = condition(edit);
                yield condition != null && bus.addCondition(condition, "");
            }
            case BusEditPayload.REMOVE_CONDITION -> bus.removeCondition(slot, "");
            case BusEditPayload.ACCESS -> bus instanceof ExternalStorageBusPart external
                    && external.setAccess((int) value, "");
            default -> false;
        };
    }

    /**
     * The condition a change asks for, or null when it names nothing that can be waited for: an item or a tag that
     * is not there, a count under one, an hour off the clock, a bus with no name.
     */
    @Nullable
    public static BusCondition condition(final BusEditPayload edit) {
        final BusCondition.Kind kind = StableIds.of(BusCondition.Kind.class).find(edit.slot());
        if (kind == null) {
            return null;
        }
        final String text = edit.text().trim();
        return switch (kind) {
            case STOCK -> {
                if (edit.value() < 1L) {
                    yield null;
                }
                if (text.startsWith("#")) {
                    final ResourceLocation id = tag(text);
                    yield id == null ? null : BusCondition.stock("#" + id, Math.min(MOST_COUNT, edit.value()));
                }
                final StorageKey key = StorageKey.byName(text);
                yield key == null ? null
                        : BusCondition.stock(key.registryId().toString(), Math.min(MOST_COUNT, edit.value()));
            }
            case HOURS -> {
                final long from = edit.value() / 24L;
                final long to = edit.value() % 24L;
                yield edit.value() < 0L || from > 23L ? null : BusCondition.hours((int) from, (int) to);
            }
            case AFTER -> text.isEmpty() || text.length() > AbstractBusPart.MAX_NAME_LENGTH ? null
                    : BusCondition.after(text);
        };
    }

    /* A tag's id as typed, with or without its #, or null when it is no id. */
    @Nullable
    private static ResourceLocation tag(final String typed) {
        final String bare = typed.trim().startsWith("#") ? typed.trim().substring(1) : typed.trim();
        return bare.isEmpty() ? null : ResourceLocation.tryParse(bare);
    }

    private static boolean inFilter(final int slot) {
        return slot >= 0 && slot < BusAbilities.FILTER_SLOTS;
    }

    private static int count(final long value) {
        return (int) Math.max(0L, Math.min(MOST_COUNT, value));
    }
}
