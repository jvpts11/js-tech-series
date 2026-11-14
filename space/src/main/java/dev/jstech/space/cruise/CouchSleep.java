/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.cruise;

import dev.jstech.core.input.KeyAction;
import dev.jstech.core.input.KeyActions;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.StructureSeatEntity;
import dev.jstech.core.multiblock.FormedPartBlockEntity;
import dev.jstech.core.text.GameText;
import dev.jstech.space.JsSpace;
import dev.jstech.space.network.CouchSleepPayload;
import dev.jstech.space.rocket.RocketKitBlocks;
import dev.jstech.space.universe.SystemSpaces;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * Sleeping in a crew seat, a capsule's or a lander's: its key, Z until the player changes it, puts a player sitting
 * in one, the rocket out in its system's space, to sleep, and wakes them again. They wake too as they leave the seat
 * or the rocket leaves space, and as the cruise passes in their sleep. Asleep as long as one in a bed must be, they
 * count among those the cruise waits for: it passes when everyone aboard sleeps.
 */
public final class CouchSleep {

    /** The key that puts a player in a crew seat to sleep, and wakes them. */
    public static final KeyAction KEY = KeyActions.declare(KeyAction.builder(
            ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "couch_sleep"), CruiseTexts.SLEEP_KEY)
            .key(GLFW.GLFW_KEY_Z).onServer((player, shift) -> pressed(player)));
    /* How long one sleeps before they count as asleep, in ticks: as long as in a bed. */
    private static final int LONG_ENOUGH = 100;
    /* When each sleeper fell asleep, by the game's tick. */
    private static final Map<ServerPlayer, Long> ASLEEP = new WeakHashMap<>();

    private CouchSleep() {
    }

    /** Declares the key and wakes whoever may sleep no longer, once, from the mod's constructor. */
    public static void declare() {
        NeoForge.EVENT_BUS.addListener(PlayerTickEvent.Post.class, CouchSleep::afterPlayer);
    }

    /**
     * {@code player} pressed the key: asleep, they wake; sitting in a crew seat, the rocket in space, they fall asleep;
     * anywhere else they are told where they can sleep.
     */
    public static void pressed(final ServerPlayer player) {
        if (ASLEEP.containsKey(player)) {
            wake(player);
        } else if (canSleep(player)) {
            ASLEEP.put(player, player.level().getGameTime());
            PacketDistributor.sendToPlayer(player, new CouchSleepPayload(true));
        } else {
            player.displayClientMessage(GameText.component(CruiseTexts.SLEEP_WHERE), true);
        }
    }

    /** Whether {@code player} sleeps in a crew seat. */
    public static boolean asleep(final ServerPlayer player) {
        return ASLEEP.containsKey(player);
    }

    /** Whether {@code player} has slept in a crew seat as long as one in a bed must to count as asleep. */
    public static boolean longEnough(final ServerPlayer player) {
        final Long since = ASLEEP.get(player);
        return since != null && player.level().getGameTime() - since >= LONG_ENOUGH;
    }

    /** Wakes {@code player} in their crew seat, if they sleep there. */
    public static void wake(final ServerPlayer player) {
        if (ASLEEP.remove(player) != null) {
            PacketDistributor.sendToPlayer(player, new CouchSleepPayload(false));
        }
    }

    /* After a player's tick: one asleep whose seat or rocket lets them sleep no longer wakes. */
    private static void afterPlayer(final PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && ASLEEP.containsKey(player) && !canSleep(player)) {
            wake(player);
        }
    }

    /* Whether {@code player} sits in a crew seat of a moving structure out in its system's space. */
    private static boolean canSleep(final ServerPlayer player) {
        return player.getVehicle() instanceof StructureSeatEntity seat
                && seat.structure() instanceof MovingStructure body && body.level() instanceof ServerLevel level
                && SystemSpaces.systemOf(level.dimension()).isPresent() && crewSeat(level, seat.seat());
    }

    /* Whether the block at {@code pos} is a crew seat: merged into a formed capsule or lander, the block it was. */
    private static boolean crewSeat(final ServerLevel level, final BlockPos pos) {
        final BlockState state = level.getBlockEntity(pos) instanceof FormedPartBlockEntity part ? part.original()
                : level.getBlockState(pos);
        return state.is(RocketKitBlocks.CREW_SEAT.get());
    }
}
