/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.crew;

import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.multiblock.shape.FormedDoorEvent;
import dev.jstech.core.multiblock.shape.FormedDoors;
import dev.jstech.core.multiblock.shape.FormedPlace;
import dev.jstech.core.multiblock.shape.FormedShape;
import dev.jstech.core.multiblock.shape.FormedShapes;
import dev.jstech.core.multiblock.shape.IFormedPlaced;
import dev.jstech.core.multiblock.shape.MeshCells;
import dev.jstech.core.text.GameText;
import dev.jstech.space.ship.ShipFixtureTexts;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Who may move a starship's doors, and when: its airlock's outer door lets in whoever may come aboard, as its crew
 * says; and an airlock never stands open inside and out at once, so its outer door opens only with the doors at its
 * ends shut, and either of those only with its outer door shut. A player kept from moving a door is told why.
 */
public final class ShipDoorRules {

    /** The piece of an airlock's model that is its outer door. */
    public static final String OUTER_DOOR = "airlock_door";
    /** The piece of a section's model that is the pressure door in its front bulkhead. */
    public static final String PRESSURE_DOOR = "pressure_door";
    /* A section's pressure door works by its block left of the middle line, on the walkway's layer, at its front. */
    private static final int DOOR_X = -1;
    private static final int DOOR_Y = 1;

    private ShipDoorRules() {
    }

    /** Hands the game's bus the rules, once, from the mod's constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(FormedDoorEvent.class, ShipDoorRules::moving);
    }

    /**
     * The doors on the other side of an airlock from {@code door}: the doors at its ends, for its outer door; its
     * outer door, for a door at either end of it; none for a door of no airlock.
     */
    public static List<FormedDoors.Door> otherSide(final ServerLevel level, final FormedDoors.Door door) {
        final List<FormedDoors.Door> out = new ArrayList<>();
        final Optional<FormedPlace> place = placeOf(level, door.uses(), door.model());
        final Optional<FormedShape> shape = FormedShapes.get(level, door.model());
        if (place.isEmpty() || shape.isEmpty()) {
            return out;
        }
        if (OUTER_DOOR.equals(door.piece())) {
            // Its front door is its own model's; its back door the front door of the section behind it.
            FormedDoors.named(level, door.uses(), place.get(), PRESSURE_DOOR).ifPresent(out::add);
            final int length = shape.get().cells().keySet().stream().mapToInt(MeshCells::z).max().orElse(0) + 1;
            final BlockPos behind = FormedDoors.blockOf(door.uses(), place.get(), MeshCells.key(DOOR_X, DOOR_Y,
                    length));
            doorOf(level, behind, PRESSURE_DOOR).ifPresent(out::add);
        } else if (PRESSURE_DOOR.equals(door.piece())) {
            // The front door of an airlock, or of the section behind one.
            if (shape.get().piece(OUTER_DOOR).isPresent()) {
                FormedDoors.named(level, door.uses(), place.get(), OUTER_DOOR).ifPresent(out::add);
            }
            final BlockPos before = FormedDoors.blockOf(door.uses(), place.get(), MeshCells.key(DOOR_X, DOOR_Y, -1));
            doorOf(level, before, OUTER_DOOR).ifPresent(out::add);
        }
        return out;
    }

    /* A door moving: kept as it is where its crew does not let the player, or where it would open an airlock. */
    private static void moving(final FormedDoorEvent event) {
        final ServerLevel level = event.level();
        final FormedDoors.Door door = event.door();
        if (event.player() instanceof ServerPlayer player && YardLayout.isYardBlock(door.uses().getX())
                && MovingStructures.at(level, door.uses()) instanceof MovingStructure ship) {
            final Optional<ShipCrew> crew = ShipCrews.of(level, ship);
            final CrewRole needed = OUTER_DOOR.equals(door.piece()) ? CrewRole.PASSENGER : CrewRole.NONE;
            if (crew.isPresent() && !crew.get().roleOf(player).holds(needed)) {
                event.setCanceled(true);
                player.displayClientMessage(GameText.component(ShipCrews.refusal(crew.get(), needed)), true);
                return;
            }
        }
        if (event.shutting()) {
            return;
        }
        for (final FormedDoors.Door other : otherSide(level, door)) {
            if (!FormedDoors.shut(level, other)) {
                event.setCanceled(true);
                if (event.player() instanceof ServerPlayer player) {
                    player.displayClientMessage(GameText.component(ShipFixtureTexts.AIRLOCK_OTHER_DOOR), true);
                }
                return;
            }
        }
    }

    /* The door {@code piece} of the model the block at {@code pos} stands as, if it has one. */
    private static Optional<FormedDoors.Door> doorOf(final ServerLevel level, final BlockPos pos,
                                                     final String piece) {
        if (!(level.getBlockEntity(pos) instanceof IFormedPlaced placed)) {
            return Optional.empty();
        }
        for (final FormedPlace place : placed.places()) {
            final Optional<FormedDoors.Door> door = FormedDoors.named(level, pos, place, piece);
            if (door.isPresent()) {
                return door;
            }
        }
        return Optional.empty();
    }

    /* The block of the formed model {@code model} that the block at {@code pos} stands as. */
    private static Optional<FormedPlace> placeOf(final ServerLevel level, final BlockPos pos,
                                                 final ResourceLocation model) {
        if (!(level.getBlockEntity(pos) instanceof IFormedPlaced placed)) {
            return Optional.empty();
        }
        return placed.places().stream().filter(place -> place.model().equals(model)).findFirst();
    }
}
