/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import java.util.Locale;

/**
 * A path on another machine of the network: {@code \\host\share\rest}.
 *
 * <p>The host is the machine's host name, the share the name its owner gave the folder, and the rest
 * the way down from there. {@code \\} alone names the network (every host with something shared),
 * {@code \\host} a host (its shares). The same path is written {@code //host/share/rest} where
 * backslashes are awkward, and {@code /net/host/share/rest} on the Linux systems; all three parse to
 * the same thing. Pure: no Minecraft in it, so the whole shape is unit-tested.
 *
 * @param host  the host name, or {@code ""} for the network itself
 * @param share the share name, or {@code ""} for the host itself
 * @param rest  the path below the share with {@code /} separators, or {@code ""} for the share's root
 */
public record NetPath(String host, String share, String rest) {

    private static final String LINUX_ROOT = "/net";

    public NetPath {
        host = host == null ? "" : host.trim();
        share = share == null ? "" : share.trim();
        rest = rest == null ? "" : rest.trim();
    }

    /** Whether this looks like a network path at all, in any of its three spellings. */
    public static boolean looksLike(final String input) {
        final String in = input == null ? "" : input.trim();
        return in.startsWith("\\\\") || in.startsWith("//") || in.equals(LINUX_ROOT)
                || in.startsWith(LINUX_ROOT + "/");
    }

    /** The network path {@code input} names, or null when it is not one. */
    public static NetPath parse(final String input) {
        if (!looksLike(input)) {
            return null;
        }
        final String in = input.trim();
        final String body = in.startsWith(LINUX_ROOT)
                ? in.substring(Math.min(in.length(), LINUX_ROOT.length() + 1))
                : in.substring(2);
        final String[] parts = body.split("[\\\\/]+", 3);
        final String host = parts.length > 0 ? parts[0] : "";
        final String share = parts.length > 1 ? parts[1] : "";
        String rest = parts.length > 2 ? parts[2].replace('\\', '/') : "";
        while (rest.endsWith("/")) {
            rest = rest.substring(0, rest.length() - 1);
        }
        return new NetPath(host, share, rest);
    }

    /** Whether this names the network itself: every host with something shared. */
    public boolean isNetwork() {
        return this.host.isEmpty();
    }

    /** Whether this names a host and nothing below it: its shares. */
    public boolean isHost() {
        return !this.host.isEmpty() && this.share.isEmpty();
    }

    /** Whether the host name is the one given, however it was typed. */
    public boolean onHost(final String hostname) {
        return this.host.equalsIgnoreCase(hostname == null ? "" : hostname.trim());
    }

    /** The path on the other machine, given where the share points: its DOS path with the rest below it. */
    public String remotePath(final String sharePath) {
        final String base = sharePath == null ? "" : sharePath.trim();
        if (this.rest.isEmpty()) {
            return base;
        }
        final String tail = this.rest.replace('/', '\\');
        return base.endsWith("\\") ? base + tail : base + "\\" + tail;
    }

    /** The last name in the path: the file or folder it points at, the share, or the host. */
    public String name() {
        if (!this.rest.isEmpty()) {
            final int slash = this.rest.lastIndexOf('/');
            return slash < 0 ? this.rest : this.rest.substring(slash + 1);
        }
        return this.share.isEmpty() ? this.host : this.share;
    }

    /** The same path one level up; the network's own root is its own parent. */
    public NetPath parent() {
        if (!this.rest.isEmpty()) {
            final int slash = this.rest.lastIndexOf('/');
            return new NetPath(this.host, this.share, slash < 0 ? "" : this.rest.substring(0, slash));
        }
        if (!this.share.isEmpty()) {
            return new NetPath(this.host, "", "");
        }
        return new NetPath("", "", "");
    }

    /** The path as a DOS shell shows it: {@code \\host\share\rest}. */
    public String display() {
        final StringBuilder out = new StringBuilder("\\\\").append(this.host);
        if (!this.share.isEmpty()) {
            out.append('\\').append(this.share);
        }
        if (!this.rest.isEmpty()) {
            out.append('\\').append(this.rest.replace('/', '\\'));
        }
        return out.toString();
    }

    /** The path as a Linux shell shows it: {@code /net/host/share/rest}. */
    public String linux() {
        final StringBuilder out = new StringBuilder(LINUX_ROOT);
        if (!this.host.isEmpty()) {
            out.append('/').append(this.host);
        }
        if (!this.share.isEmpty()) {
            out.append('/').append(this.share);
        }
        if (!this.rest.isEmpty()) {
            out.append('/').append(this.rest);
        }
        return out.toString();
    }

    /** A share name as it is compared: case does not matter to a name someone typed. */
    public static String key(final String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }
}
