/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.media;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * The bay of a device drawn as a model in the world, a Pattern Encoder or a drive: the names its model and its
 * animations share, the clip a medium plays going in or coming out, and, on the client, the medium drawn in it.
 *
 * <p>The medium drawn is the one in the bay, or, for a moment after one was taken out, that one. The slot is empty
 * as soon as the medium is taken, while its way out, the tray riding out or the floppy popping from its slot, is
 * still playing; the eject clip hides it at the moment it leaves the player's reach.
 */
public final class MediaBay {

    private ItemStack leaving = ItemStack.EMPTY;
    private long leftAt = Long.MIN_VALUE / 2;

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
    /** How long a medium taken out is still drawn, for its way out to be seen: the longest eject clip, and more. */
    public static final int LEAVING_TICKS = 40;

    private static final String[] CLIPS = {"insert_tray", "eject_tray", "insert_floppy", "eject_floppy",
            "insert_usb", "eject_usb"};

    /**
     * The controller of a device's bay, with a one-shot clip for every way a medium goes in and comes out, taken from
     * {@code animations}, the file of clips the device's models share. A device only ever plays the clips of the media
     * it takes.
     */
    public static <T extends GeoAnimatable> AnimationController<T> controller(final T device, final String animations) {
        final AnimationController<T> bay = new AnimationController<>(device, CONTROLLER, 0, state -> PlayState.STOP);
        for (final String clip : CLIPS) {
            bay.triggerableAnim(clip, RawAnimation.begin().thenPlay("animation." + animations + "." + clip));
        }
        return bay;
    }

    /** The clip a medium of that format plays going in or coming out: on the tray, through the slot, into the port. */
    public static String clip(final MediaFormat format, final boolean in) {
        final String way = switch (format) {
            case FLOPPY -> "floppy";
            case CD, DVD -> "tray";
            case USB -> "usb";
        };
        return (in ? "insert_" : "eject_") + way;
    }

    /** The medium to draw: the one in the bay, or for a moment after one was taken out, that one. */
    public ItemStack drawn(final ItemStack held, @Nullable final Level level) {
        if (!held.isEmpty() || level == null || level.getGameTime() - leftAt > LEAVING_TICKS) {
            return held;
        }
        return leaving;
    }

    /** The client saw the bay go from {@code before} to {@code now}: a medium that left is drawn a moment longer. */
    public void seen(final ItemStack before, final ItemStack now, @Nullable final Level level) {
        if (!before.isEmpty() && now.isEmpty() && level != null) {
            leaving = before;
            leftAt = level.getGameTime();
        }
    }
}
