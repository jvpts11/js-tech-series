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
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * What a program is in the package tree of a system that builds what it installs: the name it is merged
 * under, and everything that has to be built before it can be.
 *
 * <p>Most programs are one package. A desktop is not: asking for one pulls in the toolkit it is drawn with,
 * the frameworks it is written on, and its own pieces, and the package asked for is a list of the rest with
 * nothing of its own to compile. Those chains are the real ones, shortened to what a player has the patience
 * to watch, in the order the real tool would build them.
 */
final class SourceChains {

    /** How much of a program's installed size its source comes to, the usual kind of estimate. */
    private static final double SOURCE_SHARE = 0.25;

    private static final Map<String, List<Link>> DESKTOPS = Map.of(
            "kde_plasma", List.of(
                    new Link("dev-qt/qtbase", "6.7.3", "qtbase-everywhere-src-6.7.3.tar.xz", 48.0,
                            "gui network widgets", "-debug", true),
                    new Link("kde-frameworks/kconfig", "6.6.0", "kconfig-6.6.0.tar.xz", 2.1, "qml", "-debug -doc",
                            true),
                    new Link("kde-frameworks/kcoreaddons", "6.6.0", "kcoreaddons-6.6.0.tar.xz", 2.4, "dbus", "-debug",
                            true),
                    new Link("kde-plasma/kwin", "6.1.5", "kwin-6.1.5.tar.xz", 8.6, "handbook lock", "-debug", true),
                    new Link("kde-plasma/plasma-workspace", "6.1.5", "plasma-workspace-6.1.5.tar.xz", 19.0,
                            "handbook wallpapers", "-debug", true),
                    new Link("kde-plasma/plasma-meta", "6.1.5", "", 0.0, "display-manager sddm", "-cups", false)),
            "gnome", List.of(
                    new Link("dev-libs/glib", "2.80.5", "glib-2.80.5.tar.xz", 5.3, "elf mime xattr", "-debug", true),
                    new Link("gui-libs/gtk", "4.14.5", "gtk-4.14.5.tar.xz", 13.2, "introspection wayland", "-debug",
                            true),
                    new Link("x11-wm/mutter", "46.5", "mutter-46.5.tar.xz", 3.1, "introspection wayland", "-debug",
                            true),
                    new Link("gnome-base/gnome-shell", "46.5", "gnome-shell-46.5.tar.xz", 2.0, "networkmanager",
                            "-debug", true),
                    new Link("gnome-base/gnome", "46.0", "", 0.0, "bluetooth extras", "-accessibility", false)),
            "cinnamon", List.of(
                    new Link("dev-libs/glib", "2.80.5", "glib-2.80.5.tar.xz", 5.3, "elf mime xattr", "-debug", true),
                    new Link("x11-libs/gtk+", "3.24.43", "gtk+-3.24.43.tar.xz", 12.6, "introspection X", "-debug",
                            true),
                    new Link("gnome-extra/cjs", "6.2.0", "cjs-6.2.0.tar.gz", 0.7, "cairo readline", "-debug", true),
                    new Link("x11-wm/muffin", "6.2.0", "muffin-6.2.0.tar.gz", 2.9, "introspection", "-debug", true),
                    new Link("gnome-extra/cinnamon", "6.2.9", "cinnamon-6.2.9.tar.gz", 8.4, "nls networkmanager",
                            "-debug", true)));

    private SourceChains() {
    }

    /**
     * One package of a chain.
     *
     * @param atom     its category and name
     * @param archive  the file its source comes in, empty for one that is only a list of others
     * @param sizeMb   how big that file is
     * @param compiles whether it has anything of its own to compile
     */
    record Link(String atom, String version, String archive, double sizeMb, String flagsOn, String flagsOff,
                boolean compiles) {
    }

    /** Everything merging that program builds, in the order it is built, the program itself last. */
    static List<Link> of(final ProgramSpec spec) {
        final List<Link> chain = DESKTOPS.get(spec.id().getPath());
        if (chain != null) {
            return chain;
        }
        final String name = spec.commandName().toLowerCase(Locale.ROOT);
        final String version = ProgramVersions.of(spec.id());
        return List.of(new Link(category(spec.kind()) + "/" + name, version, name + "-" + version + ".tar.xz",
                Math.max(1.0, spec.minDiskMb() * SOURCE_SHARE), "nls", "-debug", true));
    }

    /** The name that program is merged under, which is the last of what merging it builds. */
    static String atomOf(final ProgramSpec spec) {
        final List<Link> chain = of(spec);
        return chain.get(chain.size() - 1).atom();
    }

    /**
     * Whether what was typed names that program the way this package tree names things: its category and its
     * name, or the name by itself, which the real tool takes as long as only one category has it.
     */
    static boolean names(final String typed, final ProgramSpec spec) {
        final String atom = atomOf(spec);
        return typed.equalsIgnoreCase(atom) || typed.equalsIgnoreCase(atom.substring(atom.indexOf('/') + 1));
    }

    /** Where the package tree files a program of that kind, near enough. */
    private static String category(final ProgramKind kind) {
        return switch (kind) {
            case APP -> "app-misc";
            case SERVICE -> "net-misc";
            case HYBRID -> "app-admin";
            case DESKTOP_ENVIRONMENT -> "x11-wm";
        };
    }
}
