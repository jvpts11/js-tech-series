/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.studio;

import dev.jstech.core.util.Loaded;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block saved and read back, as a world closed and opened again does: what its block entity saves, read into a new
 * block entity of its kind, and saved again, field by field, so a field that does not come back as it went is seen.
 * The block in the world is not touched.
 */
final class SaveService implements IStudioService {

    static final String NAME = "save";

    @Override
    public void answer(final ServerPlayer player, final CompoundTag args, final Consumer<CompoundTag> reply) {
        final ServerLevel level = player.serverLevel();
        final BlockPos pos = BlockPos.of(args.getLong(StudioKeys.POS));
        final CompoundTag out = new CompoundTag();
        final BlockEntity entity = Loaded.at(level, pos) ? level.getBlockEntity(pos) : null;
        if (entity != null) {
            out.put(StudioKeys.CHANGES, listed(roundTrip(level, pos, entity)));
        }
        reply.accept(out);
    }

    /** Each field of {@code entity} before it is saved and after it is read back into a new one. */
    static List<FieldDiff.Change> roundTrip(final ServerLevel level, final BlockPos pos, final BlockEntity entity) {
        final CompoundTag before = entity.saveWithFullMetadata(level.registryAccess());
        final BlockState state = level.getBlockState(pos);
        final BlockEntity read = BlockEntity.loadStatic(pos, state, before.copy(), level.registryAccess());
        final CompoundTag after = read == null ? new CompoundTag() : read.saveWithFullMetadata(level.registryAccess());
        return FieldDiff.of(NbtLines.of(before), NbtLines.of(after));
    }

    private static ListTag listed(final List<FieldDiff.Change> changes) {
        final ListTag out = new ListTag();
        for (final FieldDiff.Change change : changes) {
            final CompoundTag line = new CompoundTag();
            line.putString(StudioKeys.FIELD, change.field());
            line.putString(StudioKeys.BEFORE, change.before());
            line.putString(StudioKeys.AFTER, change.after());
            line.putBoolean(StudioKeys.CHANGED, change.changed());
            out.add(line);
        }
        return out;
    }
}
