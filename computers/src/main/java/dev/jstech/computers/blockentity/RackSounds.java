/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.audio.ComputingSounds;
import dev.jstech.computers.client.audio.MachineSoundSources;
import dev.jstech.core.audio.IAudible;
import dev.jstech.core.audio.LoopRequest;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The fans of the servers running in a rack. Each running server is a sound of its own, in the room many servers
 * make together: a few are heard as their own fans, and enough of them close together, in this rack and the racks
 * around it, are heard as the room instead. A server that is off, or not there, makes no sound.
 *
 * <p>The server works out which bays are running and sends that to the client whenever it changes; the client
 * keeps one running sound for each of them.
 */
final class RackSounds {

    private final ServerRackBlockEntity rack;
    private final BayFan[] fans;
    /** One bit per bay whose server is running, on both sides. */
    private int running;

    private static final String NBT_RUNNING = "RunningBays";
    /** How far in from the cabinet's top and bottom the first and last bays sit, in blocks. */
    private static final double BAY_MARGIN = 0.25;

    RackSounds(final ServerRackBlockEntity rack) {
        this.rack = rack;
        this.fans = new BayFan[ServerRackBlockEntity.CAPACITY_U];
        for (int slot = 0; slot < fans.length; slot++) {
            fans[slot] = new BayFan(slot);
        }
    }

    /** Looks at which servers run now, and tells the client when that changed. */
    void tick() {
        int now = 0;
        for (final int slot : rack.computerSlots()) {
            if (rack.unitRunning(slot)) {
                now |= 1 << slot;
            }
        }
        if (now != running) {
            running = now;
            rack.syncVisuals();
        }
    }

    void saveForClient(final CompoundTag tag) {
        tag.putInt(NBT_RUNNING, running);
    }

    void loadFromClient(@Nullable final CompoundTag tag) {
        running = tag == null ? 0 : tag.getInt(NBT_RUNNING);
    }

    /** Hands the bays' fans to the client's sound director; called on the client only. */
    void track() {
        for (final BayFan fan : fans) {
            MachineSoundSources.track(fan);
        }
    }

    /** Takes them back when the rack goes; called on the client only. */
    void untrack() {
        for (final BayFan fan : fans) {
            MachineSoundSources.untrack(fan);
        }
    }

    /** One bay's server, heard from its own height in the cabinet. */
    private final class BayFan implements IAudible {

        private final int slot;

        BayFan(final int slot) {
            this.slot = slot;
        }

        @Override
        public double audioX() {
            return rack.renderBox().getCenter().x;
        }

        @Override
        public double audioY() {
            final AABB box = rack.renderBox();
            final double span = box.getYsize() - 2 * BAY_MARGIN;
            return box.minY + BAY_MARGIN + span * (slot + 0.5) / fans.length;
        }

        @Override
        public double audioZ() {
            return rack.renderBox().getCenter().z;
        }

        @Override
        public List<LoopRequest> loops() {
            if ((running & (1 << slot)) == 0) {
                return List.of();
            }
            return List.of(new LoopRequest(ComputingSounds.SERVER_FAN, 1.0F, 1.0F,
                    ComputingSounds.SERVER_ROOM_FIELD.id()));
        }
    }
}
