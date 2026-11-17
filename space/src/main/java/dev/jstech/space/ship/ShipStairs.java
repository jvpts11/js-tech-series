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
 * trailing edge as the side door opens on the land ({@link ShipWingStairGeometry}), or, away from the wings, the
 * hold's own boarding stair, the airlock's on its right flank. In the air and in space a door opens with no stair, and
 * the ramp swings down to its full angle with nothing to climb; lifting off, a ship shuts its holds' doors, its ramp,
 * its side door and its roof, as it folds its gear. Let down, a way is the blocks that hold its treads
 * ({@link ShipStairBlock}), beside the airlock or the hold, behind the tail or behind the wing, set down in the ship's
 * yard so they go with it; the door's model draws it, or the wing's.
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
    /* The Side Door hold's model away from the wings, the one with its own boarding stair. */
    private static final String SIDE_DOOR_MODEL = "small_side_door";
    /*
     * The blocks of the Side Door hold's model just out of its door along its length, its deck's layer and the one
     * under: a wing that leaves the hull beside the door stands in some of them.
     */
    private static final List<int[]> BESIDE_THE_DOOR = List.of(new int[] {2, 1, 2}, new int[] {2, 0, 2},
            new int[] {2, 1, 1}, new int[] {2, 0, 1}, new int[] {2, 1, 3}, new int[] {2, 0, 3});
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
     * Lets the way down of {@code door} down, or takes it up, its airlock's stair, its ramp, or the stair of the wing
     * beside its side door or its own away from the wings: its blocks set down beside the airlock, behind the tail,
     * behind the wing or beside the hold where nothing stands, or taken away.
     */
    public static void stand(final ServerLevel level, final FormedDoors.Door door, final boolean down) {
        final Optional<FormedPlace> place = placeOf(level, door);
        if (place.isEmpty()) {
            return;
        }
        if (ShipDoorRules.SIDE_DOOR.equals(door.piece())) {
            // The dock builds the hold by the wings hung beside its section (ShipPartModels): away from them, it is
            // the one with its own boarding stair, the airlock's turned onto its right flank.
            if (SIDE_DOOR_MODEL.equals(ShipPartModels.name(place.get().model()))) {
                lay(level, door.uses(), place.get(), place.get().model(), ShipStairGeometry.MIRRORED_CELLS, down);
                return;
            }
            wingBeside(level, door, place.get()).ifPresent(wing -> {
                // Counted from the wing's own block, the stair under the wing on the door's side.
                final boolean left = wing.place().x() < 0;
                final ShipWingStairGeometry stair = geometryOf(wing.place());
                FormedDoors.set(level, wingStair(wing, stair, left), !down);
                lay(level, wing.at(), wing.place(), wing.place().model(), stair.cells(left), down);
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
        final String name = ShipPartModels.name(place.model());
        final Optional<ShipWingStairGeometry> wing = ShipWingStairGeometry.of(name);
        if (wing.isPresent()) {
            return wing.get().treads(place.x(), place.y(), place.z());
        }
        if (SIDE_DOOR_MODEL.equals(name)) {
            return ShipStairGeometry.mirroredTreads(place.x(), place.y(), place.z());
        }
        return place.model().getPath().endsWith(RAMP_MODEL) ? ShipRampGeometry.treads(place.x(), place.y(), place.z())
                : ShipStairGeometry.treads(place.x(), place.y(), place.z());
    }

    /**
     * The stair of the wing beside the side door {@code door}, kept as a door of the wing's: the block keeping whether
     * it is down; none for a door away from the wings, or not a Side Door hold's.
     */
    public static Optional<FormedDoors.Door> wingStairOf(final ServerLevel level, final FormedDoors.Door door) {
        return placeOf(level, door).flatMap(place -> wingBeside(level, door, place)).map(wing ->
                wingStair(wing, geometryOf(wing.place()), wing.place().x() < 0));
    }

    /**
     * A block of the model {@code model} its boarding stair stands in once let down, for its drawing to know it is:
     * the Side Door hold's, on its right flank, or the airlock's.
     */
    public static int[] stairBlock(final ResourceLocation model) {
        return (SIDE_DOOR_MODEL.equals(ShipPartModels.name(model)) ? ShipStairGeometry.MIRRORED_CELLS
                : ShipStairGeometry.CELLS).getFirst().clone();
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
     * A block of a wing with a stair standing just out of the door {@code door} of a Side Door hold built beside the
     * wings, the hold's block {@code hold}, and the block of the wing's model it stands as; none away from the wings.
     */
    private static Optional<WingBlock> wingBeside(final ServerLevel level, final FormedDoors.Door door,
                                                  final FormedPlace hold) {
        if (!ShipDoorRules.SIDE_DOOR.equals(door.piece()) || SIDE_DOOR_MODEL.equals(ShipPartModels.name(
                hold.model()))) {
            return Optional.empty();
        }
        for (final int[] cell : BESIDE_THE_DOOR) {
            final BlockPos at = FormedDoors.blockOf(door.uses(), hold, MeshCells.key(cell[0], cell[1], cell[2]));
            if (level.getBlockEntity(at) instanceof IFormedPlaced placed) {
                for (final FormedPlace place : placed.places()) {
                    if (ShipWingStairGeometry.of(ShipPartModels.name(place.model())).isPresent()) {
                        return Optional.of(new WingBlock(at, place));
                    }
                }
            }
        }
        return Optional.empty();
    }

    /* The stair of the wing {@code wing}, {@code stair}, under its left wing or its right, as a door of the wing's. */
    private static FormedDoors.Door wingStair(final WingBlock wing, final ShipWingStairGeometry stair,
                                              final boolean left) {
        final int[] cell = stair.stateCell(left);
        return new FormedDoors.Door(wing.place().model(), WING_STAIR, FormedDoors.blockOf(wing.at(), wing.place(),
                MeshCells.key(cell[0], cell[1], cell[2])), List.of(), true);
    }

    /* The stair of the wing whose model's block {@code place} is. */
    private static ShipWingStairGeometry geometryOf(final FormedPlace place) {
        return ShipWingStairGeometry.of(ShipPartModels.name(place.model())).orElseThrow();
    }

    /*
     * A block of a wing beside a Side Door hold's door: where it stands, and the block of the wing's model it is.
     *
     * @param at    where it stands
     * @param place the block of the wing's model it is
     */
    private record WingBlock(BlockPos at, FormedPlace place) {
    }

    /* The block of its door's model the block {@code door} works by stands as, if it is there still. */
    private static Optional<FormedPlace> placeOf(final ServerLevel level, final FormedDoors.Door door) {
        if (!(level.getBlockEntity(door.uses()) instanceof IFormedPlaced placed)) {
            return Optional.empty();
        }
        return placed.places().stream().filter(place -> place.model().equals(door.model())).findFirst();
    }
}
