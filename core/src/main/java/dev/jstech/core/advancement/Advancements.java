/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.advancement;

import dev.jstech.core.progression.AxisStepReachedEvent;
import dev.jstech.core.progression.PlayerProgress;
import dev.jstech.core.progression.ProgressionAxes;
import dev.jstech.core.progression.ProgressionAxis;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.jetbrains.annotations.Nullable;

/**
 * How a mod reports what its advancements are earned by, and how the Core pays out the steps a player reaches along
 * the progression axes: at once when the player is here, and again each time they join, so a step reached while they
 * were away, or an advancement added since, is earned all the same.
 */
public final class Advancements {

    private Advancements() {
    }

    /** Reports that {@code player} caused {@code event}; nothing happens for nobody, or for a machine acting as one. */
    public static void award(@Nullable final Player player, final ResourceLocation event) {
        award(player, event, "");
    }

    /** The same, with the detail that tells apart the criteria of an advancement asking for all of something. */
    public static void award(@Nullable final Player player, final ResourceLocation event,
                             @Nullable final String detail) {
        if (player instanceof ServerPlayer server && !(player instanceof FakePlayer)) {
            CoreTriggers.EVENT.get().trigger(server, event, detail == null ? "" : detail);
        }
    }

    /** Hands the game's bus what this answers, once, from the mod's constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(AxisStepReachedEvent.class, Advancements::onStepReached);
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, Advancements::onLoggedIn);
    }

    public static void onStepReached(final AxisStepReachedEvent event) {
        final ServerPlayer player = event.server().getPlayerList().getPlayer(event.player());
        if (player != null) {
            CoreTriggers.AXIS_STEP.get().trigger(player, event.axis().id(), event.to().level());
        }
    }

    public static void onLoggedIn(final PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !(player instanceof FakePlayer)) {
            for (final ProgressionAxis<?> axis : ProgressionAxes.all()) {
                final int level = PlayerProgress.reached(player.server, player.getUUID(), axis).level();
                if (level > 0) {
                    CoreTriggers.AXIS_STEP.get().trigger(player, axis.id(), level);
                }
            }
        }
    }
}
