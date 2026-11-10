/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.abandoned;

import static dev.jstech.computers.registry.ComputingContent.CONTENT;

import dev.jstech.computers.block.PartEras;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IBlockModel;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The blocks only the abandoned buildings set down (computers/docs/design/survival.md, "Abandoned buildings"): their
 * fittings, and the hardware box. Neither has an item: a player finds them, and takes what a box holds.
 */
public final class AbandonedBlocks {

    /** A fitting of a building: a board, a light, a sign; its model by its part. */
    public static final BlockEntry<AbandonedPropBlock> PROP = CONTENT.block("abandoned_prop", AbandonedPropBlock::new)
            .named("Fitting")
            .properties(properties -> properties.mapColor(MapColor.METAL).strength(1.5F).sound(SoundType.METAL)
                    .noOcclusion())
            .look(IBlockLook.facingByState(state -> new IBlockModel.Handmade("abandoned_prop/p"
                    + state.getValue(AbandonedPropBlock.PART))))
            .register();
    /** The hardware box, printed with its era's machine; drawn by era, kind and whether it stands open. */
    public static final BlockEntry<HardwareBoxBlock> HARDWARE_BOX = CONTENT.block("hardware_box",
                    HardwareBoxBlock::new)
            .named("Hardware Box")
            .properties(properties -> properties.mapColor(MapColor.WOOD).strength(0.8F).sound(SoundType.WOOL)
                    .noOcclusion())
            .look(IBlockLook.facingByState(state -> new IBlockModel.Handmade("hardware_box/"
                    + PartEras.eraOf(state.getValue(HardwareBoxBlock.ERA)).path() + "_"
                    + state.getValue(HardwareBoxBlock.KIND).getSerializedName()
                    + (state.getValue(HardwareBoxBlock.OPEN) ? "_open" : ""))))
            .register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HardwareBoxBlockEntity>> HARDWARE_BOX_BE =
            CONTENT.blockEntity("hardware_box", HardwareBoxBlockEntity::new, HARDWARE_BOX);

    private AbandonedBlocks() {
    }

    /** Declares the blocks, while the mod is built. */
    public static void init() {
        // Loading the class declares them.
    }
}
