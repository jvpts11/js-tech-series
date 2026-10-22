/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.connect;

import com.mojang.serialization.MapCodec;
import dev.jstech.core.JsCore;
import dev.jstech.core.connect.texture.OptifineRules;
import dev.jstech.core.stepaside.StepAside;
import java.util.Optional;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.neoforge.client.event.RegisterSpriteSourceTypesEvent;

/**
 * Puts the tiles of the packs' connected textures into the blocks' atlas, as the game puts its own textures there:
 * the tiles of OptiFine's and Continuity's rules, wherever their files lie, and the companions and proxies of CTM's
 * textures, and a tile's glowing texture beside it where the packs say glowing textures' ending ({@link PackEmissive}).
 * It reads the rules as it goes, so they are ready before the models are baked. The Core's resources name it in
 * {@code atlases/blocks.json}; while connected textures step aside for another mod, it puts nothing.
 */
final class PackTileSource implements SpriteSource {

    /** The one source; it takes nothing from the atlas file. */
    static final PackTileSource INSTANCE = new PackTileSource();
    static final MapCodec<PackTileSource> CODEC = MapCodec.unit(INSTANCE);
    static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "connected_tiles");
    private static final String PNG = ".png";

    /* Its type, once the game made it. */
    private static volatile SpriteSourceType type;

    private PackTileSource() {
    }

    /** Registers its type, from the event the game asks for sources' types in. */
    static void register(final RegisterSpriteSourceTypesEvent event) {
        type = event.register(ID, CODEC);
    }

    @Override
    public void run(final ResourceManager resources, final SpriteSource.Output output) {
        if (StepAside.CONNECTED_TEXTURES.steps()) {
            PackRules.clear();
            return;
        }
        final String suffix = PackEmissive.suffix(resources);
        for (final String file : PackRules.load(resources).tileFiles()) {
            final ResourceLocation location = ResourceLocation.parse(file);
            final Optional<Resource> resource = resources.getResource(location);
            final ResourceLocation sprite = ResourceLocation.parse(OptifineRules.spriteOf(file));
            resource.ifPresent(found -> output.add(sprite, found));
            if (!suffix.isEmpty() && file.endsWith(PNG)) {
                // A tile's glowing texture beside it goes into the atlas with it, under the tile's name and the ending.
                final ResourceLocation glowing = ResourceLocation.parse(file.substring(0, file.length()
                        - PNG.length()) + suffix + PNG);
                resources.getResource(glowing).ifPresent(found -> output.add(PackEmissive.withSuffix(sprite, suffix),
                        found));
            }
        }
    }

    @Override
    public SpriteSourceType type() {
        return type;
    }
}
