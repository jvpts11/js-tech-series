/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.region.Box;
import dev.jstech.core.uuid.StructureUuid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Which plots of a dimension's yard are taken, and by which structure: kept with the world, so a plot is never given
 * to two structures, and a structure that went away gives its plot back for the next. A structure taken away while
 * its plot's chunks were not loaded leaves its blocks there to be cleared: its plot is given back once they are.
 *
 * @param next     the first plot never yet given
 * @param free     plots given back, the lowest given first
 * @param owners   the structure each taken plot is for
 * @param clearing the plots whose blocks are still to be cleared, with the box they fill, before they are given back
 */
public record YardBook(int next, List<Integer> free, Map<Integer, StructureUuid> owners, Map<Integer, Box> clearing) {

    /** A yard with every plot free. */
    public static final YardBook EMPTY = new YardBook(0, List.of(), Map.of(), Map.of());
    public static final Codec<YardBook> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("next").forGetter(YardBook::next),
            Codec.INT.listOf().fieldOf("free").forGetter(YardBook::free),
            Owner.CODEC.listOf().fieldOf("owners").forGetter(book -> book.owners().entrySet().stream()
                    .map(entry -> new Owner(entry.getKey(), entry.getValue())).toList()),
            Clearing.CODEC.listOf().optionalFieldOf("clearing", List.of()).forGetter(book -> book.clearing()
                    .entrySet().stream().map(entry -> new Clearing(entry.getKey(), entry.getValue())).toList())
    ).apply(instance, (next, free, owners, clearing) -> {
        final Map<Integer, StructureUuid> byPlot = new HashMap<>();
        for (final Owner owner : owners) {
            byPlot.put(owner.plot(), owner.structure());
        }
        final Map<Integer, Box> toClear = new HashMap<>();
        for (final Clearing each : clearing) {
            toClear.put(each.plot(), each.box());
        }
        return new YardBook(next, free, byPlot, toClear);
    }));

    public YardBook {
        free = List.copyOf(free);
        owners = Map.copyOf(owners);
        clearing = Map.copyOf(clearing);
    }

    /** The plot the next structure gets: the lowest given back, or the first never given. */
    public int nextPlot() {
        return free.isEmpty() ? next : free.getFirst();
    }

    /**
     * This book with {@link #nextPlot()} given to {@code structure}.
     *
     * @throws IllegalStateException when the yard has no plot left
     */
    public YardBook taken(final StructureUuid structure) {
        final int plot = nextPlot();
        if (plot >= YardLayout.PLOTS) {
            throw new IllegalStateException("the yard has no plot left of its " + YardLayout.PLOTS);
        }
        final Map<Integer, StructureUuid> byPlot = new HashMap<>(owners);
        byPlot.put(plot, structure);
        if (free.isEmpty()) {
            return new YardBook(next + 1, free, byPlot, clearing);
        }
        return new YardBook(next, free.subList(1, free.size()), byPlot, clearing);
    }

    /** This book with {@code plot} given back; a plot not taken changes nothing. */
    public YardBook givenBack(final int plot) {
        if (!owners.containsKey(plot)) {
            return this;
        }
        final Map<Integer, StructureUuid> byPlot = new HashMap<>(owners);
        byPlot.remove(plot);
        final TreeSet<Integer> sorted = new TreeSet<>(free);
        sorted.add(plot);
        return new YardBook(next, new ArrayList<>(sorted), byPlot, clearing);
    }

    /**
     * This book with {@code plot} no longer its structure's, its blocks filling {@code box} still to be cleared: it
     * is given to no other until {@link #cleared} says they are. A plot not taken changes nothing.
     */
    public YardBook leftToClear(final int plot, final Box box) {
        if (!owners.containsKey(plot)) {
            return this;
        }
        final Map<Integer, StructureUuid> byPlot = new HashMap<>(owners);
        byPlot.remove(plot);
        final Map<Integer, Box> toClear = new HashMap<>(clearing);
        toClear.put(plot, box);
        return new YardBook(next, free, byPlot, toClear);
    }

    /** This book with the blocks of {@code plot} cleared and the plot given back; one not left to clear, unchanged. */
    public YardBook cleared(final int plot) {
        if (!clearing.containsKey(plot)) {
            return this;
        }
        final Map<Integer, Box> toClear = new HashMap<>(clearing);
        toClear.remove(plot);
        final TreeSet<Integer> sorted = new TreeSet<>(free);
        sorted.add(plot);
        return new YardBook(next, new ArrayList<>(sorted), owners, toClear);
    }

    /** The structure plot {@code plot} is for, if it is taken. */
    public Optional<StructureUuid> owner(final int plot) {
        return Optional.ofNullable(owners.get(plot));
    }

    /** One taken plot, as it is written. */
    private record Owner(int plot, StructureUuid structure) {

        static final Codec<Owner> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("plot").forGetter(Owner::plot),
                StructureUuid.CODEC.fieldOf("structure").forGetter(Owner::structure)
        ).apply(instance, Owner::new));
    }

    /** One plot left to clear, and the box its blocks fill, as it is written. */
    private record Clearing(int plot, Box box) {

        static final Codec<Clearing> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("plot").forGetter(Clearing::plot),
                Codec.INT.listOf(6, 6).xmap(list -> new Box(list.get(0), list.get(1), list.get(2), list.get(3),
                        list.get(4), list.get(5)), box -> List.of(box.minX(), box.minY(), box.minZ(), box.maxX(),
                        box.maxY(), box.maxZ())).fieldOf("box").forGetter(Clearing::box)
        ).apply(instance, Clearing::new));
    }
}
