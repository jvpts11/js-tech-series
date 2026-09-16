/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.hardware;

import dev.jstech.computers.JsComputers;

import java.util.Locale;
import java.util.Objects;

/**
 * The socket a processor sits in, and the one a board offers it: a CPU fits a board when the two are the same.
 *
 * <p>An id rather than a closed set, so that a mod bringing processors of its own can bring the socket they sit
 * in as well, instead of having to pick the nearest of ours.
 *
 * <p>The id is text in the {@code namespace:path} shape rather than Minecraft's own ResourceLocation, because
 * what a socket is has nothing to do with the game: whether a CPU fits a board is worked out here, in hardware
 * that is tested without Minecraft loaded at all. The game's own form is made at the edges that need it.
 */
public record CpuSocketId(String id) {

    public static final CpuSocketId SOCKET_3 = own("socket_3");
    public static final CpuSocketId SOCKET_7 = own("socket_7");
    public static final CpuSocketId SOCKET_A = own("socket_a");
    public static final CpuSocketId SOCKET_370 = own("socket_370");
    public static final CpuSocketId LGA_771 = own("lga_771");
    public static final CpuSocketId LGA_775 = own("lga_775");
    public static final CpuSocketId SOCKET_940 = own("socket_940");
    public static final CpuSocketId AM3 = own("am3");
    public static final CpuSocketId AM4 = own("am4");
    public static final CpuSocketId LGA_1150 = own("lga_1150");
    public static final CpuSocketId LGA_1700 = own("lga_1700");
    public static final CpuSocketId LGA_2011 = own("lga_2011");
    public static final CpuSocketId LGA_4189 = own("lga_4189");
    public static final CpuSocketId LGA_4677 = own("lga_4677");
    public static final CpuSocketId SP3 = own("sp3");
    public static final CpuSocketId SP5 = own("sp5");
    public static final CpuSocketId STR5 = own("str5");
    public static final CpuSocketId SOCKET_Q = own("socket_q");
    public static final CpuSocketId SOCKET_EM = own("socket_em");

    public CpuSocketId {
        Objects.requireNonNull(id, "a socket must have an id");
        final int colon = id.indexOf(':');
        if (colon <= 0 || colon == id.length() - 1) {
            throw new IllegalArgumentException("a socket id reads namespace:path; got '" + id + "'");
        }
    }

    /*
     * One of this mod's own sockets. The mod id is a constant the compiler writes in here as the text itself, so
     * naming it costs nothing and does not drag the mod class, and Minecraft with it, into hardware that is
     * tested without the game.
     */
    private static CpuSocketId own(final String path) {
        return new CpuSocketId(JsComputers.MODID + ":" + path);
    }

    /** The socket as a tooltip says it: LGA_1700, the way it is written on a board. */
    public String display() {
        return this.id.substring(this.id.indexOf(':') + 1).toUpperCase(Locale.ROOT);
    }
}
