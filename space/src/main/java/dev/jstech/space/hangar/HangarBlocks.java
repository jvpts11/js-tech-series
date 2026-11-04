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
import dev.jstech.core.content.Device;
import dev.jstech.core.content.Drops;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IBlockModel;
import dev.jstech.core.content.IItemLook;
import dev.jstech.core.machine.MachineMenu;
import dev.jstech.core.multiblock.MultiblockPortBlock;
import dev.jstech.core.multiblock.MultiblockPorts;
import dev.jstech.space.SpaceContent;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

/**
 * The Rocket Hangar's kit of blocks, the faces of its approved building: its terminal, a console in the control room's
 * row from which the hangar is worked; the hall's walls of panels, its dark frame, its roof, its windows, its concrete
 * floor and its lamps; the bay door, the stepped door's sections; the control room's floor, its step and the step's
 * railing, its consoles, the bay door's console, its chairs, the Rocket Design table, the status board, its personnel
 * doors and its ceiling lights; the loading dock in its outer wall, through which the crane's blocks come in; the
 * crane, its rails along the tops of the long walls, its bridge across the bay and its hoist under the bridge; and the
 * work platforms a player sets round a rocket. The Item Port is the yards' port: the Small Shipyard takes its blocks
 * through it. Formed, the crane's pieces and the platforms take their own shapes, the consoles light their screens and
 * the fittings turn the way the building has them.
 */
public final class HangarBlocks {

    /** Its row of the mod's tab. */
    public static final ContentTab.Section HANGAR = SpaceContent.TAB.section();
    /* How bright a lamp of the bay, or a light in the control room's ceiling, is. */
    private static final int LIT = 15;
    /* Where the building's models written by hand are, among the mod's block models. */
    private static final String HAND = "hangar/";
    private static final String BLOCK = "block/";
    /* The shapes of the fittings, each as it faces north, in sixteenths of a block. */
    private static final int[][] STEP_BOX = {{0, 0, 0, 16, 8, 16}};
    private static final int[][] RAILING_BOX = {{0, 0, 0, 16, 8, 16}, {0, 8, 1, 16, 24, 3}};
    /* A chair's back ends at its block's top, though drawn higher: the console's screen beyond it, looked at from
     * behind the chair, takes the click over it. */
    private static final int[][] CHAIR_BOX = {{3, 0, 3, 13, 9, 13}, {3, 9, 11, 13, 16, 13}};
    private static final int[][] BOARD_BOX = {{0, 0, 14, 16, 16, 16}};
    private static final int[][] LAMP_BOX = {{2, 5, 13, 14, 11, 16}};

    /** The terminal, a console of the control room's row, which the hangar is worked from. */
    public static final BlockEntry<HangarTerminalBlock> TERMINAL = SpaceContent.CONTENT.block("hangar_terminal",
                    properties -> new HangarTerminalBlock(properties, Device.of(() -> HangarBlocks.TERMINAL_ENTITY
                            .get()).ticks(HangarTerminalBlockEntity::serverTick).opensMenu(MachineMenu::new)))
            .named("Hangar Terminal")
            .properties(properties -> metal(properties).noOcclusion())
            .look(IBlockLook.facing(new IBlockModel.Handmade(HAND + "hangar_terminal")))
            .item().itemLook(IItemLook.parent(BLOCK + HAND + "hangar_terminal")).drops(Drops.SELF)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(HANGAR).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HangarTerminalBlockEntity>>
            TERMINAL_ENTITY = SpaceContent.CONTENT.blockEntity("hangar_terminal", HangarTerminalBlockEntity::new,
            TERMINAL);
    public static final BlockEntry<Block> WALL = cube("hangar_wall").named("Hangar Wall Panel").register();
    public static final BlockEntry<Block> FRAME = cube("hangar_frame").named("Hangar Frame").register();
    public static final BlockEntry<Block> ROOF = cube("hangar_roof").named("Hangar Roof").register();
    public static final BlockEntry<TransparentBlock> WINDOW = SpaceContent.CONTENT.block("hangar_window",
                    TransparentBlock::new).named("Hangar Window")
            .properties(properties -> metal(properties).noOcclusion().isValidSpawn((state, level, pos, type) -> false)
                    .isRedstoneConductor((state, level, pos) -> false).isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false).sound(SoundType.GLASS))
            .look(IBlockLook.fixed(new IBlockModel.BottomTop("hangar_window", "block/hangar/hangar_window",
                    "block/hangar/hangar_window", "block/hangar/hangar_window", "minecraft:translucent")))
            .item().drops(Drops.SELF).tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(HANGAR).register();
    public static final BlockEntry<Block> FLOOR = floor("hangar_floor").named("Hangar Floor").register();
    public static final BlockEntry<HangarFittingBlock> LAMP = fitting("hangar_lamp", LAMP_BOX, true)
            .named("Hangar Lamp").properties(properties -> metal(properties).lightLevel(state -> LIT)).register();
    public static final BlockEntry<Block> BAY_DOOR = piece("bay_door", HangarPieceBlock.Kind.DOOR)
            .named("Bay Door").register();
    public static final BlockEntry<Block> CONTROL_FLOOR = floor("control_floor").named("Control Room Floor")
            .register();
    public static final BlockEntry<HangarFittingBlock> STEP = fitting("control_step", STEP_BOX, false)
            .named("Control Room Step").register();
    public static final BlockEntry<HangarFittingBlock> RAILING = fitting("step_railing", RAILING_BOX, true)
            .named("Step Railing").register();
    public static final BlockEntry<HangarConsoleBlock> CONSOLE = SpaceContent.CONTENT.block("hangar_console",
                    HangarConsoleBlock::new).named("Hangar Console")
            .properties(HangarBlocks::metal)
            .look(IBlockLook.facingByState(state -> new IBlockModel.Handmade(HAND + "console_"
                    + state.getValue(HangarConsoleBlock.SCREEN).getSerializedName())))
            .item().itemLook(IItemLook.parent(BLOCK + HAND + "console_rocket")).drops(Drops.SELF)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(HANGAR).register();
    public static final BlockEntry<DoorConsoleBlock> DOOR_CONSOLE = SpaceContent.CONTENT.block("door_console",
                    DoorConsoleBlock::new).named("Bay Door Console")
            .properties(HangarBlocks::metal)
            .look(IBlockLook.facingByState(state -> new IBlockModel.Handmade(HAND + "door_console_"
                    + state.getValue(DoorConsoleBlock.SCREEN).getSerializedName())))
            .item().itemLook(IItemLook.parent(BLOCK + HAND + "door_console_right")).drops(Drops.SELF)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(HANGAR).register();
    public static final BlockEntry<HangarFittingBlock> CHAIR = fitting("console_chair", CHAIR_BOX, true)
            .named("Console Chair").register();
    public static final BlockEntry<RocketDesignTableBlock> DESIGN_TABLE = SpaceContent.CONTENT.block("design_table",
                    RocketDesignTableBlock::new).named("Rocket Design Table")
            .properties(HangarBlocks::metal)
            .look(IBlockLook.fixed(new IBlockModel.Handmade(HAND + "design_table")))
            .item().itemLook(IItemLook.parent(BLOCK + HAND + "design_table")).drops(Drops.SELF)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(HANGAR).register();
    public static final BlockEntry<HangarFittingBlock> STATUS_BOARD = fitting("status_board", BOARD_BOX, true)
            .named("Status Board").register();
    public static final BlockEntry<PersonnelDoorBlock> PERSONNEL_DOOR = SpaceContent.CONTENT.block("personnel_door",
                    PersonnelDoorBlock::new).named("Personnel Door")
            .properties(HangarBlocks::metal)
            .look(IBlockLook.facingByState(state -> new IBlockModel.Handmade(state.getValue(PersonnelDoorBlock.OPEN)
                    ? HAND + "personnel_door_open" : HAND + "personnel_door")))
            .item().itemLook(IItemLook.parent(BLOCK + HAND + "personnel_door")).drops(Drops.SELF)
            .tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(HANGAR).register();
    public static final BlockEntry<Block> CEILING_LIGHT = cube("ceiling_light").named("Ceiling Light")
            .properties(properties -> metal(properties).lightLevel(state -> LIT)).register();
    /** The loading dock in the control room's outer wall, the blocks of the rocket the crane builds come in through. */
    public static final BlockEntry<MultiblockPortBlock> LOADING_DOCK = SpaceContent.CONTENT.block("loading_dock",
                    properties -> new MultiblockPortBlock(properties, () -> HangarBlocks.LOADING_DOCK_ENTITY.get()))
            .named("Loading Dock").properties(HangarBlocks::metal)
            .look(IBlockLook.fixed(new IBlockModel.CubeAll("loading_dock", "block/hangar/loading_dock")))
            .item().drops(Drops.SELF).tag(BlockTags.MINEABLE_WITH_PICKAXE).tag(MultiblockPorts.PORTS).tab(HANGAR)
            .register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LoadingDockBlockEntity>>
            LOADING_DOCK_ENTITY = SpaceContent.CONTENT.blockEntity("loading_dock", LoadingDockBlockEntity::new,
            LOADING_DOCK);
    public static final BlockEntry<Block> CRANE_RAIL = piece("crane_rail", HangarPieceBlock.Kind.RAIL)
            .named("Crane Rail").register();
    public static final BlockEntry<Block> CRANE_BRIDGE = piece("crane_bridge", HangarPieceBlock.Kind.BRIDGE)
            .named("Crane Bridge").register();
    public static final BlockEntry<Block> CRANE_HOIST = piece("crane_hoist", HangarPieceBlock.Kind.HOIST)
            .named("Crane Hoist").register();
    public static final BlockEntry<Block> WORK_PLATFORM = piece("work_platform", HangarPieceBlock.Kind.PLATFORM)
            .named("Work Platform").register();
    /**
     * The port in a wall the blocks a yard builds come in through, the Small Shipyard's: a store of its own, filled by
     * hand or by pipes, that takes only what the ship being built lacks.
     */
    public static final BlockEntry<ItemPortBlock> ITEM_PORT = SpaceContent.CONTENT.block("item_port",
                    ItemPortBlock::new)
            .named("Item Port").properties(HangarBlocks::metal)
            .look(IBlockLook.fixed(new IBlockModel.CubeAll("item_port", "block/hangar/item_port")))
            .item().drops(Drops.SELF).tag(BlockTags.MINEABLE_WITH_PICKAXE).tag(MultiblockPorts.PORTS).tab(HANGAR)
            .register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ItemPortBlockEntity>> ITEM_PORT_ENTITY =
            SpaceContent.CONTENT.blockEntity("item_port", ItemPortBlockEntity::new, ITEM_PORT);
    /** The Item Port's screen: its hold and what the build still lacks. */
    public static final DeferredHolder<MenuType<?>, MenuType<ItemPortMenu>> ITEM_PORT_MENU =
            SpaceContent.CONTENT.menu("item_port", ItemPortMenu::new);

    /* The blocks of the hangar's building, gathered once, after they are registered. */
    private static @Nullable Set<Block> building;

    private HangarBlocks() {
    }

    /**
     * Declares the blocks, once, from the mod's constructor, before its content registers; their ports pass what
     * reaches them on to their hangar's or their yard's terminal.
     */
    public static void declare(final IEventBus modBus) {
        modBus.addListener(RegisterCapabilitiesEvent.class, event -> {
            MultiblockPorts.register(event, LOADING_DOCK_ENTITY.get());
            // The Item Port is a store of its own: pipes fill its hold, which passes what the build lacks on.
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ITEM_PORT_ENTITY.get(),
                    (port, side) -> port.hold());
        });
    }

    /** The blocks of the hangar's building, in the order the tab shows them. */
    public static List<BlockEntry<?>> blocks() {
        return List.of(TERMINAL, WALL, FRAME, ROOF, WINDOW, FLOOR, LAMP, BAY_DOOR, CONTROL_FLOOR, STEP, RAILING,
                CONSOLE, DOOR_CONSOLE, CHAIR, DESIGN_TABLE, STATUS_BOARD, PERSONNEL_DOOR, CEILING_LIGHT, LOADING_DOCK,
                CRANE_RAIL, CRANE_BRIDGE, CRANE_HOIST, WORK_PLATFORM);
    }

    /** Whether {@code state} is of the hangar's building: what a crawler leaves where it stands. */
    public static boolean building(final BlockState state) {
        if (building == null) {
            final Set<Block> made = Collections.newSetFromMap(new IdentityHashMap<>());
            blocks().forEach(entry -> made.add(entry.get()));
            made.add(ITEM_PORT.get());
            building = made;
        }
        return building.contains(state.getBlock());
    }

    /* A whole block of the building: metal, its face {@code block/hangar/<id>} on all six sides. */
    private static BlockBuilder<Block> cube(final String id) {
        return SpaceContent.CONTENT.block(id, Block::new).properties(HangarBlocks::metal)
                .look(IBlockLook.fixed(new IBlockModel.CubeAll(id, "block/hangar/" + id)))
                .item().drops(Drops.SELF).tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(HANGAR);
    }

    /* A floor: concrete, on which nothing spawns, as the bay and its control room are kept lit. */
    private static BlockBuilder<Block> floor(final String id) {
        return SpaceContent.CONTENT.block(id, Block::new)
                .properties(properties -> properties.mapColor(MapColor.STONE).strength(2.5F, 9.0F)
                        .requiresCorrectToolForDrops().sound(SoundType.STONE)
                        .isValidSpawn((state, level, pos, type) -> false))
                .look(IBlockLook.fixed(new IBlockModel.CubeAll(id, "block/hangar/" + id)))
                .item().drops(Drops.SELF).tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(HANGAR);
    }

    /* A fitting of the control room shaped as {@code boxes}, drawn as its model {@code block/hangar/<id>} is. */
    private static BlockBuilder<HangarFittingBlock> fitting(final String id, final int[][] boxes, final boolean turns) {
        final IBlockModel model = new IBlockModel.Handmade(HAND + id);
        return SpaceContent.CONTENT.block(id, properties -> new HangarFittingBlock(properties, boxes))
                .properties(HangarBlocks::metal).look(turns ? IBlockLook.facing(model) : IBlockLook.fixed(model))
                .item().itemLook(IItemLook.parent(BLOCK + HAND + id)).drops(Drops.SELF)
                .tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(HANGAR);
    }

    /*
     * A piece of the kit: the kit's face on all six sides; once its hangar is formed (but a door) nothing of its own,
     * its terminal drawing the crane and the platforms whole.
     */
    private static BlockBuilder<Block> piece(final String id, final HangarPieceBlock.Kind kind) {
        final Function<BlockBehaviour.Properties, Block> made = properties -> new HangarPieceBlock(properties, kind);
        final IBlockModel.CubeAll face = new IBlockModel.CubeAll(id, "block/hangar/" + id);
        final IBlockModel drawn = new IBlockModel.ParticleOnly(id + "_drawn", "block/hangar/" + id);
        // A door open is drawn back out of the way, showing nothing; the other pieces once their hangar is formed.
        final IBlockLook look = kind == HangarPieceBlock.Kind.DOOR
                ? IBlockLook.facing(face).whileOn(HangarPieceBlock.OPEN, drawn)
                : IBlockLook.facing(face).whileOn(HangarPieceBlock.FORMED, drawn);
        return SpaceContent.CONTENT.block(id, made).properties(HangarBlocks::metal).look(look).item()
                .drops(Drops.SELF).tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(HANGAR);
    }

    /* Metal, as hard as iron. */
    private static BlockBehaviour.Properties metal(final BlockBehaviour.Properties properties) {
        return properties.mapColor(MapColor.METAL).strength(3.0F, 6.0F).requiresCorrectToolForDrops()
                .sound(SoundType.METAL);
    }
}
