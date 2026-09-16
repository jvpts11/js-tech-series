/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.boot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What an installed system remembers about being greeted: whether it has ever come up in front of somebody, and
 * whether it should say hello again next time.
 *
 * <p>It lives with the system rather than with the machine, which is the whole point: erase the disk and install
 * again and the machine is meeting that system for the first time again, exactly as it would be. A machine with
 * two systems on two disks greets each of them once, because each carries its own mark.
 *
 * @param seen          whether this installed system has already come up in front of somebody
 * @param showAtStartup whether its welcome comes back on later starts, which is the one thing a welcome remembers
 */
public record SystemWelcome(boolean seen, boolean showAtStartup) {

    /** A system nobody has met yet, which is what a disk with no mark on it means. */
    public static final SystemWelcome UNSEEN = new SystemWelcome(false, true);

    public static final Codec<SystemWelcome> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.BOOL.optionalFieldOf("seen", false).forGetter(SystemWelcome::seen),
            Codec.BOOL.optionalFieldOf("show_at_startup", true).forGetter(SystemWelcome::showAtStartup)
    ).apply(inst, SystemWelcome::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SystemWelcome> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SystemWelcome::seen,
            ByteBufCodecs.BOOL, SystemWelcome::showAtStartup,
            SystemWelcome::new);

    /** Whether the welcome goes up this time: always the first time, and after that only while it is wanted. */
    public boolean greets() {
        return !this.seen || this.showAtStartup;
    }

    /** The same system, now met. */
    public SystemWelcome met() {
        return this.seen ? this : new SystemWelcome(true, this.showAtStartup);
    }

    /** The same system, told whether to say hello again. */
    public SystemWelcome showingAtStartup(final boolean show) {
        return new SystemWelcome(this.seen, show);
    }
}
