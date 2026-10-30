/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.shipyard;

import dev.jstech.core.content.BlockBuilder;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.ContentTab;
import dev.jstech.core.content.DeclaredModel;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.Drops;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IBlockModel;
import dev.jstech.core.machine.MachineMenu;
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
 * The Small Shipyard's kit of blocks, the dry dock a small starship is built in, and its formed model. Before it forms
 * each block shows its own face on every side; formed, they merge into the dock's model, which its terminal draws
 * whole, and take its shapes: the lattice of the towers, the gratings of the walkways, the steps of the stair. Its item
 * port is the Rocket Hangar's, the one port the yards of the mod take their blocks through.
 */
public final class ShipyardBlocks {

    /** Its row of the mod's tab. */
    public static final ContentTab.Section SHIPYARD = SpaceContent.TAB.section();
    /* How bright a floodlight's lamp is, as a block: a work light over the bay. */
    private static final int FLOODLIGHT_LIGHT = 15;
    /** The terminal in the control booth, which the dock is worked from. */
    public static final BlockEntry<ShipyardTerminalBlock> TERMINAL = SpaceContent.CONTENT.block("shipyard_terminal",
                    properties -> new ShipyardTerminalBlock(properties, Device.of(() -> ShipyardBlocks.TERMINAL_ENTITY
                            .get()).ticks(ShipyardTerminalBlockEntity::serverTick).opensMenu(MachineMenu::new)))
            .named("Shipyard Terminal")
            .properties(ShipyardBlocks::metal)
            .look(IBlockLook.facing(new IBlockModel.Orientable("shipyard_terminal", "block/shipyard/terminal_side",
                    "block/shipyard/terminal_front", "block/shipyard/terminal_top")))
            .item().drops(Drops.SELF).tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(SHIPYARD).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShipyardTerminalBlockEntity>>
            TERMINAL_ENTITY = SpaceContent.CONTENT.blockEntity("shipyard_terminal", ShipyardTerminalBlockEntity::new,
            TERMINAL);
    public static final BlockEntry<Block> TOWER = kit("dock_tower").named("Lattice Tower").register();
    public static final BlockEntry<Block> RAIL = kit("dock_rail").named("Gantry Rail").register();
    public static final BlockEntry<Block> GIRDER = kit("dock_girder").named("Dock Girder").register();
    public static final BlockEntry<Block> WALKWAY = kit("dock_walkway").named("Dock Walkway").register();
    public static final BlockEntry<Block> STAIRS = kit("dock_stairs").named("Dock Stairs").register();
    public static final BlockEntry<Block> GANTRY = kit("dock_gantry").named("Gantry Bridge").register();
    public static final BlockEntry<Block> FLOODLIGHT = kit("dock_floodlight").named("Dock Floodlight")
            .properties(properties -> properties.lightLevel(state -> FLOODLIGHT_LIGHT)).register();
    public static final BlockEntry<Block> BOOTH = kit("booth_panel").named("Control Booth Panel").register();
    public static final BlockEntry<Block> CONVEYOR = kit("dock_conveyor").named("Dock Conveyor").register();
    public static final BlockEntry<Block> FEEDER = kit("dock_feeder").named("Block Feeder").register();
    public static final BlockEntry<Block> CRADLE = kit("cradle_clamp").named("Cradle Clamp").register();
    public static final BlockEntry<Block> RIM = SpaceContent.CONTENT.block("pit_rim", Block::new).named("Pit Rim")
            .properties(properties -> properties.mapColor(MapColor.COLOR_YELLOW).strength(2.5F, 9.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.STONE))
            .look(IBlockLook.fixed(new IBlockModel.BottomTop("pit_rim", "block/shipyard/pit_rim_side",
                    "block/shipyard/pit_rim_side", "block/shipyard/pit_rim", null)))
            .item().drops(Drops.SELF).tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(SHIPYARD).register();
    /**
     * The dock formed, drawn by its terminal over the whole structure, whose blocks take their shapes from it; and,
     * drawn with it and shaping nothing, the crane's hook hanging over the bay and the booth's glass.
     */
    public static final DeclaredModel MODEL = SpaceContent.CONTENT.model("shipyard/small_shipyard")
            .file("jsmodels/shipyard/small_shipyard.gltf")
            .texture("textures/models/shipyard/small_shipyard.png")
            .formedFrom(ShipyardStructure.ID)
            .register();
    public static final DeclaredModel HOOK = SpaceContent.CONTENT.model("shipyard/crane_hook")
            .file("jsmodels/shipyard/crane_hook.gltf")
            .texture("textures/models/shipyard/crane_hook.png")
            .register();
    public static final DeclaredModel GLASS = SpaceContent.CONTENT.model("shipyard/booth_glass")
            .file("jsmodels/shipyard/booth_glass.gltf")
            .texture("textures/models/shipyard/booth_glass.png")
            .translucent()
            .register();

    private ShipyardBlocks() {
    }

    /** Declares the blocks, once, from the mod's constructor, before its content registers. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** The blocks of the dock, in the order the tab shows them. */
    public static List<BlockEntry<?>> blocks() {
        return List.of(TERMINAL, TOWER, RAIL, GIRDER, WALKWAY, STAIRS, GANTRY, FLOODLIGHT, BOOTH, CONVEYOR, FEEDER,
                CRADLE, RIM);
    }

    /* A block of the kit: metal, as hard as iron, its face {@code block/shipyard/<id>} on all six sides. */
    private static BlockBuilder<Block> kit(final String id) {
        return SpaceContent.CONTENT.block(id, Block::new).properties(ShipyardBlocks::metal)
                .look(IBlockLook.fixed(new IBlockModel.CubeAll(id, "block/shipyard/" + id)))
                .item().drops(Drops.SELF).tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(SHIPYARD);
    }

    /* Metal, as hard as iron. */
    private static BlockBehaviour.Properties metal(final BlockBehaviour.Properties properties) {
        return properties.mapColor(MapColor.METAL).strength(3.0F, 6.0F).requiresCorrectToolForDrops()
                .sound(SoundType.METAL);
    }
}
