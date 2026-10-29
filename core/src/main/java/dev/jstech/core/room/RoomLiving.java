/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.room;

import dev.jstech.core.JsCore;
import dev.jstech.core.air.AirMix;
import dev.jstech.core.air.Lungs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.level.BlockGrowFeatureEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;

/**
 * Living things and the air of rooms and places: mobs breathe by their kind, a cow suffocating in a vacuum and a zombie
 * not; whoever breathes takes oxygen from a room and gives it carbon dioxide, and so does a fire; plants grow only
 * where there is air, and in a room they slowly turn its carbon dioxide back into oxygen, so a greenhouse helps a room
 * breathe. The rates are the game's, faster than life's, so a room goes stale in an afternoon rather than a month.
 */
public final class RoomLiving {

    /** The kinds of mob that do not breathe: the undead, golems, and the rest a mod or data pack puts there. */
    public static final TagKey<EntityType<?>> BREATHES_NOTHING = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "breathes_nothing"));
    /** The blocks that breathe carbon dioxide out as oxygen: leaves, crops, saplings, and any a mod adds. */
    public static final TagKey<Block> MAKES_OXYGEN = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "makes_oxygen"));
    /** The oxygen a player breathes in a second, in moles, and a mob's. */
    public static final double PLAYER_BREATH = 0.01;
    public static final double MOB_BREATH = 0.005;
    /** The carbon dioxide a plant turns into oxygen in a second, in moles: five plants keep up with a player. */
    public static final double PLANT_BREATH = 0.002;
    /** The oxygen a fire burns in a second, in moles. */
    public static final double FIRE_BURNS = 0.2;
    private static final int TICKS_A_SECOND = 20;

    private RoomLiving() {
    }

    /** Hands the game's bus what this answers, once, from the Core's constructor. */
    static void listen() {
        NeoForge.EVENT_BUS.addListener(LivingBreatheEvent.class, RoomLiving::onBreathe);
        NeoForge.EVENT_BUS.addListener(CropGrowEvent.Pre.class, RoomLiving::onCropGrow);
        NeoForge.EVENT_BUS.addListener(BlockGrowFeatureEvent.class, RoomLiving::onGrowFeature);
    }

    /** Whether {@code state} holds water. */
    public static boolean water(final BlockState state) {
        return state.getFluidState().is(FluidTags.WATER);
    }

    /** Whether {@code state} is a plant that breathes carbon dioxide out as oxygen. */
    public static boolean plant(final BlockState state) {
        return state.is(MAKES_OXYGEN);
    }

    /** Whether a mob of {@code type} breathes. */
    public static boolean breathes(final EntityType<?> type) {
        return !type.is(BREATHES_NOTHING);
    }

    /** A second of the breathing in {@code room}: its plants, whoever breathed in it, and its fires. */
    static void second(final LevelRooms rooms, final Room room) {
        if (room.plants() > 0) {
            final double turned = Math.min(room.moles(AirMix.CARBON_DIOXIDE), room.plants() * PLANT_BREATH);
            room.add(AirMix.CARBON_DIOXIDE, -turned);
            room.add(AirMix.OXYGEN, turned);
        }
        final double used = rooms.breathed(room.id()) + room.fires().size() * FIRE_BURNS;
        if (used > 0) {
            final double burned = Math.min(room.moles(AirMix.OXYGEN), used);
            room.add(AirMix.OXYGEN, -burned);
            room.add(AirMix.CARBON_DIOXIDE, burned);
        }
    }

    /*
     * Whoever breathes takes from the air of the room they are in, unless they breathe air of their own, a suit's
     * tanks; a mob that breathes, out of the water, chokes in air it cannot breathe. A player's air is the dimension's
     * hazards' to take.
     */
    private static void onBreathe(final LivingBreatheEvent event) {
        final LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level)) {
            return;
        }
        final boolean player = entity instanceof Player;
        if (!player && !breathes(entity.getType()) || Lungs.supplied(entity).isPresent()) {
            return;
        }
        final BlockPos eyes = BlockPos.containing(entity.getEyePosition());
        final Room room = SealedRooms.roomAt(level, eyes).orElse(null);
        if (room != null) {
            Rooms.of(level).breathe(room.id(), (player ? PLAYER_BREATH : MOB_BREATH) / TICKS_A_SECOND);
        }
        if (player || !RoomBalance.mobsBreathe() || entity.isEyeInFluid(FluidTags.WATER) || !event.canBreathe()) {
            return;
        }
        if (!SealedRooms.breathableAt(level, eyes)) {
            event.setCanBreathe(false);
        }
    }

    private static void onCropGrow(final CropGrowEvent.Pre event) {
        if (!airless(event.getLevel(), event.getPos())) {
            return;
        }
        event.setResult(CropGrowEvent.Pre.Result.DO_NOT_GROW);
    }

    private static void onGrowFeature(final BlockGrowFeatureEvent event) {
        if (airless(event.getLevel(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    /* Whether a plant at {@code pos} has no air to grow in. */
    private static boolean airless(final LevelAccessor level, final BlockPos pos) {
        return RoomBalance.plantsNeedAir() && level instanceof ServerLevel server
                && SealedRooms.climateAt(server, pos).pressure() < GasMath.PLANTS_NEED;
    }
}
