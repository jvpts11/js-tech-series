/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.research;

import dev.jstech.core.id.IStableId;
import dev.jstech.core.progression.TeamProgress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

/**
 * Research, done: a team's research computers work a discovery on, by the points they make and the data they hold.
 * The progress is the team's share of the discovery's knowledge, so it is the team's whatever computer worked it, and
 * nothing is lost when a computer stops or goes.
 *
 * <p>A research computer works once a second, with the points it made in that second:
 *
 * <pre>{@code
 * Research.Outcome outcome = Research.work(server, CoreTeams.teamOf(server, owner), discovery, pointsThisSecond,
 *         myDataSlots);
 * }</pre>
 *
 * Each call that moves the share on keeps it, which sends it to the team's players, hence once a second rather than
 * every tick.
 */
public final class Research {

    private Research() {
    }

    /** How much of {@code discovery} {@code team} has made, from 0 to 1. */
    public static double share(final MinecraftServer server, final String team, final Discovery discovery) {
        return TeamProgress.share(server, team, discovery.id());
    }

    /** Whether {@code team} has made the whole of {@code discovery}. */
    public static boolean made(final MinecraftServer server, final String team, final Discovery discovery) {
        return TeamProgress.knows(server, team, discovery.id());
    }

    /** Whether {@code team} knows everything {@code discovery} comes after, so its research may start. */
    public static boolean ready(final MinecraftServer server, final String team, final Discovery discovery) {
        for (final ResourceLocation known : discovery.after()) {
            if (!TeamProgress.knows(server, team, known)) {
                return false;
            }
        }
        return true;
    }

    /** What {@code discovery} comes after that {@code team} does not know yet, in the order it was declared. */
    public static List<ResourceLocation> missing(final MinecraftServer server, final String team,
                                                 final Discovery discovery) {
        final List<ResourceLocation> missing = new ArrayList<>();
        for (final ResourceLocation known : discovery.after()) {
            if (!TeamProgress.knows(server, team, known)) {
                missing.add(known);
            }
        }
        return missing;
    }

    /**
     * Whether {@code team} could work {@code discovery} on now with {@code data}, spending nothing: {@code WORKED} when
     * it could go on, else why not. A research computer asks this before it draws power for a discovery.
     */
    public static Outcome check(final MinecraftServer server, final String team, final Discovery discovery,
                                final IDataStore data) {
        final Outcome blocked = blocked(server, team, discovery);
        if (blocked != null) {
            return blocked;
        }
        final Asked asked = asked(server, discovery, data);
        final double before = share(server, team, discovery);
        final ResearchMath.Step step = ResearchMath.advance(before, asked.points(), asked.points(), asked.counts(),
                asked.held());
        return step.share() > before ? Outcome.WORKED : Outcome.NEEDS_DATA;
    }

    /**
     * Works {@code discovery} on for {@code team} by {@code points}, spending from {@code data} the data each part of
     * the way it enters asks for. It goes only as far as the data held covers, and not at all before the team knows
     * what it comes after.
     *
     * @return what came of it
     */
    public static Outcome work(final MinecraftServer server, final String team, final Discovery discovery,
                               final double points, final IDataStore data) {
        final Outcome blocked = blocked(server, team, discovery);
        if (blocked != null) {
            return blocked;
        }
        final Asked asked = asked(server, discovery, data);
        final List<DataKind> kinds = asked.kinds();
        final double before = share(server, team, discovery);
        final ResearchMath.Step step = ResearchMath.advance(before, points, asked.points(), asked.counts(),
                asked.held());
        for (int k = 0; k < kinds.size(); k++) {
            if (step.spends()[k] > 0) {
                data.spend(kinds.get(k), step.spends()[k]);
            }
        }
        if (step.share() > before) {
            TeamProgress.grant(server, team, discovery.knowledge(), step.made() ? 1.0 : step.share());
        }
        if (step.made()) {
            return Outcome.MADE;
        }
        return step.share() < Math.min(1.0, before + Math.max(0.0, points) / asked.points()) ? Outcome.NEEDS_DATA
                : Outcome.WORKED;
    }

    /* Why {@code discovery} cannot be worked on for {@code team} whatever data it holds; none when it can. */
    private static @Nullable Outcome blocked(final MinecraftServer server, final String team,
                                             final Discovery discovery) {
        if (!Discoveries.exists(server.overworld(), discovery)) {
            return Outcome.ABSENT;
        }
        if (made(server, team, discovery)) {
            return Outcome.MADE;
        }
        return ready(server, team, discovery) ? null : Outcome.LOCKED;
    }

    /* What {@code discovery} costs and how much of each kind of data it asks for {@code data} holds. */
    private static Asked asked(final MinecraftServer server, final Discovery discovery, final IDataStore data) {
        final ResearchCost cost = Discoveries.cost(server.overworld(), discovery.id()).orElseThrow();
        final List<DataKind> kinds = new ArrayList<>(cost.data().size());
        final int[] counts = new int[cost.data().size()];
        final int[] held = new int[counts.length];
        for (final Map.Entry<ResourceLocation, Integer> asked : cost.data().entrySet()) {
            final DataKind kind = DataKinds.byId(asked.getKey()).orElseThrow();
            final int k = kinds.size();
            kinds.add(kind);
            counts[k] = asked.getValue();
            held[k] = Math.max(0, data.held(kind));
        }
        return new Asked(cost.points(), kinds, counts, held);
    }

    /* A discovery's cost, the kinds of data it asks for, how many of each, and how many of each are held. */
    private record Asked(long points, List<DataKind> kinds, int[] counts, int[] held) {
    }

    /** What a research computer holds of each kind of data, and spends as its research goes. */
    public interface IDataStore {

        /** How many data of {@code kind} it holds. */
        int held(DataKind kind);

        /** Takes {@code count} data of {@code kind} away, never more than it holds. */
        void spend(DataKind kind, int count);
    }

    /** What came of working a discovery on, with a number of its own for a screen to be sent it by. */
    public enum Outcome implements IStableId {
        /** It went on as far as the points went. */
        WORKED(0),
        /** It went on only as far as the data held covers, or not at all: more data is needed. */
        NEEDS_DATA(1),
        /** The team does not know yet everything it comes after. */
        LOCKED(2),
        /** The team has made the whole of it: now, or before. */
        MADE(3),
        /** It is not there to research: something it asks for is not installed. */
        ABSENT(4);

        private final int id;

        Outcome(final int id) {
            this.id = id;
        }

        @Override
        public int id() {
            return this.id;
        }
    }
}
