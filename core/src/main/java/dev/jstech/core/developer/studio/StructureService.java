/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.studio;

import dev.jstech.core.developer.DevSources;
import dev.jstech.core.log.CoreLogs;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.slf4j.Logger;

/**
 * Catching a structure: the blocks between two corners, as the game's structure block saves them, air left out and the
 * block entities kept with what they hold. Asked to save it, the file is written to the mod's own source in this run,
 * {@code data/<mod>/structure/<name>.nbt}, or, with no source here, where the world keeps the structures it makes.
 */
final class StructureService implements IStudioService {

    static final String NAME = "structure";
    private static final Logger LOGGER = CoreLogs.DEVELOPER.logger();
    private static final String NBT = ".nbt";
    /** Where a structure file keeps its size, as the game writes it. */
    private static final String SIZE_KEY = "size";

    @Override
    public void answer(final ServerPlayer player, final CompoundTag args, final Consumer<CompoundTag> reply) {
        final ServerLevel level = player.serverLevel();
        final BlockPos from = BlockPos.of(args.getLong(StudioKeys.FROM));
        final BlockPos to = BlockPos.of(args.getLong(StudioKeys.TO));
        final CompoundTag saved = capture(level, from, to);
        final CompoundTag out = new CompoundTag();
        final ListTag size = saved.getList(SIZE_KEY, Tag.TAG_INT);
        out.putIntArray(StudioKeys.SIZE, new int[] {size.getInt(0), size.getInt(1), size.getInt(2)});
        final ListTag blocks = saved.getList(StructureTemplate.BLOCKS_TAG, Tag.TAG_COMPOUND);
        int entities = 0;
        for (int i = 0; i < blocks.size(); i++) {
            entities += blocks.getCompound(i).contains(StructureTemplate.BLOCK_TAG_NBT) ? 1 : 0;
        }
        out.putInt(StudioKeys.BLOCKS, blocks.size());
        out.putInt(StudioKeys.ENTITIES, entities);
        if (args.getBoolean(StudioKeys.SAVE)) {
            final ResourceLocation name = ResourceLocation.parse(args.getString(StudioKeys.NAME));
            write(level, name, saved).ifPresent(path -> out.putString(StudioKeys.PATH, path.toString()));
        }
        reply.accept(out);
    }

    /** The blocks between {@code from} and {@code to}, both kept, as a structure file holds them. */
    static CompoundTag capture(final ServerLevel level, final BlockPos from, final BlockPos to) {
        final BlockPos min = new BlockPos(Math.min(from.getX(), to.getX()), Math.min(from.getY(), to.getY()),
                Math.min(from.getZ(), to.getZ()));
        final Vec3i size = new Vec3i(Math.abs(from.getX() - to.getX()) + 1, Math.abs(from.getY() - to.getY()) + 1,
                Math.abs(from.getZ() - to.getZ()) + 1);
        final StructureTemplate template = new StructureTemplate();
        template.fillFromWorld(level, min, size, false, Blocks.AIR);
        return template.save(new CompoundTag());
    }

    /* The file written, in the mod's source when this run has it, else where the world keeps its own. */
    private static Optional<Path> write(final ServerLevel level, final ResourceLocation name, final CompoundTag saved) {
        final Optional<Path> source = DevSources.resourcesOf(name.getNamespace());
        try {
            final Path file = source.isPresent() ? source.get().resolve("data").resolve(name.getNamespace())
                    .resolve("structure").resolve(name.getPath() + NBT)
                    : level.getStructureManager().createAndValidatePathToGeneratedStructure(name, NBT);
            Files.createDirectories(file.getParent());
            NbtIo.writeCompressed(saved, file);
            return Optional.of(file);
        } catch (final IOException | RuntimeException unwritten) {
            LOGGER.warn("The structure {} could not be written", name, unwritten);
            return Optional.empty();
        }
    }
}
