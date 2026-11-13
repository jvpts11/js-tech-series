/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.light;

import dev.jstech.core.light.CoreLights;
import dev.jstech.core.light.LightBlock;
import dev.jstech.core.light.LightColours;
import dev.jstech.core.light.LightMath;
import dev.jstech.core.light.LightingSettings;
import dev.jstech.core.stepaside.StepAside;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

/**
 * Dynamic light: a torch held lights the way, a mob on fire lights its surroundings, a glowing creature, an item that
 * gives light lying on the ground, a model's lamp turning on a machine, and what mods hand it
 * ({@link IDynamicLightSource}). Laid as light standing in the air ({@link LightBlock}) in the player's own copy of
 * the world, which the game's light engine spreads as any other: on this game only, nothing of the world changed,
 * what stood there put back as the light moves on. Off by the player's setting, or while a mod of dynamic light
 * does it.
 */
public final class DynamicLightsClient {

    /* What stood where each light is laid, to put it back, and the light laid there. */
    private static final Map<BlockPos, Laid> LAID = new HashMap<>();
    private static final List<IDynamicLightSource> SOURCES = new CopyOnWriteArrayList<>();
    /* Changes on this game only: the neighbours left alone, the mesh built again. */
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;

    private static @Nullable ClientLevel laidIn;

    private DynamicLightsClient() {
    }

    /** Has {@code source}'s lights lit the way too, from now on. */
    public static void provide(final IDynamicLightSource source) {
        SOURCES.add(source);
    }

    /** Whether dynamic light is drawn on this game now. */
    public static boolean on() {
        return LightingSettings.dynamic() && !StepAside.DYNAMIC_LIGHT.steps();
    }

    /** The places light is laid now, and its level at each. */
    public static Map<BlockPos, Integer> laid() {
        final ClientLevel level = laidIn;
        final Map<BlockPos, Integer> out = new HashMap<>();
        if (level != null) {
            LAID.forEach((pos, laid) -> {
                if (level.getBlockState(pos) == laid.placed()) {
                    out.put(pos, laid.placed().getValue(LightBlock.LEVEL));
                }
            });
        }
        return Map.copyOf(out);
    }

    static void listen() {
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> tick());
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> {
            LAID.clear();
            laidIn = null;
        });
    }

    private static void tick() {
        final Minecraft minecraft = Minecraft.getInstance();
        final ClientLevel level = minecraft.level;
        final LocalPlayer player = minecraft.player;
        if (level != laidIn) {
            // The world it was laid in is gone, and what stood there with it.
            LAID.clear();
            laidIn = level;
        }
        if (level == null || player == null) {
            return;
        }
        final Map<BlockPos, Lit> wanted = on() ? wanted(level, player) : Map.of();
        final Iterator<Map.Entry<BlockPos, Laid>> laid = LAID.entrySet().iterator();
        while (laid.hasNext()) {
            final Map.Entry<BlockPos, Laid> entry = laid.next();
            if (!wanted.containsKey(entry.getKey())) {
                // Put back only over the light laid here: one the server has put in its place since stays.
                if (level.getBlockState(entry.getKey()) == entry.getValue().placed()) {
                    level.setBlock(entry.getKey(), entry.getValue().original(), FLAGS);
                }
                laid.remove();
            }
        }
        // White light too: stained glass near may tint it.
        final List<ColouredLights.Source> laidNow = new ArrayList<>();
        for (final Map.Entry<BlockPos, Lit> entry : wanted.entrySet()) {
            final BlockPos pos = entry.getKey();
            final Lit lit = entry.getValue();
            if (lay(level, pos, lit.level())) {
                laidNow.add(new ColouredLights.Source(pos.getX(), pos.getY(), pos.getZ(), lit.level(), lit.colour(),
                        false));
            }
        }
        ColouredLights.dynamic(laidNow);
    }

    /* Lays light of {@code light} at {@code pos}, unless something stands there; whether it is laid. */
    private static boolean lay(final ClientLevel level, final BlockPos pos, final int light) {
        final BlockState state = level.getBlockState(pos);
        final Laid laid = LAID.get(pos);
        if (state.getBlock() instanceof LightBlock) {
            if (laid == null || state != laid.placed()) {
                // The world's own light stands there, a spotlight's, even where this one was laid before: left as
                // it is, and what stood there before it the server's to put back.
                LAID.remove(pos);
                return false;
            }
            if (state.getValue(LightBlock.LEVEL) != light) {
                final BlockState placed = CoreLights.LIGHT.get().at(light, state.getValue(LightBlock.WATERLOGGED));
                level.setBlock(pos, placed, FLAGS);
                LAID.put(pos, new Laid(laid.original(), placed));
            }
            return true;
        }
        if (!LightBlock.fits(state)) {
            LAID.remove(pos);
            return false;
        }
        final BlockState placed = CoreLights.LIGHT.get().at(light, state.is(Blocks.WATER));
        level.setBlock(pos, placed, FLAGS);
        LAID.put(pos, new Laid(state, placed));
        return true;
    }

    /* The light each place should have now, the brightest of what lights it. */
    private static Map<BlockPos, Lit> wanted(final ClientLevel level, final LocalPlayer player) {
        final Map<BlockPos, Lit> out = new HashMap<>();
        final double reach = LightingSettings.reach();
        final Vec3 eye = player.getEyePosition();
        for (final Entity entity : level.entitiesForRendering()) {
            if (entity.position().distanceToSqr(eye) > reach * reach) {
                continue;
            }
            final LightColours.Glow glow = glowOf(entity);
            if (glow != null) {
                want(level, out, entity.getEyePosition(), entity.blockPosition(), glow.level(), glow.colour());
            }
        }
        for (final KitGlows.Glowing part : KitGlows.last()) {
            if (part.level() == level && part.light() > 0 && part.at().distanceToSqr(eye) <= reach * reach) {
                want(level, out, part.at(), BlockPos.containing(part.at()), part.light(), part.colour());
            }
        }
        for (final IDynamicLightSource source : SOURCES) {
            for (final IDynamicLightSource.Light light : source.lights(level)) {
                if (light.at().distanceToSqr(eye) <= reach * reach) {
                    want(level, out, light.at(), BlockPos.containing(light.at()), light.level(), light.colour());
                }
            }
        }
        return out;
    }

    /* The brightest light an entity gives: of itself, or of what it holds or is. */
    private static LightColours.@Nullable Glow glowOf(final Entity entity) {
        LightColours.Glow best = LightColours.of(entity);
        if (entity instanceof LivingEntity living) {
            best = brighter(best, LightColours.held(living.getMainHandItem()));
            best = brighter(best, LightColours.held(living.getOffhandItem()));
        } else if (entity instanceof ItemEntity item) {
            best = brighter(best, LightColours.held(item.getItem()));
        }
        return best;
    }

    private static LightColours.@Nullable Glow brighter(final LightColours.@Nullable Glow a,
                                                        final LightColours.@Nullable Glow b) {
        if (a == null) {
            return b;
        }
        return b == null || a.level() >= b.level() ? a : b;
    }

    /* Wants light at the place of {@code at}, or at {@code fallback} where something stands there. */
    private static void want(final ClientLevel level, final Map<BlockPos, Lit> out, final Vec3 at,
                             final BlockPos fallback, final int light, final int colour) {
        BlockPos pos = BlockPos.containing(at);
        if (!LightBlock.fits(level.getBlockState(pos))) {
            pos = fallback;
            if (!LightBlock.fits(level.getBlockState(pos))) {
                return;
            }
        }
        final Lit had = out.get(pos);
        if (had == null || had.level() < light) {
            out.put(pos.immutable(), new Lit(Math.clamp(light, 1, LightMath.BRIGHTEST), colour));
        }
    }

    /* The light wanted at a place. */
    private record Lit(int level, int colour) {
    }

    /* What stood at a place before light was laid there, and the light laid. */
    private record Laid(BlockState original, BlockState placed) {
    }
}
