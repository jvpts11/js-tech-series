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
import dev.jstech.core.mixin.PlayerAccessor;
import dev.jstech.core.mixin.ReceivingLevelScreenAccessor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
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
 * no screen at all, and one it names {@link DimensionTravel#STILL} none either, the last picture drawn before it
 * held over every frame until the land is there.
 */
public final class TravelScreens {

    private static final Map<ResourceLocation, Factory> DECLARED = new ConcurrentHashMap<>();
    /* How long a passage the server announced is waited for before the screen is forgotten, in milliseconds. */
    private static final long WAITS = 10_000L;
    /* How long one passage lasts, for the game's second opening of its loading screen, in milliseconds. */
    private static final long PASSING = 3_000L;
    /* How long a still passage's picture is held at most, as long as the game's own screen waits for the land. */
    private static final long HOLDS = 30_000L;
    /* How far from the eye, in sections every way, the land in sight is built before a still passage lets go. */
    private static final int AROUND = 2;
    private static final AtomicInteger SEAMLESS = new AtomicInteger();
    private static final AtomicInteger STILL = new AtomicInteger();

    private static volatile @Nullable ResourceLocation expected;
    private static volatile long expectedAt;
    /* The passage under way, the screen it is shown through, until when. */
    private static volatile @Nullable ResourceLocation passing;
    private static volatile long passingUntil;
    /*
     * While a still passage holds its picture: whether the server has placed the player, as the game's own loading
     * screen would have been told, and since when the picture is held. Read and written on the game's thread only.
     */
    private static @Nullable BooleanSupplier placed;
    private static long heldSince;

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
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Pre.class, event -> keepTheSway());
        NeoForge.EVENT_BUS.addListener(RenderFrameEvent.Pre.class, event -> keepTheSway());
        NeoForge.EVENT_BUS.addListener(RenderFrameEvent.Post.class, event -> holdStill());
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

    /** How many passages held the last picture before them since the game started, for a test. */
    public static int stillPassages() {
        return STILL.get();
    }

    /** Whether a still passage's picture is held over the frames now, for a test. */
    public static boolean holdingStill() {
        return placed != null;
    }

    /*
     * The game's own loading screen opens: the screen expected stands in for it, or for a seamless passage nothing
     * does, and the world goes on being drawn, or for a still one nothing either, the last picture drawn before it,
     * kept as the passage opens and before anything of the new level is drawn, held over the frames instead. A screen
     * would free the mouse, its pointer seen in the middle, and let go of the keys held. The game opens its loading
     * screen twice in one passage, as it takes up the new level and as it waits for its land, so the passage's screen
     * stands for both; the second is told when the server has placed the player.
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
            } else if (DimensionTravel.STILL.equals(id)) {
                STILL.incrementAndGet();
                StillPicture.take();
                heldSince = now;
            }
        } else if (passing != null && now <= passingUntil) {
            id = passing;
        } else {
            return;
        }
        if (DimensionTravel.STILL.equals(id)) {
            placed = ((ReceivingLevelScreenAccessor) event.getNewScreen()).jscore$levelReceived();
        }
        if (DimensionTravel.SEAMLESS.equals(id) || DimensionTravel.STILL.equals(id)) {
            event.setCanceled(true);
            return;
        }
        final Factory factory = DECLARED.get(id);
        if (factory != null) {
            event.setNewScreen(factory.create(TravelScreens::landed, ReceivingLevelScreen.Reason.OTHER));
        }
    }

    /*
     * The game makes the player anew for the new level: in a passage with no screen the new one takes the old one's
     * swing and the sway of their view, so their hand neither drops nor shows the swing's cooldown, and nothing on the
     * screen tells that the dimension changed. It also takes what the old one holds: the game's new player has empty
     * hands until the server sends their inventory a moment later, and the item coming back would read as a new one,
     * lowering the hand and starting the swing over.
     */
    private static void onClone(final ClientPlayerNetworkEvent.Clone event) {
        if (!screenless()) {
            return;
        }
        final LocalPlayer from = event.getOldPlayer();
        final LocalPlayer to = event.getNewPlayer();
        final Inventory held = from.getInventory();
        for (int slot = 0; slot < held.getContainerSize(); slot++) {
            to.getInventory().setItem(slot, held.getItem(slot).copy());
        }
        to.getInventory().selected = held.selected;
        ((PlayerAccessor) to).jscore$setLastItemInMainHand(((PlayerAccessor) from).jscore$lastItemInMainHand().copy());
        ((LivingEntityAccessor) to).jscore$setAttackStrengthTicker(
                ((LivingEntityAccessor) from).jscore$attackStrengthTicker());
        to.xBob = from.xBob;
        to.yBob = from.yBob;
        to.xBobO = from.xBobO;
        to.yBobO = from.yBobO;
    }

    /*
     * The server keeps a player's heading within one turn, -180 to 180, while their game lets it run on as they turn,
     * so the player made anew for a new level faces the same way by a number whole turns away from the old one's. The
     * hand sways by how far the view has turned past its sway, and would swing round those whole turns, out of sight
     * and back. Through a passage with no screen the sway is kept within half a turn of the heading, before each tick
     * and each frame, the heading set first by the game and then by the server.
     */
    private static void keepTheSway() {
        final LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !screenless()) {
            return;
        }
        player.yBob = player.getYRot() + Mth.wrapDegrees(player.yBob - player.getYRot());
        player.yBobO = player.yRotO + Mth.wrapDegrees(player.yBobO - player.yRotO);
    }

    /*
     * After each frame is drawn, before it is shown: while a still passage holds its picture, the picture is drawn over
     * the whole of it, until the server has placed the player and the land round them is built, when the frame drawn
     * shows that land and the picture is let go; or after as long as the game's own screen would wait, or once the
     * player has left the world.
     */
    private static void holdStill() {
        final BooleanSupplier there = placed;
        if (there == null) {
            return;
        }
        final boolean left = Minecraft.getInstance().level == null;
        if (left || (there.getAsBoolean() && builtAround()) || Util.getMillis() - heldSince > HOLDS) {
            placed = null;
            StillPicture.release();
            return;
        }
        StillPicture.draw();
    }

    /* Whether a passage with no screen of its own, seamless or still, is under way. */
    private static boolean screenless() {
        final ResourceLocation id = passing;
        return (DimensionTravel.SEAMLESS.equals(id) || DimensionTravel.STILL.equals(id))
                && Util.getMillis() <= passingUntil;
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

    /*
     * Whether the land in sight round the player is built to be drawn, and not only the section they stand in, which
     * the game waits for: standing on the ground, that can be the empty one over it, built before the ground is. The
     * land round them has reached their game, so the sections near them can be built, each needing its neighbours;
     * and every section the frame draws within AROUND sections of the eye is built. The game builds only what is in
     * sight, so a section behind the player or over their head is never waited for.
     */
    private static boolean builtAround() {
        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            return false;
        }
        final ChunkPos at = minecraft.player.chunkPosition();
        final int reach = Math.min(AROUND + 1, minecraft.options.getEffectiveRenderDistance() - 1);
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                if (dx * dx + dz * dz <= reach * reach && !level.getChunkSource().hasChunk(at.x + dx, at.z + dz)) {
                    return false;
                }
            }
        }
        final SectionPos eye = SectionPos.of(minecraft.gameRenderer.getMainCamera().getBlockPosition());
        for (final SectionRenderDispatcher.RenderSection section : minecraft.levelRenderer.visibleSections) {
            final SectionPos drawn = SectionPos.of(section.getOrigin());
            final boolean near = Math.abs(drawn.x() - eye.x()) <= AROUND && Math.abs(drawn.y() - eye.y()) <= AROUND
                    && Math.abs(drawn.z() - eye.z()) <= AROUND;
            if (near && section.getCompiled() == SectionRenderDispatcher.CompiledSection.UNCOMPILED) {
                return false;
            }
        }
        return true;
    }

    /** Makes a passage's screen, which closes once {@code ready} says the land is there. */
    @FunctionalInterface
    public interface Factory {

        ReceivingLevelScreen create(BooleanSupplier ready, ReceivingLevelScreen.Reason reason);
    }
}
