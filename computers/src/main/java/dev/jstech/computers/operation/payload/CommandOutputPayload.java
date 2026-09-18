/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Server to client: the styled output of one Command Prompt line, plus whether the console should be
 * cleared before printing it (the {@code clear} command). Each line carries its text and the id of a
 * style the client maps to a colour.
 *
 * <p>{@code replaceLast} says the lines redraw over the last one printed rather than follow it, the way
 * a progress bar at a real terminal grows on the same line instead of filling the screen with copies.
 * {@code keyboard} says who has the keyboard once these lines are on the glass: the prompt, or a tool that
 * the command left running in front of it.
 */
public record CommandOutputPayload(boolean clear, String prompt, List<WireLine> lines,
                                   String editor, String editorPath, boolean replaceLast,
                                   TerminalKeyboard keyboard)
        implements CustomPacketPayload {

    public static final int MAX_LINES = 256;

    /** A reply that only printed, which is what nearly every command does. */
    public CommandOutputPayload(final boolean clear, final String prompt, final List<WireLine> lines) {
        this(clear, prompt, lines, "", "", false, TerminalKeyboard.PROMPT);
    }

    /** A reply that hands the terminal to an editor. */
    public CommandOutputPayload(final boolean clear, final String prompt, final List<WireLine> lines,
                                final String editor, final String editorPath) {
        this(clear, prompt, lines, editor, editorPath, false, TerminalKeyboard.PROMPT);
    }

    /** Lines the machine prints on its own, over the last line printed or after it. */
    public CommandOutputPayload(final boolean clear, final String prompt, final List<WireLine> lines,
                                final String editor, final String editorPath, final boolean replaceLast) {
        this(clear, prompt, lines, editor, editorPath, replaceLast, TerminalKeyboard.PROMPT);
    }

    /** Lines printed while a tool is in front, or as it gives the prompt back. */
    public CommandOutputPayload(final String prompt, final List<WireLine> lines, final TerminalKeyboard keyboard) {
        this(false, prompt, lines, "", "", false, keyboard);
    }

    /** Whether the machine gave the terminal to an editor. */
    public boolean handsOver() {
        return !this.editor.isEmpty() && !this.editorPath.isEmpty();
    }

    public static final CustomPacketPayload.Type<CommandOutputPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "command_output"));

    private static final StreamCodec<RegistryFriendlyByteBuf, List<WireLine>> LINES_CODEC =
            WireLine.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_LINES));

    /* Seven fields is past what the composite form takes, so the two halves are written by hand. */
    public static final StreamCodec<RegistryFriendlyByteBuf, CommandOutputPayload> STREAM_CODEC =
            StreamCodec.of(CommandOutputPayload::write, CommandOutputPayload::read);

    private static void write(final RegistryFriendlyByteBuf buf, final CommandOutputPayload payload) {
        ByteBufCodecs.BOOL.encode(buf, payload.clear());
        ByteBufCodecs.stringUtf8(256).encode(buf, payload.prompt());
        LINES_CODEC.encode(buf, payload.lines());
        ByteBufCodecs.stringUtf8(32).encode(buf, payload.editor());
        ByteBufCodecs.stringUtf8(160).encode(buf, payload.editorPath());
        ByteBufCodecs.BOOL.encode(buf, payload.replaceLast());
        TerminalKeyboard.STREAM_CODEC.encode(buf, payload.keyboard());
    }

    private static CommandOutputPayload read(final RegistryFriendlyByteBuf buf) {
        final boolean clear = ByteBufCodecs.BOOL.decode(buf);
        final String prompt = ByteBufCodecs.stringUtf8(256).decode(buf);
        final List<WireLine> lines = LINES_CODEC.decode(buf);
        final String editor = ByteBufCodecs.stringUtf8(32).decode(buf);
        final String editorPath = ByteBufCodecs.stringUtf8(160).decode(buf);
        final boolean replaceLast = ByteBufCodecs.BOOL.decode(buf);
        return new CommandOutputPayload(clear, prompt, lines, editor, editorPath, replaceLast,
                TerminalKeyboard.STREAM_CODEC.decode(buf));
    }

    /* Copied on the way in, so what the terminal is handed cannot change under it after it arrives. */
    public CommandOutputPayload {
        lines = List.copyOf(lines);
    }

    @Override
    public CustomPacketPayload.Type<CommandOutputPayload> type() {
        return TYPE;
    }
}
