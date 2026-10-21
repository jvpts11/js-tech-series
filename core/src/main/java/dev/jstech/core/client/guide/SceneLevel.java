/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.guide;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.jstech.core.log.CoreLogs;
import dev.jstech.core.guide.GuideBlock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * The few blocks of a scene as the game's renderers ask for a world: each block in its state, its block entity holding
 * what the scene says it holds, full light everywhere, the plains for the colour of grass and leaves, and air all
 * round. Above the layers a reader has peeled away there is air too, so the layer under shows its top.
 *
 * <p>A block entity is given the world the reader is in, since many renderers ask their block entity's world for the
 * time or for what it holds; it is never added to that world.
 */
final class SceneLevel implements BlockAndTintGetter {

    private final Map<BlockPos, BlockState> states = new HashMap<>();
    private final Map<BlockPos, BlockEntity> entities = new HashMap<>();
    private final LevelLightEngine light;
    private final int tall;
    private int shown;

    private static final Logger LOGGER = CoreLogs.GUIDE.logger();
    /** The brightest light there is, which every place of a scene has. */
    private static final int FULL = 15;
    /** A tint that leaves a colour as it is: every channel at its brightest. */
    private static final int NO_TINT = -1;

    /** The world of a scene's blocks, every layer of it shown. */
    SceneLevel(final GuideBlock.Scene scene) {
        this(scene, scene.blocks());
    }

    /** The scene standing as {@code blocks} say, one of its steps: its frame and its layers are the whole scene's. */
    SceneLevel(final GuideBlock.Scene scene, final List<GuideBlock.SceneBlock> blocks) {
        this.tall = scene.tall();
        this.shown = this.tall;
        this.light = new LevelLightEngine(new NoChunks(this), false, false);
        final Minecraft game = Minecraft.getInstance();
        for (final GuideBlock.SceneBlock block : blocks) {
            final BlockPos pos = new BlockPos(block.x(), block.y(), block.z());
            final BlockState state = stateOf(block);
            this.states.put(pos, state);
            if (state.getBlock() instanceof EntityBlock maker) {
                final BlockEntity entity = maker.newBlockEntity(pos, state);
                if (entity != null) {
                    if (game.level != null) {
                        entity.setLevel(game.level);
                    }
                    this.load(entity, block);
                    this.entities.put(pos, entity);
                }
            }
        }
    }

    /** How many layers are shown, counted up from the bottom; the scene's height shows them all. */
    void showLayers(final int layers) {
        this.shown = Math.max(1, Math.min(this.tall, layers));
    }

    /** The blocks shown and where they stand, for the renderer. */
    Map<BlockPos, BlockState> shownStates() {
        final Map<BlockPos, BlockState> out = new HashMap<>();
        this.states.forEach((pos, state) -> {
            if (pos.getY() < this.shown) {
                out.put(pos, state);
            }
        });
        return out;
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(final BlockPos pos) {
        return pos.getY() < this.shown ? this.entities.get(pos) : null;
    }

    @Override
    public BlockState getBlockState(final BlockPos pos) {
        return pos.getY() < this.shown ? this.states.getOrDefault(pos, Blocks.AIR.defaultBlockState())
                : Blocks.AIR.defaultBlockState();
    }

    @Override
    public FluidState getFluidState(final BlockPos pos) {
        return this.getBlockState(pos).getFluidState();
    }

    @Override
    public float getShade(final Direction direction, final boolean shade) {
        if (!shade) {
            return 1.0F;
        }
        // The shading the game gives each side of a block under an ordinary sky.
        return switch (direction) {
            case DOWN -> 0.5F;
            case UP -> 1.0F;
            case NORTH, SOUTH -> 0.8F;
            case WEST, EAST -> 0.6F;
        };
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return this.light;
    }

    @Override
    public int getBrightness(final LightLayer layer, final BlockPos pos) {
        return FULL;
    }

    @Override
    public int getRawBrightness(final BlockPos pos, final int darkened) {
        return FULL;
    }

    @Override
    public int getBlockTint(final BlockPos pos, final ColorResolver resolver) {
        final Minecraft game = Minecraft.getInstance();
        if (game.level == null) {
            // With no world there is no biome to colour by, so nothing is tinted.
            return NO_TINT;
        }
        final Biome plains = game.level.registryAccess().registryOrThrow(Registries.BIOME)
                .getHolderOrThrow(Biomes.PLAINS).value();
        return resolver.getColor(plains, pos.getX(), pos.getZ());
    }

    @Override
    public int getHeight() {
        return GuideBlock.Scene.GRID;
    }

    @Override
    public int getMinBuildHeight() {
        return 0;
    }

    /** A scene block's state, as the game reads one written in a command; air for a block the game does not know. */
    private static BlockState stateOf(final GuideBlock.SceneBlock block) {
        // A block of a mod that is not installed leaves its place empty, as the scene's author meant, unremarked.
        if (!ModList.get().isLoaded(namespaceOf(block.state()))) {
            return Blocks.AIR.defaultBlockState();
        }
        try {
            return BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK.asLookup(), block.state(), false)
                    .blockState();
        } catch (final CommandSyntaxException e) {
            LOGGER.warn("A manual's scene names {}, which is no block's state: {}", block.state(), e.getMessage());
            return Blocks.AIR.defaultBlockState();
        }
    }

    /** The namespace of a block's state as written, the vanilla one when it names none. */
    private static String namespaceOf(final String state) {
        final int colon = state.indexOf(':');
        final int bracket = state.indexOf('[');
        return colon < 0 || bracket >= 0 && bracket < colon ? ResourceLocation.DEFAULT_NAMESPACE
                : state.substring(0, colon);
    }

    /**
     * Gives a block entity what the scene says it holds, read both as what the block keeps and as what a server sends
     * a client about it, since many renderers draw from the second: which parts a cabinet shows, whether it runs.
     */
    private void load(final BlockEntity entity, final GuideBlock.SceneBlock block) {
        if (block.data().isEmpty() || Minecraft.getInstance().level == null) {
            return;
        }
        try {
            final CompoundTag data = TagParser.parseTag(block.data());
            entity.loadWithComponents(data, Minecraft.getInstance().level.registryAccess());
            entity.handleUpdateTag(data, Minecraft.getInstance().level.registryAccess());
        } catch (final CommandSyntaxException e) {
            LOGGER.warn("A manual's scene gives {} data that is no compound tag: {}", block.state(), e.getMessage());
        }
    }

    /** The chunks a scene's light engine is handed: none, since a scene keeps no light of its own. */
    private record NoChunks(BlockGetter level) implements LightChunkGetter {

        @Nullable
        @Override
        public LightChunk getChunkForLighting(final int x, final int z) {
            return null;
        }

        @Override
        public BlockGetter getLevel() {
            return this.level;
        }

        @Override
        public void onLightUpdate(final LightLayer layer, final SectionPos pos) {
            // A scene's light never changes.
        }
    }
}
