/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A machine's visual effects as its settings keep them and its desktop applies them: the ones switched off on the
 * system's own settings page, by the names its motion profile gives them, and how long the rest take.
 *
 * @param off   the effects switched off, by name
 * @param speed how long the effects take against their own time, in percent: 100 as the system made them, 0 at once
 */
public record DesktopEffects(List<String> off, int speed) {

    /** A machine whose player never touched its effects: every one on, at the system's own pace. */
    public static final DesktopEffects ALL_ON = new DesktopEffects(List.of(), 100);

    /** The longest name an effect has on the wire. */
    private static final int NAME_MAX = 24;
    /** The most names sent, which is the most a machine keeps. */
    private static final int NAMES_MAX = 32;

    public static final StreamCodec<ByteBuf, DesktopEffects> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(NAME_MAX).apply(ByteBufCodecs.list(NAMES_MAX)), DesktopEffects::off,
            ByteBufCodecs.VAR_INT, DesktopEffects::speed,
            DesktopEffects::new);

    public DesktopEffects {
        off = List.copyOf(off);
    }

    /** Whether the effect of that name is switched off. */
    public boolean isOff(final String name) {
        return off.contains(name);
    }
}
