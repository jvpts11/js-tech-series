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
 * system's own settings page, by the names its motion profile gives them, how long the rest take, and whether the
 * machine's graphics are too weak for them at all, in which case its desktop runs its basic look with every effect
 * off whatever the settings say.
 *
 * @param off   the effects switched off, by name
 * @param speed how long the effects take against their own time, in percent: 100 as the system made them, 0 at once
 * @param basic whether the graphics cannot run the effects, which turns every one of them off
 */
public record DesktopEffects(List<String> off, int speed, boolean basic) {

    /** A machine whose player never touched its effects: every one on, at the system's own pace. */
    public static final DesktopEffects ALL_ON = new DesktopEffects(List.of(), 100, false);

    /** The longest name an effect has on the wire. */
    private static final int NAME_MAX = 24;
    /** The most names sent, which is the most a machine keeps. */
    private static final int NAMES_MAX = 32;

    public static final StreamCodec<ByteBuf, DesktopEffects> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(NAME_MAX).apply(ByteBufCodecs.list(NAMES_MAX)), DesktopEffects::off,
            ByteBufCodecs.VAR_INT, DesktopEffects::speed,
            ByteBufCodecs.BOOL, DesktopEffects::basic,
            DesktopEffects::new);

    public DesktopEffects {
        off = List.copyOf(off);
    }

    /** The effects as the settings keep them, on graphics that can run them. */
    public DesktopEffects(final List<String> off, final int speed) {
        this(off, speed, false);
    }

    /** Whether the effect of that name is off: switched off in the settings, or every one in the basic look. */
    public boolean isOff(final String name) {
        return basic || off.contains(name);
    }

    /** Whether the effect of that name is switched off in the settings, whatever the graphics can do. */
    public boolean switchedOff(final String name) {
        return off.contains(name);
    }

    /** The same settings on graphics that can, or cannot, run the effects. */
    public DesktopEffects withBasic(final boolean value) {
        return new DesktopEffects(off, speed, value);
    }
}
