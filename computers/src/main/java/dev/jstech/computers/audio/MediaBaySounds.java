/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.audio;

import dev.jstech.computers.os.media.FormattedMediaItem;
import dev.jstech.computers.os.media.MediaFormat;
import dev.jstech.core.audio.Audio;
import dev.jstech.core.audio.SoundKey;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Hears a bay take a medium in and give it back: a drive, or the Pattern Encoder. It listens to the bay's slot
 * rather than to the ways a medium gets there, because there are many (a click on the block, the bay's screen, the
 * system ejecting it) and they all end in the slot.
 *
 * <p>Only the slot going from empty to full, or back, makes a sound. A file written onto the medium already in the
 * bay changes the slot as well and is not a medium moving; nor is a bay emptied because its block was broken, which
 * {@link #quietly} covers.
 */
public final class MediaBaySounds {

    private boolean full;
    /** The format of the medium in the bay, remembered so the sound of it leaving is its own. */
    @Nullable
    private MediaFormat format;
    private boolean quiet;

    /** Takes the bay as it now is, with no sound: after its contents were loaded rather than moved. */
    public void settle(final ItemStack held) {
        full = !held.isEmpty();
        format = formatOf(held);
    }

    /** The bay's slot changed: plays the medium going in or coming out, when that is what happened. */
    public void changed(@Nullable final Level level, final BlockPos pos, final ItemStack held) {
        final boolean nowFull = !held.isEmpty();
        if (nowFull == full) {
            if (nowFull) {
                format = formatOf(held);
            }
            return;
        }
        final MediaFormat moved = nowFull ? formatOf(held) : format;
        full = nowFull;
        format = nowFull ? moved : null;
        if (quiet || !(level instanceof ServerLevel server)) {
            return;
        }
        final SoundKey sound = soundOf(moved, nowFull);
        if (sound != null) {
            Audio.at(server, pos, sound);
        }
    }

    /** Runs {@code emptying} without a sound: the bay is being emptied because its block is going. */
    public void quietly(final Runnable emptying) {
        quiet = true;
        try {
            emptying.run();
        } finally {
            quiet = false;
        }
    }

    /**
     * The sound of a medium of that format going in or coming out: a floppy disk slides in and pops out, a disc
     * rides the tray both ways, a USB drive is pushed in and pulled out. A medium with no format of its own is
     * silent.
     */
    @Nullable
    public static SoundKey soundOf(@Nullable final MediaFormat format, final boolean in) {
        if (format == null) {
            return null;
        }
        return switch (format) {
            case FLOPPY -> in ? ComputingSounds.FLOPPY_INSERT : ComputingSounds.FLOPPY_EJECT;
            case CD, DVD -> ComputingSounds.DISC_TRAY;
            case USB -> in ? ComputingSounds.USB_INSERT : ComputingSounds.USB_REMOVE;
        };
    }

    @Nullable
    private static MediaFormat formatOf(final ItemStack stack) {
        return stack.getItem() instanceof FormattedMediaItem media ? media.format() : null;
    }
}
