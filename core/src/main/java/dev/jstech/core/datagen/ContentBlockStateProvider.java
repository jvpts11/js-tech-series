/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.datagen;

import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IBlockModel;
import dev.jstech.core.content.ModContent;
import dev.jstech.core.util.DeclarationError;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Writes the block state and the block models of every block a mod declared, from the look it was declared with.
 */
public final class ContentBlockStateProvider extends BlockStateProvider {

    private final ModContent content;
    /* A varied block is turned by quarters, four of them. */
    private static final int QUARTERS = 4;
    private static final int QUARTER = 90;
    /* The light a lamp drawn at full light is drawn at, of the block and of the sky. */
    private static final int FULL_LIGHT = 15;

    public ContentBlockStateProvider(final PackOutput output, final ModContent content,
                                     final ExistingFileHelper existingFiles) {
        super(output, content.modid(), existingFiles);
        this.content = content;
    }

    @Override
    protected void registerStatesAndModels() {
        for (final BlockEntry<?> entry : content.declaredBlocks()) {
            final Block block = entry.get();
            switch (entry.look()) {
                case IBlockLook.Fixed fixed -> simpleBlock(block, model(fixed.model()));
                case IBlockLook.Axis axis -> {
                    if (!(block instanceof RotatedPillarBlock pillar)) {
                        throw DeclarationError.of(content.modid(), "the block " + entry.getId(),
                                "is laid along an axis but has none", "make it a RotatedPillarBlock, or give it "
                                        + "another look");
                    }
                    final ModelFile pillarModel = model(axis.model());
                    axisBlock(pillar, pillarModel, pillarModel);
                }
                case IBlockLook.ByState byState -> {
                    // Each model written once, however many states show it.
                    final Map<String, ModelFile> files = new HashMap<>();
                    getVariantBuilder(block).forAllStates(state -> {
                        final IBlockModel chosen = byState.model().apply(state);
                        return ConfiguredModel.builder()
                                .modelFile(files.computeIfAbsent(chosen.name(), name -> model(chosen))).build();
                    });
                }
                case IBlockLook.Facing facing -> {
                    final ModelFile off = model(facing.model());
                    final IBlockLook.Toggle toggle = facing.toggle();
                    final ModelFile on = toggle == null ? off : model(toggle.on());
                    horizontalBlock(block, state -> toggle != null && state.getValue(toggle.property()) ? on : off,
                            facing.angleOffset());
                }
                case IBlockLook.FacingByState byState -> {
                    // Each model written once, however many states show it.
                    final Map<String, ModelFile> files = new HashMap<>();
                    horizontalBlock(block, state -> {
                        final IBlockModel chosen = byState.model().apply(state);
                        return files.computeIfAbsent(chosen.name(), name -> model(chosen));
                    }, byState.angleOffset());
                }
                case IBlockLook.Pipe pipe -> pipe(block, entry.getId().getPath(), pipe);
                case IBlockLook.Varied varied -> varied(block, varied);
            }
        }
    }

    /* Every state shows one of the models, the game picking by where the block stands, each turned if wanted. */
    private void varied(final Block block, final IBlockLook.Varied varied) {
        final List<ConfiguredModel> each = new ArrayList<>();
        for (final IBlockModel one : varied.models()) {
            final ModelFile file = model(one);
            for (int turn = 0; turn < (varied.turned() ? QUARTERS : 1); turn++) {
                each.add(new ConfiguredModel(file, 0, turn * QUARTER, false));
            }
        }
        final ConfiguredModel[] models = each.toArray(new ConfiguredModel[0]);
        getVariantBuilder(block).forAllStates(state -> models);
    }

    private ModelFile model(final IBlockModel model) {
        return switch (model) {
            case IBlockModel.CubeAll cube -> models().cubeAll(cube.name(), file(cube.texture()));
            case IBlockModel.CubeAllLit cube -> cubeAllLit(cube);
            case IBlockModel.Column column -> models().cubeColumn(column.name(), file(column.side()),
                    file(column.end()));
            case IBlockModel.BottomTop box -> {
                final BlockModelBuilder built = models().cubeBottomTop(box.name(), file(box.side()),
                        file(box.bottom()), file(box.top()));
                yield box.renderType() == null ? built : built.renderType(box.renderType());
            }
            case IBlockModel.Orientable machine -> models().orientable(machine.name(), file(machine.side()),
                    file(machine.front()), file(machine.top()));
            case IBlockModel.OrientableLit machine -> orientableLit(machine);
            case IBlockModel.Controller controller -> controller(controller);
            case IBlockModel.SixFaces cube -> models().cube(cube.name(), file(cube.down()), file(cube.up()),
                    file(cube.north()), file(cube.south()), file(cube.east()), file(cube.west()))
                    .texture("particle", file(cube.particle()));
            case IBlockModel.ParticleOnly body -> models().getBuilder(body.name())
                    .texture("particle", file(body.particle()));
            case IBlockModel.Handmade handmade -> models().getExistingFile(modLoc("block/" + handmade.name()));
        };
    }

    /** A core in the middle and an arm turned toward each side the cable connects to. */
    /*
     * The casing as a whole cube, and the panel over its north face, the front a facing turns: drawn after the
     * casing on the same plane, as the grass block's side lays its overlay, cut out where the panel is clear.
     */
    private ModelFile controller(final IBlockModel.Controller controller) {
        final BlockModelBuilder built = models().withExistingParent(controller.name(), mcLoc("block/block"))
                .texture("particle", file(controller.casing()))
                .texture("casing", file(controller.casing()))
                .texture("panel", file(controller.panel()))
                .renderType("cutout");
        built.element().from(0, 0, 0).to(16, 16, 16)
                .allFaces((direction, face) -> face.texture("#casing").cullface(direction)).end();
        built.element().from(0, 0, 0).to(16, 16, 16)
                .face(Direction.NORTH).texture("#panel").cullface(Direction.NORTH).end().end();
        return built;
    }

    /*
     * The texture as a whole cube, and the lit texture over it on the same planes, drawn after it as the controller's
     * panel is, cut out where it is clear and at full light, so the lamps shine in the dark.
     */
    private ModelFile cubeAllLit(final IBlockModel.CubeAllLit cube) {
        final BlockModelBuilder built = models().withExistingParent(cube.name(), mcLoc("block/block"))
                .texture("particle", file(cube.texture()))
                .texture("all", file(cube.texture()))
                .texture("lit", file(cube.lit()))
                .renderType("cutout");
        built.element().from(0, 0, 0).to(16, 16, 16)
                .allFaces((direction, face) -> face.texture("#all").cullface(direction)).end();
        built.element().from(0, 0, 0).to(16, 16, 16).emissivity(FULL_LIGHT, FULL_LIGHT)
                .allFaces((direction, face) -> face.texture("#lit").cullface(direction)).end();
        return built;
    }

    /*
     * An orientable cube (its front north, the way a facing turns it), and the lit texture over its front on the same
     * plane, cut out where it is clear and at full light.
     */
    private ModelFile orientableLit(final IBlockModel.OrientableLit machine) {
        final BlockModelBuilder built = models().withExistingParent(machine.name(), mcLoc("block/block"))
                .texture("particle", file(machine.front()))
                .texture("side", file(machine.side()))
                .texture("front", file(machine.front()))
                .texture("top", file(machine.top()))
                .texture("lit", file(machine.frontLit()))
                .renderType("cutout");
        built.element().from(0, 0, 0).to(16, 16, 16).allFaces((direction, face) -> face.texture(
                direction == Direction.NORTH ? "#front" : direction.getAxis() == Direction.Axis.Y ? "#top" : "#side")
                .cullface(direction)).end();
        built.element().from(0, 0, 0).to(16, 16, 16).emissivity(FULL_LIGHT, FULL_LIGHT)
                .face(Direction.NORTH).texture("#lit").cullface(Direction.NORTH).end().end();
        return built;
    }

    private void pipe(final Block block, final String name, final IBlockLook.Pipe pipe) {
        final ResourceLocation texture = file(pipe.texture());
        final ModelFile core = models().withExistingParent(name + "_core", file(pipe.coreParent()))
                .texture("cable", texture);
        final ModelFile arm = models().withExistingParent(name + "_arm", file(pipe.armParent()))
                .texture("cable", texture);
        final MultiPartBlockStateBuilder builder = getMultipartBuilder(block);
        builder.part().modelFile(core).addModel().end();
        builder.part().modelFile(arm).addModel().condition(PipeBlock.DOWN, true).end();
        builder.part().modelFile(arm).rotationX(180).addModel().condition(PipeBlock.UP, true).end();
        builder.part().modelFile(arm).rotationX(270).addModel().condition(PipeBlock.NORTH, true).end();
        builder.part().modelFile(arm).rotationX(270).rotationY(180).addModel()
                .condition(PipeBlock.SOUTH, true).end();
        builder.part().modelFile(arm).rotationX(270).rotationY(90).addModel()
                .condition(PipeBlock.EAST, true).end();
        builder.part().modelFile(arm).rotationX(270).rotationY(270).addModel()
                .condition(PipeBlock.WEST, true).end();
    }

    private ResourceLocation file(final String name) {
        return ContentFiles.named(content.modid(), name);
    }
}
