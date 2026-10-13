/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.audio;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Many sources of one kind close together heard as one: forty racks in a room are the sound of a datacenter, not
 * forty fans. Sources are grouped by the field they belong to and by how close they stand; a group big enough
 * becomes one bed at its middle, louder the more it holds, and its members stop playing their own sounds.
 */
public final class AmbientClusters {

    /** How loud a bed is at the smallest group that makes one. */
    private static final double BED_FLOOR = 0.4;
    /** How much louder each member past the smallest makes it, up to full. */
    private static final double BED_STEP = 0.05;

    private AmbientClusters() {
    }

    /**
     * The beds the sources make and the sources they took in.
     *
     * @param sources the sources that want to be heard, each naming its field, or none
     * @param rules   for each field, how many sources make a bed and how close they must stand
     */
    public static Result group(final List<Source> sources, final Map<String, Rule> rules) {
        final Map<String, List<Cluster>> byField = new LinkedHashMap<>();
        final List<Source> ordered = new ArrayList<>(sources);
        ordered.sort(Comparator.comparing(Source::id));
        for (final Source source : ordered) {
            final Rule rule = source.field() == null ? null : rules.get(source.field());
            if (rule == null) {
                continue;
            }
            final List<Cluster> clusters = byField.computeIfAbsent(source.field(), field -> new ArrayList<>());
            Cluster home = null;
            for (final Cluster cluster : clusters) {
                if (cluster.reaches(source, rule.radius())) {
                    home = cluster;
                    break;
                }
            }
            if (home == null) {
                home = new Cluster();
                clusters.add(home);
            }
            home.add(source);
        }
        final List<Bed> beds = new ArrayList<>();
        final Set<String> absorbed = new HashSet<>();
        byField.forEach((field, clusters) -> {
            final Rule rule = rules.get(field);
            for (final Cluster cluster : clusters) {
                if (cluster.members.size() >= rule.threshold()) {
                    beds.add(cluster.bed(field, rule.threshold()));
                    cluster.members.forEach(member -> absorbed.add(member.id()));
                }
            }
        });
        return new Result(beds, absorbed);
    }

    /** How loud a bed of that many members is, when {@code threshold} make the smallest one. */
    public static double bedVolume(final int members, final int threshold) {
        return Math.min(1.0, BED_FLOOR + BED_STEP * Math.max(0, members - threshold));
    }

    /**
     * A source that may be taken into a bed.
     *
     * @param id    what tells it apart
     * @param field the field it belongs to, or null for a source that is always heard on its own
     */
    public record Source(String id, String field, double x, double y, double z) {
    }

    /**
     * How a field makes beds.
     *
     * @param threshold how many sources close together make a bed
     * @param radius    how far from a group's middle a source may stand and still belong to it, in blocks
     */
    public record Rule(int threshold, double radius) {
    }

    /**
     * One group heard as one.
     *
     * @param field   the field it is the sound of
     * @param members the ids of the sources it took in
     * @param volume  how loud it is, from the number of its members
     */
    public record Bed(String field, double x, double y, double z, List<String> members, double volume) {
    }

    /** The beds made and every source that is now heard through one of them. */
    public record Result(List<Bed> beds, Set<String> absorbed) {
    }

    /** A group being gathered, whose middle moves as members join. */
    private static final class Cluster {

        private final List<Source> members = new ArrayList<>();
        private double sumX;
        private double sumY;
        private double sumZ;

        boolean reaches(final Source source, final double radius) {
            final double n = members.size();
            final double dx = sumX / n - source.x();
            final double dy = sumY / n - source.y();
            final double dz = sumZ / n - source.z();
            return dx * dx + dy * dy + dz * dz <= radius * radius;
        }

        void add(final Source source) {
            members.add(source);
            sumX += source.x();
            sumY += source.y();
            sumZ += source.z();
        }

        Bed bed(final String field, final int threshold) {
            final double n = members.size();
            return new Bed(field, sumX / n, sumY / n, sumZ / n, members.stream().map(Source::id).toList(),
                    bedVolume(members.size(), threshold));
        }
    }
}
