/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.audio;

import dev.jstech.computers.JsComputers;
import dev.jstech.core.audio.AmbientField;
import dev.jstech.core.audio.AmbientFields;
import dev.jstech.core.audio.AudioChannels;
import dev.jstech.core.audio.SoundKey;
import net.minecraft.resources.ResourceLocation;

import static dev.jstech.computers.registry.ComputingContent.CONTENT;

/**
 * The sounds the machines make as machines: a computer's power button, the beep of its self-test, its hard drive
 * spinning up, turning and winding down, the drives taking and giving back their media, a monitor coming on, and the
 * fans of the servers in their racks, which a room full of them turns into one hum.
 *
 * <p>What a system plays through a sound card is not here: those are the system's sounds, not the machine's.
 */
public final class ComputingSounds {

    /** The power button pressed and let go. */
    public static final SoundKey POWER_BUTTON = CONTENT.sound("computer/power_button")
            .channel(AudioChannels.DEVICES).subtitle("Power button clicks").register();

    /** The one short beep of a self-test that passed, from the speaker inside the case. */
    public static final SoundKey POST_BEEP = CONTENT.sound("computer/post_beep")
            .channel(AudioChannels.INTERFACE).subtitle("Computer beeps").register();

    /** An old machine coming on all at once: its fan, its drives and their heads. */
    public static final SoundKey VINTAGE_STARTUP = CONTENT.sound("computer/vintage_startup")
            .subtitle("Old computer starts up").register();

    public static final SoundKey HARD_DRIVE_SPIN_UP = CONTENT.sound("computer/hard_drive_spin_up")
            .range(12).subtitle("Hard drive spins up").register();

    public static final SoundKey HARD_DRIVE_IDLE = CONTENT.sound("computer/hard_drive_idle").loop()
            .range(8).subtitle("Hard drive whirs").register();

    public static final SoundKey HARD_DRIVE_SPIN_DOWN = CONTENT.sound("computer/hard_drive_spin_down")
            .range(12).subtitle("Hard drive winds down").register();

    /** A disc going into or coming out of a CD or DVD drive, and the Pattern Encoder's bay. */
    public static final SoundKey DISC_TRAY = CONTENT.sound("media/disc_tray")
            .channel(AudioChannels.DEVICES).subtitle("Disc tray slides").register();

    public static final SoundKey USB_INSERT = CONTENT.sound("media/usb_insert")
            .channel(AudioChannels.DEVICES).subtitle("USB drive plugged in").register();

    public static final SoundKey USB_REMOVE = CONTENT.sound("media/usb_remove")
            .channel(AudioChannels.DEVICES).subtitle("USB drive pulled out").register();

    public static final SoundKey FLOPPY_INSERT = CONTENT.sound("media/floppy_insert")
            .channel(AudioChannels.DEVICES).subtitle("Floppy disk slides in").register();

    /** The drive's head stepping across a disk while a system installs from it. */
    public static final SoundKey FLOPPY_READ = CONTENT.sound("media/floppy_read").loop()
            .range(8).subtitle("Floppy drive reads").register();

    /** The disk popping out and being drawn from the drive. */
    public static final SoundKey FLOPPY_EJECT = CONTENT.sound("media/floppy_eject")
            .channel(AudioChannels.DEVICES).subtitle("Floppy disk ejected").register();

    public static final SoundKey MONITOR_POWER_ON = CONTENT.sound("monitor/power_on")
            .channel(AudioChannels.DEVICES).subtitle("Monitor switches on").register();

    /** One running server's fans. */
    public static final SoundKey SERVER_FAN = CONTENT.sound("server/fan").loop()
            .subtitle("Server fans whir").register();

    /** Many running servers close together, heard as the room they fill. */
    public static final SoundKey SERVER_ROOM = CONTENT.sound("server/room").loop()
            .channel(AudioChannels.AMBIENCE).range(24).subtitle("Server room hums").register();

    /*
     * Five running servers within sixteen blocks of one another are a room: their fans stop and the room is heard
     * in their middle instead. Counted by distance rather than by chunk, so a room across a chunk border is still
     * one room; below five, each server is heard on its own, so no more than four fans play in one group.
     */
    public static final AmbientField SERVER_ROOM_FIELD = AmbientFields.register(new AmbientField(
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "server_room"), SERVER_ROOM, 5, 16.0));

    private ComputingSounds() {
    }

    /** Loads the declarations, so they reach the registries with the rest of the mod's content. */
    public static void init() {
        // Nothing to do: loading the class is what declares the sounds.
    }
}
