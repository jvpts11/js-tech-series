/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import dev.jstech.core.multiblock.FormedPartBlock;
import dev.jstech.core.multiblock.shape.FormedDoorEvent;
import dev.jstech.core.multiblock.shape.FormedDoors;
import dev.jstech.core.multiblock.shape.FormedPlace;
import dev.jstech.core.multiblock.shape.FormedShape;
import dev.jstech.core.multiblock.shape.FormedShapes;
import dev.jstech.core.multiblock.shape.IFormedPlaced;
import dev.jstech.core.room.Room;
import dev.jstech.core.room.SealedRooms;
import dev.jstech.space.crew.ShipDoorRules;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;

/**
 * A small ship's airlock cycling, as a real one does: its pump, the Airlock Cycler, draws the chamber's air back into
 * the cabin before the outer door opens, all but a little, as the International Space Station's Quest airlock still
 * loses a little; and, the outer door shut again, it fills the chamber from the cabin until the two stand at one
 * pressure. An airlock whose pump is gone loses its chamber's air through the door on every passage, and stays empty
 * until a door to the cabin opens. The cabin is the room beyond the airlock's first door to the rest of the ship.
 */
public final class ShipAirlocks {

    /** The share of the chamber's air its pump leaves in it, to go out of the door. */
    public static final double LEFT_BY_THE_PUMP = 0.02;
    /* No room's number, for the chamber looked for with no room to pass over. */
    private static final long NONE = Long.MIN_VALUE;

    private ShipAirlocks() {
    }

    /** Hands the game's bus the airlocks' pumps, once, from the mod's constructor. */
    public static void listen() {
        // After the airlock's rules have had their say: a door they keep shut moves no air.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, FormedDoorEvent.class, ShipAirlocks::doorMoving);
    }

    /* An airlock's outer door moving: its chamber pumped down before it opens, filled again as it shuts. */
    private static void doorMoving(final FormedDoorEvent event) {
        final FormedDoors.Door door = event.door();
        if (!ShipDoorRules.OUTER_DOOR.equals(door.piece()) || !pumped(event.level(), door)) {
            return;
        }
        final Optional<Room> chamber = beside(event.level(), door, NONE);
        if (chamber.isEmpty()) {
            return;
        }
        final Optional<Room> cabin = ShipDoorRules.otherSide(event.level(), door).stream()
                .map(inner -> beside(event.level(), inner, chamber.get().id())).flatMap(Optional::stream)
                .findFirst();
        if (cabin.isEmpty()) {
            return;
        }
        if (event.shutting()) {
            fill(chamber.get(), cabin.get());
        } else {
            pumpDown(chamber.get(), cabin.get());
        }
    }

    /* The chamber's air drawn back into the cabin, all but the share the pump leaves. */
    private static void pumpDown(final Room chamber, final Room cabin) {
        for (final Map.Entry<ResourceLocation, Double> gas : Map.copyOf(chamber.gases()).entrySet()) {
            cabin.add(gas.getKey(), chamber.take(gas.getKey(), gas.getValue() * (1 - LEFT_BY_THE_PUMP)));
        }
    }

    /* The chamber filled from the cabin until the two stand at one pressure, each gas in its share of their room. */
    private static void fill(final Room chamber, final Room cabin) {
        final double share = chamber.gasVolume() / (chamber.gasVolume() + cabin.gasVolume());
        final Set<ResourceLocation> gases = new HashSet<>(cabin.gases().keySet());
        gases.addAll(chamber.gases().keySet());
        for (final ResourceLocation gas : gases) {
            final double moving = (chamber.moles(gas) + cabin.moles(gas)) * share - chamber.moles(gas);
            if (moving > 0) {
                chamber.add(gas, cabin.take(gas, moving));
            }
        }
    }

    /* Whether the airlock of {@code outer} has its pump: a block of its model that was an Airlock Cycler. */
    private static boolean pumped(final ServerLevel level, final FormedDoors.Door outer) {
        if (!(level.getBlockEntity(outer.uses()) instanceof IFormedPlaced placed)) {
            return false;
        }
        for (final FormedPlace place : placed.places()) {
            final Optional<FormedShape> shape = FormedShapes.get(level, place.model());
            if (!place.model().equals(outer.model()) || shape.isEmpty()) {
                continue;
            }
            for (final long cell : shape.get().cells().keySet()) {
                final BlockPos at = FormedDoors.blockOf(outer.uses(), place, cell);
                if (FormedPartBlock.originalAt(level, at, level.getBlockState(at))
                        .is(ShipKitBlocks.AIRLOCK_CYCLER.get())) {
                    return true;
                }
            }
        }
        return false;
    }

    /* The first room beside {@code door}'s leaf, round it level with it, but the room {@code not}. */
    private static Optional<Room> beside(final ServerLevel level, final FormedDoors.Door door, final long not) {
        for (final BlockPos leaf : door.leaf()) {
            for (final Direction side : Direction.Plane.HORIZONTAL) {
                final Optional<Room> room = SealedRooms.roomAt(level, leaf.relative(side));
                if (room.isPresent() && room.get().id() != not) {
                    return room;
                }
            }
        }
        return Optional.empty();
    }
}
