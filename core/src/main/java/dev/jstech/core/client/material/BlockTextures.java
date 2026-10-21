/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.material;

import dev.jstech.core.material.BlockModelFiles;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

/**
 * The texture a block of any mod is drawn with, read from its own files as the player's game holds them: its block
 * state's first model, that model's texture of its whole cube, its side or its particles, or, with none, the model it
 * is built on, and so on ({@link BlockModelFiles}). What the Core makes of other mods' blocks is drawn with it: an ore
 * in another mod's rock, a variant of another mod's wood.
 */
public final class BlockTextures {

    /** How many models a block's model may be built on, one on another, before its texture is given up on. */
    private static final int MOST_PARENTS = 4;

    private BlockTextures() {
    }

    /** The texture {@code block} is drawn with, as {@code othermod:block/limestone}, when its files say. */
    public static Optional<ResourceLocation> of(final Block block) {
        final ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        Optional<String> model = GameTexture.fileText(id.withPath("blockstates/" + id.getPath() + ".json"))
                .flatMap(BlockModelFiles::modelOf);
        for (int depth = 0; model.isPresent() && depth < MOST_PARENTS; depth++) {
            final ResourceLocation modelId = ResourceLocation.tryParse(model.get());
            if (modelId == null) {
                break;
            }
            final Optional<String> file = GameTexture.fileText(modelId.withPath("models/" + modelId.getPath()
                    + ".json"));
            if (file.isEmpty()) {
                break;
            }
            final Optional<ResourceLocation> drawn = BlockModelFiles.textureOf(file.get())
                    .map(ResourceLocation::tryParse);
            if (drawn.isPresent()) {
                return drawn;
            }
            model = BlockModelFiles.parentOf(file.get());
        }
        return Optional.empty();
    }
}
