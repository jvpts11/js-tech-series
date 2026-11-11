/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.Printer3DBlockEntity;
import dev.jstech.computers.menu.Printer3DMenu;
import dev.jstech.computers.teracoin.Printer3DModel;
import dev.jstech.core.connect.FacePorts;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.DeviceBlock;
import dev.jstech.core.jstechseries.tier.Era;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A 3D printer (computers/docs/design/teracoin.md, "The 3D Printer"), a peripheral of one block named for its model:
 * the Standard's Prosa i3 MK3 and the Advanced's Rattan Lab X1. Its device port is in the middle of its back, as every
 * printer of the mod has it; a click opens its window.
 */
public class Printer3DBlock extends DeviceBlock implements IEraChassisBlock {

    private final Printer3DModel model;
    private final FacePorts ports;

    public static final MapCodec<Printer3DBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            propertiesCodec(),
            Codec.STRING.xmap(Printer3DModel::byName, Printer3DModel::name).fieldOf("model")
                    .forGetter(Printer3DBlock::model)
    ).apply(i, Printer3DBlock::new));
    /** Its block entity, ticking its link and its work, with its window as its menu. */
    private static final Device<Printer3DBlockEntity> DEVICE = Device.of(() -> ComputingModule.PRINTER_3D_BE.get())
            .ticks(Printer3DBlockEntity::serverTick)
            .opensMenu(Printer3DMenu::new);

    public Printer3DBlock(final Properties properties, final Printer3DModel model) {
        super(properties, DEVICE);
        this.model = model;
        this.ports = PeripheralSockets.back(model.era());
    }

    /** The model this block is. */
    public Printer3DModel model() {
        return model;
    }

    @Override
    public Era chassisEra() {
        return model.era();
    }

    /** The device port of its era, in the middle of its back. */
    @Override
    public FacePorts ports() {
        return ports;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    /** The printer is one model drawn by the block entity; the block itself paints nothing over it. */
    @Override
    protected RenderShape getRenderShape(final BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    /* Each printer stands on a base that fills its block, so the shape is the plain cube. */
    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos,
                                  final CollisionContext context) {
        return Shapes.block();
    }
}
