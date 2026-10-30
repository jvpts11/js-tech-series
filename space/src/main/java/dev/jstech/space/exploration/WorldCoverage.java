/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.exploration;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.state.CoreState;
import dev.jstech.core.state.CoreStates;
import dev.jstech.core.state.TeamState;
import dev.jstech.core.team.CoreTeams;
import dev.jstech.space.JsSpace;
import dev.jstech.space.api.SpaceWorld;
import dev.jstech.space.api.SpaceWorlds;
import dev.jstech.space.universe.SolarSystemWorlds;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;

/**
 * What each team has seen of each world's ground ({@link ReliefCoverage}): the strips its rockets photograph from
 * orbit, one as they arrive and one more every pass while they stay, and the ground round where its players stand on a
 * world. The Overworld is home, its ground known whole.
 */
public final class WorldCoverage {

    /* How long a pass of a low orbit takes, in ticks, as the world slides under it in the window. */
    public static final long PASS_TICKS = 1_500;
    /* What a team has seen of a world, and of every world, by its id. */
    private static final Codec<ReliefCoverage> COVERAGE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.listOf().fieldOf("strips").forGetter(ReliefCoverage::strips),
            Codec.LONG.listOf().<Set<Long>>xmap(Set::copyOf, List::copyOf).fieldOf("visited")
                    .forGetter(ReliefCoverage::visited)
    ).apply(instance, ReliefCoverage::new));
    private static final Codec<Map<String, ReliefCoverage>> SEEN_CODEC = Codec.unboundedMap(Codec.STRING,
            COVERAGE_CODEC);
    private static final TeamState<Map<String, ReliefCoverage>> STATE = CoreState.builder(
                    ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "world_coverage"), SEEN_CODEC, Map.of())
            .fileName("jsspace_world_coverage")
            .team();
    /* How often a player on a world is looked at, in ticks. */
    private static final int LOOK_EVERY = 20;

    private WorldCoverage() {
    }

    /** Registers the state it is kept in and looks at the players on the worlds, from the mod's constructor. */
    public static void register() {
        CoreStates.register(STATE);
        NeoForge.EVENT_BUS.addListener(PlayerTickEvent.Post.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player && player.tickCount % LOOK_EVERY == 0) {
                stoodOn(player);
            }
        });
    }

    /** What {@code team} has seen of {@code world}'s ground. */
    public static ReliefCoverage of(final MinecraftServer server, final String team, final ResourceLocation world) {
        return STATE.get(server, team).getOrDefault(world.toString(), ReliefCoverage.NONE);
    }

    /** {@code team} photographed the pass of {@code world}'s low orbit crossing its equator at {@code node}, east. */
    public static void photograph(final MinecraftServer server, final String team, final ResourceLocation world,
                                  final double node) {
        change(server, team, world, seen -> seen.photographed(node));
    }

    /**
     * {@code team} photographed the pass of {@code world}'s low orbit under it at the game's tick {@code tick}: the
     * world turns a pass's shift under the orbit with every pass.
     */
    public static void photographAt(final MinecraftServer server, final String team, final ResourceLocation world,
                                    final long tick) {
        photograph(server, team, world, Math.floorMod(tick / PASS_TICKS, ReliefCoverage.PASSES)
                * ReliefCoverage.PASS_SHIFT);
    }

    /** A player of {@code team} stood on {@code world} at the block {@code x}, {@code z}. */
    public static void visit(final MinecraftServer server, final String team, final ResourceLocation world,
                             final double x, final double z) {
        change(server, team, world, seen -> seen.visitedAt(x, z));
    }

    /* A player standing on a world other than the Overworld: the ground round them is seen by their team. */
    private static void stoodOn(final ServerPlayer player) {
        final ResourceLocation world = worldOf(player.level());
        if (world == null || world.equals(SolarSystemWorlds.OVERWORLD) || !player.onGround()) {
            return;
        }
        visit(player.server, CoreTeams.teamOf(player.server, player.getUUID()), world, player.getX(), player.getZ());
    }

    /* The world {@code level} is, by its id among the worlds; none for a level that is not one. */
    private static @Nullable ResourceLocation worldOf(final Level level) {
        for (final Map.Entry<ResourceLocation, SpaceWorld> world : SpaceWorlds.all(level).entrySet()) {
            if (world.getValue().dimension().equals(level.dimension())) {
                return world.getKey();
            }
        }
        return null;
    }

    /* Changes what {@code team} has seen of {@code world}, saving it only when it grew. */
    private static void change(final MinecraftServer server, final String team, final ResourceLocation world,
                               final UnaryOperator<ReliefCoverage> grow) {
        final ReliefCoverage was = of(server, team, world);
        final ReliefCoverage now = grow.apply(was);
        if (now == was) {
            return;
        }
        STATE.update(server, team, held -> {
            final Map<String, ReliefCoverage> next = new HashMap<>(held);
            next.put(world.toString(), now);
            return Map.copyOf(next);
        });
    }
}
