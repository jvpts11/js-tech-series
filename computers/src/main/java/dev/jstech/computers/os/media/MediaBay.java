/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.media;

import dev.jstech.core.model.network.ModelAnimations;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * The bay of a device drawn as a model in the world, a Pattern Encoder or a drive: the names its model and its
 * animations share, the clip a medium plays going in or coming out, and, on the client, the medium drawn in it.
 *
 * <p>The medium drawn is the one in the bay, or, for a moment after one was taken out, that one. The slot is empty
 * as soon as the medium is taken, while its way out, the floppy popping from its slot, is still playing; the eject clip
 * hides it at the moment it leaves the player's reach. A disc has no way out of its own: it lies on the
 * {@link DiscTray tray} the player opened, and is gone from it the moment it is taken. Nor has a stick, which the hand
 * plugs into its port and pulls out of it, as on every other device with a port.
 */
public final class MediaBay {

    private ItemStack leaving = ItemStack.EMPTY;
    private long leftAt = Long.MIN_VALUE / 2;
    /* How long the medium that left is drawn: as long as its own eject clip, which hides it by its end. */
    private int leavingFor = LEAVING_TICKS;

    /** The controller the bay's clips play on. */
    public static final String CONTROLLER = "bay";
    /** The bone the medium in the bay is drawn at: in the slot, or on the tray, whose child it is. */
    public static final String MEDIUM_BONE = "medium";
    /** The bone a USB stick is drawn at, in its port. */
    public static final String USB_BONE = "usb";
    /** The bone of the power lamp, lit while a computer is at the other end of the cable. */
    public static final String POWER_LAMP = "led_power";
    /** The bone of the activity lamp, which blinks while the device works. */
    public static final String BUSY_LAMP = "led_busy";
    /** How long a medium taken out is still drawn when its format is not known: the longest eject clip, and more. */
    public static final int LEAVING_TICKS = 40;

    /**
     * Plays {@code clip} on the bay of {@code device}, from the server, for every player who sees it: one of the
     * one-shot clips of a medium going in or coming out by itself, which the file of clips the device's models share
     * names after it ({@code animation.<file>.insert_floppy}). A device only ever plays the clips of the media it
     * takes.
     */
    public static void play(final BlockEntity device, final String clip) {
        ModelAnimations.replay(device, CONTROLLER, clip);
    }

    /**
     * The clip a medium of that format plays going in or coming out through the slot. Null for a disc, which the
     * player lays on the open tray and lifts off it, and for a stick, which the hand plugs in and pulls out: neither
     * moves by itself.
     */
    @Nullable
    public static String clip(final MediaFormat format, final boolean in) {
        return format == MediaFormat.FLOPPY ? (in ? "insert_floppy" : "eject_floppy") : null;
    }

    /**
     * The medium to draw: the one in the bay, or, while its eject clip plays, the one taken out. Not a tick longer:
     * once the clip ends the model is back at rest, and a floppy still drawn then stood in its slot again for a moment.
     */
    public ItemStack drawn(final ItemStack held, @Nullable final Level level) {
        if (!held.isEmpty() || level == null || level.getGameTime() - leftAt >= leavingFor) {
            return held;
        }
        return leaving;
    }

    /** The client saw the bay go from {@code before} to {@code now}: a medium that left is drawn a moment longer. */
    public void seen(final ItemStack before, final ItemStack now, @Nullable final Level level) {
        if (!before.isEmpty() && now.isEmpty() && level != null) {
            leaving = before;
            leftAt = level.getGameTime();
            leavingFor = before.getItem() instanceof FormattedMediaItem medium ? ejectTicks(medium.format())
                    : LEAVING_TICKS;
        }
    }

    /**
     * How long a medium of that format takes to come out: its eject clip, in ticks; none for a disc lifted off or a
     * stick pulled out.
     */
    public static int ejectTicks(final MediaFormat format) {
        return switch (format) {
            case FLOPPY -> 20;
            case CD, DVD, BLU_RAY, USB -> 0;
        };
    }
}
