/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.os.ProgramKind;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.ProgramVersions;
import dev.jstech.computers.program.cli.DosPath;
import dev.jstech.computers.program.install.MirrorPackage.Piece;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * The ports tree of a system that keeps one: a folder for every program the Mirror serves that system, filed by
 * category, each holding the three files a port is (its Makefile, the checksum of its source and its description),
 * and the index the tree is searched by.
 *
 * <p>It is written onto the system disk as real files, so the tree takes the room it really takes. A port is a few
 * lines of text, but every file fills at least one block of the disk it is on, which is why a ports tree has always
 * been far bigger on a disk than the text in it, and bigger again on a disk of a later generation, whose blocks are
 * bigger.
 */
final class PortsTree {

    /** Where the tree is kept, as a path on the system disk. */
    static final String ROOT = "usr/ports";
    static final String INDEX = ROOT + "/INDEX-14";
    /** What {@code portsnap fetch} leaves behind, which {@code extract} and {@code update} work from. */
    static final String SNAPSHOT_TAG = "var/db/portsnap/tag";

    private static final String MASTER_SITES = "mirror://mainframe/distfiles/";
    private static final String MAINTAINER = "ports@mainframe";
    private static final String PREFIX = "/usr/local";

    /** How much of a program's installed size its source comes to, the usual kind of estimate. */
    private static final double SOURCE_SHARE = 0.25;

    /**
     * The programs the real tree has ports of, filed where it files them and under the licence it names. Anything
     * else is filed by the kind of program it is, and declares no licence.
     */
    private static final Map<String, Known> KNOWN = Map.ofEntries(
            Map.entry("screenfetch", new Known("sysutils", "GPLv3")),
            Map.entry("vim", new Known("editors", "VIM")),
            Map.entry("emacs", new Known("editors", "GPLv3+")),
            Map.entry("virtual_studio_code", new Known("editors", "")),
            Map.entry("minesweeper", new Known("games", "")),
            Map.entry("solitaire", new Known("games", "")),
            Map.entry("snake", new Known("games", "")),
            Map.entry("paint", new Known("graphics", "")),
            Map.entry("exposure", new Known("graphics", "")),
            Map.entry("ark", new Known("archivers", "")),
            Map.entry("scc", new Known("lang", "")),
            Map.entry("sgsc", new Known("lang", "")),
            Map.entry("sigma", new Known("lang", "")),
            Map.entry("knot", new Known("devel", "")),
            Map.entry("knothub", new Known("devel", "")),
            Map.entry("messenger", new Known("net-im", "")),
            Map.entry("messenger_service", new Known("net-im", "")),
            Map.entry("kde_plasma", new Known("x11", "LGPL21")),
            Map.entry("gnome", new Known("x11", "GPLv2")),
            Map.entry("cinnamon", new Known("x11", "GPLv2")),
            Map.entry("cde", new Known("x11", "LGPL20")));

    /** Where a program is filed and the licence its port names, empty for none. */
    private record Known(String category, String license) {
    }

    private PortsTree() {
    }

    /** Every file of the tree for those ports, the index first, each by its path on the system disk. */
    static Map<String, String> files(final List<ProgramSpec> ports, final long seconds) {
        final Map<String, String> out = new LinkedHashMap<>();
        final StringBuilder index = new StringBuilder();
        for (final ProgramSpec port : ports) {
            index.append(indexLine(port)).append('\n');
        }
        out.put(INDEX, index.toString());
        for (final ProgramSpec port : ports) {
            out.put(dir(port) + "/Makefile", makefile(port));
            out.put(dir(port) + "/distinfo", distinfo(port, seconds));
            out.put(dir(port) + "/pkg-descr", descr(port));
        }
        return out;
    }

    /** The port whose folder that is, or null when it is no port's folder. */
    @Nullable
    static ProgramSpec at(final List<ProgramSpec> ports, final DosPath.Location where) {
        final List<String> segments = where.segments();
        if (where.drive() != 'C' || segments.size() != 4 || !segments.get(0).equalsIgnoreCase("usr")
                || !segments.get(1).equalsIgnoreCase("ports")) {
            return null;
        }
        for (final ProgramSpec port : ports) {
            if (category(port).equalsIgnoreCase(segments.get(2)) && name(port).equalsIgnoreCase(segments.get(3))) {
                return port;
            }
        }
        return null;
    }

    /** Where a program is filed: the real tree's category for one it has a port of, else by its kind. */
    static String category(final ProgramSpec port) {
        final Known known = KNOWN.get(port.id().getPath());
        if (known != null) {
            return known.category();
        }
        return switch (port.kind()) {
            case APP -> "misc";
            case SERVICE, OPERATING_SPACE -> "net";
            case HYBRID -> "sysutils";
            case DESKTOP_ENVIRONMENT -> "x11";
        };
    }

    /** The name the port goes by, which is the program's command. */
    static String name(final ProgramSpec port) {
        return port.commandName().toLowerCase(Locale.ROOT);
    }

    /** Its category and its name, which is how the tree names a port. */
    static String origin(final ProgramSpec port) {
        return category(port) + "/" + name(port);
    }

    /** Its folder, as a path on the system disk. */
    static String dir(final ProgramSpec port) {
        return ROOT + "/" + origin(port);
    }

    /** Its name and version, which is the package it builds. */
    static String pkgName(final ProgramSpec port) {
        return name(port) + "-" + ProgramVersions.of(port.id());
    }

    /** The file its source comes in. */
    static String distfile(final ProgramSpec port) {
        return pkgName(port) + ".tar.gz";
    }

    /** The licence its port names, or empty when it names none. */
    static String license(final ProgramSpec port) {
        final Known known = KNOWN.get(port.id().getPath());
        return known == null ? "" : known.license();
    }

    /**
     * How big its source is: the source of everything it is built from, for a program that is built out of others,
     * and a share of what it installs to for one that is a single piece.
     */
    static double sourceMb(final ProgramSpec port) {
        double total = 0.0;
        for (final Piece piece : SourceChains.of(port)) {
            total += piece.compiles() ? piece.sizeMb() : 0.0;
        }
        return total > 0.0 ? total : Math.max(1.0, port.minDiskMb() * SOURCE_SHARE);
    }

    /** What a finished build leaves in the port's work folder, which a later install finds instead of building. */
    static String buildCookie(final ProgramSpec port) {
        return workDir(port) + "/.build_done." + name(port) + "._usr_local";
    }

    /** Where a port builds, which cleaning it takes away. */
    static String workDir(final ProgramSpec port) {
        return dir(port) + "/work";
    }

    private static String makefile(final ProgramSpec port) {
        final String license = license(port);
        return "PORTNAME=\t" + name(port) + "\n"
                + "DISTVERSION=\t" + ProgramVersions.of(port.id()) + "\n"
                + "CATEGORIES=\t" + category(port) + "\n"
                + "MASTER_SITES=\t" + MASTER_SITES + "\n"
                + "\n"
                + "MAINTAINER=\t" + MAINTAINER + "\n"
                + "COMMENT=\t" + port.displayName() + "\n"
                + "WWW=\t\tmirror://mainframe/ports/" + origin(port) + "/\n"
                + "\n"
                + (license.isEmpty() ? "" : "LICENSE=\t" + license + "\n\n")
                + ".include <bsd.port.mk>\n";
    }

    private static String distinfo(final ProgramSpec port, final long seconds) {
        return "TIMESTAMP = " + seconds + "\n"
                + "SHA256 (" + distfile(port) + ") = " + sha256(pkgName(port)) + "\n"
                + "SIZE (" + distfile(port) + ") = " + Math.round(sourceMb(port) * 1024 * 1024) + "\n";
    }

    private static String descr(final ProgramSpec port) {
        return port.displayName() + ", built from its source on the machine that runs it.\n"
                + "\n"
                + "WWW: mirror://mainframe/ports/" + origin(port) + "/\n";
    }

    /** One port's line of the index, in the fields and the order the real index keeps. */
    private static String indexLine(final ProgramSpec port) {
        return String.join("|", pkgName(port), "/" + dir(port), PREFIX, port.displayName(),
                "/" + dir(port) + "/pkg-descr", MAINTAINER, category(port), "", "",
                "mirror://mainframe/ports/" + origin(port) + "/", "", "", "");
    }

    /** A real checksum of the text, so the same port always has the same one. */
    private static String sha256(final String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (final NoSuchAlgorithmException never) {
            // Every Java runtime is required to have SHA-256.
            throw new IllegalStateException(never);
        }
    }
}
