/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.dimension;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
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
 * says the land is there, or after half a minute.
 */
public final class TravelScreens {

    private static final Map<ResourceLocation, Factory> DECLARED = new ConcurrentHashMap<>();
    /* How long a passage the server announced is waited for before the screen is forgotten, in milliseconds. */
    private static final long WAITS = 10_000L;

    private static volatile @Nullable ResourceLocation expected;
    private static volatile long expectedAt;

    private TravelScreens() {
    }

    /** Declares the screen {@code id}, which a passage the server makes with it is shown through. */
    public static void register(final ResourceLocation id, final Factory factory) {
        DECLARED.put(id, factory);
    }

    /** Hands the game's bus what this answers, once, from the mod's constructor on a player's game. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ScreenEvent.Opening.class, TravelScreens::onOpening);
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

    /* The game's own loading screen opens: the screen expected stands in for it. */
    private static void onOpening(final ScreenEvent.Opening event) {
        final ResourceLocation id = expected;
        final boolean theGames = event.getNewScreen() != null
                && event.getNewScreen().getClass() == ReceivingLevelScreen.class;
        if (id == null || !theGames) {
            return;
        }
        expected = null;
        final Factory factory = DECLARED.get(id);
        if (factory != null && Util.getMillis() - expectedAt <= WAITS) {
            event.setNewScreen(factory.create(TravelScreens::landed, ReceivingLevelScreen.Reason.OTHER));
        }
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
