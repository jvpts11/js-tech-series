/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.progression;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.JsCore;
import dev.jstech.core.state.CoreState;
import dev.jstech.core.state.CoreStates;
import dev.jstech.core.state.TeamState;
import dev.jstech.core.team.CoreTeams;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.NeoForge;

/**
 * What each team has reached, beside what each player has ({@link PlayerProgress}): its furthest step along each axis,
 * the milestones it has passed, and how much it knows of each piece of knowledge. A player's team is the one
 * {@link CoreTeams} says, FTB Teams' when installed, the game's own otherwise, or the player alone. What a team reaches
 * is the team's alone: a player who leaves takes nothing with them, a player who joins has all the team has, and two
 * teams that merge ({@link #merge}) keep everything either had. Kept with the world and sent to each team's players.
 */
public final class TeamProgress {

    /** The most axes, milestones or pieces of knowledge a team's progress is sent with. */
    private static final int MOST = 4_096;
    private static final double WHOLE = 1.0;

    private static final TeamState<Held> STATE = CoreState.builder(
                    ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "team_progression"), Held.CODEC, Held.NONE)
            .fileName("jstech_team_progression")
            .synced(Held.STREAM_CODEC)
            .team();

    private TeamProgress() {
    }

    /** Registers the state the progress is kept in, from the Core's constructor. */
    public static void register() {
        CoreStates.register(STATE);
    }

    /** All {@code team} has reached. */
    public static Held of(final MinecraftServer server, final String team) {
        return STATE.get(server, team);
    }

    /** All the team {@code player} is on has reached. */
    public static Held ofPlayer(final MinecraftServer server, final UUID player) {
        return of(server, CoreTeams.teamOf(server, player));
    }

    /** All this client's player's team has reached, as the server last said. */
    public static Held here() {
        return STATE.client();
    }

    /** The furthest step {@code team} has reached along {@code axis}. */
    public static <S extends IAxisStep> S reached(final MinecraftServer server, final String team,
                                                  final ProgressionAxis<S> axis) {
        return axis.at(of(server, team).step(axis.id()));
    }

    /**
     * Moves {@code team} to {@code step} along {@code axis} if it is further than any step it has reached, and posts
     * a {@link TeamProgressEvent.StepReached} when it is.
     *
     * @return whether the team moved forward
     */
    public static <S extends IAxisStep> boolean reach(final MinecraftServer server, final String team,
                                                      final ProgressionAxis<S> axis, final S step) {
        final S before = reached(server, team, axis);
        if (step.level() <= before.level()) {
            return false;
        }
        STATE.update(server, team, held -> held.withStep(axis.id(), step.level()));
        NeoForge.EVENT_BUS.post(new TeamProgressEvent.StepReached(server, team, axis, before, step));
        return true;
    }

    /** Whether {@code team} has reached {@code milestone}. */
    public static boolean reached(final MinecraftServer server, final String team, final Milestone milestone) {
        return of(server, team).milestones().contains(milestone.id().toString());
    }

    /**
     * Has {@code team} reach {@code milestone}, once, and know it as knowledge; posts a
     * {@link TeamProgressEvent.MilestoneReached} the first time.
     *
     * @return whether the team reached it now, not before
     */
    public static boolean reach(final MinecraftServer server, final String team, final Milestone milestone) {
        if (reached(server, team, milestone)) {
            return false;
        }
        STATE.update(server, team, held -> held.withMilestone(milestone.id().toString()));
        NeoForge.EVENT_BUS.post(new TeamProgressEvent.MilestoneReached(server, team, milestone));
        grant(server, team, milestone.knowledge(), WHOLE);
        return true;
    }

    /** How much of the knowledge {@code id} {@code team} knows, from 0 to 1. */
    public static double share(final MinecraftServer server, final String team, final ResourceLocation id) {
        return of(server, team).share(id);
    }

    /** Whether {@code team} knows the whole of the knowledge {@code id}. */
    public static boolean knows(final MinecraftServer server, final String team, final ResourceLocation id) {
        return of(server, team).knows(id);
    }

    /** Whether this client's player's team knows the whole of the knowledge {@code id}, as the server last said. */
    public static boolean knowsHere(final ResourceLocation id) {
        return here().knows(id);
    }

    /**
     * Has {@code team} know {@code share} of {@code knowledge}, from 0 to 1, if that is more than it knew; knowledge
     * that comes whole is known whole by any share. Posts a {@link TeamProgressEvent.KnowledgeGained} when it is more.
     *
     * @return whether the team knows more now
     */
    public static boolean grant(final MinecraftServer server, final String team, final Knowledge knowledge,
                                final double share) {
        final double before = share(server, team, knowledge.id());
        final double now = knowledge.partial() ? Math.clamp(share, 0.0, WHOLE) : WHOLE;
        if (now <= before) {
            return false;
        }
        STATE.update(server, team, held -> held.withShare(knowledge.id().toString(), now));
        NeoForge.EVENT_BUS.post(new TeamProgressEvent.KnowledgeGained(server, team, knowledge, before, now));
        return true;
    }

    /** Makes {@code team} forget the knowledge {@code id}: what an operator's command does. */
    public static void forget(final MinecraftServer server, final String team, final ResourceLocation id) {
        STATE.update(server, team, held -> held.withoutShare(id.toString()));
    }

    /**
     * Joins what {@code from} has reached into {@code into}, when two teams merge: the further step along each axis,
     * every milestone either passed, and the more of each piece of knowledge. {@code from} keeps its own.
     */
    public static void merge(final MinecraftServer server, final String from, final String into) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(into, "into");
        if (from.equals(into)) {
            return;
        }
        final Held theirs = of(server, from);
        STATE.update(server, into, held -> held.merged(theirs));
    }

    /**
     * What a team has reached: its furthest level along each axis, by the axis's id; the ids of the milestones it
     * passed; and how much it knows of each piece of knowledge, by its id, from 0 to 1.
     *
     * @param axes       the furthest level along each axis; an axis not there is at its first step
     * @param milestones the milestones passed
     * @param knowledge  how much of each piece of knowledge it knows; one not there it knows none of
     */
    public record Held(Map<String, Integer> axes, Set<String> milestones, Map<String, Double> knowledge) {

        /** Nothing reached. */
        public static final Held NONE = new Held(Map.of(), Set.of(), Map.of());
        public static final Codec<Held> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("axes", Map.of()).forGetter(Held::axes),
                Codec.STRING.listOf().<Set<String>>xmap(Set::copyOf, set -> set.stream().sorted().toList())
                        .optionalFieldOf("milestones", Set.of()).forGetter(Held::milestones),
                Codec.unboundedMap(Codec.STRING, Codec.doubleRange(0.0, WHOLE)).optionalFieldOf("knowledge", Map.of())
                        .forGetter(Held::knowledge)
        ).apply(instance, Held::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Held> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT, MOST), Held::axes,
                ByteBufCodecs.collection(HashSet::new, ByteBufCodecs.STRING_UTF8, MOST), Held::milestones,
                ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.DOUBLE, MOST),
                Held::knowledge,
                (axes, milestones, knowledge) -> new Held(axes, milestones, knowledge));

        public Held {
            axes = Map.copyOf(axes);
            milestones = Set.copyOf(milestones);
            knowledge = Map.copyOf(knowledge);
        }

        /** Its furthest level along the axis {@code axis}. */
        public int step(final ResourceLocation axis) {
            return this.axes.getOrDefault(axis.toString(), 0);
        }

        /** How much it knows of the knowledge {@code id}, from 0 to 1. */
        public double share(final ResourceLocation id) {
            return this.knowledge.getOrDefault(id.toString(), 0.0);
        }

        /** Whether it knows the whole of the knowledge {@code id}. */
        public boolean knows(final ResourceLocation id) {
            return share(id) >= WHOLE;
        }

        Held withStep(final ResourceLocation axis, final int level) {
            final Map<String, Integer> next = new HashMap<>(this.axes);
            next.put(axis.toString(), level);
            return new Held(next, this.milestones, this.knowledge);
        }

        Held withMilestone(final String id) {
            final Set<String> next = new HashSet<>(this.milestones);
            next.add(id);
            return new Held(this.axes, next, this.knowledge);
        }

        Held withShare(final String id, final double share) {
            final Map<String, Double> next = new HashMap<>(this.knowledge);
            next.put(id, share);
            return new Held(this.axes, this.milestones, next);
        }

        Held withoutShare(final String id) {
            final Map<String, Double> next = new HashMap<>(this.knowledge);
            next.remove(id);
            return new Held(this.axes, this.milestones, next);
        }

        Held merged(final Held other) {
            final Map<String, Integer> axesNext = new HashMap<>(this.axes);
            other.axes.forEach((axis, level) -> axesNext.merge(axis, level, Math::max));
            final Set<String> milestonesNext = new HashSet<>(this.milestones);
            milestonesNext.addAll(other.milestones);
            final Map<String, Double> knowledgeNext = new HashMap<>(this.knowledge);
            other.knowledge.forEach((id, share) -> knowledgeNext.merge(id, share, Math::max));
            return new Held(axesNext, milestonesNext, knowledgeNext);
        }

        /** The ids of the knowledge it knows whole, sorted. */
        public List<String> known() {
            return this.knowledge.entrySet().stream().filter(entry -> entry.getValue() >= WHOLE)
                    .map(Map.Entry::getKey).sorted().toList();
        }
    }
}
