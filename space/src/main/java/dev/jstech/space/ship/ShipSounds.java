/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.ship;

import static dev.jstech.space.SpaceContent.CONTENT;

import dev.jstech.core.audio.Audio;
import dev.jstech.core.audio.AudioChannels;
import dev.jstech.core.audio.SoundKey;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.multiblock.shape.FormedDoorEvent;
import dev.jstech.core.multiblock.shape.FormedDoors;
import dev.jstech.space.crew.ShipDoorRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The sounds of a small starship: its pressure doors and its airlock's outer door sliding, its boarding stair coming
 * down and going up, a switch of its flight console, its engines humming and roaring, heard muffled from inside its
 * hull, its boost, and its gear going up and coming down. A ship that meets the land too fast is heard as the game's
 * own explosion. Its blocks sound where the ship carries them, wherever the ship is; in a vacuum only those aboard,
 * in its air, hear them.
 */
public final class ShipSounds {

    /** A pressure door's leaf sliding into its wall or out of it, and stopping. */
    public static final SoundKey PRESSURE_DOOR = CONTENT.sound("ship/pressure_door")
            .subtitle("Pressure door slides").register();
    /** The airlock's outer door sliding open along the hull. */
    public static final SoundKey OUTER_DOOR_OPEN = CONTENT.sound("ship/outer_door_open")
            .subtitle("Airlock door opens").register();
    /** The airlock's outer door shutting home. */
    public static final SoundKey OUTER_DOOR_SHUT = CONTENT.sound("ship/outer_door_shut")
            .subtitle("Airlock door shuts").register();
    /** The boarding stair unfolding to the land, or folding up into its bay. */
    public static final SoundKey STAIR = CONTENT.sound("ship/stair")
            .subtitle("Boarding stair moves").register();
    /** A guarded switch of the flight console thrown. */
    public static final SoundKey CONSOLE_SWITCH = CONTENT.sound("ship/console_switch")
            .channel(AudioChannels.DEVICES).range(8).subtitle("Console switch clicks").register();
    /** The engines running with no thrust, heard from outside. */
    public static final SoundKey ENGINE_IDLE = CONTENT.sound("ship/engine_idle").loop()
            .range(32).subtitle("Engines hum").register();
    /** The engines running with no thrust, heard from inside through the hull. */
    public static final SoundKey ENGINE_IDLE_INSIDE = CONTENT.sound("ship/engine_idle_inside").loop()
            .range(32).subtitle("Engines hum").register();
    /** The engines pushing the ship on, heard from outside. */
    public static final SoundKey ENGINE_THRUST = CONTENT.sound("ship/engine_thrust").loop()
            .range(64).subtitle("Engines roar").register();
    /** The engines pushing the ship on, heard from inside through the hull. */
    public static final SoundKey ENGINE_THRUST_INSIDE = CONTENT.sound("ship/engine_thrust_inside").loop()
            .range(64).subtitle("Engines roar").register();
    /** The boost lit. */
    public static final SoundKey ENGINE_BOOST = CONTENT.sound("ship/engine_boost")
            .range(64).subtitle("Engines boost").register();
    /** The gear going up into the floor or coming down out of it. */
    public static final SoundKey GEAR = CONTENT.sound("ship/gear")
            .subtitle("Landing gear moves").register();
    private static final double MIDDLE = 0.5;

    private ShipSounds() {
    }

    /** Declares the sounds, once, from the mod's constructor. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** Hands the game's bus what sounds a ship's doors as they move, once, from the mod's constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, FormedDoorEvent.class, ShipSounds::doorMoving);
    }

    /** The boarding stair at {@code at}, one of its blocks' places, moving down or up. */
    public static void stair(final ServerLevel level, final BlockPos at) {
        Audio.at(level, at, STAIR);
    }

    /**
     * A ship meeting the land too fast at {@code at}, the yard's place of the block of it that struck first: the
     * game's own explosion, where the ship is.
     */
    public static void impact(final ServerLevel level, final BlockPos at) {
        final Vec3 heard = MovingStructures.inWorld(level, Vec3.atCenterOf(at));
        level.playSound(null, heard.x, heard.y, heard.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS,
                1.0F, 1.0F);
    }

    /* A door of a ship moving, the move kept: its leaf heard from the middle of its doorway. */
    private static void doorMoving(final FormedDoorEvent event) {
        if (event.isCanceled()) {
            return;
        }
        final FormedDoors.Door door = event.door();
        final SoundKey sound;
        if (ShipDoorRules.PRESSURE_DOOR.equals(door.piece())) {
            sound = PRESSURE_DOOR;
        } else if (ShipDoorRules.OUTER_DOOR.equals(door.piece())) {
            sound = event.shutting() ? OUTER_DOOR_SHUT : OUTER_DOOR_OPEN;
        } else {
            return;
        }
        double x = 0;
        double y = 0;
        double z = 0;
        for (final BlockPos leaf : door.leaf()) {
            x += leaf.getX() + MIDDLE;
            y += leaf.getY() + MIDDLE;
            z += leaf.getZ() + MIDDLE;
        }
        final int count = Math.max(1, door.leaf().size());
        if (door.leaf().isEmpty()) {
            x = door.uses().getX() + MIDDLE;
            y = door.uses().getY() + MIDDLE;
            z = door.uses().getZ() + MIDDLE;
        }
        Audio.at(event.level(), x / count, y / count, z / count, sound, 1.0F, 1.0F);
    }
}
