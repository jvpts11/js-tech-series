/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.monitor;

import dev.jstech.computers.operation.payload.DesktopWindowsPayload;
import dev.jstech.computers.operation.payload.WireLine;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextCodecs;
import dev.jstech.core.tier.HardwareEra;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * What a monitor's face shows in the world, described for the players who can see it: the server says what the
 * machine shows, and each viewer's game draws it.
 *
 * <p>The description is what the machine holds, never a picture: a machine that is off is dark glass; one testing
 * itself, coming up, standing at its boot manager or at its firmware setup, or copying a system, shows lines of
 * text; one at a prompt shows its console; one with a desktop shows that desktop with the windows it has open. Two
 * equal descriptions are the same picture, so the server sends one only when it changes.
 */
public sealed interface IMonitorPicture
        permits IMonitorPicture.Dark, IMonitorPicture.Lines, IMonitorPicture.Console, IMonitorPicture.Desktop {

    /** The glass with nothing on it. */
    Dark DARK = new Dark();

    /** The most lines a text screen or a console carries. */
    int MAX_LINES = 24;

    /** The longest prompt carried. */
    int PROMPT_MAX = 256;

    /** The most windows a desktop picture carries. */
    int WINDOWS_MAX = 32;

    StreamCodec<RegistryFriendlyByteBuf, IMonitorPicture> STREAM_CODEC =
            StreamCodec.of(IMonitorPicture::write, IMonitorPicture::read);

    /** Nothing on the glass: a machine that is off, or one with nothing to show yet. */
    record Dark() implements IMonitorPicture {
    }

    /**
     * A screen of text in the machine's era: a heading and lines, each a label on the left and what came of it on the
     * right: a self-test, a system coming up or going down, a boot manager, a firmware setup, a copy under way.
     */
    record Lines(HardwareEra era, Text title, List<Line> lines) implements IMonitorPicture {

        public Lines {
            lines = List.copyOf(lines.size() > MAX_LINES ? lines.subList(lines.size() - MAX_LINES, lines.size())
                    : lines);
        }
    }

    /**
     * One line of a text screen.
     *
     * @param tone how it is coloured: {@link #PLAIN}, {@link #DIM}, {@link #GOOD}, {@link #BAD}, {@link #PICKED}
     */
    record Line(Text left, Text right, int tone) {

        public static final int PLAIN = 0;
        public static final int DIM = 1;
        public static final int GOOD = 2;
        public static final int BAD = 3;
        /** The entry a boot manager has picked, or a heading. */
        public static final int PICKED = 4;

        public static final StreamCodec<RegistryFriendlyByteBuf, Line> STREAM_CODEC = StreamCodec.composite(
                TextCodecs.STREAM_CODEC, Line::left,
                TextCodecs.STREAM_CODEC, Line::right,
                ByteBufCodecs.VAR_INT, Line::tone,
                Line::new);

        /** A line with nothing on its right. */
        public static Line of(final Text left, final int tone) {
            return new Line(left, Text.EMPTY, tone);
        }
    }

    /** A machine at its prompt: the last lines its console printed, and the prompt waiting under them. */
    record Console(HardwareEra era, List<WireLine> lines, String prompt) implements IMonitorPicture {

        public Console {
            lines = List.copyOf(lines.size() > MAX_LINES ? lines.subList(lines.size() - MAX_LINES, lines.size())
                    : lines);
        }
    }

    /**
     * A machine at its desktop: what the desktop is drawn from (the machine, its system and its desktop, its memory),
     * and the windows it has open, front-most last.
     */
    record Desktop(BlockPos host, ResourceLocation osId, ResourceLocation desktopId, int ramTotalMb,
                   int ramReservedMb, List<DesktopWindowsPayload.WireWindow> windows, int workspace)
            implements IMonitorPicture {

        public Desktop {
            windows = List.copyOf(windows);
        }
    }

    private static void write(final RegistryFriendlyByteBuf buf, final IMonitorPicture picture) {
        switch (picture) {
            case Dark dark -> buf.writeByte(0);
            case Lines lines -> {
                buf.writeByte(1);
                buf.writeEnum(lines.era());
                TextCodecs.STREAM_CODEC.encode(buf, lines.title());
                Line.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_LINES)).encode(buf, lines.lines());
            }
            case Console console -> {
                buf.writeByte(2);
                buf.writeEnum(console.era());
                WireLine.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_LINES)).encode(buf, console.lines());
                buf.writeUtf(clip(console.prompt()), PROMPT_MAX);
            }
            case Desktop desktop -> {
                buf.writeByte(3);
                BlockPos.STREAM_CODEC.encode(buf, desktop.host());
                buf.writeResourceLocation(desktop.osId());
                buf.writeResourceLocation(desktop.desktopId());
                buf.writeVarInt(desktop.ramTotalMb());
                buf.writeVarInt(desktop.ramReservedMb());
                final List<DesktopWindowsPayload.WireWindow> windows = desktop.windows();
                DesktopWindowsPayload.WireWindow.STREAM_CODEC.apply(ByteBufCodecs.list(WINDOWS_MAX))
                        .encode(buf, windows.size() > WINDOWS_MAX ? windows.subList(0, WINDOWS_MAX) : windows);
                buf.writeVarInt(desktop.workspace());
            }
        }
    }

    private static IMonitorPicture read(final RegistryFriendlyByteBuf buf) {
        return switch (buf.readByte()) {
            case 1 -> new Lines(buf.readEnum(HardwareEra.class), TextCodecs.STREAM_CODEC.decode(buf),
                    Line.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_LINES)).decode(buf));
            case 2 -> new Console(buf.readEnum(HardwareEra.class),
                    WireLine.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_LINES)).decode(buf), buf.readUtf(PROMPT_MAX));
            case 3 -> new Desktop(BlockPos.STREAM_CODEC.decode(buf), buf.readResourceLocation(),
                    buf.readResourceLocation(), buf.readVarInt(), buf.readVarInt(),
                    new ArrayList<>(DesktopWindowsPayload.WireWindow.STREAM_CODEC
                            .apply(ByteBufCodecs.list(WINDOWS_MAX)).decode(buf)), buf.readVarInt());
            default -> DARK;
        };
    }

    private static String clip(final String prompt) {
        return prompt.length() <= PROMPT_MAX ? prompt : prompt.substring(0, PROMPT_MAX);
    }
}
