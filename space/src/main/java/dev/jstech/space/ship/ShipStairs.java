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
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * A small ship's boarding stair (as the approved airlock has it): folded in a bay under the airlock's floor, it comes
 * down to the land as the outer door opens while the ship stands on the land, and goes up again as the door shuts or
 * the ship lifts off. In the air and in space the door opens with no stair. Let down, it is the blocks beside the
 * airlock that hold its treads ({@link ShipStairBlock}), set down in the ship's yard so they go with it; the airlock's
 * model draws it.
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
    /* Each ship's outer door, as last found, by the ship. */
    private static final Map<MovingStructure, FormedDoors.Door> OUTER_DOORS = new WeakHashMap<>();

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
     * Lets the stair of the airlock whose outer door is {@code door} down, or takes it up: its blocks set down beside
     * the airlock where nothing stands, or taken away.
     */
    public static void stand(final ServerLevel level, final FormedDoors.Door door, final boolean down) {
        final Optional<FormedPlace> place = placeOf(level, door);
        if (place.isEmpty()) {
            return;
        }
        BlockPos moved = null;
        for (final int[] cell : ShipStairGeometry.CELLS) {
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
     * {@code ship} came down onto the land, or lifted off it ({@code grounded}): after its tick, its stair comes down
     * where its outer door stands open, or goes up.
     */
    public static void groundChanged(final ServerLevel level, final MovingStructure ship, final boolean grounded) {
        level.getServer().tell(new TickTask(level.getServer().getTickCount(), () -> outerDoor(level, ship)
                .ifPresent(door -> stand(level, door, grounded && !FormedDoors.shut(level, door)))));
    }

    /* An airlock's outer door moving aboard a ship: its stair comes down as it opens on the land, up as it shuts. */
    private static void doorMoving(final FormedDoorEvent event) {
        final FormedDoors.Door door = event.door();
        if (!ShipDoorRules.OUTER_DOOR.equals(door.piece()) || !YardLayout.isYardBlock(door.uses().getX())
                || !(MovingStructures.at(event.level(), door.uses()) instanceof MovingStructure ship)
                || !(ship.motion() instanceof ShipFlight flight)) {
            return;
        }
        stand(event.level(), door, !event.shutting() && flight.grounded());
    }

    /* The outer door of {@code ship}'s airlock, found among its blocks once, and again once it is not there. */
    private static Optional<FormedDoors.Door> outerDoor(final ServerLevel level, final MovingStructure ship) {
        final FormedDoors.Door known = OUTER_DOORS.get(ship);
        if (known != null && placeOf(level, known).isPresent()) {
            return Optional.of(known);
        }
        final Box box = ship.bounds();
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int y = box.minY(); y <= box.maxY(); y++) {
                for (int z = box.minZ(); z <= box.maxZ(); z++) {
                    at.set(x, y, z);
                    if (!FormedPartBlock.originalAt(level, at, level.getBlockState(at))
                            .is(ShipKitBlocks.AIRLOCK_DOOR.get())
                            || !(level.getBlockEntity(at) instanceof IFormedPlaced placed)) {
                        continue;
                    }
                    for (final FormedPlace place : placed.places()) {
                        final Optional<FormedDoors.Door> door = FormedDoors.named(level, at.immutable(), place,
                                ShipDoorRules.OUTER_DOOR);
                        if (door.isPresent()) {
                            OUTER_DOORS.put(ship, door.get());
                            return door;
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    /* The block of its airlock's model the block {@code door} works by stands as, if it is there still. */
    private static Optional<FormedPlace> placeOf(final ServerLevel level, final FormedDoors.Door door) {
        if (!(level.getBlockEntity(door.uses()) instanceof IFormedPlaced placed)) {
            return Optional.empty();
        }
        return placed.places().stream().filter(place -> place.model().equals(door.model())).findFirst();
    }
}
