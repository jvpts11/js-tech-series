/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.dimension;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.DimensionTransition;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerRespawnPositionEvent;

/**
 * Where players come back after dying, by rules mods give for their dimensions: the game sends a player back to their
 * bed or the world's spawn whatever the dimension, and a mod may want a bed to count only in a room of air, or its
 * dead sent home. Rules are asked in the order they were added; the first to say where wins, and with none the game's
 * own choice stands.
 *
 * <pre>{@code
 * DimensionRespawns.add((player, died, planned) -> died.dimension().equals(MY_DUNGEON)
 *         ? Optional.of(new DimensionTransition(died, DOOR, Vec3.ZERO, 0, 0, DimensionTransition.DO_NOTHING))
 *         : Optional.empty());
 * }</pre>
 */
public final class DimensionRespawns {

    private static final List<IRespawnRule> RULES = new CopyOnWriteArrayList<>();

    private DimensionRespawns() {
    }

    /** Adds {@code rule}, after every rule added before it, while the game loads. */
    public static void add(final IRespawnRule rule) {
        RULES.add(Objects.requireNonNull(rule, "rule"));
    }

    /** Hands the game's bus what asks the rules, once, from the Core's constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(PlayerRespawnPositionEvent.class, DimensionRespawns::onRespawn);
    }

    /** Where {@code player} comes back, by the rules: the first rule's answer, or {@code planned} with none. */
    public static DimensionTransition respawn(final ServerPlayer player, final DimensionTransition planned) {
        for (final IRespawnRule rule : RULES) {
            final Optional<DimensionTransition> answer = rule.respawn(player, player.serverLevel(), planned);
            if (answer.isPresent()) {
                return answer.get();
            }
        }
        return planned;
    }

    private static void onRespawn(final PlayerRespawnPositionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            final DimensionTransition chosen = respawn(player, event.getDimensionTransition());
            if (chosen != event.getDimensionTransition()) {
                event.setDimensionTransition(chosen);
            }
        }
    }
}
