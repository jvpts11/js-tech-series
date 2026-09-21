/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.tty.TtyQuestion;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.Nullable;

/**
 * Who has the keyboard at a terminal, the prompt or a tool running in front of it, and what stands on the glass
 * before what is typed.
 *
 * <p>While a tool has it there is no prompt, as there is none at a real terminal while something runs. What is
 * typed goes to the tool, and only when the tool has stopped to ask: its question then stands where the prompt
 * would, and an answer it asked for unseen is typed without anything appearing, not even dots. With nothing in
 * front, what stands there is the shell's own prompt, in the colours that shell gives it.
 *
 * @param busy     whether a tool is in front
 * @param standing what stands before what is typed: a tool's question, or the shell's prompt a run at a time;
 *                 empty while a tool is simply working, and empty from a machine with nothing new to say about
 *                 its prompt, in which case the terminal keeps the one it had
 * @param unseen   whether what is typed in answer is kept off the glass
 */
public record TerminalKeyboard(boolean busy, WireLine standing, boolean unseen) {

    /** Nothing in front: the prompt has the keyboard, and the terminal already knows what it looks like. */
    public static final TerminalKeyboard PROMPT = new TerminalKeyboard(false, new WireLine(List.of()), false);

    public static final StreamCodec<RegistryFriendlyByteBuf, TerminalKeyboard> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, TerminalKeyboard::busy,
                    WireLine.STREAM_CODEC, TerminalKeyboard::standing,
                    ByteBufCodecs.BOOL, TerminalKeyboard::unseen,
                    TerminalKeyboard::new);

    /** A tool in front, asking that or, with nothing asked, simply working. */
    public static TerminalKeyboard heldBy(@Nullable final TtyQuestion asking) {
        return asking == null
                ? new TerminalKeyboard(true, new WireLine(List.of()), false)
                : new TerminalKeyboard(true, WireLine.of(asking.text()), asking.masked());
    }

    /** Nothing in front, and this is the prompt that has the keyboard, in its own colours. */
    public static TerminalKeyboard atPrompt(final CliLine prompt) {
        return new TerminalKeyboard(false, WireLine.of(prompt), false);
    }

    /** Whether the tool in front is waiting to be told something. */
    public boolean asking() {
        return this.busy && !this.standing.spans().isEmpty();
    }

    /** Whether this says what the prompt looks like, which a terminal then shows in place of the one it had. */
    public boolean namesThePrompt() {
        return !this.busy && !this.standing.spans().isEmpty();
    }
}
