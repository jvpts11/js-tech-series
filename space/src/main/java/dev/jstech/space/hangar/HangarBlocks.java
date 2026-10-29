/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.hangar;

import dev.jstech.core.content.BlockBuilder;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.ContentTab;
import dev.jstech.core.content.Drops;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IBlockModel;
import dev.jstech.space.SpaceContent;
import java.util.function.Function;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The Rocket Hangar's kit of blocks, the faces of the approved kit: its terminal, set in a wall, from which the hangar
 * finds its bay; the Assembly Console, where a rocket is designed; the crane, its rails along the tops of two facing
 * walls, its bridge across the bay and its hoist under the bridge; the bay doors, set in the walls; and the work
 * platforms round the rocket. Its walls and roof are any solid blocks a player likes. Formed, the crane's pieces, the
 * doors and the platforms take their own shapes.
 */
public final class HangarBlocks {

    /** Its row of the mod's tab. */
    public static final ContentTab.Section HANGAR = SpaceContent.TAB.section();
    public static final BlockEntry<HangarTerminalBlock> TERMINAL = SpaceContent.CONTENT.block("hangar_terminal",
                    HangarTerminalBlock::new).named("Hangar Terminal")
            .properties(HangarBlocks::metal)
            .look(IBlockLook.fixed(new IBlockModel.CubeAll("hangar_terminal", "block/hangar/hangar_terminal")))
            .item().drops(Drops.SELF).tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(HANGAR).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HangarTerminalBlockEntity>>
            TERMINAL_ENTITY = SpaceContent.CONTENT.blockEntity("hangar_terminal", HangarTerminalBlockEntity::new,
            TERMINAL);
    public static final BlockEntry<AssemblyConsoleBlock> ASSEMBLY_CONSOLE = SpaceContent.CONTENT.block(
                    "assembly_console", AssemblyConsoleBlock::new).named("Assembly Console")
            .properties(HangarBlocks::metal)
            .look(IBlockLook.fixed(new IBlockModel.CubeAll("assembly_console", "block/hangar/assembly_console")))
            .item().drops(Drops.SELF).tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(HANGAR).register();
    public static final BlockEntry<Block> CRANE_RAIL = piece("crane_rail", HangarPieceBlock.Kind.RAIL)
            .named("Crane Rail").register();
    public static final BlockEntry<Block> CRANE_BRIDGE = piece("crane_bridge", HangarPieceBlock.Kind.BRIDGE)
            .named("Crane Bridge").register();
    public static final BlockEntry<Block> CRANE_HOIST = piece("crane_hoist", HangarPieceBlock.Kind.HOIST)
            .named("Crane Hoist").register();
    public static final BlockEntry<Block> BAY_DOOR = piece("bay_door", HangarPieceBlock.Kind.DOOR)
            .named("Bay Door").register();
    public static final BlockEntry<Block> WORK_PLATFORM = piece("work_platform", HangarPieceBlock.Kind.PLATFORM)
            .named("Work Platform").register();

    private HangarBlocks() {
    }

    /** Declares the blocks, once, from the mod's constructor, before its content registers. */
    public static void declare() {
        // Loading the class declares them.
    }

    /* A piece of the kit: the kit's face on all six sides, its own shape once its hangar is formed (but a door's). */
    private static BlockBuilder<Block> piece(final String id, final HangarPieceBlock.Kind kind) {
        final Function<BlockBehaviour.Properties, Block> made = properties -> new HangarPieceBlock(properties, kind);
        final IBlockModel.CubeAll face = new IBlockModel.CubeAll(id, "block/hangar/" + id);
        final IBlockLook look = kind == HangarPieceBlock.Kind.DOOR ? IBlockLook.facing(face)
                : IBlockLook.facing(face).whileOn(HangarPieceBlock.FORMED, new IBlockModel.Handmade(
                        "hangar/" + id + "_formed"));
        return SpaceContent.CONTENT.block(id, made).properties(HangarBlocks::metal).look(look).item()
                .drops(Drops.SELF).tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(HANGAR);
    }

    /* Metal, as hard as iron. */
    private static BlockBehaviour.Properties metal(final BlockBehaviour.Properties properties) {
        return properties.mapColor(MapColor.METAL).strength(3.0F, 6.0F).requiresCorrectToolForDrops()
                .sound(SoundType.METAL);
    }
}
