/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.region;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * Things kept by where they are, each under a key: a block, a box of blocks, a claimed area. Each is filed under every
 * region of 512 by 512 blocks its box touches, so finding what is near a place reads only the regions round it,
 * however many things the world holds. A box wider than a few thousand regions is kept apart and looked at on every
 * search, so one huge claim does not fill the regions' files.
 *
 * @param <K> what an entry is known by
 * @param <T> what an entry holds
 */
public final class SpatialIndex<K, T> {

    private final Map<K, Entry<K, T>> byKey = new LinkedHashMap<>();
    private final Map<Long, Set<K>> byRegion = new HashMap<>();
    /** The entries too wide to file region by region. */
    private final Set<K> wide = new LinkedHashSet<>();

    /** A region is 2^9 = 512 blocks on a side, the size of the game's own region files. */
    public static final int REGION_SHIFT = 9;
    /** The most regions an entry is filed under before it is kept apart instead. */
    private static final long MOST_REGIONS = 4096L;

    /**
     * One thing in the index.
     *
     * @param key   what it is known by
     * @param box   where it is
     * @param value what it holds
     */
    public record Entry<K, T>(K key, Box box, T value) {

        public Entry {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(box, "box");
            Objects.requireNonNull(value, "value");
        }
    }

    /** Files {@code value} under {@code key} over {@code box}, in place of whatever the key held before. */
    public void put(final K key, final Box box, final T value) {
        remove(key);
        final Entry<K, T> entry = new Entry<>(key, box, value);
        byKey.put(key, entry);
        if (regionsOf(box) > MOST_REGIONS) {
            wide.add(key);
            return;
        }
        forEachRegion(box, region -> byRegion.computeIfAbsent(region, any -> new LinkedHashSet<>()).add(key));
    }

    /** Takes out what {@code key} held, and gives it back; null when it held nothing. */
    @Nullable
    public Entry<K, T> remove(final K key) {
        final Entry<K, T> entry = byKey.remove(key);
        if (entry == null) {
            return null;
        }
        if (!wide.remove(key)) {
            forEachRegion(entry.box(), region -> {
                final Set<K> keys = byRegion.get(region);
                if (keys != null) {
                    keys.remove(key);
                    if (keys.isEmpty()) {
                        byRegion.remove(region);
                    }
                }
            });
        }
        return entry;
    }

    @Nullable
    public Entry<K, T> get(final K key) {
        return byKey.get(key);
    }

    /**
     * Every entry whose box meets {@code area}. They come grouped by the region they are filed under, region by
     * region, with the entries kept apart for being too wide last; only inside one region is it the order they were
     * filed.
     */
    public List<Entry<K, T>> within(final Box area) {
        final Set<K> found = new LinkedHashSet<>();
        if (regionsOf(area) > MOST_REGIONS) {
            found.addAll(byKey.keySet());
        } else {
            forEachRegion(area, region -> {
                final Set<K> keys = byRegion.get(region);
                if (keys != null) {
                    found.addAll(keys);
                }
            });
            found.addAll(wide);
        }
        final List<Entry<K, T>> out = new ArrayList<>();
        for (final K key : found) {
            final Entry<K, T> entry = byKey.get(key);
            if (entry.box().intersects(area)) {
                out.add(entry);
            }
        }
        return out;
    }

    /** Every entry whose box holds the block. */
    public List<Entry<K, T>> containing(final int x, final int y, final int z) {
        return within(Box.at(x, y, z));
    }

    /**
     * The entry nearest the block within {@code radius} blocks, or null when there is none that close. Of entries
     * equally near, the first in the order {@link #within} gives wins.
     */
    @Nullable
    public Entry<K, T> nearest(final int x, final int y, final int z, final int radius) {
        Entry<K, T> best = null;
        long bestDistance = (long) radius * radius;
        for (final Entry<K, T> entry : within(Box.around(x, y, z, radius))) {
            final long distance = entry.box().distanceSquared(x, y, z);
            if (distance <= bestDistance && (best == null || distance < bestDistance)) {
                best = entry;
                bestDistance = distance;
            }
        }
        return best;
    }

    /** Every entry filed under the region at {@code (regionX, regionZ)}, counted in regions from the world's origin. */
    public List<Entry<K, T>> inRegion(final int regionX, final int regionZ) {
        final int size = 1 << REGION_SHIFT;
        return within(new Box(regionX * size, Integer.MIN_VALUE / 2, regionZ * size, regionX * size + size - 1,
                Integer.MAX_VALUE / 2, regionZ * size + size - 1));
    }

    /** Every entry, in the order they were filed. */
    public Collection<Entry<K, T>> all() {
        return Collections.unmodifiableCollection(byKey.values());
    }

    public int size() {
        return byKey.size();
    }

    /** How many regions hold at least one entry; for a test. */
    public int filledRegions() {
        return byRegion.size();
    }

    private static long regionsOf(final Box box) {
        final long width = ((long) box.maxX() >> REGION_SHIFT) - ((long) box.minX() >> REGION_SHIFT) + 1;
        final long depth = ((long) box.maxZ() >> REGION_SHIFT) - ((long) box.minZ() >> REGION_SHIFT) + 1;
        return width * depth;
    }

    private static void forEachRegion(final Box box, final RegionVisitor visitor) {
        for (int rx = box.minX() >> REGION_SHIFT; rx <= box.maxX() >> REGION_SHIFT; rx++) {
            for (int rz = box.minZ() >> REGION_SHIFT; rz <= box.maxZ() >> REGION_SHIFT; rz++) {
                visitor.visit((long) rx << 32 | rz & 0xFFFFFFFFL);
            }
        }
    }

    @FunctionalInterface
    private interface RegionVisitor {
        void visit(long region);
    }
}
