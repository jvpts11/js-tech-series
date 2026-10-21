/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.studio;

import com.mojang.authlib.GameProfile;
import dev.jstech.core.diagnostic.CostMap;
import dev.jstech.core.team.IOwned;
import dev.jstech.core.team.Ownership;
import dev.jstech.core.util.Loaded;
import java.util.function.Consumer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * What each machine costs the server: a measure of that many seconds, as {@code /jstech cost} takes, answered when it
 * ends, its dearest places first, each with its block, where it is, its cost a tick and its owner.
 */
final class CostService implements IStudioService {

    static final String NAME = "cost";
    /** The most places answered. */
    private static final int MOST = 60;

    @Override
    public void answer(final ServerPlayer player, final CompoundTag args, final Consumer<CompoundTag> reply) {
        final MinecraftServer server = player.server;
        final int seconds = Math.clamp(args.getInt(StudioKeys.SECONDS), 1, CostMap.LONGEST_SECONDS);
        CostMap.measure(server, seconds, measure -> {
            final CompoundTag out = new CompoundTag();
            out.putLong(StudioKeys.TICKS, measure.ticks());
            out.putInt(StudioKeys.MEASURED, measure.places().size());
            out.putLong(StudioKeys.NANOS, measure.totalNanosPerTick());
            final ListTag entries = new ListTag();
            for (final CostMap.Entry entry : measure.places().subList(0, Math.min(MOST, measure.places().size()))) {
                final CompoundTag place = new CompoundTag();
                place.putString(StudioKeys.DIMENSION, entry.dimension().location().toString());
                place.putLong(StudioKeys.POS, entry.pos().asLong());
                place.putString(StudioKeys.KIND, entry.kind().toString());
                place.putLong(StudioKeys.NANOS, entry.nanosPerTick());
                final ServerLevel level = server.getLevel(entry.dimension());
                if (level != null && Loaded.at(level, entry.pos())) {
                    place.putString(StudioKeys.BLOCK, BuiltInRegistries.BLOCK.getKey(
                            level.getBlockState(entry.pos()).getBlock()).toString());
                    place.putString(StudioKeys.OWNER, ownerOf(server, level.getBlockEntity(entry.pos())));
                }
                entries.add(place);
            }
            out.put(StudioKeys.ENTRIES, entries);
            reply.accept(out);
        });
    }

    /* The name of whoever owns the block, as the server last knew it; empty for nobody's. */
    private static String ownerOf(final MinecraftServer server, final BlockEntity entity) {
        if (!(entity instanceof IOwned owned)) {
            return "";
        }
        final Ownership ownership = owned.ownership();
        if (ownership == null || server.getProfileCache() == null) {
            return "";
        }
        return server.getProfileCache().get(ownership.owner()).map(GameProfile::getName).orElse("");
    }
}
