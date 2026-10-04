/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import dev.jstech.computers.block.part.AbstractBusPart;
import dev.jstech.computers.block.part.ExternalStorageBusPart;
import dev.jstech.computers.block.part.NamedBus;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.bus.BusAbilities;
import dev.jstech.computers.bus.BusCondition;
import dev.jstech.computers.bus.BusFeature;
import dev.jstech.computers.program.iql.IqlBusStatement;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.uuid.NetworkUuid;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Applies a {@code SET BUS} statement to the bus of that name on the Mainframe's network, as software sets it: the
 * setting carries the mark of what set it (a job by its name, a statement typed at a prompt as IQL), which the bus's
 * window shows until a hand changes it. A setting the bus's era cannot be set to is refused, as its window refuses it.
 */
@TextHolder
public final class IqlBusSetter {

    /** What a statement typed at a prompt is marked with on the bus. */
    public static final String TYPED = "IQL";

    private static final TextKey SET = TextKey.of("jsc.service.iql.bus_set", "bus %s set");
    private static final TextKey NO_BUS = TextKey.of("jsc.service.iql.bus_missing", "no bus named %s on this network");
    private static final TextKey CANNOT = TextKey.of("jsc.service.iql.bus_cannot",
            "the bus %s cannot be set so: its era or its kind has no such setting");
    private static final TextKey NOT_LISTED = TextKey.of("jsc.service.iql.bus_not_listed",
            "%s is not in the filter of the bus %s");
    private static final TextKey NO_ITEM = TextKey.of("jsc.service.iql.bus_no_item", "no item or fluid named %s");

    private IqlBusSetter() {
    }

    /** Sets what {@code statement} says on its bus on the Mainframe's network; {@code by} names what set it. */
    public static IqlEngine.Outcome apply(final MainframeBlockEntity mainframe, final IqlBusStatement statement,
                                          final String by) {
        if (!(mainframe.getLevel() instanceof ServerLevel level)) {
            return IqlEngine.Outcome.fail(NO_BUS.with(statement.bus()));
        }
        return apply(level, mainframe.networkUuid(), statement, by);
    }

    /** Sets what {@code statement} says on its bus on {@code network}; {@code by} names what set it. */
    public static IqlEngine.Outcome apply(final ServerLevel level, @Nullable final NetworkUuid network,
                                          final IqlBusStatement statement, final String by) {
        final NamedBus.Located at = NamedBus.find(level, network, statement.bus());
        if (at == null || !(at.cable().getPart(at.face()) instanceof AbstractBusPart bus)) {
            return IqlEngine.Outcome.fail(NO_BUS.with(statement.bus()));
        }
        return applyTo(bus, statement, by);
    }

    /**
     * Sets what {@code statement} says on {@code bus}, found by whoever calls: a Crafting Input Router, which is on a
     * crafting cable rather than the network's, takes the settings a bus takes.
     */
    public static IqlEngine.Outcome applyTo(final AbstractBusPart bus, final IqlBusStatement statement,
                                            final String by) {
        final boolean external = bus instanceof ExternalStorageBusPart;
        final boolean took = switch (statement.change()) {
            case IqlBusStatement.Power power -> !external && bus.setPowered(power.on(), by);
            case IqlBusStatement.Mode mode -> !external && bus.setMode(mode.onDemand()
                    ? AbstractBusPart.MODE_REDSTONE : AbstractBusPart.MODE_CONTINUOUS, by);
            case IqlBusStatement.Filter filter -> {
                final List<ItemStack> stacks = stacks(filter.items());
                if (stacks == null) {
                    yield false;
                }
                yield filter(bus, filter.allBut(), stacks, by);
            }
            case IqlBusStatement.Tags tags -> bus.setTags(tags.tags().stream().map(IqlBusSetter::tag)
                    .filter(id -> id != null).toList(), by);
            case IqlBusStatement.Match match -> bus.setFuzzy(match.fuzzy(), by);
            case IqlBusStatement.Quantities quantities ->
                    (quantities.keep() == IqlBusStatement.UNCHANGED || bus.setKeep(quantities.keep(), by))
                            && (quantities.max() == IqlBusStatement.UNCHANGED || bus.setMax(quantities.max(), by));
            case IqlBusStatement.ItemQuantities quantities -> {
                final int slot = slotListing(bus, quantities.item());
                if (slot < 0) {
                    yield false;
                }
                yield bus.setItemQuantities(slot, quantities.keep() == IqlBusStatement.UNCHANGED
                        ? bus.itemKeep(slot) : quantities.keep(), quantities.max() == IqlBusStatement.UNCHANGED
                        ? bus.itemMax(slot) : quantities.max(), by);
            }
            case IqlBusStatement.Priority priority -> bus.setPriority(priority.value(), by);
            case IqlBusStatement.Stock stock -> wait(bus, BusCondition.stock(subject(stock.subject()),
                    stock.below()), by);
            case IqlBusStatement.After after -> wait(bus, BusCondition.after(after.bus()), by);
            case IqlBusStatement.Hours hours -> hours(bus, hours.from(), hours.to(), by);
            case IqlBusStatement.Access access -> bus instanceof ExternalStorageBusPart storage
                    && storage.setAccess(access.access(), by);
        };
        if (took) {
            return IqlEngine.Outcome.ok(SET.with(statement.bus()));
        }
        if (statement.change() instanceof IqlBusStatement.ItemQuantities quantities
                && slotListing(bus, quantities.item()) < 0) {
            return IqlEngine.Outcome.fail(NOT_LISTED.with(quantities.item(), statement.bus()));
        }
        if (statement.change() instanceof IqlBusStatement.Filter filter && stacks(filter.items()) == null) {
            return IqlEngine.Outcome.fail(NO_ITEM.with(String.join(", ", filter.items())));
        }
        return IqlEngine.Outcome.fail(CANNOT.with(statement.bus()));
    }

    /* Lists {@code stacks} in the filter's slots, the rest emptied, only these or all but these. */
    private static boolean filter(final AbstractBusPart bus, final boolean allBut, final List<ItemStack> stacks,
                                  final String by) {
        final BusAbilities can = bus.abilities();
        if (!can.can(BusFeature.FILTER) && (allBut || stacks.size() > 1)) {
            return false;
        }
        if (stacks.size() > BusAbilities.FILTER_SLOTS) {
            return false;
        }
        boolean took = !can.can(BusFeature.FILTER) || bus.setExclude(allBut, by);
        for (int slot = 0; slot < BusAbilities.FILTER_SLOTS && took; slot++) {
            final ItemStack stack = slot < stacks.size() ? stacks.get(slot) : ItemStack.EMPTY;
            if (slot > 0 && !can.can(BusFeature.FILTER)) {
                break;
            }
            took = bus.setFilterSlot(slot, stack, by);
        }
        return took;
    }

    /* A condition the bus waits for; one it already has is left as it is. */
    private static boolean wait(final AbstractBusPart bus, final BusCondition condition, final String by) {
        return bus.conditions().contains(condition) || bus.addCondition(condition, by);
    }

    /* The hours the bus moves in: in place of the hours it had. */
    private static boolean hours(final AbstractBusPart bus, final int from, final int to, final String by) {
        final List<BusCondition> conditions = bus.conditions();
        for (int i = conditions.size() - 1; i >= 0; i--) {
            if (conditions.get(i).kind() == BusCondition.Kind.HOURS) {
                bus.removeCondition(i, by);
            }
        }
        return bus.addCondition(BusCondition.hours(from, to), by);
    }

    /* The filter slot listing {@code item}, or -1. */
    private static int slotListing(final AbstractBusPart bus, final String item) {
        final List<ItemStack> stacks = stacks(List.of(item));
        if (stacks == null) {
            return -1;
        }
        final StorageKey wanted = StorageKey.of(stacks.get(0));
        for (int slot = 0; slot < BusAbilities.FILTER_SLOTS; slot++) {
            final ItemStack inSlot = bus.getFilterHandler().getStackInSlot(slot);
            if (!inSlot.isEmpty() && StorageKey.of(inSlot).equals(wanted)) {
                return slot;
            }
        }
        return -1;
    }

    /* What the ids name, an item each or a fluid's bucket, or null when one names nothing. */
    @Nullable
    private static List<ItemStack> stacks(final List<String> ids) {
        final List<ItemStack> stacks = new ArrayList<>();
        for (final String id : ids) {
            final String upper = id.toUpperCase(Locale.ROOT);
            if (upper.startsWith("FLUID ")) {
                final ResourceLocation location = ResourceLocation.tryParse(qualified(id.substring(6).strip()));
                final var fluid = location == null ? Fluids.EMPTY
                        : BuiltInRegistries.FLUID.getOptional(location).orElse(Fluids.EMPTY);
                if (fluid == Fluids.EMPTY || fluid.getBucket() == null) {
                    return null;
                }
                stacks.add(new ItemStack(fluid.getBucket()));
                continue;
            }
            final StorageKey key = StorageKey.byName(id);
            if (key == null) {
                return null;
            }
            stacks.add(key.stack(1));
        }
        return stacks;
    }

    /* A stock condition's subject as the bus keeps it: an item by its full id, a tag after its #. */
    private static String subject(final String typed) {
        return typed.startsWith("#") ? typed : qualified(typed);
    }

    @Nullable
    private static ResourceLocation tag(final String typed) {
        return ResourceLocation.tryParse(typed.startsWith("#") ? typed.substring(1) : typed);
    }

    private static String qualified(final String id) {
        return id.contains(":") ? id : "minecraft:" + id;
    }
}
