/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.life;

import dev.jstech.core.dimension.DimensionHazardEvent;
import dev.jstech.core.hazard.DamageKind;
import dev.jstech.core.hazard.DamageKinds;
import dev.jstech.core.hazard.Resistances;
import dev.jstech.core.jstechseries.hazard.CosmicRadiation;
import dev.jstech.core.room.SealedRooms;
import dev.jstech.space.api.SpaceSuit;
import dev.jstech.space.api.SpaceSuits;
import dev.jstech.space.api.SpaceWorlds;
import dev.jstech.space.api.SuitModule;
import dev.jstech.space.api.SuitModules;
import dev.jstech.space.api.WorldDanger;
import dev.jstech.space.api.WorldDangers;
import dev.jstech.space.universe.SolarSystemWorlds;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Life support: a pressure suit worn whole lets its wearer breathe where there is no air for as long as its tanks
 * hold, each second of breathing spent from them, the only clock of a walk outside, and stand any cold or heat within
 * its range. The air left lives on the piece worn on the chest, which carries the tanks; only refilling them gives it
 * back. Modules fitted to the pieces add to the tanks, stop the sky's radiation, or light the way. And each world says
 * its dangers, and how much of the sky's radiation reaches its open ground.
 */
public final class LifeSupport {

    /* How many of the game's days pass in an hour, each twenty minutes long. */
    private static final double GAME_DAYS_AN_HOUR = 3;
    /* The dose the sky gives on the open ground, in millisieverts a day of the game: as Chang'e 4 and Curiosity
    measured it a day of their own. */
    private static final double MOON_DOSE = 1.4;
    private static final double MARS_DOSE = 0.7;

    private LifeSupport() {
    }

    /** Hands the game's bus what life support answers, and declares the worlds' dangers, once, from the constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(DimensionHazardEvent.class, LifeSupport::onHazard);
        Resistances.source(LifeSupport::fitted);
        CosmicRadiation.declare(dimension(SolarSystemWorlds.MOON), MOON_DOSE * GAME_DAYS_AN_HOUR);
        CosmicRadiation.declare(dimension(SolarSystemWorlds.MARS), MARS_DOSE * GAME_DAYS_AN_HOUR);
        WorldDangers.declare(SolarSystemWorlds.MOON, List.of(WorldDanger.VACUUM, WorldDanger.HEAT, WorldDanger.COLD,
                WorldDanger.COSMIC_RADIATION, WorldDanger.SOLAR_STORMS, WorldDanger.DUST, WorldDanger.GRAVITY));
        WorldDangers.declare(SolarSystemWorlds.MARS, List.of(WorldDanger.THIN_AIR, WorldDanger.COLD,
                WorldDanger.POISONOUS_AIR, WorldDanger.COSMIC_RADIATION, WorldDanger.SOLAR_STORMS,
                WorldDanger.PERCHLORATES, WorldDanger.GRAVITY));
    }

    /**
     * The seconds of breathing the tanks of {@code chest}, the chest piece of {@code suit}, hold full: the suit's, and
     * a share more for each module of air fitted to it.
     */
    public static int capacity(final Level level, final ItemStack chest, final SpaceSuit suit) {
        double share = 1;
        for (final SuitModule module : modules(level, chest)) {
            if (module.kind() == SuitModule.Kind.AIR) {
                share += module.amount();
            }
        }
        return (int) Math.round(suit.air() * share);
    }

    /** The seconds of breathing left in the tanks of {@code chest}, the chest piece of {@code suit}. */
    public static int air(final Level level, final ItemStack chest, final SpaceSuit suit) {
        final int full = capacity(level, chest, suit);
        return Math.min(full, chest.getOrDefault(SuitData.AIR.get(), full));
    }

    /** Sets the seconds of breathing left in the tanks of {@code chest}, never past what they hold. */
    public static void setAir(final Level level, final ItemStack chest, final SpaceSuit suit, final int seconds) {
        chest.set(SuitData.AIR.get(), Math.max(0, Math.min(capacity(level, chest, suit), seconds)));
    }

    /** The modules fitted to {@code piece}, as the side {@code level} is on knows them. */
    public static List<SuitModule> modules(final Level level, final ItemStack piece) {
        final List<SuitModule> out = new ArrayList<>();
        for (final ResourceLocation id : fittedIds(piece)) {
            SuitModules.get(level, id).ifPresent(out::add);
        }
        return out;
    }

    /** The ids of the modules fitted to {@code piece}. */
    public static List<ResourceLocation> fittedIds(final ItemStack piece) {
        final List<ResourceLocation> ids = piece.get(SuitData.MODULES.get());
        return ids == null ? List.of() : ids;
    }

    /**
     * Fits the module {@code module} to {@code piece}, if it is a suit's piece where the module goes and the module is
     * not on it already; whether it did.
     */
    public static boolean fit(final Level level, final ItemStack piece, final ResourceLocation module) {
        final Optional<SuitModule> fitted = SuitModules.get(level, module);
        if (fitted.isEmpty() || !isPiece(level, piece, fitted.get().piece())) {
            return false;
        }
        final List<ResourceLocation> on = fittedIds(piece);
        if (on.contains(module)) {
            return false;
        }
        final List<ResourceLocation> more = new ArrayList<>(on);
        more.add(module);
        piece.set(SuitData.MODULES.get(), List.copyOf(more));
        return true;
    }

    /* A suit worn whole breathes from its tanks and stands the cold and heat of its range. */
    private static void onHazard(final DimensionHazardEvent event) {
        final Player player = event.player();
        final Optional<SpaceSuit> suit = SpaceSuits.worn(player);
        if (suit.isEmpty()) {
            return;
        }
        final ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        switch (event.hazard()) {
            case AIR -> {
                // Weighed once a second: a second of breathing from the tanks, while they hold any.
                final int left = air(player.level(), chest, suit.get());
                if (left > 0) {
                    setAir(player.level(), chest, suit.get(), left - 1);
                    event.setCanceled(true);
                }
            }
            case COLD, HEAT -> {
                if (suit.get().stands(SealedRooms.climateAt(player.level(), player.blockPosition()).temperature())) {
                    event.setCanceled(true);
                }
            }
            default -> {
                // Whoever breathes the tanks' air breathes nothing of the place's.
                if (air(player.level(), chest, suit.get()) > 0) {
                    event.setCanceled(true);
                }
            }
        }
    }

    /* What the radiation modules fitted to a worn piece stop of the sky's radiation. */
    private static double fitted(final ItemStack stack, final DamageKind kind) {
        if (kind != DamageKinds.RADIATION) {
            return 0;
        }
        double share = 0;
        for (final ResourceLocation id : fittedIds(stack)) {
            final SuitModule module = SuitModules.declared().get(id);
            if (module != null && module.kind() == SuitModule.Kind.RADIATION) {
                share += module.amount();
            }
        }
        return share;
    }

    /* Whether {@code piece} is some suit's piece worn in {@code slot}. */
    private static boolean isPiece(final Level level, final ItemStack piece, final EquipmentSlot slot) {
        final Equipable worn = Equipable.get(piece);
        if (worn == null || worn.getEquipmentSlot() != slot) {
            return false;
        }
        final ResourceLocation id = piece.getItemHolder().unwrapKey().map(ResourceKey::location).orElse(null);
        return SpaceSuits.all(level).values().stream().anyMatch(suit -> switch (slot) {
            case HEAD -> suit.helmet().equals(id);
            case CHEST -> suit.chest().equals(id);
            case LEGS -> suit.legs().equals(id);
            case FEET -> suit.feet().equals(id);
            default -> false;
        });
    }

    private static ResourceKey<Level> dimension(final ResourceLocation world) {
        return SpaceWorlds.declared().get(world).dimension();
    }
}
