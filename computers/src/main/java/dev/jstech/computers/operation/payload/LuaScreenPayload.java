/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * The screen of the Lua program in front of a machine's terminal, as it stands: every row's text, the
 * colour of every cell's text and ground (one hexadecimal digit a cell), where the cursor is, and the
 * sixteen colours the digits stand for. It is sent whole whenever it changes, to everyone looking at the
 * machine's console.
 *
 * @param program what the program was started from, which the window's title names
 * @param revision how many changes the screen has had, so a window can tell an old copy from a new one
 */
public record LuaScreenPayload(BlockPos hostPos, String program, long revision, int cursorX, int cursorY,
                               boolean blink, int textColour, List<String> text, List<String> textColours,
                               List<String> groundColours, int[] palette) implements CustomPacketPayload {

    /** The size of the screen, in cells. */
    public static final int WIDTH = 51;
    public static final int HEIGHT = 19;
    public static final int COLOURS = 16;

    public LuaScreenPayload {
        text = List.copyOf(text);
        textColours = List.copyOf(textColours);
        groundColours = List.copyOf(groundColours);
        palette = palette.clone();
    }

    public static final CustomPacketPayload.Type<LuaScreenPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "lua_screen"));

    /* Eleven fields is past what the composite form takes, so the two halves are written by hand. */
    public static final StreamCodec<RegistryFriendlyByteBuf, LuaScreenPayload> STREAM_CODEC =
            StreamCodec.of(LuaScreenPayload::write, LuaScreenPayload::read);

    private static final StreamCodec<io.netty.buffer.ByteBuf, String> ROW = ByteBufCodecs.stringUtf8(WIDTH * 2);

    private static void write(final RegistryFriendlyByteBuf buf, final LuaScreenPayload payload) {
        BlockPos.STREAM_CODEC.encode(buf, payload.hostPos());
        ByteBufCodecs.stringUtf8(64).encode(buf, payload.program());
        ByteBufCodecs.VAR_LONG.encode(buf, payload.revision());
        ByteBufCodecs.VAR_INT.encode(buf, payload.cursorX());
        ByteBufCodecs.VAR_INT.encode(buf, payload.cursorY());
        ByteBufCodecs.BOOL.encode(buf, payload.blink());
        ByteBufCodecs.VAR_INT.encode(buf, payload.textColour());
        for (int y = 0; y < HEIGHT; y++) {
            ROW.encode(buf, row(payload.text(), y));
            ROW.encode(buf, row(payload.textColours(), y));
            ROW.encode(buf, row(payload.groundColours(), y));
        }
        for (int i = 0; i < COLOURS; i++) {
            buf.writeInt(i < payload.palette().length ? payload.palette()[i] : 0);
        }
    }

    private static LuaScreenPayload read(final RegistryFriendlyByteBuf buf) {
        final BlockPos host = BlockPos.STREAM_CODEC.decode(buf);
        final String program = ByteBufCodecs.stringUtf8(64).decode(buf);
        final long revision = ByteBufCodecs.VAR_LONG.decode(buf);
        final int cursorX = ByteBufCodecs.VAR_INT.decode(buf);
        final int cursorY = ByteBufCodecs.VAR_INT.decode(buf);
        final boolean blink = ByteBufCodecs.BOOL.decode(buf);
        final int textColour = ByteBufCodecs.VAR_INT.decode(buf);
        final List<String> text = new ArrayList<>(HEIGHT);
        final List<String> fg = new ArrayList<>(HEIGHT);
        final List<String> bg = new ArrayList<>(HEIGHT);
        for (int y = 0; y < HEIGHT; y++) {
            text.add(ROW.decode(buf));
            fg.add(ROW.decode(buf));
            bg.add(ROW.decode(buf));
        }
        final int[] palette = new int[COLOURS];
        for (int i = 0; i < COLOURS; i++) {
            palette[i] = buf.readInt();
        }
        return new LuaScreenPayload(host, program, revision, cursorX, cursorY, blink, textColour, text, fg, bg,
                palette);
    }

    private static String row(final List<String> rows, final int y) {
        final String row = y < rows.size() ? rows.get(y) : "";
        return row.length() > WIDTH ? row.substring(0, WIDTH) : row;
    }

    @Override
    public CustomPacketPayload.Type<LuaScreenPayload> type() {
        return TYPE;
    }
}
