/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.progression;

import com.mojang.serialization.Codec;
import dev.jstech.core.JsCore;
import dev.jstech.core.state.CoreState;
import dev.jstech.core.state.CoreStates;
import dev.jstech.core.state.PlayerState;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.NeoForge;

/**
 * How far each player has come along each axis, kept with the world whether the player is online or not, and sent to
 * each player for their own screens. A player only ever moves forward: reaching a step already passed changes
 * nothing.
 */
public final class PlayerProgress {

    /** The most axes a player's progress is sent with; a world never holds more than a handful. */
    private static final int MOST_AXES = 64;

    /** Each player's furthest level along each axis, by the axis's id; an axis not there is at its first step. */
    private static final PlayerState<Map<String, Integer>> PROGRESS = CoreState.builder(
                    ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "progression"),
                    Codec.unboundedMap(Codec.STRING, Codec.INT), Map.<String, Integer>of())
            .fileName("jstech_progression")
            .synced(ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT, MOST_AXES))
            .player();

    private PlayerProgress() {
    }

    /** Registers the state the progress is kept in, from the Core's constructor. */
    public static void register() {
        CoreStates.register(PROGRESS);
    }

    /** The furthest step {@code player} has reached along {@code axis}. */
    public static <S extends IAxisStep> S reached(final MinecraftServer server, final UUID player,
                                                  final ProgressionAxis<S> axis) {
        return axis.at(PROGRESS.get(server, player).getOrDefault(axis.id().toString(), 0));
    }

    /** The furthest step this client's player has reached along {@code axis}, as the server last said. */
    public static <S extends IAxisStep> S reachedHere(final ProgressionAxis<S> axis) {
        return axis.at(PROGRESS.client().getOrDefault(axis.id().toString(), 0));
    }

    /**
     * Moves {@code player} to {@code step} along {@code axis} if it is further than any step they have reached, and
     * posts an {@link AxisStepReachedEvent} when it is.
     *
     * @return whether the player moved forward
     */
    public static <S extends IAxisStep> boolean reach(final MinecraftServer server, final UUID player,
                                                      final ProgressionAxis<S> axis, final S step) {
        final S before = reached(server, player, axis);
        if (step.level() <= before.level()) {
            return false;
        }
        PROGRESS.update(server, player, kept -> {
            final Map<String, Integer> next = new HashMap<>(kept);
            next.put(axis.id().toString(), step.level());
            return Map.copyOf(next);
        });
        NeoForge.EVENT_BUS.post(new AxisStepReachedEvent(server, player, axis, before, step));
        return true;
    }

    /**
     * Puts {@code player} back at {@code step} along {@code axis}, further or not: what an operator's command does.
     * Moving forward this way posts the event as reaching does.
     */
    public static <S extends IAxisStep> void set(final MinecraftServer server, final UUID player,
                                                 final ProgressionAxis<S> axis, final S step) {
        final S before = reached(server, player, axis);
        PROGRESS.update(server, player, kept -> {
            final Map<String, Integer> next = new HashMap<>(kept);
            if (step.level() == 0) {
                next.remove(axis.id().toString());
            } else {
                next.put(axis.id().toString(), step.level());
            }
            return Map.copyOf(next);
        });
        if (step.level() > before.level()) {
            NeoForge.EVENT_BUS.post(new AxisStepReachedEvent(server, player, axis, before, step));
        }
    }
}
