/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.landing;

import dev.jstech.core.content.BlockBuilder;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.ContentTab;
import dev.jstech.core.content.DeclaredModel;
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
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.space.JsSpace;
import dev.jstech.space.SpaceContent;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Where a starship sets down, as No Man's Sky's ships come down on a base's pads: the ship landing pad, a round deck of
 * dark heat-proof plates twenty-one blocks across, raised three blocks on a steel truss, a stair up to it from the
 * south and four lattice light masts on its rim, its terminal on the deck by the stair's head; formed, it is drawn as
 * the approved pad, its touchdown ring and hazard band painted, flush lights round its rim and floodlights on its
 * masts lit. And the runway beacon, a block at the start of any flat stretch, facing along it, that a wheeled ship's
 * autopilot lines up on to touch down past it and roll on along the stretch.
 */
public final class ShipLandingBlocks {

    /** Its row of the mod's tab. */
    public static final ContentTab.Section LANDING = SpaceContent.TAB.section();
    /** The deck's layer, over the two of the truss, from the ground. */
    public static final int DECK = 2;
    /** How far from its middle the deck reaches, squared, in blocks: its plates are those within. */
    public static final int DECK_REACH = 108;
    /** How far its plates reach from the middle along either axis, at the most, in blocks. */
    public static final int DECK_RADIUS = 10;
    /** The terminal on the deck by the stair's head, from the deck's middle block: across, up, along. */
    public static final int TERMINAL_X = 2;
    public static final int TERMINAL_Y = DECK + 1;
    public static final int TERMINAL_Z = 9;
    /* The pattern's bounds round the deck's middle: west, east, north, south and its top layer. */
    private static final int WEST = -10;
    private static final int EAST = 10;
    private static final int NORTH = -10;
    private static final int SOUTH = 15;
    private static final int TOP = 8;
    /* The truss's eight posts round the middle, how far out, and how near a chord between two a truss block stands. */
    private static final int POSTS = 8;
    private static final double POST_RING = 8.6;
    private static final double ON_CHORD = 0.5;
    /* The stair down from the deck's south edge: its first block's row, and the layer of each block on from it. */
    private static final int STAIR_FROM = 11;
    private static final int[] STAIR_LAYERS = {2, 1, 1, 0, 0};
    private static final int STAIR_HALF = 1;
    /* The light masts, at the corners this far out, as tall as this over the deck. */
    private static final int MAST = 7;
    private static final int MAST_HEIGHT = 6;
    /* How bright a runway beacon's lamps are, as a block. */
    private static final int BEACON_LIGHT = 8;
    /** A plate of the pad, dark heat-proof steel, the deck laid of them. */
    public static final BlockEntry<Block> PLATE = SpaceContent.CONTENT.block("ship_pad_plate", Block::new)
            .named("Ship Pad Plate")
            .properties(properties -> properties.mapColor(MapColor.COLOR_GRAY).strength(3.0F, 9.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL))
            .look(IBlockLook.fixed(new IBlockModel.BottomTop("ship_pad_plate", "block/ship_pad/plate_side",
                    "block/ship_pad/plate_side", "block/ship_pad/plate", null)))
            .item().drops(Drops.SELF).tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(LANDING).register();
    /** A block of the pad's truss: its posts, its column and the bracing between them, under the deck. */
    public static final BlockEntry<Block> TRUSS = kit("ship_pad_truss", "block/ship_pad/truss")
            .named("Ship Pad Truss").register();
    /** A step of the stair up to the deck. */
    public static final BlockEntry<Block> STEP = kit("ship_pad_step", "block/ship_pad/step")
            .named("Ship Pad Step").register();
    /** A block of one of the pad's lattice light masts. */
    public static final BlockEntry<Block> MAST_BLOCK = kit("ship_pad_mast", "block/ship_pad/mast")
            .named("Ship Pad Light Mast").register();
    /** The pad's terminal, a pedestal on its deck by the stair's head, its screen turned to the deck's middle. */
    public static final BlockEntry<DeviceBlock> TERMINAL = SpaceContent.CONTENT.block("ship_pad",
                    Device.of(() -> ShipLandingBlocks.ENTITY.get()).ticks(MachineBlockEntity::serverTick)
                            .opensMenu(MachineMenu::new)::block).named("Ship Landing Pad")
            .properties(properties -> properties.mapColor(MapColor.COLOR_GRAY).strength(3.0F, 9.0F)
                    .requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion())
            .look(IBlockLook.facing(new IBlockModel.Handmade("ship_pad")))
            .item().drops(Drops.SELF).tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(LANDING).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShipPadBlockEntity>> ENTITY =
            SpaceContent.CONTENT.blockEntity("ship_pad", ShipPadBlockEntity::new, TERMINAL);
    /** The pad's shape: its deck, its truss, its stair and its masts round its terminal. */
    public static final ResourceLocation PAD_ID = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "ship_pad");
    public static final MultiblockPattern PAD = MultiblockPatterns.declare(PAD_ID, raised());
    /** The pad formed, as the approved raised pad draws it; its lamps glow. */
    public static final DeclaredModel MODEL = SpaceContent.CONTENT.model("ship_pad/ship_pad")
            .file("jsmodels/ship_pad/ship_pad.gltf")
            .texture("textures/models/ship_pad/ship_pad.png")
            .formedFrom(PAD_ID).register();
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

    /**
     * What stands at {@code x}, {@code y}, {@code z} from the block under the deck's middle on the ground, as the pad
     * faces north, its stair to the south: {@code '#'} its terminal, {@code 'P'} a plate, {@code 'T'} its truss,
     * {@code 'S'} a step, {@code 'M'} a mast, or {@link MultiblockPattern#IGNORE_CHAR} anything.
     */
    public static char letterAt(final int x, final int y, final int z) {
        if (x == TERMINAL_X && y == TERMINAL_Y && z == TERMINAL_Z) {
            return MultiblockPattern.TERMINAL_CHAR;
        }
        if (y == DECK && x * x + z * z <= DECK_REACH) {
            return 'P';
        }
        if (y >= 0 && y < DECK && (x == 0 && z == 0 || onChord(x, z))) {
            return 'T';
        }
        if (Math.abs(x) <= STAIR_HALF && z >= STAIR_FROM && z < STAIR_FROM + STAIR_LAYERS.length
                && y == STAIR_LAYERS[z - STAIR_FROM]) {
            return 'S';
        }
        if (y > DECK && y <= DECK + MAST_HEIGHT && Math.abs(x) == MAST && Math.abs(z) == MAST) {
            return 'M';
        }
        return MultiblockPattern.IGNORE_CHAR;
    }

    /**
     * The deck's middle block of the pad whose terminal stands at {@code terminal}, the pad formed standing as
     * {@code orientation} says: where its ships set down, on top of it.
     */
    public static BlockPos middle(final BlockPos terminal, final Orientation orientation) {
        final int[] back = orientation.apply(-TERMINAL_X, DECK - TERMINAL_Y, -TERMINAL_Z);
        return terminal.offset(back[0], back[1], back[2]);
    }

    /* A block of its kit, of steel, cut with a pickaxe, painted all round by {@code texture}. */
    private static BlockBuilder<Block> kit(final String id, final String texture) {
        return SpaceContent.CONTENT.block(id, Block::new)
                .properties(properties -> properties.mapColor(MapColor.COLOR_GRAY).strength(3.0F, 9.0F)
                        .requiresCorrectToolForDrops().sound(SoundType.METAL))
                .look(IBlockLook.fixed(new IBlockModel.CubeAll(id, texture)))
                .item().drops(Drops.SELF).tag(BlockTags.MINEABLE_WITH_PICKAXE).tab(LANDING);
    }

    /* Whether a chord between two neighbouring posts of the truss passes within half a block of the block's middle. */
    private static boolean onChord(final int x, final int z) {
        for (int k = 0; k < POSTS; k++) {
            final double a = Math.PI * 2 * (k + 0.5) / POSTS;
            final double b = Math.PI * 2 * (k + 1.5) / POSTS;
            final double ax = POST_RING * Math.cos(a);
            final double az = POST_RING * Math.sin(a);
            final double dx = POST_RING * Math.cos(b) - ax;
            final double dz = POST_RING * Math.sin(b) - az;
            final double t = Math.clamp(((x - ax) * dx + (z - az) * dz) / (dx * dx + dz * dz), 0.0, 1.0);
            if (Math.hypot(x - ax - t * dx, z - az - t * dz) <= ON_CHORD) {
                return true;
            }
        }
        return false;
    }

    /* The deck on its truss, the stair down its south side and the masts at its corners, layer by layer. */
    private static MultiblockPattern raised() {
        final MultiblockPattern.Builder builder = MultiblockPattern.builder("ship_pad");
        for (int y = 0; y <= TOP; y++) {
            final String[] rows = new String[SOUTH - NORTH + 1];
            for (int z = NORTH; z <= SOUTH; z++) {
                final StringBuilder row = new StringBuilder();
                for (int x = WEST; x <= EAST; x++) {
                    row.append(letterAt(x, y, z));
                }
                rows[z - NORTH] = row.toString();
            }
            builder.layer(rows);
        }
        return builder.where('P', BlockMatch.blocks(PLATE.getId().toString()))
                .where('T', BlockMatch.blocks(TRUSS.getId().toString()))
                .where('S', BlockMatch.blocks(STEP.getId().toString()))
                .where('M', BlockMatch.blocks(MAST_BLOCK.getId().toString()))
                .terminal(TERMINAL.getId().toString())
                .build();
    }
}
