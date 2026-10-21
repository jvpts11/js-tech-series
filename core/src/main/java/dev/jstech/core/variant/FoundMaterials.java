/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.variant;

import dev.jstech.core.material.Forms;
import dev.jstech.core.material.FoundRocks;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialKind;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.core.material.ModTagFiles;
import dev.jstech.core.material.RegisteredThing;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import org.jetbrains.annotations.ApiStatus;

/**
 * The woods, stones and metals of every installed mod, the game's own among them, found through the common tags with
 * no list kept by hand, as the game registers its items (once every mod's blocks are registered): the woods by
 * {@code minecraft:planks}, the stones by the tags of the world's stones ({@link FoundRocks}), the metals by the tags
 * {@code c:storage_blocks/<name>} that have ingots of the same name ({@code c:ingots/<name>}), the Core's own metals
 * first. Only whole blocks with no block entity are taken, which a variant can copy. The tags are read from the mods'
 * jars ({@link ModTagFiles}), since the game has read none yet.
 */
public final class FoundMaterials {

    private static final String PLANKS = "minecraft:planks";
    private static final String STORAGE_BLOCKS = "c:storage_blocks";
    private static final String INGOTS = "c:ingots/";
    private static final String COMMON = "c";

    private FoundMaterials() {
    }

    /** Every material of every family found, wood, then stone, then metal; each once. */
    @ApiStatus.Internal
    public static List<FoundMaterial> all() {
        final List<FoundMaterial> out = new ArrayList<>(woods());
        out.addAll(stones());
        out.addAll(metals());
        return out;
    }

    /** The woods, by their planks. */
    public static List<FoundMaterial> woods() {
        final Map<ResourceLocation, FoundMaterial> out = new LinkedHashMap<>();
        for (final String member : ModTagFiles.blocks(PLANKS)) {
            final ResourceLocation planks = ResourceLocation.tryParse(member);
            if (planks != null && whole(planks)) {
                final ResourceLocation wood = planks.withPath(VariantPlan.woodOf(planks.getPath()));
                out.putIfAbsent(wood, new FoundMaterial(VariantFamily.WOOD, wood, planks));
            }
        }
        return List.copyOf(out.values());
    }

    /** The stones, each its own block. */
    public static List<FoundMaterial> stones() {
        final List<FoundMaterial> out = new ArrayList<>();
        for (final ResourceLocation stone : FoundRocks.inTags().keySet()) {
            if (whole(stone)) {
                out.add(new FoundMaterial(VariantFamily.STONE, stone, stone));
            }
        }
        return List.copyOf(out);
    }

    /** The metals, each by its common name and a block of it: the Core's own first, then the game's, then others'. */
    public static List<FoundMaterial> metals() {
        final Map<ResourceLocation, FoundMaterial> out = new LinkedHashMap<>();
        for (final RegisteredThing thing : MaterialUses.registered()) {
            final Material material = thing.thing().material();
            if (thing.thing().form() == Forms.BLOCK && material.kind() == MaterialKind.METAL && whole(thing.id())) {
                final ResourceLocation metal = ResourceLocation.fromNamespaceAndPath(COMMON, material.id().getPath());
                out.putIfAbsent(metal, new FoundMaterial(VariantFamily.METAL, metal, thing.id()));
            }
        }
        for (final String tag : ModTagFiles.blockTagsIn(STORAGE_BLOCKS)) {
            final String name = tag.substring(tag.indexOf('/') + 1);
            if (!tag.startsWith(STORAGE_BLOCKS + "/") || !ModTagFiles.anyItems(INGOTS + name)) {
                continue;
            }
            final ResourceLocation metal = ResourceLocation.tryParse(COMMON + ":" + name);
            final Optional<ResourceLocation> block = blockOf(tag);
            if (metal != null && block.isPresent()) {
                out.putIfAbsent(metal, new FoundMaterial(VariantFamily.METAL, metal, block.get()));
            }
        }
        return List.copyOf(out.values());
    }

    /* The block a metal's variants copy: the game's own when it has one, otherwise the first by id. */
    private static Optional<ResourceLocation> blockOf(final String tag) {
        ResourceLocation first = null;
        for (final String member : ModTagFiles.blocks(tag)) {
            final ResourceLocation id = ResourceLocation.tryParse(member);
            if (id == null || !whole(id)) {
                continue;
            }
            if (id.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE)) {
                return Optional.of(id);
            }
            if (first == null) {
                first = id;
            }
        }
        return Optional.ofNullable(first);
    }

    /* Whether {@code id} is a registered whole cube with no block entity, as a variant can copy. */
    private static boolean whole(final ResourceLocation id) {
        final Optional<Block> block = BuiltInRegistries.BLOCK.getOptional(id);
        return block.isPresent() && !(block.get() instanceof EntityBlock) && Block.isShapeFullBlock(block.get()
                .defaultBlockState().getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
    }
}
