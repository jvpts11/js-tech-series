/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.function.Function;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The parts of a terminal reply that every payload carrying one writes the same way, kept in one place so the
 * caps and the line codec of the Command Prompt reply and of the desktop shell reply cannot drift apart.
 *
 * <p>Every part is cut to its cap as it is written. A message longer than its cap is not cut on the way but drops the
 * player from the game, and a reply is made of what a program printed, which nothing else bounds: {@code ls} in a
 * folder of three hundred files, a prompt standing in a deep folder.
 */
final class TerminalWire {

    /** The most lines one reply carries. */
    static final int MAX_LINES = 256;

    /** The longest prompt and editor name a reply carries. */
    static final int MAX_PROMPT = 256;
    static final int MAX_EDITOR = 32;

    /** The lines of a reply: the last {@link #MAX_LINES} of them, the ones a terminal keeps in sight. */
    static final StreamCodec<RegistryFriendlyByteBuf, List<WireLine>> LINES_CODEC =
            WireLine.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_LINES)).map(Function.identity(),
                    lines -> lines.size() > MAX_LINES ? lines.subList(lines.size() - MAX_LINES, lines.size()) : lines);
    /** A reply's prompt, cut to {@link #MAX_PROMPT}. */
    static final StreamCodec<ByteBuf, String> PROMPT = clipped(MAX_PROMPT);
    /** The editor a reply opens, cut to {@link #MAX_EDITOR}. */
    static final StreamCodec<ByteBuf, String> EDITOR = clipped(MAX_EDITOR);

    private TerminalWire() {
    }

    private static StreamCodec<ByteBuf, String> clipped(final int most) {
        return ByteBufCodecs.stringUtf8(most).map(Function.identity(),
                text -> text.length() > most ? text.substring(0, most) : text);
    }
}
