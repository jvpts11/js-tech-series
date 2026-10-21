/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.creature;

import com.mojang.serialization.MapCodec;
import dev.jstech.core.JsCore;
import dev.jstech.core.data.DataRegistry;
import dev.jstech.core.data.DeclaredData;
import dev.jstech.core.dimension.Climate;
import dev.jstech.core.dimension.DimensionRulesData;
import dev.jstech.core.room.SealedRooms;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The creatures, by the id of their entity type: declared in code by their mods, and written by data packs as files
 * under {@code data/<namespace>/jstech/creatures/}, a file winning over a declaration of its id. A creature's entity
 * is a {@link CreatureEntity} its mod declares; this says how it lives.
 *
 * <p>Where creatures appear is added to the biomes as the server starts, by the Core's biome modifier {@code
 * jscore:creatures}; a data pack read again with the server running changes how creatures live at once, but where
 * they appear only when the server starts again. A creature appears only in a dimension it names, placed as it says,
 * and where its air suits it.
 */
public final class Creatures {

    private static final DeclaredData<CreatureDef> DEFS = new DeclaredData<>(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "creatures"), "jstech/creatures", CreatureDef.CODEC);
    private static final DeferredRegister<MapCodec<? extends BiomeModifier>> MODIFIERS = DeferredRegister.create(
            NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, JsCore.MODID);

    /** {@code jscore:creatures}: adds where every creature appears to the biomes it names. */
    public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<CreaturesBiomeModifier>>
            BIOME_MODIFIER = MODIFIERS.register("creatures", () -> CreaturesBiomeModifier.CODEC);

    private Creatures() {
    }

    /** Hands the buses the biome modifier and the check of where a creature appears, once, from the Core. */
    public static void listen(final IEventBus modBus) {
        MODIFIERS.register(modBus);
        NeoForge.EVENT_BUS.addListener(MobSpawnEvent.SpawnPlacementCheck.class, Creatures::onSpawnCheck);
    }

    /** Declares how the creature whose entity type is {@code entity} lives, while no data pack says otherwise. */
    public static CreatureDef declare(final ResourceLocation entity, final CreatureDef def) {
        return DEFS.declare(entity, def);
    }

    /** How a creature of {@code type} lives on the side {@code level} is on, or none. */
    public static Optional<CreatureDef> get(final Level level, final EntityType<?> type) {
        return DEFS.get(level, BuiltInRegistries.ENTITY_TYPE.getKey(type));
    }

    /** How every creature lives on the side {@code level} is on, by the id of its entity type. */
    public static Map<ResourceLocation, CreatureDef> all(final Level level) {
        return DEFS.all(level);
    }

    /** The data packs' files of creatures, as the server read them. */
    public static DataRegistry<CreatureDef> files() {
        return DEFS.files();
    }

    /** How every creature lives as the server read it: the files over the declared. */
    static Map<ResourceLocation, CreatureDef> onServer() {
        final Map<ResourceLocation, CreatureDef> all = new TreeMap<>(DEFS.declared());
        all.putAll(DEFS.files().entries());
        return all;
    }

    /** Which reading of the data packs the server has: a creature rebuilds what it does when this changes. */
    static long version() {
        return DEFS.files().version();
    }

    /*
     * A creature appears only in a dimension it names, placed as it says, and where it can breathe. What it stands in
     * is read from where the game asks: while a chunk is made, the region being made, never the level, whose asking
     * for that same chunk would wait for itself for ever; and its air then the dimension's, no room being there yet.
     */
    private static void onSpawnCheck(final MobSpawnEvent.SpawnPlacementCheck event) {
        final ServerLevelAccessor where = event.getLevel();
        final ServerLevel level = where.getLevel();
        final Optional<CreatureDef> def = get(level, event.getEntityType());
        if (def.isEmpty()) {
            return;
        }
        final BlockPos pos = event.getPos();
        boolean placed = false;
        for (final CreatureDef.Spawn spawn : def.get().spawns()) {
            if (spawn.in(level.dimension()) && spawn.in(where.getBiome(pos))
                    && spawn.placement().rule().isSpawnPositionOk(where, pos, event.getEntityType())) {
                placed = true;
                break;
            }
        }
        final Climate air = where == level ? SealedRooms.climateAt(level, pos.above())
                : DimensionRulesData.of(level.dimension()).at(pos.getY() + 1);
        if (!placed || !def.get().breathes(air)) {
            event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
        }
    }
}
