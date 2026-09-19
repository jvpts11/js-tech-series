/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.operation.payload;

import dev.jstech.computers.os.OpenWindow;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * The windows open on a machine's desktop, in both directions. Client to server when a player leaves
 * the monitor (the layout they left behind becomes the machine's); server to client when the desktop
 * opens (so the player gets back the windows the machine has). One shape for both, so the two ends
 * cannot describe a window differently.
 *
 * @param host      the machine
 * @param windows   the open windows, front-most last
 * @param workspace which workspace is up on that desktop, counted from nought
 */
public record DesktopWindowsPayload(BlockPos host, List<WireWindow> windows, int workspace)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<DesktopWindowsPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("jsc", "desktop_windows"));

    private static final int FLAG_MINIMIZED = 1;
    private static final int FLAG_MAXIMIZED = 2;

    /**
     * One window on the wire. The two booleans travel as flags so the codec stays a plain composite,
     * and the program's state is already cut to what the wire carries by the time it is a window.
     */
    public record WireWindow(String key, int x, int y, int w, int h, int flags, String state, int workspace) {

        /*
         * More fields than a composite takes, so the two halves are written out; the key and the state
         * are cut to their caps before writing, since a string over its cap is not a bad packet but a
         * dropped connection. The workspace is written one up, so "every workspace", which is minus
         * one, travels as nought and not as a five-byte negative.
         */
        public static final StreamCodec<RegistryFriendlyByteBuf, WireWindow> STREAM_CODEC = StreamCodec.of(
                (buf, window) -> {
                    buf.writeUtf(clip(window.key(), 64), 64);
                    buf.writeVarInt(window.x());
                    buf.writeVarInt(window.y());
                    buf.writeVarInt(window.w());
                    buf.writeVarInt(window.h());
                    buf.writeVarInt(window.flags());
                    buf.writeUtf(OpenWindow.clipState(window.state()), OpenWindow.STATE_MAX);
                    buf.writeVarInt(window.workspace() + 1);
                },
                buf -> new WireWindow(buf.readUtf(64), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                        buf.readVarInt(), buf.readVarInt(), buf.readUtf(OpenWindow.STATE_MAX),
                        buf.readVarInt() - 1));

        private static String clip(final String text, final int max) {
            return text.length() <= max ? text : text.substring(0, max);
        }

        public static WireWindow of(final OpenWindow window) {
            return new WireWindow(window.key(), window.x(), window.y(), window.w(), window.h(),
                    (window.minimized() ? FLAG_MINIMIZED : 0) | (window.maximized() ? FLAG_MAXIMIZED : 0),
                    OpenWindow.clipState(window.state()), window.workspace());
        }

        public OpenWindow toOpenWindow() {
            return new OpenWindow(key, x, y, w, h,
                    (flags & FLAG_MINIMIZED) != 0, (flags & FLAG_MAXIMIZED) != 0, state, workspace);
        }
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, DesktopWindowsPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, DesktopWindowsPayload::host,
                    WireWindow.STREAM_CODEC.apply(ByteBufCodecs.list(OpenWindow.MAX)),
                    DesktopWindowsPayload::windows,
                    ByteBufCodecs.VAR_INT, DesktopWindowsPayload::workspace,
                    DesktopWindowsPayload::new);

    public static DesktopWindowsPayload of(final BlockPos host, final List<OpenWindow> windows,
                                           final int workspace) {
        final List<WireWindow> wire = new ArrayList<>(windows.size());
        for (final OpenWindow window : windows) {
            wire.add(WireWindow.of(window));
        }
        return new DesktopWindowsPayload(host, wire, workspace);
    }

    public List<OpenWindow> toOpenWindows() {
        final List<OpenWindow> out = new ArrayList<>(windows.size());
        for (final WireWindow window : windows) {
            out.add(window.toOpenWindow());
        }
        return out;
    }

    /* Copied on the way in, so what the desktop is handed cannot change under it after it arrives. */
    public DesktopWindowsPayload {
        windows = List.copyOf(windows);
        workspace = OpenWindow.clampWorkspace(workspace);
    }

    @Override
    public CustomPacketPayload.Type<DesktopWindowsPayload> type() {
        return TYPE;
    }
}
