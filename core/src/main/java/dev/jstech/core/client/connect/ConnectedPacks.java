/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.connect;

import com.mojang.logging.LogUtils;
import dev.jstech.core.connect.texture.CtmTexture;
import dev.jstech.core.connect.texture.OptifineRule;
import dev.jstech.core.multipart.Microblock;
import dev.jstech.core.multipart.MicroblockLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterSpriteSourceTypesEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.slf4j.Logger;

/**
 * The connected textures of resource packs on the player's game, read from both formats packs write them in: OptiFine's
 * and Continuity's, and CTM's. Their tiles go into the blocks' atlas, and every block model a rule may draw is drawn
 * through them; a microblock of a space joins its like in the spaces around it as a whole block of its material
 * would. While Continuity or CTM is installed, they draw the packs' connected textures and the Core steps aside, as
 * its setting says ({@code StepAside}); the Core's own connected blocks, declared in code, are drawn either way.
 */
public final class ConnectedPacks {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final long SEED = 42L;
    private static final String INVENTORY = "inventory";
    private static final String STANDALONE = "standalone";

    private ConnectedPacks() {
    }

    /** Hands the mod's bus what this answers, once, from the Core's client. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterSpriteSourceTypesEvent.class, PackTileSource::register);
        modBus.addListener(ModelEvent.ModifyBakingResult.class, ConnectedPacks::onModifyBakingResult);
        PackEmissive.listen(modBus);
    }

    /** Whether the packs in use say any connected texture the Core draws. */
    public static boolean any() {
        return !PackRules.current().isEmpty();
    }

    /**
     * The data of a space of microblocks with what its microblocks need to join their like: what stands around it,
     * and the microblocks of the spaces around it. The data as it is while the packs say nothing.
     */
    public static ModelData withNeighbours(final BlockAndTintGetter level, final BlockPos pos,
                                           final BlockState state, final ModelData data) {
        final MicroblockLayout own = data.get(MicroblockLayout.PROPERTY);
        if (!any() || own == null || own.isEmpty()) {
            return data;
        }
        final Object[] spaces = new Object[PackSurroundings.count()];
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    final ModelData there = dx == 0 && dy == 0 && dz == 0 ? data
                            : level.getModelData(at.setWithOffset(pos, dx, dy, dz));
                    spaces[PackSurroundings.cell(dx, dy, dz)] = there.get(MicroblockLayout.PROPERTY);
                }
            }
        }
        return PackSurroundings.of(level, pos, state, false, false).with(spaces).into(data);
    }

    /**
     * The quads of the microblock {@code block} drawn with the packs' connected textures of its material, joining a
     * whole block of its material and a microblock like it at the same place of a space around; as they are where the
     * packs say nothing of it.
     */
    public static List<BakedQuad> microblock(final List<BakedQuad> quads, final Microblock block,
                                             final ModelData data, final RenderType renderType) {
        final PackSurroundings around = data.get(PackSurroundings.PROPERTY);
        if (around == null || quads.isEmpty()) {
            return quads;
        }
        final BlockState material = block.piece().material().defaultBlockState();
        final PackSurroundings seen = asMaterial(around, block, material);
        final List<BakedQuad> out = new ArrayList<>(quads.size());
        for (final BakedQuad quad : quads) {
            for (final PackFaces.Drawn drawn : PackFaces.draw(quad, material, seen, Map.of())) {
                if (renderType == null || drawn.layer() == null || drawn.layer() == renderType) {
                    out.add(drawn.quad());
                }
            }
        }
        return out;
    }

    /* The blocks around as the microblock sees them: a space holding one like it is a block of its material. */
    private static PackSurroundings asMaterial(final PackSurroundings around, final Microblock block,
                                               final BlockState material) {
        final BlockState[] cells = new BlockState[PackSurroundings.count()];
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    final int cell = PackSurroundings.cell(dx, dy, dz);
                    final BlockState real = around.cells()[cell];
                    final Object extra = around.extra() == null ? null : around.extra()[cell];
                    final boolean alike = extra instanceof MicroblockLayout layout && layout.drawn().stream()
                            .anyMatch(drawn -> drawn.block().equals(block));
                    cells[cell] = alike || dx == 0 && dy == 0 && dz == 0 ? material : real;
                }
            }
        }
        return new PackSurroundings(cells, around.pos(), around.biome(), around.name(), null);
    }

    private static void onModifyBakingResult(final ModelEvent.ModifyBakingResult event) {
        PackCandidates.forget();
        PackJoins.forget();
        final PackRules rules = PackRules.current();
        if (rules.isEmpty()) {
            return;
        }
        final boolean withBiome = rules(rules).anyMatch(rule -> !rule.where().biomes().isEmpty());
        final boolean withName = rules(rules).anyMatch(rule -> !rule.name().isEmpty());
        final ChunkRenderTypeSet layers = layers(rules);
        final ModelBakery bakery = event.getModelBakery();
        int wrapped = 0;
        for (final Map.Entry<ModelResourceLocation, BakedModel> entry : event.getModels().entrySet()) {
            final ModelResourceLocation location = entry.getKey();
            if (INVENTORY.equals(location.variant()) || STANDALONE.equals(location.variant())) {
                continue;
            }
            final Block block = BuiltInRegistries.BLOCK.get(location.id());
            if (block == Blocks.AIR) {
                continue;
            }
            final Map<Integer, CtmTexture> overrides = overridesOf(bakery.topLevelModels.get(location), rules);
            if (drawsAny(entry.getValue(), block, overrides, rules)) {
                entry.setValue(new ConnectedPackModel(entry.getValue(), overrides, layers, withBiome, withName));
                wrapped++;
            }
        }
        LOGGER.debug("Connected textures of resource packs draw {} models of blocks", wrapped);
    }

    /* Whether a rule of the packs may draw a face of the model of {@code block}. */
    private static boolean drawsAny(final BakedModel model, final Block block,
                                    final Map<Integer, CtmTexture> overrides, final PackRules rules) {
        if (!overrides.isEmpty() || !rules.byTag().isEmpty()
                || rules.byBlock().containsKey(BuiltInRegistries.BLOCK.getKey(block).toString())) {
            return true;
        }
        final BlockState state = block.defaultBlockState();
        final RandomSource random = RandomSource.create(SEED);
        final List<Direction> sides = new ArrayList<>(List.of(Direction.values()));
        sides.add(null);
        for (final Direction side : sides) {
            for (final BakedQuad quad : model.getQuads(state, side, random, ModelData.EMPTY, null)) {
                final String sprite = quad.getSprite().contents().name().toString();
                if (rules.byTile().containsKey(sprite) || rules.ctm().containsKey(sprite)) {
                    return true;
                }
            }
        }
        return false;
    }

    /* CTM's overrides of the models a block's state was baked from, by tint index. */
    private static Map<Integer, CtmTexture> overridesOf(final UnbakedModel unbaked, final PackRules rules) {
        if (unbaked == null || rules.overrides().isEmpty()) {
            return Map.of();
        }
        final Map<Integer, CtmTexture> out = new HashMap<>();
        for (final ResourceLocation model : unbaked.getDependencies()) {
            out.putAll(rules.overrides().getOrDefault(model.toString(), Map.of()));
        }
        return out;
    }

    /* The layers the packs' rules draw in besides a model's own. */
    private static ChunkRenderTypeSet layers(final PackRules rules) {
        final Set<RenderType> out = new HashSet<>();
        rules(rules).filter(rule -> rule.method().overlays()).forEach(rule -> out.add(
                "translucent".equals(rule.overlay().layer()) ? RenderType.translucent() : RenderType.cutout()));
        Stream.concat(rules.ctm().values().stream(), rules.overrides().values().stream()
                .flatMap(byTint -> byTint.values().stream())).forEach(texture -> texture.layer().ifPresent(layer ->
                out.add(PackFaces.layerOf(layer))));
        return ChunkRenderTypeSet.of(out);
    }

    private static Stream<OptifineRule> rules(final PackRules rules) {
        return Stream.concat(Stream.concat(rules.byTile().values().stream().flatMap(List::stream),
                rules.byBlock().values().stream().flatMap(List::stream)), rules.byTag().stream());
    }
}
