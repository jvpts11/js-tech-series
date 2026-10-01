/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.model;

import dev.jstech.core.JsCore;
import dev.jstech.core.cable.CableType;
import dev.jstech.core.cable.CoreCables;
import dev.jstech.core.connect.ConnectedQuadrants;
import dev.jstech.core.connect.IJoinRule;
import dev.jstech.core.multipart.CoreParts;
import dev.jstech.core.multipart.PartType;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * The models the Core puts together as the game runs, declared by each mod from its client setup:
 *
 * <ul>
 *   <li>{@link #multipart}: a block whose block entity places parts and wire pieces on top of its own model;</li>
 *   <li>{@link #connected}: a full block whose faces run on into the blocks it joins, drawn from five tiles;</li>
 *   <li>{@link #standalone}: a model a multipart block places that no part kind names, such as a wire's piece.</li>
 * </ul>
 *
 * <p>The model of every part kind registered with the Core is loaded by itself. A connected texture's tiles have to
 * be on the block atlas, which a texture is when any block model uses it.
 */
@EventBusSubscriber(modid = JsCore.MODID, value = Dist.CLIENT)
public final class CoreModels {

    private static final List<Supplier<? extends Block>> MULTIPART = new CopyOnWriteArrayList<>();
    private static final List<Connected> CONNECTED = new CopyOnWriteArrayList<>();
    private static final Set<ResourceLocation> STANDALONE = ConcurrentHashMap.newKeySet();

    private CoreModels() {
    }

    /** Draws {@code block}'s parts and wire pieces, as its block entity's layout places them, on its own model. */
    public static void multipart(final Supplier<? extends Block> block) {
        MULTIPART.add(Objects.requireNonNull(block, "block"));
    }

    /**
     * Draws {@code block} as a full block whose faces run on into the blocks {@code joins} says it joins.
     *
     * @param tiles the five tiles, by texture id, in the order {@link ConnectedQuadrants} numbers them
     */
    public static void connected(final Supplier<? extends Block> block, final IJoinRule joins,
                                 final List<ResourceLocation> tiles) {
        if (tiles.size() != ConnectedQuadrants.TILES) {
            throw new IllegalArgumentException("a connected texture is drawn from five tiles, not " + tiles.size());
        }
        CONNECTED.add(new Connected(block, joins, List.copyOf(tiles)));
    }

    /** Loads {@code model}, a standalone block model a multipart block places. */
    public static void standalone(final ResourceLocation model) {
        STANDALONE.add(Objects.requireNonNull(model, "model"));
    }

    @SubscribeEvent
    public static void onRegisterAdditional(final ModelEvent.RegisterAdditional event) {
        for (final PartType<?> type : CoreParts.REGISTRY) {
            event.register(ModelResourceLocation.standalone(type.model()));
        }
        for (final CableType type : CoreCables.REGISTRY) {
            event.register(ModelResourceLocation.standalone(type.plug()));
        }
        for (final ResourceLocation model : STANDALONE) {
            event.register(ModelResourceLocation.standalone(model));
        }
    }

    @SubscribeEvent
    public static void onModifyBakingResult(final ModelEvent.ModifyBakingResult event) {
        MultipartBakedModel.forget();
        CableBakedModel.forget();
        final Map<ModelResourceLocation, BakedModel> models = event.getModels();
        // The Core's own cable block draws its wires on top of what its parts and plugs place.
        for (final BlockState state : CoreCables.BLOCK.get().getStateDefinition().getPossibleStates()) {
            models.computeIfPresent(BlockModelShaper.stateToModelLocation(state),
                    (location, model) -> new CableBakedModel(new MultipartBakedModel(model)));
        }
        for (final Supplier<? extends Block> block : MULTIPART) {
            for (final BlockState state : block.get().getStateDefinition().getPossibleStates()) {
                models.computeIfPresent(BlockModelShaper.stateToModelLocation(state),
                        (location, model) -> new MultipartBakedModel(model));
            }
        }
        final Function<Material, TextureAtlasSprite> sprites = event.getTextureGetter();
        for (final Connected connected : CONNECTED) {
            final TextureAtlasSprite[] tiles = connected.tiles().stream()
                    .map(tile -> sprites.apply(new Material(InventoryMenu.BLOCK_ATLAS, tile)))
                    .toArray(TextureAtlasSprite[]::new);
            for (final BlockState state : connected.block().get().getStateDefinition().getPossibleStates()) {
                models.computeIfPresent(BlockModelShaper.stateToModelLocation(state),
                        (location, model) -> new ConnectedBakedModel(model, connected.joins(), tiles));
            }
        }
    }

    /**
     * A block drawn with a connected texture.
     *
     * @param block the block
     * @param joins which neighbours it joins
     * @param tiles its five tiles
     */
    private record Connected(Supplier<? extends Block> block, IJoinRule joins, List<ResourceLocation> tiles) {
    }
}
