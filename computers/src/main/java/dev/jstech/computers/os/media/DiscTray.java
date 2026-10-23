/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.media;

import dev.jstech.computers.audio.ComputingSounds;
import dev.jstech.core.audio.Audio;
import dev.jstech.core.blockentity.BlockEntityFields;
import dev.jstech.core.blockentity.BoolField;
import dev.jstech.core.blockentity.LongField;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * The disc tray of a device, an optical drive or a Pattern Encoder that writes discs: out or in, as its eject button
 * left it. A disc goes on the tray and comes off it only while the tray is out, and the device reads nothing lying on
 * an open tray, as a real drive reads only the disc it has drawn in.
 *
 * <p>Whether it is out is saved and sent to the players who see it, with the moment it last moved, so the client
 * plays the tray riding out or in once and otherwise draws it where it stands: a tray left open is drawn open when the
 * device comes into view, not seen opening again.
 */
@TextHolder
public final class DiscTray {

    private final BoolField open;
    private final LongField movedAt;

    /** The controller the tray's clips play on, beside the bay's own. */
    public static final String CONTROLLER = "tray";
    /** What a player putting a disc on a closed tray, or reaching for the disc in one, is told. */
    public static final TextKey CLOSED = TextKey.of("jsc.media.disc_tray.closed",
            "The tray is closed - press the eject button to open it.");
    /* Longer than either clip of the tray riding out or in, after which the tray is drawn standing where it is. */
    private static final int MOVING_TICKS = 20;
    /* Long before any world began, so a tray that never moved is drawn at rest from the first frame. */
    private static final long NEVER = Long.MIN_VALUE / 2;

    /** The tray of a device whose block entity keeps {@code fields}, closed until its button is pressed. */
    public DiscTray(final BlockEntityFields fields) {
        this(fields, "Tray");
    }

    /**
     * One of the trays of a block entity that keeps several, such as a computer with a drive in each of its bays: its
     * fields named from {@code key}, {@code <key>Open} and {@code <key>MovedAt}.
     */
    public DiscTray(final BlockEntityFields fields, final String key) {
        this.open = fields.flag(key + "Open", false).save().toClient();
        this.movedAt = fields.longInteger(key + "MovedAt", NEVER).toClient();
    }

    /** Whether the tray is out. */
    public boolean isOpen() {
        return open.get();
    }

    /**
     * Whether a hand reaches {@code medium} in the device: a disc only on an open tray, a floppy in its slot or a stick
     * in its port always.
     */
    public boolean reaches(final ItemStack medium) {
        return open.get() || !lies(medium);
    }

    /** Whether the device reads {@code medium}: anything but a disc on an open tray. */
    public boolean reads(final ItemStack medium) {
        return !open.get() || !lies(medium);
    }

    /** Opens a closed tray or closes an open one, with the sound of it riding. */
    public void press(final Level level, final BlockPos pos) {
        open.set(!open.get());
        movedAt.set(level.getGameTime());
        if (level instanceof ServerLevel server) {
            Audio.at(server, pos, ComputingSounds.DISC_TRAY);
        }
    }

    /**
     * The clip of {@code clips} the tray plays at this moment, on the player's game: riding out and held there, then
     * standing out, which is how a device that comes into view with it open is drawn; riding back in. None while it
     * rests inside, and none for no clips, as a bay with no drive.
     */
    public @Nullable String playing(@Nullable final Clips clips, @Nullable final Level level) {
        if (clips == null) {
            return null;
        }
        final boolean moving = moving(level);
        if (open.get()) {
            return moving ? clips.opening() : clips.standingOut();
        }
        return moving ? clips.closing() : null;
    }

    /** Whether the tray is still riding from its last press, which the device draws it doing. */
    public boolean moving(@Nullable final Level level) {
        return level != null && level.getGameTime() - movedAt.get() < MOVING_TICKS;
    }

    /**
     * A tray's three clips, by the names of their animations: riding out and held there, riding back in, and standing
     * out for a device that comes into view with it open. Each file says how its clip ends: the two rides hold on
     * their last frame.
     *
     * @param opening     out, and held
     * @param closing     back in
     * @param standingOut held out
     */
    public record Clips(String opening, String closing, String standingOut) {

        /** The tray's clips in {@code animations}, the file of clips a device's models share. */
        public static Clips of(final String animations) {
            return new Clips("animation." + animations + ".open_tray", "animation." + animations + ".close_tray",
                    "animation." + animations + ".tray_out");
        }
    }

    /* Whether that medium lies on a tray rather than going through a slot or into a port. */
    private static boolean lies(final ItemStack medium) {
        return medium.getItem() instanceof FormattedMediaItem disc && disc.format().onTray();
    }
}
