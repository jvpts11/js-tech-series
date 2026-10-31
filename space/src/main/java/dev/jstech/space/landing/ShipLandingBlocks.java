/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.landing;

import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.ContentTab;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.DeviceBlock;
import dev.jstech.core.content.Drops;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IBlockModel;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.MachineMenu;
import dev.jstech.core.multiblock.BlockMatch;
import dev.jstech.core.multiblock.MultiblockPattern;
import dev.jstech.core.multiblock.MultiblockPatterns;
import dev.jstech.space.JsSpace;
import dev.jstech.space.SpaceContent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Where a starship sets down, as No Man's Sky's ships come down on a base's pads: the ship landing pad, an octagon of
 * dark heat-proof plates twenty-one blocks across with its terminal in the middle, painted once formed with its yellow
 * ring, its touchdown circle and the chevrons of the way in, flush lights round its edge; and the runway beacon, a
 * block at the start of any flat stretch, facing along it, that a wheeled ship's autopilot lines up on to touch down
 * past it and roll on along the stretch.
 */
public final class ShipLandingBlocks {

    /** Its row of the mod's tab. */
    public static final ContentTab.Section LANDING = SpaceContent.TAB.section();
    /** How far from its terminal a pad reaches along either axis, in blocks, and across a corner, both together. */
    public static final int PAD_REACH = 10;
    public static final int PAD_CORNER = 14;
    /* How bright a runway beacon's lamps are, as a block. */
    private static final int BEACON_LIGHT = 8;
    /** A plate of the pad, dark heat-proof steel, the pad laid of them. */
    public static final BlockEntry<Block> PLATE = SpaceContent.CONTENT.block("ship_pad_plate", Block::new)
            .named("Ship Pad Plate")
            .properties(properties -> properties.mapColor(MapColor.COLOR_GRAY).strength(3.0F, 9.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL))
            .look(IBlockLook.fixed(new IBlockModel.BottomTop("ship_pad_plate", "block/ship_pad/plate_side",
                    "block/ship_pad/plate_side", "block/ship_pad/plate", null)))
            .item().drops(Drops.SELF).tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(LANDING).register();
    /** The pad's terminal, in the middle of its plates, under the ship that sets down on it. */
    public static final BlockEntry<DeviceBlock> TERMINAL = SpaceContent.CONTENT.block("ship_pad",
                    Device.of(() -> ShipLandingBlocks.ENTITY.get()).ticks(MachineBlockEntity::serverTick)
                            .opensMenu(MachineMenu::new)::block).named("Ship Landing Pad")
            .properties(properties -> properties.mapColor(MapColor.COLOR_GRAY).strength(3.0F, 9.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL))
            .look(IBlockLook.fixed(new IBlockModel.BottomTop("ship_pad", "block/ship_pad/plate_side",
                    "block/ship_pad/plate_side", "block/ship_pad/terminal", null)))
            .item().drops(Drops.SELF).tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(LANDING).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShipPadBlockEntity>> ENTITY =
            SpaceContent.CONTENT.blockEntity("ship_pad", ShipPadBlockEntity::new, TERMINAL);
    /** The pad's shape: its octagon of plates round its terminal. */
    public static final ResourceLocation PAD_ID = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "ship_pad");
    public static final MultiblockPattern PAD = MultiblockPatterns.declare(PAD_ID, octagon());
    /** The runway beacon, facing the way its runway runs. */
    public static final BlockEntry<RunwayBeaconBlock> BEACON = SpaceContent.CONTENT.block("runway_beacon",
                    RunwayBeaconBlock::new).named("Runway Beacon")
            .properties(properties -> properties.mapColor(MapColor.COLOR_YELLOW).strength(2.0F, 6.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL).lightLevel(state -> BEACON_LIGHT))
            .look(IBlockLook.facing(new IBlockModel.Handmade("runway_beacon")))
            .item().drops(Drops.SELF).tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(LANDING).register();

    private ShipLandingBlocks() {
    }

    /** Declares the pad and the beacon, once, from the mod's constructor, before its content registers. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** Whether the block {@code x}, {@code z} from a pad's terminal is of the pad. */
    public static boolean onPad(final int x, final int z) {
        return Math.abs(x) <= PAD_REACH && Math.abs(z) <= PAD_REACH && Math.abs(x) + Math.abs(z) <= PAD_CORNER;
    }

    /* The octagon of plates, its terminal in the middle; the corners cut off it are anything. */
    private static MultiblockPattern octagon() {
        final String[] rows = new String[2 * PAD_REACH + 1];
        for (int z = -PAD_REACH; z <= PAD_REACH; z++) {
            final StringBuilder row = new StringBuilder();
            for (int x = -PAD_REACH; x <= PAD_REACH; x++) {
                row.append(x == 0 && z == 0 ? '#' : onPad(x, z) ? 'P' : MultiblockPattern.IGNORE_CHAR);
            }
            rows[z + PAD_REACH] = row.toString();
        }
        return MultiblockPattern.builder("ship_pad").layer(rows)
                .where('P', BlockMatch.blocks(PLATE.getId().toString()))
                .terminal(TERMINAL.getId().toString())
                .build();
    }
}
