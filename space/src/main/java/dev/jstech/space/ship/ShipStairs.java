/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IBlockModel;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.multiblock.FormedPartBlock;
import dev.jstech.core.multiblock.shape.FormedDoorEvent;
import dev.jstech.core.multiblock.shape.FormedDoors;
import dev.jstech.core.multiblock.shape.FormedPlace;
import dev.jstech.core.multiblock.shape.IFormedPlaced;
import dev.jstech.core.multiblock.shape.MeshCells;
import dev.jstech.core.region.Box;
import dev.jstech.space.SpaceContent;
import dev.jstech.space.crew.ShipDoorRules;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * A small ship's ways down to the land: its boarding stair (as the approved airlock has it), folded in a bay under the
 * airlock's floor, coming down to the land as the outer door opens while the ship stands on the land and going up
 * again as the door shuts or the ship lifts off; and its Ramp hold's ramp, its walking face laid on the land as it
 * opens there. In the air and in space a door opens with no stair, and the ramp swings down to its full angle with
 * nothing to climb; lifting off, a ship shuts its holds' doors, its ramp, its side door and its roof, as it folds its
 * gear. Let down, a way is the blocks that hold its treads ({@link ShipStairBlock}), beside the airlock or behind the
 * tail, set down in the ship's yard so they go with it; the door's model draws it.
 */
public final class ShipStairs {

    /** The blocks of a stair let down. */
    public static final BlockEntry<ShipStairBlock> BLOCK = SpaceContent.CONTENT.block("boarding_stair",
            ShipStairBlock::new)
            .properties(properties -> properties.mapColor(MapColor.METAL).strength(-1.0F, 3_600_000.0F)
                    .noOcclusion().dynamicShape().pushReaction(PushReaction.BLOCK)
                    .sound(SoundType.METAL))
            .look(IBlockLook.fixed(new IBlockModel.ParticleOnly("boarding_stair", "jsspace:block/ship/deck_grating")))
            .named("Boarding Stair").register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShipStairEntity>> ENTITY =
            SpaceContent.CONTENT.blockEntity("boarding_stair", ShipStairEntity::new, BLOCK);
    /* The end of the Ramp hold's models' names, in each finish. */
    private static final String RAMP_MODEL = "/small_ramp";
    /* Each ship's doors out to the world, its outer door and its holds' ramp and roof, as last found, by the ship. */
    private static final Map<MovingStructure, List<FormedDoors.Door>> DOORS = new WeakHashMap<>();

    private ShipStairs() {
    }

    /** Declares the stair's block, once, from the mod's constructor. */
    public static void declare() {
        // Loading the class declares it.
    }

    /** Hands the game's bus what lets the stair down as its door moves, once, from the mod's constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, FormedDoorEvent.class, ShipStairs::doorMoving);
    }

    /**
     * Lets the way down of {@code door} down, or takes it up, its airlock's stair or its ramp: its blocks set down
     * beside the airlock or behind the tail where nothing stands, or taken away.
     */
    public static void stand(final ServerLevel level, final FormedDoors.Door door, final boolean down) {
        final Optional<FormedPlace> place = placeOf(level, door);
        if (place.isEmpty()) {
            return;
        }
        BlockPos moved = null;
        // A hold's roof has no way down.
        final List<int[]> cells = ShipDoorRules.RAMP.equals(door.piece()) ? ShipRampGeometry.CELLS
                : ShipDoorRules.OUTER_DOOR.equals(door.piece()) ? ShipStairGeometry.CELLS : List.of();
        for (final int[] cell : cells) {
            final BlockPos at = FormedDoors.blockOf(door.uses(), place.get(), MeshCells.key(cell[0], cell[1],
                    cell[2]));
            if (down && level.getBlockState(at).isAir()) {
                level.setBlockAndUpdate(at, BLOCK.get().defaultBlockState());
                if (level.getBlockEntity(at) instanceof ShipStairEntity stair) {
                    stair.place(new FormedPlace(place.get().model(), place.get().orientation(), cell[0], cell[1],
                            cell[2]));
                }
                moved = moved == null ? at : moved;
            } else if (!down && level.getBlockState(at).is(BLOCK.get())) {
                level.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
                moved = moved == null ? at : moved;
            }
        }
        if (moved != null) {
            ShipSounds.stair(level, moved);
        }
    }

    /** Whether a stair stands let down at {@code at}, one of its blocks' places. */
    public static boolean down(final BlockGetter level, final BlockPos at) {
        return level.getBlockState(at).is(BLOCK.get());
    }

    /**
     * The treads of a way down within its block {@code place}, each its lowest corner then its highest in the block's
     * own space: the ramp's for a block of the Ramp hold's model, the airlock's stair's for a block of the airlock's.
     */
    public static List<double[]> treads(final FormedPlace place) {
        return place.model().getPath().endsWith(RAMP_MODEL) ? ShipRampGeometry.treads(place.x(), place.y(), place.z())
                : ShipStairGeometry.treads(place.x(), place.y(), place.z());
    }

    /**
     * {@code ship} came down onto the land, or lifted off it ({@code grounded}): after its tick, its ways down come
     * down where their doors stand open, or go up; lifting off, it shuts its holds' doors first, as it folds its gear.
     */
    public static void groundChanged(final ServerLevel level, final MovingStructure ship, final boolean grounded) {
        level.getServer().tell(new TickTask(level.getServer().getTickCount(), () -> {
            for (final FormedDoors.Door door : doors(level, ship)) {
                if (!grounded && !ShipDoorRules.OUTER_DOOR.equals(door.piece()) && !FormedDoors.shut(level, door)) {
                    FormedDoors.set(level, door, true);
                }
                stand(level, door, grounded && !FormedDoors.shut(level, door));
            }
        }));
    }

    /** Takes {@code ship}'s ways down up now, if they are down: as a dock takes the ship back, not in its plan. */
    public static void takeUp(final ServerLevel level, final MovingStructure ship) {
        doors(level, ship).forEach(door -> stand(level, door, false));
    }

    /*
     * An airlock's outer door or a ramp moving aboard a ship: its way down comes down as it opens on the land, up as it
     * shuts.
     */
    private static void doorMoving(final FormedDoorEvent event) {
        final FormedDoors.Door door = event.door();
        if (!ShipDoorRules.OUTER_DOOR.equals(door.piece()) && !ShipDoorRules.RAMP.equals(door.piece())
                || !YardLayout.isYardBlock(door.uses().getX())
                || !(MovingStructures.at(event.level(), door.uses()) instanceof MovingStructure ship)
                || !(ship.motion() instanceof ShipFlight flight)) {
            return;
        }
        stand(event.level(), door, !event.shutting() && flight.grounded());
    }

    /*
     * The doors of {@code ship} out to the world, its airlock's outer door and its holds' ramp, side door and roof,
     * found among its blocks once, and again once one of them is not there.
     */
    private static List<FormedDoors.Door> doors(final ServerLevel level, final MovingStructure ship) {
        final List<FormedDoors.Door> known = DOORS.get(ship);
        if (known != null && known.stream().allMatch(door -> placeOf(level, door).isPresent())) {
            return known;
        }
        final Map<BlockPos, FormedDoors.Door> found = new LinkedHashMap<>();
        final Box box = ship.bounds();
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int y = box.minY(); y <= box.maxY(); y++) {
                for (int z = box.minZ(); z <= box.maxZ(); z++) {
                    at.set(x, y, z);
                    final BlockState original = FormedPartBlock.originalAt(level, at, level.getBlockState(at));
                    // A door plate is a ramp's or a side door's, whichever its hold's model has.
                    final List<String> pieces = original.is(ShipKitBlocks.AIRLOCK_DOOR.get())
                            ? List.of(ShipDoorRules.OUTER_DOOR)
                            : original.is(ShipKitBlocks.RAMP_PLATE.get())
                            ? List.of(ShipDoorRules.RAMP, ShipDoorRules.SIDE_DOOR)
                            : original.is(ShipKitBlocks.HULL_PLATING.get()) ? List.of(ShipDoorRules.BAY_DOORS)
                            : List.of();
                    if (pieces.isEmpty() || !(level.getBlockEntity(at) instanceof IFormedPlaced placed)) {
                        continue;
                    }
                    for (final FormedPlace place : placed.places()) {
                        for (final String piece : pieces) {
                            FormedDoors.named(level, at.immutable(), place, piece)
                                    .ifPresent(door -> found.putIfAbsent(door.uses(), door));
                        }
                    }
                }
            }
        }
        final List<FormedDoors.Door> out = List.copyOf(found.values());
        DOORS.put(ship, out);
        return out;
    }

    /* The block of its door's model the block {@code door} works by stands as, if it is there still. */
    private static Optional<FormedPlace> placeOf(final ServerLevel level, final FormedDoors.Door door) {
        if (!(level.getBlockEntity(door.uses()) instanceof IFormedPlaced placed)) {
            return Optional.empty();
        }
        return placed.places().stream().filter(place -> place.model().equals(door.model())).findFirst();
    }
}
