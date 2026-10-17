/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.persistor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.id.StableCodecs;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The area a Space Persistor keeps loaded, as its player set it: a shape round its own chunk, its radius, and the
 * chunks clicked on its map, added or taken off, each an offset from its own chunk ({@link PersistorShapes}).
 *
 * @param shape   the shape
 * @param radius  how far the shape reaches, in chunks
 * @param added   the offsets added outside the shape
 * @param removed the offsets of the shape taken off
 */
public record PersistorArea(PersistorShape shape, int radius, List<Long> added, List<Long> removed) {

    /** The most a radius reaches: as far as its thumbwheel's two drums count. */
    public static final int MOST_RADIUS = 99;
    /** A Persistor as it is placed: a square of three by three chunks. */
    public static final PersistorArea START = new PersistorArea(PersistorShape.SQUARE, 1, List.of(), List.of());
    /* The most chunks clicked in or out an area keeps. */
    private static final int MOST_CLICKS = 4_096;
    public static final Codec<PersistorArea> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            StableCodecs.byName(PersistorShape.class).optionalFieldOf("shape", PersistorShape.SQUARE)
                    .forGetter(PersistorArea::shape),
            Codec.intRange(0, MOST_RADIUS).optionalFieldOf("radius", 1).forGetter(PersistorArea::radius),
            Codec.LONG.listOf().optionalFieldOf("added", List.of()).forGetter(PersistorArea::added),
            Codec.LONG.listOf().optionalFieldOf("removed", List.of()).forGetter(PersistorArea::removed))
            .apply(instance, PersistorArea::new));

    public PersistorArea {
        radius = Math.clamp(radius, 0, MOST_RADIUS);
        added = List.copyOf(added.size() > MOST_CLICKS ? added.subList(0, MOST_CLICKS) : added);
        removed = List.copyOf(removed.size() > MOST_CLICKS ? removed.subList(0, MOST_CLICKS) : removed);
    }

    /** The offsets it covers, a line running along x or along z. */
    public Set<Long> offsets(final boolean alongX) {
        return PersistorShapes.area(this.shape, this.radius, alongX, Set.copyOf(this.added),
                Set.copyOf(this.removed));
    }

    /** The same clicks on another shape. */
    public PersistorArea shaped(final PersistorShape to) {
        return new PersistorArea(to, this.radius, this.added, this.removed);
    }

    /** The same with another radius, held between none and {@link #MOST_RADIUS}. */
    public PersistorArea reaching(final int to) {
        return new PersistorArea(this.shape, to, this.added, this.removed);
    }

    /** The area after a click on the offset {@code dx, dz} of its map. */
    public PersistorArea clicked(final int dx, final int dz, final boolean alongX) {
        final PersistorShapes.Clicks clicks = PersistorShapes.click(this.shape, this.radius, alongX,
                Set.copyOf(this.added), Set.copyOf(this.removed), dx, dz);
        return new PersistorArea(this.shape, this.radius, List.copyOf(new LinkedHashSet<>(clicks.added())),
                List.copyOf(new LinkedHashSet<>(clicks.removed())));
    }
}
