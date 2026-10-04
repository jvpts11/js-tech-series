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
import dev.jstech.core.audio.SoundContext;
import dev.jstech.core.audio.SoundCue;
import dev.jstech.core.audio.SoundKey;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

import static dev.jstech.computers.registry.ComputingContent.CONTENT;

/**
 * The sounds the machines make as machines: a computer's power button, the beeps of its self-test, its hard drive
 * spinning up, turning, seeking and winding down, the drives taking, reading and giving back their media, a monitor
 * coming on and going off, a server sliding in and out of its rack, the tapes of a Vintage Mainframe, and the fans of
 * the servers in their racks, which a room full of them turns into one hum.
 *
 * <p>A system's own sounds are here too, as cues picked by the system the machine runs and the desktop it booted, and
 * the sounds of the series' own programs: they come out of its monitor, through its sound card or the sound on its
 * board.
 */
public final class ComputingSounds {

    /** The power button pressed and let go. */
    public static final SoundKey POWER_BUTTON = CONTENT.sound("computer/power_button")
            .channel(AudioChannels.DEVICES).subtitle("Power button clicks").register();

    /** The one short beep of a self-test that passed, from the speaker inside the case. */
    public static final SoundKey POST_BEEP = CONTENT.sound("computer/post_beep")
            .channel(AudioChannels.INTERFACE).subtitle("Computer beeps").register();

    /** The beeps of a self-test that found the parts do not make a computer, from the speaker inside the case. */
    public static final SoundKey POST_FAIL = CONTENT.sound("computer/post_fail")
            .channel(AudioChannels.INTERFACE).subtitle("Computer beeps an error").register();

    /** An old machine coming on all at once: its fan, its drives and their heads. */
    public static final SoundKey VINTAGE_STARTUP = CONTENT.sound("computer/vintage_startup")
            .subtitle("Old computer starts up").register();

    public static final SoundKey HARD_DRIVE_SPIN_UP = CONTENT.sound("computer/hard_drive_spin_up")
            .range(12).subtitle("Hard drive spins up").register();

    public static final SoundKey HARD_DRIVE_IDLE = CONTENT.sound("computer/hard_drive_idle").loop()
            .range(8).subtitle("Hard drive whirs").register();

    public static final SoundKey HARD_DRIVE_SPIN_DOWN = CONTENT.sound("computer/hard_drive_spin_down")
            .range(12).subtitle("Hard drive winds down").register();

    /** The heads of a hard drive seeking while the machine reads and writes it, over the platter's turning. */
    public static final SoundKey HARD_DRIVE_SEEK = CONTENT.sound("computer/hard_drive_seek").loop()
            .range(8).subtitle("Hard drive clatters").register();

    /** A disc going into or coming out of a CD or DVD drive, and the Pattern Encoder's bay. */
    public static final SoundKey DISC_TRAY = CONTENT.sound("media/disc_tray")
            .channel(AudioChannels.DEVICES).subtitle("Disc tray slides").register();

    public static final SoundKey USB_INSERT = CONTENT.sound("media/usb_insert")
            .channel(AudioChannels.DEVICES).subtitle("USB drive plugged in").register();

    public static final SoundKey USB_REMOVE = CONTENT.sound("media/usb_remove")
            .channel(AudioChannels.DEVICES).subtitle("USB drive pulled out").register();

    public static final SoundKey FLOPPY_INSERT = CONTENT.sound("media/floppy_insert")
            .channel(AudioChannels.DEVICES).subtitle("Floppy disk slides in").register();

    /** The drive's head stepping across a disk while a system or a program installs from it. */
    public static final SoundKey FLOPPY_READ = CONTENT.sound("media/floppy_read").loop()
            .range(8).subtitle("Floppy drive reads").register();

    /** The disk popping out and being drawn from the drive. */
    public static final SoundKey FLOPPY_EJECT = CONTENT.sound("media/floppy_eject")
            .channel(AudioChannels.DEVICES).subtitle("Floppy disk ejected").register();

    public static final SoundKey MONITOR_POWER_ON = CONTENT.sound("monitor/power_on")
            .channel(AudioChannels.DEVICES).subtitle("Monitor switches on").register();

    /** A picture tube going dark: its buzz collapsing, then the switch. */
    public static final SoundKey MONITOR_POWER_OFF = CONTENT.sound("monitor/power_off")
            .channel(AudioChannels.DEVICES).subtitle("Monitor switches off").register();

    /** A CD or DVD turning in its drive while the machine reads it. */
    public static final SoundKey OPTICAL_READ = CONTENT.sound("media/optical_read").loop()
            .range(8).subtitle("Disc drive spins").register();

    /** One running server's fans. */
    public static final SoundKey SERVER_FAN = CONTENT.sound("server/fan").loop()
            .subtitle("Server fans whir").register();

    /** Many running servers close together, heard as the room they fill. */
    public static final SoundKey SERVER_ROOM = CONTENT.sound("server/room").loop()
            .channel(AudioChannels.AMBIENCE).range(24).subtitle("Server room hums").register();

    /** A server's rails taking its weight as it goes into a rack's bay, and the knock at the end. */
    public static final SoundKey RACK_SLIDE_IN = CONTENT.sound("rack/slide_in")
            .channel(AudioChannels.DEVICES).subtitle("Server slides into a rack").register();

    public static final SoundKey RACK_SLIDE_OUT = CONTENT.sound("rack/slide_out")
            .channel(AudioChannels.DEVICES).subtitle("Server slides out of a rack").register();

    /*
     * The printers, each one page coming out: a printer plays its loop while it prints, once a page, and stops when
     * its tray runs out of paper or its queue empties.
     */

    public static final SoundKey PRINTER_EPSILON_FX_80 = CONTENT.sound("printer/epsilon_fx_80").loop()
            .channel(AudioChannels.DEVICES).range(12).subtitle("Dot matrix printer prints").register();

    public static final SoundKey PRINTER_PAKARD_DESKJOT_940 = CONTENT.sound("printer/pakard_deskjot_940").loop()
            .channel(AudioChannels.DEVICES).range(10).subtitle("Inkjet printer prints").register();

    public static final SoundKey PRINTER_PAKARD_FOTOSMART_C4280 = CONTENT.sound("printer/pakard_fotosmart_c4280")
            .loop().channel(AudioChannels.DEVICES).range(10).subtitle("Inkjet printer prints").register();

    public static final SoundKey PRINTER_PAKARD_LASERJOT_1102 = CONTENT.sound("printer/pakard_laserjot_1102").loop()
            .channel(AudioChannels.DEVICES).range(10).subtitle("Laser printer prints").register();

    public static final SoundKey PRINTER_EPSILON_ECOTONK_ET_2720 = CONTENT.sound("printer/epsilon_ecotonk_et_2720")
            .loop().channel(AudioChannels.DEVICES).range(10).subtitle("Ink tank printer prints").register();

    /** The tape reels of a Vintage Mainframe turning while it runs. */
    public static final SoundKey MAINFRAME_TAPE = CONTENT.sound("mainframe/tape").loop()
            .range(12).subtitle("Tape reels turn").register();

    /**
     * The sound context's name for the system a machine runs, by its id ({@code jsc:frames_xp}): what picks a
     * system's own chime. A resource pack gives another system its chimes by adding a rule for its id.
     */
    public static final String SYSTEM = "system";
    /**
     * The sound context's name for the desktop a machine booted, by its id ({@code jsc:gnome}): what picks the chimes
     * of the Unix systems, which have none of their own and sound like the desktop they run.
     */
    public static final String DESKTOP = "desktop";
    private static final String KDE = "jsc:kde_plasma";
    private static final String GNOME = "jsc:gnome";
    private static final String CINNAMON = "jsc:cinnamon";

    public static final SoundKey FRAMES_95_STARTUP = CONTENT.sound("os/frames_95/startup")
            .stereo().channel(AudioChannels.INTERFACE).subtitle("Frames 95 chimes").register();
    public static final SoundKey FRAMES_95_ERROR = CONTENT.sound("os/frames_95/error")
            .stereo().channel(AudioChannels.INTERFACE).subtitle("Frames 95 error").register();
    public static final SoundKey FRAMES_XP_STARTUP = CONTENT.sound("os/frames_xp/startup")
            .stereo().channel(AudioChannels.INTERFACE).subtitle("Frames XP chimes").register();
    public static final SoundKey FRAMES_XP_ERROR = CONTENT.sound("os/frames_xp/error")
            .stereo().channel(AudioChannels.INTERFACE).subtitle("Frames XP error").register();
    public static final SoundKey FRAMES_11_STARTUP = CONTENT.sound("os/frames_11/startup")
            .stereo().channel(AudioChannels.INTERFACE).subtitle("Frames 11 chimes").register();
    public static final SoundKey FRAMES_11_ERROR = CONTENT.sound("os/frames_11/error")
            .stereo().channel(AudioChannels.INTERFACE).subtitle("Frames 11 error").register();
    /* The three Frames editions share their notice, their bell and their device sounds. */
    public static final SoundKey FRAMES_NOTIFY = CONTENT.sound("os/frames/notify")
            .stereo().channel(AudioChannels.INTERFACE).subtitle("Frames chimes a notice").register();
    public static final SoundKey FRAMES_BEEP = CONTENT.sound("os/frames/beep")
            .channel(AudioChannels.INTERFACE).subtitle("Frames dings").register();
    public static final SoundKey FRAMES_DEVICE_CONNECT = CONTENT.sound("os/frames/device_connect")
            .stereo().channel(AudioChannels.INTERFACE).subtitle("Device connects").register();
    public static final SoundKey FRAMES_DEVICE_DISCONNECT = CONTENT.sound("os/frames/device_disconnect")
            .stereo().channel(AudioChannels.INTERFACE).subtitle("Device disconnects").register();

    /* GNOME as it looked on Legacy machines, with the sounds of its second series. */
    public static final SoundKey GNOME_LEGACY_STARTUP = CONTENT.sound("os/gnome_legacy/startup")
            .stereo().channel(AudioChannels.INTERFACE).subtitle("GNOME chimes").register();
    public static final SoundKey GNOME_LEGACY_SHUTDOWN = CONTENT.sound("os/gnome_legacy/shutdown")
            .stereo().channel(AudioChannels.INTERFACE).subtitle("GNOME says goodbye").register();
    public static final SoundKey GNOME_LEGACY_ERROR = CONTENT.sound("os/gnome_legacy/error")
            .stereo().channel(AudioChannels.INTERFACE).subtitle("GNOME error").register();
    public static final SoundKey GNOME_LEGACY_NOTIFY = CONTENT.sound("os/gnome_legacy/notify")
            .stereo().channel(AudioChannels.INTERFACE).subtitle("GNOME notice").register();
    /* GNOME today, with the freedesktop sounds, which Cinnamon uses too. It has no chime to come up or go down. */
    public static final SoundKey GNOME_ERROR = CONTENT.sound("os/gnome/error")
            .stereo().channel(AudioChannels.INTERFACE).subtitle("GNOME error").register();
    public static final SoundKey GNOME_NOTIFY = CONTENT.sound("os/gnome/notify")
            .stereo().channel(AudioChannels.INTERFACE).subtitle("GNOME notice").register();
    public static final SoundKey GNOME_BELL = CONTENT.sound("os/gnome/bell")
            .stereo().channel(AudioChannels.INTERFACE).subtitle("GNOME bell").register();
    public static final SoundKey GNOME_DEVICE_CONNECT = CONTENT.sound("os/gnome/device_connect")
            .stereo().channel(AudioChannels.INTERFACE).subtitle("Device connects").register();
    public static final SoundKey GNOME_DEVICE_DISCONNECT = CONTENT.sound("os/gnome/device_disconnect")
            .stereo().channel(AudioChannels.INTERFACE).subtitle("Device disconnects").register();

    /* KDE as it looked on Legacy machines, with the Oxygen sounds. */
    public static final SoundKey KDE_LEGACY_STARTUP = CONTENT.sound("os/kde_plasma_legacy/startup")
            .stereo().channel(AudioChannels.INTERFACE).subtitle("KDE chimes").register();
    public static final SoundKey KDE_LEGACY_SHUTDOWN = CONTENT.sound("os/kde_plasma_legacy/shutdown")
            .stereo().channel(AudioChannels.INTERFACE).subtitle("KDE says goodbye").register();
    public static final SoundKey KDE_LEGACY_ERROR = CONTENT.sound("os/kde_plasma_legacy/error")
            .stereo().channel(AudioChannels.INTERFACE).subtitle("KDE error").register();
    public static final SoundKey KDE_LEGACY_NOTIFY = CONTENT.sound("os/kde_plasma_legacy/notify")
            .stereo().channel(AudioChannels.INTERFACE).subtitle("KDE notice").register();
    public static final SoundKey KDE_LEGACY_BELL = CONTENT.sound("os/kde_plasma_legacy/bell")
            .stereo().channel(AudioChannels.INTERFACE).subtitle("KDE bell").register();
    /* KDE Plasma today, with the Ocean sounds. */
    public static final SoundKey KDE_STARTUP = CONTENT.sound("os/kde_plasma/startup")
            .stereo().channel(AudioChannels.INTERFACE).subtitle("KDE chimes").register();
    public static final SoundKey KDE_SHUTDOWN = CONTENT.sound("os/kde_plasma/shutdown")
            .stereo().channel(AudioChannels.INTERFACE).subtitle("KDE says goodbye").register();
    public static final SoundKey KDE_ERROR = CONTENT.sound("os/kde_plasma/error")
            .stereo().channel(AudioChannels.INTERFACE).subtitle("KDE error").register();
    public static final SoundKey KDE_NOTIFY = CONTENT.sound("os/kde_plasma/notify")
            .stereo().channel(AudioChannels.INTERFACE).subtitle("KDE notice").register();
    public static final SoundKey KDE_BELL = CONTENT.sound("os/kde_plasma/bell")
            .stereo().channel(AudioChannels.INTERFACE).subtitle("KDE bell").register();
    public static final SoundKey KDE_DEVICE_CONNECT = CONTENT.sound("os/kde_plasma/device_connect")
            .stereo().channel(AudioChannels.INTERFACE).subtitle("Device connects").register();
    public static final SoundKey KDE_DEVICE_DISCONNECT = CONTENT.sound("os/kde_plasma/device_disconnect")
            .stereo().channel(AudioChannels.INTERFACE).subtitle("Device disconnects").register();

    public static final SoundKey MINESWEEPER_CLICK = CONTENT.sound("program/minesweeper_click")
            .stereo().channel(AudioChannels.INTERFACE).subtitle("Minefield clicks").register();
    public static final SoundKey MINESWEEPER_EXPLODE = CONTENT.sound("program/minesweeper_explode")
            .channel(AudioChannels.INTERFACE).subtitle("Mine explodes").register();
    public static final SoundKey MINESWEEPER_WIN = CONTENT.sound("program/minesweeper_win")
            .stereo().channel(AudioChannels.INTERFACE).subtitle("Minefield cleared").register();

    /**
     * A song a computer plays, out of its monitors and speakers: a recording the server keeps rather than a file of
     * the mod's, handed over as it plays. Each place picks its own side of a stereo song as it plays it.
     */
    public static final SoundKey MUSIC = CONTENT.sound("music/song").made()
            .channel(AudioChannels.MUSIC).range(24).subtitle("Music plays").register();

    /** The notes a program beeps out of the speaker inside the case, square and one at a time. */
    public static final SoundKey PC_SPEAKER = CONTENT.sound("computer/pc_speaker").made()
            .channel(AudioChannels.DEVICES).range(16).subtitle("Computer beeps a tune").register();

    /** The tunes a program plays through the machine's sound card, in its voice, out of its monitors and speakers. */
    public static final SoundKey PROGRAM_TONES = CONTENT.sound("computer/tones").made()
            .channel(AudioChannels.DEVICES).range(24).subtitle("Computer plays a tune").register();

    /*
     * The Unix desktops sound like the desktop, not the system under it, and KDE and GNOME sound like their older
     * selves on a Legacy machine, where they wear their older face too. A rule that names the Legacy era comes
     * before the one for the desktop as a whole; where the older self had no such sound, the newer one's rule names
     * the Standard era, so the older self stays silent instead of borrowing it.
     */

    /** A system reaching its desktop. Systems with no chime of their own stay silent, GNOME today among them. */
    public static final SoundCue SYSTEM_STARTUP = CONTENT.cue("system/startup").world()
            .channel(AudioChannels.INTERFACE)
            .when(SYSTEM, "jsc:frames_95", FRAMES_95_STARTUP)
            .when(SYSTEM, "jsc:frames_xp", FRAMES_XP_STARTUP)
            .when(SYSTEM, "jsc:frames_11", FRAMES_11_STARTUP)
            .when(legacy(KDE), KDE_LEGACY_STARTUP.id())
            .when(DESKTOP, KDE, KDE_STARTUP)
            .when(legacy(GNOME), GNOME_LEGACY_STARTUP.id()).register();

    /** A system shutting down. The Frames editions play their chime again; the Unix desktops say goodbye. */
    public static final SoundCue SYSTEM_SHUTDOWN = CONTENT.cue("system/shutdown").world()
            .channel(AudioChannels.INTERFACE)
            .when(SYSTEM, "jsc:frames_95", FRAMES_95_STARTUP)
            .when(SYSTEM, "jsc:frames_xp", FRAMES_XP_STARTUP)
            .when(SYSTEM, "jsc:frames_11", FRAMES_11_STARTUP)
            .when(legacy(KDE), KDE_LEGACY_SHUTDOWN.id())
            .when(DESKTOP, KDE, KDE_SHUTDOWN)
            .when(legacy(GNOME), GNOME_LEGACY_SHUTDOWN.id()).register();

    /** A system raising an error box. */
    public static final SoundCue SYSTEM_ERROR = CONTENT.cue("system/error").world()
            .channel(AudioChannels.INTERFACE)
            .when(SYSTEM, "jsc:frames_95", FRAMES_95_ERROR)
            .when(SYSTEM, "jsc:frames_xp", FRAMES_XP_ERROR)
            .when(SYSTEM, "jsc:frames_11", FRAMES_11_ERROR)
            .when(legacy(KDE), KDE_LEGACY_ERROR.id())
            .when(DESKTOP, KDE, KDE_ERROR)
            .when(legacy(GNOME), GNOME_LEGACY_ERROR.id())
            .when(DESKTOP, GNOME, GNOME_ERROR)
            .when(DESKTOP, CINNAMON, GNOME_ERROR).register();

    /** A system raising a notice in the corner of its desktop. */
    public static final SoundCue SYSTEM_NOTIFY = CONTENT.cue("system/notify").world()
            .channel(AudioChannels.INTERFACE)
            .when(SYSTEM, "jsc:frames_95", FRAMES_NOTIFY)
            .when(SYSTEM, "jsc:frames_xp", FRAMES_NOTIFY)
            .when(SYSTEM, "jsc:frames_11", FRAMES_NOTIFY)
            .when(legacy(KDE), KDE_LEGACY_NOTIFY.id())
            .when(DESKTOP, KDE, KDE_NOTIFY)
            .when(legacy(GNOME), GNOME_LEGACY_NOTIFY.id())
            .when(DESKTOP, GNOME, GNOME_NOTIFY)
            .when(DESKTOP, CINNAMON, GNOME_NOTIFY).register();

    /**
     * The bell: an action that goes nowhere, or a program ringing it. A system with no bell of its own here rings the
     * speaker in the case instead, which is what the text systems and CDE did.
     */
    public static final SoundCue SYSTEM_BEEP = CONTENT.cue("system/beep").world()
            .channel(AudioChannels.INTERFACE)
            .when(SYSTEM, "jsc:frames_95", FRAMES_BEEP)
            .when(SYSTEM, "jsc:frames_xp", FRAMES_BEEP)
            .when(SYSTEM, "jsc:frames_11", FRAMES_BEEP)
            .when(legacy(KDE), KDE_LEGACY_BELL.id())
            .when(DESKTOP, KDE, KDE_BELL)
            .when(DESKTOP, GNOME, GNOME_BELL)
            .when(DESKTOP, CINNAMON, GNOME_BELL).register();

    /** A device plugged into the machine, or its network coming up. Frames 95 and the older desktops had none. */
    public static final SoundCue SYSTEM_DEVICE_CONNECT = CONTENT.cue("system/device_connect").world()
            .channel(AudioChannels.INTERFACE)
            .when(SYSTEM, "jsc:frames_xp", FRAMES_DEVICE_CONNECT)
            .when(SYSTEM, "jsc:frames_11", FRAMES_DEVICE_CONNECT)
            .when(current(KDE), KDE_DEVICE_CONNECT.id())
            .when(current(GNOME), GNOME_DEVICE_CONNECT.id())
            .when(DESKTOP, CINNAMON, GNOME_DEVICE_CONNECT).register();

    /** A device pulled out of the machine, or its network going down. */
    public static final SoundCue SYSTEM_DEVICE_DISCONNECT = CONTENT.cue("system/device_disconnect").world()
            .channel(AudioChannels.INTERFACE)
            .when(SYSTEM, "jsc:frames_xp", FRAMES_DEVICE_DISCONNECT)
            .when(SYSTEM, "jsc:frames_11", FRAMES_DEVICE_DISCONNECT)
            .when(current(KDE), KDE_DEVICE_DISCONNECT.id())
            .when(current(GNOME), GNOME_DEVICE_DISCONNECT.id())
            .when(DESKTOP, CINNAMON, GNOME_DEVICE_DISCONNECT).register();

    public static final SoundCue PROGRAM_MINESWEEPER_CLICK = CONTENT.cue("program/minesweeper_click").world()
            .channel(AudioChannels.INTERFACE).otherwise(MINESWEEPER_CLICK).register();
    public static final SoundCue PROGRAM_MINESWEEPER_EXPLODE = CONTENT.cue("program/minesweeper_explode").world()
            .channel(AudioChannels.INTERFACE).otherwise(MINESWEEPER_EXPLODE).register();
    public static final SoundCue PROGRAM_MINESWEEPER_WIN = CONTENT.cue("program/minesweeper_win").world()
            .channel(AudioChannels.INTERFACE).otherwise(MINESWEEPER_WIN).register();

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

    /* What the context says for that desktop on a Legacy machine. */
    private static Map<String, String> legacy(final String desktop) {
        return Map.of(DESKTOP, desktop, SoundContext.ERA, HardwareEra.LEGACY.serializedName());
    }

    /* What the context says for that desktop on a Standard machine. */
    private static Map<String, String> current(final String desktop) {
        return Map.of(DESKTOP, desktop, SoundContext.ERA, HardwareEra.STANDARD.serializedName());
    }
}
