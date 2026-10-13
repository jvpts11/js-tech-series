/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.bus;

import dev.jstech.core.tier.HardwareEra;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Everything a bus is set to, as plain values: what its window shows and what software reads back, the same in both,
 * so each says what the other set.
 *
 * @param name       the name software finds it by, empty when it has none
 * @param era        the era it is of, which decides what it can be set to
 * @param filter     what each of its filter slots lists, by the text id of what is there, empty for an empty slot
 * @param exclude    whether it moves all but what it lists rather than only that
 * @param keep       what the faced chest keeps of each item
 * @param max        the most a move takes, 0 for as many as it can
 * @param itemKeep   for each filter slot, what the chest keeps of that item, 0 for nothing of its own
 * @param itemMax    for each filter slot, the most a move of that item takes, 0 for nothing of its own
 * @param priority   which of the network's buses goes first when they want the same thing, the higher first
 * @param conditions what it waits for before it moves
 * @param tags       the item tags it lists besides its slots, by id
 * @param fuzzy      whether it takes an item whatever its damage and components
 * @param powered    whether it is on
 * @param onDemand   whether it moves only while its cable has a redstone signal
 * @param setBy      which setting a program set last, by the setting's word, and that program's name
 * @param external   whether it is an External Storage Bus, which moves nothing and shows its inventory to the network
 * @param access     for an External Storage Bus, which way the network may use it: {@link #READ_WRITE},
 *                   {@link #READ_ONLY} or {@link #WRITE_ONLY}
 */
public record BusSettings(String name, HardwareEra era, List<String> filter, boolean exclude, int keep, int max,
                          List<Integer> itemKeep, List<Integer> itemMax, int priority, List<BusCondition> conditions,
                          List<String> tags, boolean fuzzy, boolean powered, boolean onDemand,
                          Map<String, String> setBy, boolean external, int access) {

    /** The words the settings go by where software sets them, and where the window marks what a program set. */
    public static final String POWER = "power";
    public static final String MODE = "mode";
    public static final String FILTER = "filter";
    public static final String KEEP = "keep";
    public static final String MAX = "max";
    public static final String PRIORITY = "priority";
    public static final String CONDITIONS = "conditions";
    public static final String MATCH = "match";
    public static final String ACCESS = "access";
    /** The network reads the inventory and writes to it. */
    public static final int READ_WRITE = 0;
    /** The network only reads it: it is never filled. */
    public static final int READ_ONLY = 1;
    /** The network only writes to it: what is in it is never taken, nor seen. */
    public static final int WRITE_ONLY = 2;

    public BusSettings {
        name = name == null ? "" : name;
        filter = List.copyOf(filter);
        itemKeep = perSlot(itemKeep, filter.size());
        itemMax = perSlot(itemMax, filter.size());
        conditions = List.copyOf(conditions);
        tags = List.copyOf(tags);
        setBy = Map.copyOf(setBy);
        access = access == READ_ONLY || access == WRITE_ONLY ? access : READ_WRITE;
    }

    /** The settings of a bus that moves: an Import, an Export, or a crafting bus. */
    public BusSettings(final String name, final HardwareEra era, final List<String> filter, final boolean exclude,
                       final int keep, final int max, final List<Integer> itemKeep, final List<Integer> itemMax,
                       final int priority, final List<BusCondition> conditions, final List<String> tags,
                       final boolean fuzzy, final boolean powered, final boolean onDemand,
                       final Map<String, String> setBy) {
        this(name, era, filter, exclude, keep, max, itemKeep, itemMax, priority, conditions, tags, fuzzy, powered,
                onDemand, setBy, false, READ_WRITE);
    }

    /** A new bus of {@code era}: no name, nothing listed, nothing kept, on, and moving all the time. */
    public static BusSettings fresh(final HardwareEra era) {
        final List<String> empty = List.of("", "", "", "", "");
        final List<Integer> zeros = List.of(0, 0, 0, 0, 0);
        return new BusSettings("", era, empty, false, 0, 0, zeros, zeros, 0, List.of(), List.of(), false, true, false,
                Map.of());
    }

    /** A new External Storage Bus of {@code era}: the whole inventory, read and written. */
    public static BusSettings freshExternal(final HardwareEra era) {
        final BusSettings bus = fresh(era);
        return new BusSettings("", era, bus.filter(), false, 0, 0, bus.itemKeep(), bus.itemMax(), 0, List.of(),
                List.of(), false, true, false, Map.of(), true, READ_WRITE);
    }

    /** What the bus of its era and kind can be set to. */
    public BusAbilities abilities() {
        return external ? BusAbilities.external(era) : BusAbilities.of(era);
    }

    /** Whether any filter slot lists something. */
    public boolean listsAny() {
        return filter.stream().anyMatch(id -> !id.isEmpty());
    }

    /** The program that set {@code setting} last, or empty when a hand did or nothing has. */
    public String setByOf(final String setting) {
        return setBy.getOrDefault(setting, "");
    }

    /*
     * A per-slot list made exactly {@code slots} long: cut short, or padded with zeros. A filter whose size changed
     * since the bus was saved would otherwise leave a slot with no quantity to read.
     */
    private static List<Integer> perSlot(final List<Integer> values, final int slots) {
        if (values.size() == slots) {
            return List.copyOf(values);
        }
        final List<Integer> fitted = new ArrayList<>(values.subList(0, Math.min(values.size(), slots)));
        while (fitted.size() < slots) {
            fitted.add(0);
        }
        return List.copyOf(fitted);
    }
}
