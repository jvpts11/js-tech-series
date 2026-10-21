/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import dev.jstech.core.content.GeoLook;
import dev.jstech.core.developer.GeoBones;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The model: a block's states file and the model the game baked from it, or the GeckoLib model, atlas and animation
 * of a block drawn with GeckoLib; an item's model file and its baked model. Wrong when a file is missing, when the
 * game draws the missing model, when a model names a texture that is not there, or when an animation moves a bone the
 * model does not have.
 */
final class ModelReader implements IStudioReader {

    private static final ItemStack ICON = new ItemStack(Items.ARMOR_STAND);
    private static final String JSON = ".json";

    @Override
    public void read(final StudioThing thing, final StudioContext context, final StudioSheet sheet) {
        final Block block = thing.block();
        if (block != null && thing.kind() != ThingKind.FLUIDS) {
            final Optional<GeoLook<?>> geo = thing.entry() == null ? Optional.empty() : thing.entry().geoLook();
            if (geo.isPresent()) {
                geo(thing, block, geo.get(), context, sheet);
            } else {
                block(thing, block, context, sheet);
            }
        } else if (thing.item() != null && thing.kind() != ThingKind.FLUIDS) {
            item(thing, true, context, sheet);
        }
    }

    private static void block(final StudioThing thing, final Block block, final StudioContext context,
                              final StudioSheet sheet) {
        final ResourceLocation states = file(thing.id(), "blockstates/");
        final boolean there = context.exists(states);
        sheet.part(new StudioSheet.Part(ICON, GameText.resolve(StudioTexts.MODEL), states.getPath(),
                there ? StudioSheet.Mark.FINE : StudioSheet.Mark.ERROR, Optional.of(StudioFiles.asset(states))));
        if (!there) {
            missing(sheet, states);
        }
        final BlockState state = block.defaultBlockState();
        if (state.getRenderShape() == RenderShape.MODEL) {
            final BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
            if (there && model == Minecraft.getInstance().getModelManager().getMissingModel()) {
                sheet.check(new StudioSheet.Check(StudioSheet.Mark.ERROR, GameText.resolve(StudioTexts.NO_MODEL),
                        GameText.resolve(StudioTexts.NO_MODEL_DETAIL.with(state.toString())),
                        Optional.of(StudioFiles.asset(states))));
            } else if (missesTexture(model, state)) {
                textureMissing(sheet, states);
            }
        }
        if (thing.item() != null) {
            item(thing, false, context, sheet);
        }
    }

    /* An item's model: its own row for an item, only its checks for a block's item. */
    private static void item(final StudioThing thing, final boolean row, final StudioContext context,
                             final StudioSheet sheet) {
        final ResourceLocation file = file(thing.id(), "models/item/");
        final boolean there = context.exists(file);
        if (row) {
            sheet.part(new StudioSheet.Part(ICON, GameText.resolve(StudioTexts.MODEL), file.getPath(),
                    there ? StudioSheet.Mark.FINE : StudioSheet.Mark.ERROR, Optional.of(StudioFiles.asset(file))));
        }
        if (!there) {
            missing(sheet, file);
            return;
        }
        final ItemStack stack = new ItemStack(thing.item());
        final BakedModel model = Minecraft.getInstance().getItemRenderer().getModel(stack, null, null, 0);
        if (model == Minecraft.getInstance().getModelManager().getMissingModel()) {
            sheet.check(new StudioSheet.Check(StudioSheet.Mark.ERROR, GameText.resolve(StudioTexts.NO_MODEL),
                    GameText.resolve(StudioTexts.NO_MODEL_DETAIL.with(thing.id().toString())),
                    Optional.of(StudioFiles.asset(file))));
        } else if (missesTexture(model, null)) {
            textureMissing(sheet, file);
        }
    }

    /* A block drawn with GeckoLib: the model its block entity names, the atlas, and the family's animation. */
    @SuppressWarnings("unchecked")
    private static void geo(final StudioThing thing, final Block block, final GeoLook<?> look,
                            final StudioContext context, final StudioSheet sheet) {
        // The block entity names the model it is drawn with; with none, the block's own name stands for it.
        String name = thing.id().getPath();
        final Optional<BlockEntity> drawn = MadeBlockEntities.of(block);
        if (drawn.isPresent()) {
            try {
                name = ((Function<Object, String>) look.model()).apply(drawn.get());
            } catch (final RuntimeException notItsKind) {
                // A look of another kind of block entity than this one: the block's own name stands for it.
            }
        }
        final ResourceLocation model = look.modelFile(name);
        final boolean there = context.exists(model);
        sheet.part(new StudioSheet.Part(ICON, GameText.resolve(StudioTexts.MODEL), model.getPath(),
                there ? StudioSheet.Mark.FINE : StudioSheet.Mark.ERROR, Optional.of(StudioFiles.asset(model))));
        if (!there) {
            missing(sheet, model);
        }
        final ResourceLocation atlas = look.textureFile(name);
        if (!context.exists(atlas)) {
            missing(sheet, atlas);
        }
        final ResourceLocation animation = look.animationFile();
        if (!context.exists(animation)) {
            missing(sheet, animation);
        } else if (there) {
            bones(context, look.modid(), animation, sheet);
        }
    }

    /*
     * A family shares its animation file, and each of its models has only some of the bones it moves, as a computer
     * without a drive has no tray; so a bone is missing only when no model of the mod has it, as a bone renamed.
     */
    private static void bones(final StudioContext context, final String modid, final ResourceLocation animation,
                              final StudioSheet sheet) {
        final Set<String> has = context.geoBones(modid);
        if (has.isEmpty()) {
            return;
        }
        context.json(animation).map(GeoBones::ofAnimations).orElse(List.of()).stream()
                .filter(moved -> !has.contains(moved.bone()))
                .forEach(moved -> sheet.check(new StudioSheet.Check(StudioSheet.Mark.WARNING,
                        GameText.resolve(StudioTexts.MISSING_BONE),
                        GameText.resolve(StudioTexts.MISSING_BONE_DETAIL.with(moved.animation(), moved.bone(),
                                modid)),
                        Optional.of(StudioFiles.asset(animation)))));
    }

    private static boolean missesTexture(final BakedModel model, final @Nullable BlockState state) {
        final ResourceLocation missing = MissingTextureAtlasSprite.getLocation();
        if (model.getParticleIcon().contents().name().equals(missing)) {
            return true;
        }
        final RandomSource random = RandomSource.create(0L);
        final List<Direction> sides = new ArrayList<>(List.of(Direction.values()));
        sides.add(null);
        for (final Direction side : sides) {
            for (final BakedQuad quad : model.getQuads(state, side, random)) {
                if (quad.getSprite().contents().name().equals(missing)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void missing(final StudioSheet sheet, final ResourceLocation file) {
        sheet.check(new StudioSheet.Check(StudioSheet.Mark.ERROR,
                GameText.resolve(StudioTexts.NO_FILE.with(file.getPath())),
                GameText.resolve(StudioTexts.NO_FILE_DETAIL), Optional.of(StudioFiles.asset(file))));
    }

    private static void textureMissing(final StudioSheet sheet, final ResourceLocation file) {
        sheet.check(new StudioSheet.Check(StudioSheet.Mark.WARNING, GameText.resolve(StudioTexts.NO_TEXTURE),
                GameText.resolve(StudioTexts.NO_TEXTURE_DETAIL.with(file.getPath())),
                Optional.of(StudioFiles.asset(file))));
    }

    private static ResourceLocation file(final ResourceLocation id, final String folder) {
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), folder + id.getPath() + JSON);
    }
}
