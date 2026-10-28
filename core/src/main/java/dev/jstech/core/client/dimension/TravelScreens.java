/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.dimension;

import dev.jstech.core.dimension.DimensionTravel;
import dev.jstech.core.mixin.LivingEntityAccessor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

/**
 * The screens a passage between dimensions can be shown through, declared by mods on the player's game: when the
 * server sends a player to another dimension with one of them, it stands in for the game's "loading terrain" until
 * the land round the player is there to be seen.
 *
 * <pre>{@code
 * TravelScreens.register(REENTRY, (ready, reason) -> new ReentryScreen(ready, reason));
 * }</pre>
 *
 * A screen is a {@link ReceivingLevelScreen} of the mod's own, so it closes as the game's does: once {@code ready}
 * says the land is there, or after half a minute. A passage the server names {@link DimensionTravel#SEAMLESS} opens
 * no screen at all.
 */
public final class TravelScreens {

    private static final Map<ResourceLocation, Factory> DECLARED = new ConcurrentHashMap<>();
    /* How long a passage the server announced is waited for before the screen is forgotten, in milliseconds. */
    private static final long WAITS = 10_000L;
    /* How long one passage lasts, for the game's second opening of its loading screen, in milliseconds. */
    private static final long PASSING = 3_000L;
    private static final AtomicInteger SEAMLESS = new AtomicInteger();

    private static volatile @Nullable ResourceLocation expected;
    private static volatile long expectedAt;
    /* The passage under way, the screen it is shown through, until when. */
    private static volatile @Nullable ResourceLocation passing;
    private static volatile long passingUntil;

    private TravelScreens() {
    }

    /** Declares the screen {@code id}, which a passage the server makes with it is shown through. */
    public static void register(final ResourceLocation id, final Factory factory) {
        DECLARED.put(id, factory);
    }

    /** Hands the game's bus what this answers, once, from the mod's constructor on a player's game. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ScreenEvent.Opening.class, TravelScreens::onOpening);
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.Clone.class, TravelScreens::onClone);
    }

    /** The server said the coming passage is shown through the screen {@code id}. */
    public static void expect(final ResourceLocation id) {
        expected = id;
        expectedAt = Util.getMillis();
    }

    /** The screen the coming passage is shown through, if one is expected, for a test. */
    public static @Nullable ResourceLocation expected() {
        return expected;
    }

    /** How many passages went through no screen at all since the game started, for a test. */
    public static int seamlessPassages() {
        return SEAMLESS.get();
    }

    /*
     * The game's own loading screen opens: the screen expected stands in for it, or for a seamless passage nothing
     * does, and the world goes on being drawn. The game opens it twice in one passage, as it takes up the new level
     * and as it waits for its land, so the passage's screen stands for both.
     */
    private static void onOpening(final ScreenEvent.Opening event) {
        final boolean theGames = event.getNewScreen() != null
                && event.getNewScreen().getClass() == ReceivingLevelScreen.class;
        if (!theGames) {
            return;
        }
        final long now = Util.getMillis();
        ResourceLocation id = expected;
        if (id != null && now - expectedAt <= WAITS) {
            expected = null;
            passing = id;
            passingUntil = now + PASSING;
            if (DimensionTravel.SEAMLESS.equals(id)) {
                SEAMLESS.incrementAndGet();
            }
        } else if (passing != null && now <= passingUntil) {
            id = passing;
        } else {
            return;
        }
        if (DimensionTravel.SEAMLESS.equals(id)) {
            event.setCanceled(true);
            return;
        }
        final Factory factory = DECLARED.get(id);
        if (factory != null) {
            event.setNewScreen(factory.create(TravelScreens::landed, ReceivingLevelScreen.Reason.OTHER));
        }
    }

    /*
     * The game makes the player anew for the new level: in a seamless passage the new one takes the old one's swing
     * and the sway of their view, so their hand neither drops nor shows the swing's cooldown, and nothing on the
     * screen tells that the dimension changed.
     */
    private static void onClone(final ClientPlayerNetworkEvent.Clone event) {
        if (!DimensionTravel.SEAMLESS.equals(passing) || Util.getMillis() > passingUntil) {
            return;
        }
        final LocalPlayer from = event.getOldPlayer();
        final LocalPlayer to = event.getNewPlayer();
        ((LivingEntityAccessor) to).jscore$setAttackStrengthTicker(
                ((LivingEntityAccessor) from).jscore$attackStrengthTicker());
        to.xBob = from.xBob;
        to.yBob = from.yBob;
        to.xBobO = from.xBobO;
        to.yBobO = from.yBobO;
    }

    /* Whether the land round the player is there to be seen: their chunk received and its section built. */
    private static boolean landed() {
        final Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return false;
        }
        final BlockPos at = minecraft.player.blockPosition();
        return minecraft.level.hasChunkAt(at) && minecraft.levelRenderer.isSectionCompiled(at);
    }

    /** Makes a passage's screen, which closes once {@code ready} says the land is there. */
    @FunctionalInterface
    public interface Factory {

        ReceivingLevelScreen create(BooleanSupplier ready, ReceivingLevelScreen.Reason reason);
    }
}
