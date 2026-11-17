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
import net.minecraft.resources.ResourceLocation;
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
 * again as the door shuts or the ship lifts off; its Ramp hold's ramp, its walking face laid on the land as it opens
 * there; and the stair of the wing beside its Side Door hold, folded flat under the wing, swinging down from its
 * trailing edge as the side door opens on the land ({@link ShipWingStairGeometry}). In the air and in space a door
 * opens with no stair, and the ramp swings down to its full angle with nothing to climb; lifting off, a ship shuts its
 * holds' doors, its ramp, its side door and its roof, as it folds its gear. Let down, a way is the blocks that hold its
 * treads ({@link ShipStairBlock}), beside the airlock, behind the tail or behind the wing, set down in the ship's yard
 * so they go with it; the door's model draws it, or the wing's.
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
    /**
     * The wing's stair the Side Door hold's door lets down: the wing's model draws it swung down while the block the
     * door works by keeps it so, as it keeps the door's own state.
     */
    public static final String WING_STAIR = "wing_stair";
    /* The end of the Ramp hold's models' names, in each finish. */
    private static final String RAMP_MODEL = "/small_ramp";
    /*
     * The blocks of the Side Door hold's model just out of its door, its deck's layer and the one under: a wing that
     * leaves the hull there stands in one of them.
     */
    private static final List<int[]> BESIDE_THE_DOOR = List.of(new int[] {2, 1, 2}, new int[] {2, 0, 2});
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
     * Lets the way down of {@code door} down, or takes it up, its airlock's stair, its ramp or the stair of the wing
     * beside its side door: its blocks set down beside the airlock, behind the tail or behind the wing where nothing
     * stands, or taken away.
     */
    public static void stand(final ServerLevel level, final FormedDoors.Door door, final boolean down) {
        final Optional<FormedPlace> place = placeOf(level, door);
        if (place.isEmpty()) {
            return;
        }
        if (ShipDoorRules.SIDE_DOOR.equals(door.piece())) {
            wingBeside(level, door, place.get()).ifPresent(wing -> {
                // The wing's model and the hold's stand from the same section's front, so one cell is one block.
                final ShipWingStairGeometry stair = ShipWingStairGeometry.of(ShipPartModels.name(wing.model()))
                        .orElseThrow();
                FormedDoors.set(level, new FormedDoors.Door(wing.model(), WING_STAIR, door.uses(), List.of(), true),
                        !down);
                lay(level, door.uses(), place.get(), wing.model(), stair.cells(), down);
            });
            return;
        }
        // A hold's roof has no way down.
        final List<int[]> cells = ShipDoorRules.RAMP.equals(door.piece()) ? ShipRampGeometry.CELLS
                : ShipDoorRules.OUTER_DOOR.equals(door.piece()) ? ShipStairGeometry.CELLS : List.of();
        lay(level, door.uses(), place.get(), place.get().model(), cells, down);
    }

    /** Whether a stair stands let down at {@code at}, one of its blocks' places. */
    public static boolean down(final BlockGetter level, final BlockPos at) {
        return level.getBlockState(at).is(BLOCK.get());
    }

    /**
     * The treads of a way down within its block {@code place}, each its lowest corner then its highest in the block's
     * own space: a wing's stair's for a block of the wing's model, the ramp's for a block of the Ramp hold's model,
     * the airlock's stair's for a block of the airlock's.
     */
    public static List<double[]> treads(final FormedPlace place) {
        final Optional<ShipWingStairGeometry> wing = ShipWingStairGeometry.of(ShipPartModels.name(place.model()));
        if (wing.isPresent()) {
            return wing.get().treads(place.x(), place.y(), place.z());
        }
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
     * An airlock's outer door, a ramp or a side door moving aboard a ship: its way down comes down as it opens on the
     * land, up as it shuts.
     */
    private static void doorMoving(final FormedDoorEvent event) {
        final FormedDoors.Door door = event.door();
        if (!ShipDoorRules.OUTER_DOOR.equals(door.piece()) && !ShipDoorRules.RAMP.equals(door.piece())
                && !ShipDoorRules.SIDE_DOOR.equals(door.piece())
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

    /*
     * Sets the blocks of a way down at {@code cells} of the model {@code model} down, where nothing stands, or takes
     * them away ({@code down}), each found from the block {@code uses} standing as {@code place}, and sounds it once.
     */
    private static void lay(final ServerLevel level, final BlockPos uses, final FormedPlace place,
                            final ResourceLocation model, final List<int[]> cells, final boolean down) {
        BlockPos moved = null;
        for (final int[] cell : cells) {
            final BlockPos at = FormedDoors.blockOf(uses, place, MeshCells.key(cell[0], cell[1], cell[2]));
            if (down && level.getBlockState(at).isAir()) {
                level.setBlockAndUpdate(at, BLOCK.get().defaultBlockState());
                if (level.getBlockEntity(at) instanceof ShipStairEntity stair) {
                    stair.place(new FormedPlace(model, place.orientation(), cell[0], cell[1], cell[2]));
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

    /*
     * The block of a wing with a stair standing just out of the Side Door hold's door {@code door}, the hold's block
     * {@code hold}: the wing's model's, which then hangs from the hold's section; none away from the wings.
     */
    private static Optional<FormedPlace> wingBeside(final ServerLevel level, final FormedDoors.Door door,
                                                    final FormedPlace hold) {
        for (final int[] cell : BESIDE_THE_DOOR) {
            final BlockPos at = FormedDoors.blockOf(door.uses(), hold, MeshCells.key(cell[0], cell[1], cell[2]));
            if (level.getBlockEntity(at) instanceof IFormedPlaced placed) {
                for (final FormedPlace place : placed.places()) {
                    if (ShipWingStairGeometry.of(ShipPartModels.name(place.model())).isPresent()) {
                        return Optional.of(place);
                    }
                }
            }
        }
        return Optional.empty();
    }

    /* The block of its door's model the block {@code door} works by stands as, if it is there still. */
    private static Optional<FormedPlace> placeOf(final ServerLevel level, final FormedDoors.Door door) {
        if (!(level.getBlockEntity(door.uses()) instanceof IFormedPlaced placed)) {
            return Optional.empty();
        }
        return placed.places().stream().filter(place -> place.model().equals(door.model())).findFirst();
    }
}
