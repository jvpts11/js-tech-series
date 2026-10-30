/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.pad;

import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.ContentTab;
import dev.jstech.core.content.Drops;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IBlockModel;
import dev.jstech.space.SpaceContent;
import java.util.List;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The launch pad's kit of J's Space, beside the Pad Pedestal the launcher stands on: the Flame Deflector at the foot
 * of the flame trench, the Trench Lining of its walls and floor, and the Pad Service Port where the pad's lines come
 * up.
 */
public final class PadBlocks {

    /** Its row of the mod's tab. */
    public static final ContentTab.Section PAD = SpaceContent.TAB.section();
    public static final BlockEntry<FlameDeflectorBlock> FLAME_DEFLECTOR = SpaceContent.CONTENT.block("flame_deflector",
                    FlameDeflectorBlock::new).properties(PadBlocks::steel)
            .look(IBlockLook.facing(new IBlockModel.Handmade("flame_deflector"))).item().drops(Drops.SELF)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(PAD).named("Flame Deflector").register();
    public static final BlockEntry<Block> TRENCH_LINING = SpaceContent.CONTENT.block("trench_lining", Block::new)
            .properties(properties -> properties.mapColor(MapColor.SAND).strength(4.0F, 9.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.STONE))
            .look(IBlockLook.fixed(new IBlockModel.CubeAll("trench_lining", "block/pad/trench_lining"))).item()
            .drops(Drops.SELF).tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(PAD).named("Trench Lining").register();
    public static final BlockEntry<PadServicePortBlock> PAD_SERVICE_PORT = SpaceContent.CONTENT.block(
                    "pad_service_port", PadServicePortBlock::new).properties(PadBlocks::steel)
            .look(IBlockLook.fixed(new IBlockModel.CubeAll("pad_service_port", "block/pad/pad_service_port"))).item()
            .drops(Drops.SELF).tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(PAD).named("Pad Service Port").register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PadServicePortBlockEntity>> PORT_ENTITY =
            SpaceContent.CONTENT.blockEntity("pad_service_port", PadServicePortBlockEntity::new, PAD_SERVICE_PORT);

    private PadBlocks() {
    }

    /** Declares the blocks, once, from the mod's constructor, before its content registers. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** Every block of the kit but the pedestal, which the launcher's kit holds, in its order. */
    public static List<BlockEntry<? extends Block>> all() {
        return List.of(FLAME_DEFLECTOR, TRENCH_LINING, PAD_SERVICE_PORT);
    }

    /* Steel, as hard as iron. */
    private static BlockBehaviour.Properties steel(final BlockBehaviour.Properties properties) {
        return properties.mapColor(MapColor.METAL).strength(3.0F, 6.0F).requiresCorrectToolForDrops()
                .sound(SoundType.METAL);
    }
}
