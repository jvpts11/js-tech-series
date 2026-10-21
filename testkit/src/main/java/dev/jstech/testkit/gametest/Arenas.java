/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core Test Kit.
 */
package dev.jstech.testkit.gametest;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderGetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Arenas made in code: the empty templates GameTests run inside, declared once with their size, with no structure
 * file drawn by hand or written by the data generation. An arena is a floor of barrier blocks as wide and deep as it
 * is declared, and the air above it as high; the game finds it as it finds any template, after a reload as well.
 *
 * <pre>{@code
 * Arenas.declare("mymod", "empty", 9, 6, 9);   // from the mod's constructor
 *
 * @GameTestHolder("mymod")
 * @PrefixGameTestTemplate(false)
 * public final class KilnGameTests {
 *     @GameTest(template = "empty")
 *     public static void kiln_smelts(final GameTestHelper helper) { ... }
 * }
 * }</pre>
 *
 * <p>An arena is in the namespace of the mod whose tests use it, as a GameTest names its template in its holder's
 * namespace and a run that enables only some namespaces leaves out every other's.
 */
public final class Arenas {

    private static final Map<ResourceLocation, Size> DECLARED = new ConcurrentHashMap<>();
    private static final String BARRIER = "minecraft:barrier";

    private Arenas() {
    }

    /**
     * Declares the arena {@code namespace:name}, {@code width} by {@code height} by {@code depth} blocks, its floor
     * the lowest of them.
     *
     * @return its id, as a template
     * @throws IllegalArgumentException when a side is less than one block, or the arena is declared again with
     *                                  another size
     */
    public static ResourceLocation declare(final String namespace, final String name, final int width,
                                           final int height, final int depth) {
        if (width < 1 || height < 1 || depth < 1) {
            throw new IllegalArgumentException("an arena is at least a block a side, not " + width + " by " + height
                    + " by " + depth);
        }
        final ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, name);
        final Size size = new Size(width, height, depth);
        final Size before = DECLARED.putIfAbsent(id, size);
        if (before != null && !before.equals(size)) {
            throw new IllegalArgumentException("the arena " + id + " is already " + before + ", not " + size);
        }
        return id;
    }

    /** Whether {@code id} is an arena declared here. */
    public static boolean declared(final ResourceLocation id) {
        return DECLARED.containsKey(id);
    }

    /** The template of the arena {@code id}, when it is one, made with the game's blocks. */
    public static Optional<StructureTemplate> template(final ResourceLocation id, final HolderGetter<Block> blocks) {
        final Size size = DECLARED.get(id);
        if (size == null) {
            return Optional.empty();
        }
        final StructureTemplate template = new StructureTemplate();
        template.load(blocks, tagOf(size));
        return Optional.of(template);
    }

    /** The arena's template as a structure file holds it: a floor of barrier blocks, every other place left void. */
    public static CompoundTag tagOf(final Size size) {
        final CompoundTag root = new CompoundTag();
        root.putInt("DataVersion", SharedConstants.getCurrentVersion().getDataVersion().getVersion());
        root.put("size", ints(size.width(), size.height(), size.depth()));
        root.put("entities", new ListTag());
        final ListTag palette = new ListTag();
        final CompoundTag barrier = new CompoundTag();
        barrier.putString("Name", BARRIER);
        palette.add(barrier);
        root.put("palette", palette);
        final ListTag blocks = new ListTag();
        for (int x = 0; x < size.width(); x++) {
            for (int z = 0; z < size.depth(); z++) {
                final CompoundTag block = new CompoundTag();
                block.putInt("state", 0);
                block.put("pos", ints(x, 0, z));
                blocks.add(block);
            }
        }
        root.put("blocks", blocks);
        return root;
    }

    private static ListTag ints(final int... values) {
        final ListTag list = new ListTag();
        for (final int value : values) {
            list.add(IntTag.valueOf(value));
        }
        return list;
    }

    /**
     * How big an arena is, in blocks.
     *
     * @param width  along x
     * @param height along y, its floor included
     * @param depth  along z
     */
    public record Size(int width, int height, int depth) {

        @Override
        public String toString() {
            return width + " by " + height + " by " + depth;
        }
    }
}
