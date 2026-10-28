/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.exploration;

import com.mojang.serialization.Codec;
import dev.jstech.core.progression.TeamProgress;
import dev.jstech.core.state.CoreState;
import dev.jstech.core.state.CoreStates;
import dev.jstech.core.state.TeamState;
import dev.jstech.space.JsSpace;
import dev.jstech.space.api.WorldProperties;
import dev.jstech.space.api.WorldProperty;
import dev.jstech.space.config.SpaceServerConfig;
import dev.jstech.space.universe.SolarSystemWorlds;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

/**
 * What each team knows of each world: for every property, a share from nothing to all of it, which only grows. A
 * world's share known is the mean of its properties'; at all of it, the team reaches the milestone of the first world
 * known whole. The doors follow, as in reality: a probe is sent only to a world whose orbit and position are known, and
 * a crew lands only where what kills is known (its atmosphere, temperature, radiation, gravity and relief), unless the
 * server's rule says otherwise. Every team knows from the start the Overworld whole, and the orbit and position of the
 * worlds its sky shows to the naked eye, the Moon and Mars.
 */
public final class WorldDiscovery {

    /* A team's knowledge: world, then property, then the share known. */
    private static final Codec<Map<String, Map<String, Double>>> KNOWN_CODEC = Codec.unboundedMap(Codec.STRING,
            Codec.unboundedMap(Codec.STRING, Codec.doubleRange(0.0, 1.0)));
    private static final TeamState<Map<String, Map<String, Double>>> STATE = CoreState.builder(
                    ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "world_discovery"), KNOWN_CODEC, Map.of())
            .fileName("jsspace_world_discovery")
            .synced(ByteBufCodecs.fromCodec(KNOWN_CODEC))
            .team();
    private static final double WHOLE = 1.0;
    /* The worlds whose orbit and position the Overworld's sky shows to the naked eye. */
    private static final Set<ResourceLocation> SEEN_FROM_HOME = Set.of(SolarSystemWorlds.MOON,
            SolarSystemWorlds.MARS);

    private WorldDiscovery() {
    }

    /** Registers the state the knowledge is kept in, from the mod's constructor. */
    public static void register() {
        CoreStates.register(STATE);
    }

    /** How much of {@code property} of {@code world} {@code team} knows, from 0 to 1. */
    public static double known(final MinecraftServer server, final String team, final ResourceLocation world,
                               final ResourceLocation property) {
        if (knownFromHome(world, property)) {
            return WHOLE;
        }
        return STATE.get(server, team).getOrDefault(world.toString(), Map.of()).getOrDefault(property.toString(), 0.0);
    }

    /**
     * Has {@code team} know {@code share} of {@code property} of {@code world}, if that is more than it knew; reaching
     * all of the world reaches the milestone of the first world known whole.
     *
     * @return whether the team knows more now
     */
    public static boolean learn(final MinecraftServer server, final String team, final ResourceLocation world,
                                final ResourceLocation property, final double share) {
        final double now = Math.min(WHOLE, share);
        if (now <= known(server, team, world, property)) {
            return false;
        }
        STATE.update(server, team, held -> {
            final Map<String, Map<String, Double>> next = new HashMap<>(held);
            final Map<String, Double> ofWorld = new HashMap<>(next.getOrDefault(world.toString(), Map.of()));
            ofWorld.put(property.toString(), now);
            next.put(world.toString(), Map.copyOf(ofWorld));
            return Map.copyOf(next);
        });
        if (knownShare(server, team, world) >= WHOLE) {
            TeamProgress.reach(server, team, SpaceMilestones.WORLD_KNOWN_WHOLE);
        }
        return true;
    }

    /** How much of {@code world} {@code team} knows, the mean of every property's share, from 0 to 1. */
    public static double knownShare(final MinecraftServer server, final String team, final ResourceLocation world) {
        final Map<ResourceLocation, WorldProperty> properties = WorldProperties.all();
        if (properties.isEmpty()) {
            return WHOLE;
        }
        double sum = 0;
        for (final ResourceLocation property : properties.keySet()) {
            sum += known(server, team, world, property);
        }
        return sum / properties.size();
    }

    /** Whether {@code team} can send a probe to {@code world}: its orbit and position are known. */
    public static boolean canSendProbe(final MinecraftServer server, final String team,
                                       final ResourceLocation world) {
        return known(server, team, world, SpaceProperties.ORBIT) >= WHOLE;
    }

    /**
     * Whether a crew of {@code team} may land on {@code world}: every essential property is known, or the server lets
     * crews land on worlds they know nothing of.
     */
    public static boolean canLandCrew(final MinecraftServer server, final String team,
                                      final ResourceLocation world) {
        if (!SpaceServerConfig.FILE.get(SpaceServerConfig.LANDING_NEEDS_THE_ESSENTIALS)) {
            return true;
        }
        for (final Map.Entry<ResourceLocation, WorldProperty> property : WorldProperties.all().entrySet()) {
            if (property.getValue().essential() && known(server, team, world, property.getKey()) < WHOLE) {
                return false;
            }
        }
        return true;
    }

    /* The Overworld is home, known whole; the worlds its sky shows, by where they stand. */
    private static boolean knownFromHome(final ResourceLocation world, final ResourceLocation property) {
        return world.equals(SolarSystemWorlds.OVERWORLD)
                || SEEN_FROM_HOME.contains(world) && property.equals(SpaceProperties.ORBIT);
    }
}
