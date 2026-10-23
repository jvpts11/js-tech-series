/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.registry;

import com.mojang.serialization.Codec;
import dev.jstech.core.JsCore;
import dev.jstech.core.grid.LevelGrids;
import dev.jstech.core.hazard.BodyWarmth;
import dev.jstech.core.hazard.HazardLevels;
import dev.jstech.core.hazard.HazardMap;
import dev.jstech.core.hazard.LevelRadiation;
import dev.jstech.core.heat.LevelHeat;
import dev.jstech.core.mechanical.LevelMechanics;
import dev.jstech.core.moving.Footing;
import dev.jstech.core.moving.LevelMovingStructures;
import dev.jstech.core.multiblock.LevelStructures;
import dev.jstech.core.persistence.CoreChunkData;
import dev.jstech.core.persistence.SaveLayout;
import dev.jstech.core.physics.Laws;
import dev.jstech.core.uuid.NetworkUuid;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Registers the mod's {@link AttachmentType}s.
 */
public final class CoreAttachments {

    private CoreAttachments() {
    }

    private static final Codec<NetworkUuid> NETWORK_UUID_CODEC =
            UUIDUtil.CODEC.xmap(NetworkUuid::new, NetworkUuid::value);

    private static final Codec<CoreChunkData> CHUNK_DATA_CODEC =
            NETWORK_UUID_CODEC.listOf().xmap(
                    list -> CoreChunkData.of(new LinkedHashSet<>(list)),
                    data -> List.copyOf(data.networks()));

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, JsCore.MODID);

    /**
     * A dimension's grids of every kind but data, with the work that shares what each carries; never saved, since the
     * cables put themselves back as they load.
     */
    public static final Supplier<AttachmentType<LevelGrids>> GRIDS =
            ATTACHMENT_TYPES.register("grids", () -> AttachmentType.builder(LevelGrids::new).build());

    /**
     * A dimension's heat: the blocks that hold heat and are not in balance; never saved, since each block's heat is
     * saved with it and wakes as it loads.
     */
    public static final Supplier<AttachmentType<LevelHeat>> HEAT =
            ATTACHMENT_TYPES.register("heat", () -> AttachmentType.builder(LevelHeat::new).build());

    /**
     * A dimension's mechanical power: its networks of parts that turn; never saved, since each part's speed is saved
     * with it and its network is joined again as it loads.
     */
    public static final Supplier<AttachmentType<LevelMechanics>> MECHANICS =
            ATTACHMENT_TYPES.register("mechanics", () -> AttachmentType.builder(LevelMechanics::new).build());

    /**
     * A dimension's multiblock structures, by the places their controllers watch; never saved, since each controller
     * files itself again as it loads.
     */
    public static final Supplier<AttachmentType<LevelStructures>> STRUCTURES =
            ATTACHMENT_TYPES.register("structures", () -> AttachmentType.builder(LevelStructures::new).build());

    /** The generation a chunk has had, by the ids of the work generating old chunks: saved with the chunk. */
    public static final Supplier<AttachmentType<List<String>>> RETROGEN =
            ATTACHMENT_TYPES.register("retrogen", () -> AttachmentType.builder(() -> List.<String>of())
                    .serialize(Codec.STRING.listOf()).build());

    /** The laws an entity carries with it, wherever it stands: saved with it. */
    public static final Supplier<AttachmentType<Laws>> OWN_LAWS =
            ATTACHMENT_TYPES.register("own_laws", () -> AttachmentType.builder(() -> Laws.NONE)
                    .serialize(Laws.CODEC).build());

    /** What has built up of each hazard in a living thing: saved with it, and gone with its death. */
    public static final Supplier<AttachmentType<HazardLevels>> HAZARDS =
            ATTACHMENT_TYPES.register("hazards", () -> AttachmentType.builder(HazardLevels::new)
                    .serialize(HazardLevels.CODEC).build());

    /** How warm a living thing's core is, and how far it is used to a climate: saved with it, born again normal. */
    public static final Supplier<AttachmentType<BodyWarmth>> BODY_WARMTH =
            ATTACHMENT_TYPES.register("body_warmth", () -> AttachmentType.builder(() -> BodyWarmth.NORMAL)
                    .serialize(BodyWarmth.CODEC).build());

    /** A dimension's sources of radiation; never saved, since each block that gives it off says so as it loads. */
    public static final Supplier<AttachmentType<LevelRadiation>> RADIATION =
            ATTACHMENT_TYPES.register("radiation", () -> AttachmentType.builder(LevelRadiation::new).build());

    /**
     * A dimension's moving structures loaded now, on the server and on a player's game; never saved, since each
     * structure is saved with its entity and files itself again as it loads.
     */
    public static final Supplier<AttachmentType<LevelMovingStructures>> MOVING_STRUCTURES =
            ATTACHMENT_TYPES.register("moving_structures",
                    () -> AttachmentType.builder(LevelMovingStructures::new).build());

    /**
     * The moving structure a player stands on and where on it, by the structure's own places: saved with the player,
     * so one who leaves the game on a deck in flight comes back standing where they were on it.
     */
    public static final Supplier<AttachmentType<Footing>> FOOTING =
            ATTACHMENT_TYPES.register("footing", () -> AttachmentType.builder(() -> Footing.NONE)
                    .serialize(Footing.CODEC).build());

    /** What a player knows of the land's hazards, chunk by chunk: saved with them, and kept through death. */
    public static final Supplier<AttachmentType<HazardMap>> HAZARD_MAP =
            ATTACHMENT_TYPES.register("hazard_map", () -> AttachmentType.builder(HazardMap::new)
                    .serialize(HazardMap.CODEC).copyOnDeath().build());

    /**
     * The recipe a player chose among those that fit one grid, kept under the recipes' ids: saved with them, and kept
     * through death.
     */
    public static final Supplier<AttachmentType<Map<String, ResourceLocation>>> RECIPE_CHOICES =
            ATTACHMENT_TYPES.register("recipe_choices", () -> AttachmentType.<Map<String, ResourceLocation>>builder(
                            () -> Map.of())
                    .serialize(Codec.unboundedMap(Codec.STRING, ResourceLocation.CODEC)).copyOnDeath().build());

    /** The recipe a furnace was set to smelt each item by, among those that fit it, by the item's id. */
    public static final Supplier<AttachmentType<Map<ResourceLocation, ResourceLocation>>> FURNACE_CHOICES =
            ATTACHMENT_TYPES.register("furnace_choices", () -> AttachmentType
                    .<Map<ResourceLocation, ResourceLocation>>builder(() -> Map.of())
                    .serialize(Codec.unboundedMap(ResourceLocation.CODEC, ResourceLocation.CODEC)).build());

    /** The layout a chunk's networks are saved in; a chunk saved before it had one holds the bare list. */
    public static final SaveLayout CHUNK_NETWORKS_LAYOUT = SaveLayout.of(JsCore.MODID + ":chunk_networks");

    public static final Supplier<AttachmentType<CoreChunkData>> CHUNK_NETWORKS =
            ATTACHMENT_TYPES.register("chunk_networks",
                    () -> AttachmentType.builder(CoreChunkData::empty)
                            .serialize(CHUNK_NETWORKS_LAYOUT.codec(CHUNK_DATA_CODEC))
                            .build());

    public static void register(final IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
