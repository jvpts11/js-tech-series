/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.universe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.state.CoreState;
import dev.jstech.core.state.CoreStates;
import dev.jstech.core.state.PlayerState;
import dev.jstech.space.JsSpace;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * The cabin of the module each player last landed in, on each world: its landing marks it, so a player who dies there
 * with no bed in a room of air comes back to it, as long as it is still whole and has air. Whole means the block
 * marked is still the block that was there.
 */
public final class LandingCabins {

    /* Each player's cabins, by the dimension they stand in. */
    private static final Codec<Map<String, Cabin>> CABINS_CODEC = Codec.unboundedMap(Codec.STRING, Cabin.CODEC);
    private static final PlayerState<Map<String, Cabin>> STATE = CoreState.builder(
                    ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "landing_cabins"), CABINS_CODEC, Map.of())
            .fileName("jsspace_landing_cabins")
            .player();

    private LandingCabins() {
    }

    /** Registers the state the cabins are kept in, from the mod's constructor. */
    public static void register() {
        CoreStates.register(STATE);
    }

    /** Marks the cabin at {@code pos} of {@code level} as the one {@code player} landed in there. */
    public static void mark(final ServerPlayer player, final ServerLevel level, final BlockPos pos) {
        final Cabin cabin = new Cabin(pos.immutable(), BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos)
                .getBlock()));
        STATE.update(player.server, player.getUUID(), held -> {
            final Map<String, Cabin> next = new HashMap<>(held);
            next.put(level.dimension().location().toString(), cabin);
            return Map.copyOf(next);
        });
    }

    /** The cabin {@code player} last landed in on {@code level}, if any. */
    public static Optional<Cabin> of(final ServerPlayer player, final ServerLevel level) {
        return Optional.ofNullable(STATE.get(player.server, player.getUUID())
                .get(level.dimension().location().toString()));
    }

    /**
     * A cabin landed in.
     *
     * @param pos   where it stands
     * @param block the block that stood there as it landed, which a whole cabin still has
     */
    public record Cabin(BlockPos pos, ResourceLocation block) {

        public static final Codec<Cabin> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(Cabin::pos),
                ResourceLocation.CODEC.fieldOf("block").forGetter(Cabin::block))
                .apply(instance, Cabin::new));

        /** Whether the block marked in {@code level} is still the one that stood there. */
        public boolean whole(final ServerLevel level) {
            return BuiltInRegistries.BLOCK.getKey(level.getBlockState(this.pos).getBlock()).equals(this.block);
        }
    }
}
