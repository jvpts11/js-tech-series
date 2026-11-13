/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.launch;

import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.space.launcher.LauncherDeckBlockEntity;
import dev.jstech.space.pad.LaunchPads;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A Launch Panel as its block entity: the LCC-1 console by the pad. It finds the launcher standing on the pad and shows
 * and works that launcher's count ({@link LaunchCount}), the one the rocket's flight computer shows and works too; a
 * panel is one way of working a pad's count, not where it is kept, so a pad needs none.
 */
public final class LaunchPanelBlockEntity extends SyncedBlockEntity {

    private final ValueField<BlockPos> launcher;
    private int wait;

    /** How far round it, under it and over it, the launcher on its pad is looked for. */
    public static final int REACH = 24;
    public static final int BELOW = 8;
    public static final int ABOVE = 16;

    public LaunchPanelBlockEntity(final BlockPos pos, final BlockState state) {
        super(LaunchPanel.ENTITY.get(), pos, state);
        // The launcher whose count it works, told to the players who see its face, which they draw from that count.
        this.launcher = fields().nullable("Launcher", BlockPos.CODEC).toClient();
    }

    /** The count of the launcher on its pad, which it shows and works; none where no launcher stands by it. */
    public Optional<LaunchCount> count() {
        final BlockPos at = this.launcher.get();
        return at == null || this.level == null ? Optional.empty() : LauncherDeckBlockEntity.countAt(this.level, at);
    }

    /** All its console shows: its launcher's count, or a safe pad with nothing on it where no launcher stands by it. */
    public LaunchCount.Shown shown() {
        return count().map(LaunchCount::shown).orElse(LaunchCount.Shown.NONE);
    }

    /** Whether it works the count of the launcher whose measuring arm stands at {@code arm}. */
    public boolean works(final BlockPos arm) {
        return arm.equals(this.launcher.get());
    }

    /** A control of its face pressed, by its number ({@link LaunchCount#press}). */
    public void press(final int control) {
        press(control, null);
    }

    /** A control of its face pressed by {@code by}, whose rocket it is once they press LAUNCH. */
    public void press(final int control, @Nullable final UUID by) {
        count().ifPresent(count -> count.press(control, by));
    }

    /** Gives its pad {@code wanted} as its name ({@link LaunchCount#namePad}); false where none can keep it. */
    public boolean namePad(final String wanted) {
        return count().map(count -> count.namePad(wanted)).orElse(false);
    }

    /**
     * Every second, on the server: keeps to the launcher it found while that launcher stands, else looks round its pad
     * for one; and tells that launcher's count it is worked from here.
     */
    public void tick() {
        if (!(this.level instanceof ServerLevel server) || --this.wait > 0) {
            return;
        }
        this.wait = LaunchCount.SECOND;
        if (count().isEmpty()) {
            this.launcher.set(LaunchPads.launcherBy(server, getBlockPos(), REACH, BELOW, ABOVE)
                    .map(LauncherDeckBlockEntity::getBlockPos).orElse(null));
        }
        count().ifPresent(count -> count.workedFrom(getBlockPos()));
    }
}
