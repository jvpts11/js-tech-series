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
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Writes the block state and the block models of every block a mod declared, from the look it was declared with.
 */
public final class ContentBlockStateProvider extends BlockStateProvider {

    private final ModContent content;

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
                case IBlockLook.Facing facing -> {
                    final ModelFile off = model(facing.model());
                    final IBlockLook.Toggle toggle = facing.toggle();
                    final ModelFile on = toggle == null ? off : model(toggle.on());
                    horizontalBlock(block, state -> toggle != null && state.getValue(toggle.property()) ? on : off,
                            facing.angleOffset());
                }
                case IBlockLook.Pipe pipe -> pipe(block, entry.getId().getPath(), pipe);
            }
        }
    }

    private ModelFile model(final IBlockModel model) {
        return switch (model) {
            case IBlockModel.CubeAll cube -> models().cubeAll(cube.name(), file(cube.texture()));
            case IBlockModel.Column column -> models().cubeColumn(column.name(), file(column.side()),
                    file(column.end()));
            case IBlockModel.BottomTop box -> {
                final BlockModelBuilder built = models().cubeBottomTop(box.name(), file(box.side()),
                        file(box.bottom()), file(box.top()));
                yield box.renderType() == null ? built : built.renderType(box.renderType());
            }
            case IBlockModel.Orientable machine -> models().orientable(machine.name(), file(machine.side()),
                    file(machine.front()), file(machine.top()));
            case IBlockModel.SixFaces cube -> models().cube(cube.name(), file(cube.down()), file(cube.up()),
                    file(cube.north()), file(cube.south()), file(cube.east()), file(cube.west()))
                    .texture("particle", file(cube.particle()));
            case IBlockModel.ParticleOnly body -> models().getBuilder(body.name())
                    .texture("particle", file(body.particle()));
            case IBlockModel.Handmade handmade -> models().getExistingFile(modLoc("block/" + handmade.name()));
        };
    }

    /** A core in the middle and an arm turned toward each side the cable connects to. */
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
