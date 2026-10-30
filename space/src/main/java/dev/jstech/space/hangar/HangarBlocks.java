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
import dev.jstech.core.multiblock.MultiblockPortBlock;
import dev.jstech.core.multiblock.MultiblockPorts;
import dev.jstech.space.SpaceContent;
import java.util.function.Function;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The Rocket Hangar's kit of blocks, the faces of the approved kit: its terminal, set in a wall, from which the hangar
 * finds its bay; the Assembly Console, where a rocket is designed; the crane, its rails along the tops of two facing
 * walls, its bridge across the bay and its hoist under the bridge; the bay doors, set in the walls; the work platforms
 * round the rocket; and the item ports, set in its shell, through which the crane's blocks come in. Its walls and roof
 * are any solid blocks a player likes. Formed, the crane's pieces, the doors and the platforms take their own shapes.
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
    /** The port in a wall the blocks of the rocket the crane builds come in through. */
    public static final BlockEntry<MultiblockPortBlock> ITEM_PORT = SpaceContent.CONTENT.block("item_port",
                    properties -> new MultiblockPortBlock(properties, () -> HangarBlocks.ITEM_PORT_ENTITY.get()))
            .named("Item Port").properties(HangarBlocks::metal)
            .look(IBlockLook.fixed(new IBlockModel.CubeAll("item_port", "block/hangar/item_port")))
            .item().drops(Drops.SELF).tag(BlockTags.MINEABLE_WITH_PICKAXE).tag(MultiblockPorts.PORTS).tab(HANGAR)
            .register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ItemPortBlockEntity>> ITEM_PORT_ENTITY =
            SpaceContent.CONTENT.blockEntity("item_port", ItemPortBlockEntity::new, ITEM_PORT);

    private HangarBlocks() {
    }

    /**
     * Declares the blocks, once, from the mod's constructor, before its content registers; their ports pass what
     * reaches them on to their hangar's terminal.
     */
    public static void declare(final IEventBus modBus) {
        modBus.addListener(RegisterCapabilitiesEvent.class,
                event -> MultiblockPorts.register(event, ITEM_PORT_ENTITY.get()));
    }

    /*
     * A piece of the kit: the kit's face on all six sides; once its hangar is formed (but a door) nothing of its own,
     * its terminal drawing the crane and the platforms whole.
     */
    private static BlockBuilder<Block> piece(final String id, final HangarPieceBlock.Kind kind) {
        final Function<BlockBehaviour.Properties, Block> made = properties -> new HangarPieceBlock(properties, kind);
        final IBlockModel.CubeAll face = new IBlockModel.CubeAll(id, "block/hangar/" + id);
        final IBlockLook look = kind == HangarPieceBlock.Kind.DOOR ? IBlockLook.facing(face)
                : IBlockLook.facing(face).whileOn(HangarPieceBlock.FORMED, new IBlockModel.ParticleOnly(
                        id + "_drawn", "block/hangar/" + id));
        return SpaceContent.CONTENT.block(id, made).properties(HangarBlocks::metal).look(look).item()
                .drops(Drops.SELF).tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(HANGAR);
    }

    /* Metal, as hard as iron. */
    private static BlockBehaviour.Properties metal(final BlockBehaviour.Properties properties) {
        return properties.mapColor(MapColor.METAL).strength(3.0F, 6.0F).requiresCorrectToolForDrops()
                .sound(SoundType.METAL);
    }
}
