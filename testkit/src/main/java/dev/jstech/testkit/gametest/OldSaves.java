/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core Test Kit.
 */
package dev.jstech.testkit.gametest;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.locating.IModFile;

/**
 * Old saves: pieces of worlds saved by earlier versions of a mod, kept beside its tests and shown to load in each new
 * version. A piece is a structure file, the blocks of a few machines with everything they held, saved with the
 * version of the game it was saved in; it is read back through the game's own upgrades from that version, and placed
 * in the arena, where a test looks at what came back.
 *
 * <pre>{@code
 * for (final OldSaves.Sample sample : OldSaves.samples("mymod", "old_saves")) {
 *     final List<BlockEntity> placed = OldSaves.place(helper, sample, new BlockPos(1, 1, 1));
 *     ...
 * }
 * }</pre>
 *
 * <p>A sample is made from a test that builds the machines and {@link #keep keeps} them: run with
 * {@code -Djstestkit.oldsaves=<folder>}, it writes {@code <folder>/<version>/<name>.nbt}, to be copied into the
 * mod's resources under its folder of samples; run without it, keeping does nothing.
 */
public final class OldSaves {

    /** The system property that names the folder a run keeps samples in. */
    public static final String FOLDER_PROPERTY = "jstestkit.oldsaves";

    private static final String EXTENSION = ".nbt";
    private static final String SNAPSHOT = "-SNAPSHOT";

    private OldSaves() {
    }

    /** Every sample kept under {@code folder} of the mod {@code modid}'s resources, each in its version's folder. */
    public static List<Sample> samples(final String modid, final String folder) {
        final IModFile file = ModList.get().getModFileById(modid).getFile();
        final Path root = file.findResource(folder);
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        final List<Sample> out = new ArrayList<>();
        try (Stream<Path> files = Files.walk(root, 2)) {
            files.filter(path -> path.getFileName().toString().endsWith(EXTENSION))
                    .sorted(Comparator.comparing(Path::toString))
                    .forEach(path -> out.add(new Sample(path.getParent().getFileName().toString(),
                            nameOf(path), path)));
        } catch (final IOException e) {
            throw new UncheckedIOException("the old saves under " + folder + " of " + modid + " could not be listed",
                    e);
        }
        return out;
    }

    /**
     * Places {@code sample} with its lowest corner at the arena's place {@code at}, read through the game's
     * upgrades from the version it was saved in.
     *
     * @return the block entities placed, as they loaded
     */
    public static List<BlockEntity> place(final GameTestHelper helper, final Sample sample, final BlockPos at) {
        final CompoundTag saved;
        try (InputStream in = Files.newInputStream(sample.file())) {
            saved = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
        } catch (final IOException e) {
            throw new UncheckedIOException("the old save " + sample + " could not be read", e);
        }
        return place(helper, saved, at);
    }

    /** Places the structure {@code saved}, read through the game's upgrades, at the arena's place {@code at}. */
    public static List<BlockEntity> place(final GameTestHelper helper, final CompoundTag saved, final BlockPos at) {
        final ServerLevel level = helper.getLevel();
        final StructureTemplate template = level.getStructureManager().readStructure(saved);
        final BlockPos corner = helper.absolutePos(at);
        if (!template.placeInWorld(level, corner, corner, new StructurePlaceSettings(), level.random,
                Block.UPDATE_ALL)) {
            throw Expect.failure(helper, at, "an old save placed: the game placed none of its blocks");
        }
        final Vec3i size = template.getSize();
        final List<BlockEntity> placed = new ArrayList<>();
        for (final BlockPos pos : BlockPos.betweenClosed(corner, corner.offset(size).offset(-1, -1, -1))) {
            final BlockEntity entity = level.getBlockEntity(pos);
            if (entity != null) {
                placed.add(entity);
            }
        }
        return placed;
    }

    /** The blocks from the arena's place {@code from} to {@code to}, both included, as a structure file holds them. */
    public static CompoundTag capture(final GameTestHelper helper, final BlockPos from, final BlockPos to) {
        final BlockPos low = helper.absolutePos(BlockPos.min(from, to));
        final BlockPos high = helper.absolutePos(BlockPos.max(from, to));
        final StructureTemplate template = new StructureTemplate();
        template.fillFromWorld(helper.getLevel(), low, high.subtract(low).offset(1, 1, 1), false,
                Blocks.STRUCTURE_VOID);
        return template.save(new CompoundTag());
    }

    /**
     * Keeps the blocks from {@code from} to {@code to} as the sample {@code name} of {@code modid}'s version now, when
     * the run names a folder to keep samples in.
     *
     * @return where it was written, when it was
     */
    public static Optional<Path> keep(final GameTestHelper helper, final String modid, final String name,
                                      final BlockPos from, final BlockPos to) {
        final String folder = System.getProperty(FOLDER_PROPERTY, "").trim();
        if (folder.isEmpty()) {
            return Optional.empty();
        }
        final Path file = Path.of(folder).resolve(versionOf(modid)).resolve(name + EXTENSION);
        try {
            Files.createDirectories(file.getParent());
            NbtIo.writeCompressed(capture(helper, from, to), file);
        } catch (final IOException e) {
            throw new UncheckedIOException("the old save " + name + " could not be kept at " + file, e);
        }
        return Optional.of(file);
    }

    /** The version of {@code modid} running, without a development build's suffix: what a sample is kept under. */
    public static String versionOf(final String modid) {
        final String version = ModList.get().getModContainerById(modid).map(ModContainer::getModInfo)
                .map(IModInfo::getVersion).map(Object::toString).orElse("unknown");
        final int snapshot = version.indexOf(SNAPSHOT);
        return snapshot < 0 ? version : version.substring(0, snapshot);
    }

    private static String nameOf(final Path file) {
        final String name = file.getFileName().toString();
        return name.substring(0, name.length() - EXTENSION.length());
    }

    /**
     * A piece of a world kept from an earlier version.
     *
     * @param version the version it was saved in, its folder's name
     * @param name    what it is, its file's name
     * @param file    where it is, among the mod's resources
     */
    public record Sample(String version, String name, Path file) {

        @Override
        public String toString() {
            return version + "/" + name;
        }
    }
}
