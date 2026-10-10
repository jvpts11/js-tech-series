/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The parts of a terminal reply that every payload carrying one writes the same way, kept in one place so the
 * caps and the line codec of the Command Prompt reply and of the desktop shell reply cannot drift apart.
 */
final class TerminalWire {

    /** The most lines one reply carries. */
    static final int MAX_LINES = 256;

    /** The longest prompt, editor name and editor path a reply carries. */
    static final int MAX_PROMPT = 256;
    static final int MAX_EDITOR = 32;
    static final int MAX_EDITOR_PATH = 160;

    /** The lines of a reply, cut to {@link #MAX_LINES} by the list codec. */
    static final StreamCodec<RegistryFriendlyByteBuf, List<WireLine>> LINES_CODEC =
            WireLine.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_LINES));

    private TerminalWire() {
    }
}
