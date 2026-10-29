/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.life;

import dev.jstech.core.air.AirMix;
import dev.jstech.core.air.Lungs;
import dev.jstech.core.dimension.Climate;
import dev.jstech.core.room.SealedRooms;
import dev.jstech.space.api.SpaceSuit;
import dev.jstech.space.api.SpaceSuits;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * A suit's air, which its wearer breathes through J's Core's lungs: a suit worn whole, where the air round it cannot
 * be breathed, gives its wearer the pure oxygen of its tanks at the pressure the A7L held, a third of a bar, and each
 * second of breathing is spent from the tanks; where the air round can be breathed, the wearer breathes that, as with
 * the visor open, and the tanks are spared. A hole leaks: the tanks spend more each second and the suit holds less
 * pressure, until with two holes it no longer holds oxygen enough to breathe; a patch mends one. An oxygen bottle pours
 * its air into the tanks.
 */
public final class SuitAir {

    /** The suit's pressure, sealed, in kilopascals: the 3.7 pounds an inch of pure oxygen the A7L held. */
    public static final double PRESSURE = 25.5;
    /** The share of its pressure each hole loses. */
    public static final double HOLE_DROP = 0.3;
    /** The seconds of air each hole leaks each second, besides the second breathed. */
    public static final int LEAK = 4;
    /* What the tanks hold, and the warmth the suit keeps. */
    private static final AirMix PURE_OXYGEN = new AirMix(Map.of(AirMix.OXYGEN, 1.0));
    private static final double TEMPERED = 20;
    private static final int TICKS_A_SECOND = 20;

    private SuitAir() {
    }

    /** Hands J's Core's lungs the suits' air, and the game's bus each second of breathing, once, from the mod. */
    public static void listen() {
        Lungs.supply(SuitAir::supplied);
        NeoForge.EVENT_BUS.addListener(PlayerTickEvent.Post.class, SuitAir::onTick);
    }

    /**
     * The air {@code body}'s suit gives it now: the tanks' pure oxygen at the suit's pressure, where it wears a suit
     * whole whose tanks hold air and the air round it cannot be breathed; none otherwise.
     */
    public static Optional<Climate> supplied(final LivingEntity body) {
        if (!(body instanceof Player player)) {
            return Optional.empty();
        }
        final Optional<SpaceSuit> suit = SpaceSuits.worn(player);
        if (suit.isEmpty()) {
            return Optional.empty();
        }
        final Level level = player.level();
        final ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (LifeSupport.air(level, chest, suit.get()) <= 0
                || SealedRooms.climateAt(level, BlockPos.containing(player.getEyePosition())).breathable()) {
            return Optional.empty();
        }
        final double kilopascals = pressure(chest);
        return Optional.of(new Climate(kilopascals, TEMPERED, Optional.of(PURE_OXYGEN),
                PURE_OXYGEN.breathable(kilopascals)));
    }

    /** The pressure the suit whose chest piece is {@code chest} holds while its tanks hold air, in kilopascals. */
    public static double pressure(final ItemStack chest) {
        return PRESSURE * Math.max(0, 1 - HOLE_DROP * holes(chest));
    }

    /** The holes in the suit whose chest piece is {@code chest}. */
    public static int holes(final ItemStack chest) {
        return chest.getOrDefault(SuitData.HOLES.get(), 0);
    }

    /** Holes the suit whose chest piece is {@code chest}, as a micrometeoroid does. */
    public static void hole(final ItemStack chest) {
        chest.set(SuitData.HOLES.get(), holes(chest) + 1);
    }

    /** Mends a hole of the suit whose chest piece is {@code chest}; whether there was one. */
    public static boolean patch(final ItemStack chest) {
        final int holes = holes(chest);
        if (holes <= 0) {
            return false;
        }
        if (holes == 1) {
            chest.remove(SuitData.HOLES.get());
        } else {
            chest.set(SuitData.HOLES.get(), holes - 1);
        }
        return true;
    }

    /**
     * A second of breathing for {@code player}: the second its suit gives is spent from the tanks, and what the holes
     * leak with it; nothing where it breathes the air round it.
     */
    public static void second(final Player player) {
        if (player.isCreative() || player.isSpectator() || supplied(player).isEmpty()) {
            return;
        }
        final Optional<SpaceSuit> suit = SpaceSuits.worn(player);
        if (suit.isPresent()) {
            final ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
            final Level level = player.level();
            LifeSupport.setAir(level, chest, suit.get(), LifeSupport.air(level, chest, suit.get()) - 1
                    - LEAK * holes(chest));
        }
    }

    /**
     * Pours up to {@code seconds} of air into the tanks of {@code chest}, if it is a suit's chest piece; how many
     * seconds went in, never past what the tanks hold.
     */
    public static int refill(final Level level, final ItemStack chest, final int seconds) {
        final Optional<SpaceSuit> suit = suitOf(level, chest);
        if (suit.isEmpty() || seconds <= 0) {
            return 0;
        }
        final int left = LifeSupport.air(level, chest, suit.get());
        final int poured = Math.min(seconds, LifeSupport.capacity(level, chest, suit.get()) - left);
        if (poured > 0) {
            LifeSupport.setAir(level, chest, suit.get(), left + poured);
        }
        return Math.max(0, poured);
    }

    /** The suit whose chest piece {@code chest} is, if it is one. */
    public static Optional<SpaceSuit> suitOf(final Level level, final ItemStack chest) {
        final ResourceLocation id = chest.getItemHolder().unwrapKey().map(ResourceKey::location).orElse(null);
        return SpaceSuits.all(level).values().stream().filter(suit -> suit.chest().equals(id)).findFirst();
    }

    private static void onTick(final PlayerTickEvent.Post event) {
        final Player player = event.getEntity();
        if (!player.level().isClientSide() && player.tickCount % TICKS_A_SECOND == 0) {
            second(player);
        }
    }
}
