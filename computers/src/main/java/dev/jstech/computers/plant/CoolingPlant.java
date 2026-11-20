/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.plant;

import static dev.jstech.computers.registry.ComputingContent.CONTENT;
import static dev.jstech.computers.registry.ComputingContent.shelf;

import dev.jstech.computers.registry.ComputingContent.Shelf;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.Drops;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IBlockModel;
import dev.jstech.core.content.IItemLook;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The datacenter's cooling plant's own blocks besides the chain machines (computers/docs/design/temperature.md): the
 * CDU, placed whole as the Supercomputer Rack is, of the Standard as that rack is, drawn by its block entity with its
 * approved model, the bay of a rack's cabinet holding its pumps and its plate exchanger.
 */
public final class CoolingPlant {

    /* The cabinet's blocks show nothing of their own: the CDU is drawn whole from its origin. */
    private static final IBlockLook BODY = IBlockLook.fixed(new IBlockModel.ParticleOnly("cdu", "block/rack_particle"));

    public static final BlockEntry<CduBlock> CDU = CONTENT.block("cdu", CduBlock::new)
            .properties(properties -> properties.mapColor(MapColor.METAL).strength(1.5F).sound(SoundType.METAL)
                    .noOcclusion())
            .named("CDU").look(BODY).item().itemLook(IItemLook.DRAWN_BY_ENTITY).drops(Drops.NONE)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(shelf(Shelf.SERVER_RACK, HardwareEra.STANDARD)).register();
    public static final BlockEntry<CduPartBlock> CDU_PART = CONTENT.block("cdu_part", CduPartBlock::new)
            .properties(properties -> properties.mapColor(MapColor.METAL).strength(1.5F).sound(SoundType.METAL)
                    .noOcclusion())
            .named("CDU").look(BODY).tag(BlockTags.MINEABLE_WITH_PICKAXE).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CduBlockEntity>> CDU_BE =
            CONTENT.blockEntity("cdu", CduBlockEntity::new, CDU);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CduPartBlockEntity>> CDU_PART_BE =
            CONTENT.blockEntity("cdu_part", CduPartBlockEntity::new, CDU_PART);
    /** The CDU's model, as its approved design draws it, with its working loop. */
    public static final ResourceLocation CDU_MODEL = CONTENT.model("plant/cdu").file("jsmodels/chain/cdu.glb")
            .register().id();

    private CoolingPlant() {
    }

    /** Declares the plant's blocks, from the mod's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }
}
