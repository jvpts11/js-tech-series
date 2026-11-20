/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.landing;

import com.mojang.serialization.Codec;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.StructureFooting;
import dev.jstech.core.moving.StructureSeatEntity;
import dev.jstech.space.network.LiftOffsPayload;
import dev.jstech.space.universe.SystemSpaces;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Where each player last took off from in each world: the place under their ship as it lifted off the land, noted for
 * everyone aboard it, so its pilot's HUD shows them the way back there by hand. A system's space has no land to come
 * back to and notes none. Kept with the world; a player's places are sent to their game as they join and as one of
 * them changes.
 */
public final class ShipLiftOffs extends SavedData {

    private final Map<UUID, Map<ResourceKey<Level>, BlockPos>> places = new HashMap<>();

    private static final String FILE = "jsspace_ship_lift_offs";
    private static final String PLACES = "Places";
    private static final Codec<Map<UUID, Map<ResourceKey<Level>, BlockPos>>> CODEC = Codec.unboundedMap(
            UUIDUtil.STRING_CODEC, Codec.unboundedMap(ResourceKey.codec(Registries.DIMENSION), BlockPos.CODEC));

    private ShipLiftOffs() {
    }

    /** Hands the game's bus what tells a player joining where they last took off from, once, as the mod starts. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedInEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                tell(player, of(player.server));
            }
        });
    }

    /**
     * {@code ship} lifted off the land of {@code level}: the place under its middle is where everyone aboard it,
     * sitting in it or standing on it, last took off from in this world.
     */
    public static void liftedOff(final ServerLevel level, final MovingStructure ship) {
        if (SystemSpaces.systemOf(level.dimension()).isPresent()) {
            return;
        }
        final AABB box = ship.worldBox();
        final BlockPos place = BlockPos.containing(box.getCenter().x, box.minY, box.getCenter().z);
        for (final ServerPlayer player : level.players()) {
            if (StructureFooting.standingOn(player) == ship
                    || player.getVehicle() instanceof StructureSeatEntity seat && seat.structure() == ship) {
                note(player, level, place);
            }
        }
    }

    /** Notes {@code place} of {@code level} as where {@code player} last took off from in it, and tells their game. */
    public static void note(final ServerPlayer player, final ServerLevel level, final BlockPos place) {
        final ShipLiftOffs all = of(level.getServer());
        if (!place.equals(all.places.computeIfAbsent(player.getUUID(), id -> new HashMap<>())
                .put(level.dimension(), place.immutable()))) {
            all.setDirty();
            tell(player, all);
        }
    }

    /** Where {@code player} last took off from in {@code dimension}, if they ever did. */
    public static Optional<BlockPos> of(final ServerPlayer player, final ResourceKey<Level> dimension) {
        return Optional.ofNullable(of(player.server).places.getOrDefault(player.getUUID(), Map.of()).get(dimension));
    }

    @Override
    public CompoundTag save(final CompoundTag tag, final HolderLookup.Provider registries) {
        CODEC.encodeStart(NbtOps.INSTANCE, this.places).result().ifPresent(encoded -> tag.put(PLACES, encoded));
        return tag;
    }

    private static ShipLiftOffs load(final CompoundTag tag, final HolderLookup.Provider registries) {
        final ShipLiftOffs loaded = new ShipLiftOffs();
        final Tag places = tag.get(PLACES);
        if (places != null) {
            CODEC.parse(NbtOps.INSTANCE, places).result().ifPresent(read -> read.forEach((player, theirs) ->
                    loaded.places.put(player, new HashMap<>(theirs))));
        }
        return loaded;
    }

    private static ShipLiftOffs of(final MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(ShipLiftOffs::new,
                ShipLiftOffs::load), FILE);
    }

    /* Sends {@code player}'s game where they last took off from in each world, as {@code all} has it. */
    private static void tell(final ServerPlayer player, final ShipLiftOffs all) {
        final Map<ResourceLocation, BlockPos> theirs = new HashMap<>();
        all.places.getOrDefault(player.getUUID(), Map.of()).forEach((dimension, place) -> theirs.put(
                dimension.location(), place));
        PacketDistributor.sendToPlayer(player, new LiftOffsPayload(theirs));
    }
}
