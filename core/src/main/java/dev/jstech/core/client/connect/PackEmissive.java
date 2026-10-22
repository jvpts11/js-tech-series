/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.connect;

import com.mojang.logging.LogUtils;
import dev.jstech.core.connect.texture.OptifineRules;
import dev.jstech.core.stepaside.StepAside;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.slf4j.Logger;

/**
 * The glowing textures of resource packs on blocks, as OptiFine reads them and Continuity after it. A pack says the
 * ending of a glowing texture's name in {@code optifine/emissive.properties} ({@code suffix.emissive=_e}); a block's
 * texture with one of that name beside it ({@code textures/block/furnace_front_on_e.png} beside
 * {@code furnace_front_on.png}) has it drawn over it at full light, so its glowing pixels shine in the dark, and so has
 * a connected texture's tile. A pack that says no ending has no glowing textures, as in OptiFine. While Continuity or
 * CTM is installed, the Core leaves these to it with the packs' connected textures.
 */
public final class PackEmissive {

    private static final Logger LOGGER = LogUtils.getLogger();
    /* Where a pack says the ending, in the game's own namespace as OptiFine looks for it. */
    private static final ResourceLocation PROPERTIES = ResourceLocation.withDefaultNamespace(
            "optifine/emissive.properties");
    private static final String SUFFIX = "suffix.emissive";
    private static final String INVENTORY = "inventory";
    private static final String STANDALONE = "standalone";
    private static final long SEED = 42L;

    /* Each texture's glowing one, by the texture's name, as the packs read last say. */
    private static volatile Map<ResourceLocation, TextureAtlasSprite> glowing = Map.of();

    private PackEmissive() {
    }

    /** Hands the mod's bus what this answers, once, from the Core's client. */
    static void listen(final IEventBus modBus) {
        // After the connected textures wrap their models, so a connected tile's glowing texture is drawn too.
        modBus.addListener(EventPriority.LOW, ModelEvent.ModifyBakingResult.class, PackEmissive::onModifyBakingResult);
    }

    /** The glowing texture drawn over {@code sprite}, if a pack has one. */
    public static Optional<TextureAtlasSprite> glowingOf(final TextureAtlasSprite sprite) {
        return Optional.ofNullable(glowing.get(sprite.contents().name()));
    }

    /** Whether any texture the packs in use draw has a glowing one. */
    public static boolean any() {
        return !glowing.isEmpty();
    }

    /** The ending the packs in {@code resources} give a glowing texture's name; empty where none says one. */
    static String suffix(final ResourceManager resources) {
        final Optional<Resource> resource = resources.getResource(PROPERTIES);
        if (resource.isEmpty()) {
            return "";
        }
        try (BufferedReader reader = resource.get().openAsReader()) {
            final Properties properties = new Properties();
            properties.load(reader);
            return properties.getProperty(SUFFIX, "").strip();
        } catch (final IOException | IllegalArgumentException broken) {
            LOGGER.warn("The glowing textures of {} are left out: {}", PROPERTIES, broken.getMessage());
            return "";
        }
    }

    /** The texture named {@code texture} with {@code suffix} at the end of its name. */
    static ResourceLocation withSuffix(final ResourceLocation texture, final String suffix) {
        return texture.withPath(texture.getPath() + suffix);
    }

    private static void onModifyBakingResult(final ModelEvent.ModifyBakingResult event) {
        glowing = Map.of();
        if (StepAside.CONNECTED_TEXTURES.steps()) {
            return;
        }
        final String suffix = suffix(Minecraft.getInstance().getResourceManager());
        if (suffix.isEmpty()) {
            return;
        }
        final Function<Material, TextureAtlasSprite> textures = event.getTextureGetter();
        final Map<ResourceLocation, TextureAtlasSprite> found = new HashMap<>();
        final Map<ResourceLocation, Boolean> looked = new HashMap<>();
        for (final String tile : PackRules.current().tileFiles()) {
            look(ResourceLocation.parse(OptifineRules.spriteOf(tile)), suffix, textures, found, looked);
        }
        final boolean tilesGlow = !found.isEmpty();
        final List<Map.Entry<ModelResourceLocation, BakedModel>> wrap = new ArrayList<>();
        for (final Map.Entry<ModelResourceLocation, BakedModel> entry : event.getModels().entrySet()) {
            final ModelResourceLocation location = entry.getKey();
            if (INVENTORY.equals(location.variant()) || STANDALONE.equals(location.variant())) {
                continue;
            }
            final Block block = BuiltInRegistries.BLOCK.get(location.id());
            if (block == Blocks.AIR) {
                continue;
            }
            final boolean connected = entry.getValue() instanceof ConnectedPackModel;
            if (glows(entry.getValue(), block.defaultBlockState(), suffix, textures, found, looked)
                    || connected && tilesGlow) {
                wrap.add(entry);
            }
        }
        glowing = Map.copyOf(found);
        for (final Map.Entry<ModelResourceLocation, BakedModel> entry : wrap) {
            entry.setValue(new PackEmissiveModel(entry.getValue()));
        }
        LOGGER.debug("Glowing textures of resource packs: {} textures on {} models of blocks", found.size(),
                wrap.size());
    }

    /* Whether a face of {@code model} has a texture with a glowing one, each texture looked up once. */
    private static boolean glows(final BakedModel model, final BlockState state, final String suffix,
                                 final Function<Material, TextureAtlasSprite> textures,
                                 final Map<ResourceLocation, TextureAtlasSprite> found,
                                 final Map<ResourceLocation, Boolean> looked) {
        final RandomSource random = RandomSource.create(SEED);
        boolean any = false;
        for (final Direction side : sides()) {
            for (final BakedQuad quad : model.getQuads(state, side, random, ModelData.EMPTY, null)) {
                any |= look(quad.getSprite().contents().name(), suffix, textures, found, looked);
            }
        }
        return any;
    }

    /* Looks for the glowing texture of {@code texture} in the blocks' atlas; whether it has one. */
    private static boolean look(final ResourceLocation texture, final String suffix,
                                final Function<Material, TextureAtlasSprite> textures,
                                final Map<ResourceLocation, TextureAtlasSprite> found,
                                final Map<ResourceLocation, Boolean> looked) {
        final Boolean known = looked.get(texture);
        if (known != null) {
            return known;
        }
        final TextureAtlasSprite sprite = textures.apply(new Material(InventoryMenu.BLOCK_ATLAS,
                withSuffix(texture, suffix)));
        final boolean has = !sprite.contents().name().equals(MissingTextureAtlasSprite.getLocation());
        if (has) {
            found.put(texture, sprite);
        }
        looked.put(texture, has);
        return has;
    }

    private static List<Direction> sides() {
        final List<Direction> sides = new ArrayList<>(List.of(Direction.values()));
        sides.add(null);
        return sides;
    }
}
