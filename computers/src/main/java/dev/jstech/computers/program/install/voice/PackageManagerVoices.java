/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install.voice;

import dev.jstech.computers.os.PackageManagerKind;
import dev.jstech.computers.program.cli.ICliPackages;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;

/**
 * What a package manager that ships built packages says before it installs or removes one, in its own words.
 *
 * <p>Each of them has a voice a player who has used the real one knows on sight, and the lines are that voice: what
 * was resolved, what will be installed or removed, how big it is, and where it comes from. They are one message, a
 * line at a time, and the bar the machine draws afterwards follows them.
 *
 * <p>The managers' sentences are the player's language. The names, versions, sizes and addresses in them, and the
 * lines that are a listing rather than a sentence (a package row, a fetch line), are the same in every language.
 */
@TextHolder
public final class PackageManagerVoices {

    private static final TextKey APT_READING =
            TextKey.of("jsc.install.package_manager_voices.apt_reading", "Reading package lists... Done");
    private static final TextKey APT_TREE =
            TextKey.of("jsc.install.package_manager_voices.apt_tree", "Building dependency tree... Done");
    private static final TextKey APT_NEW = TextKey.of("jsc.install.package_manager_voices.apt_new",
            "The following NEW packages will be installed:");
    private static final TextKey APT_NEED =
            TextKey.of("jsc.install.package_manager_voices.apt_need", "Need to get %s of archives.");
    private static final TextKey APT_REMOVED = TextKey.of("jsc.install.package_manager_voices.apt_removed",
            "The following packages will be REMOVED:");
    private static final TextKey APT_FREED = TextKey.of("jsc.install.package_manager_voices.apt_freed",
            "After this operation, %s disk space will be freed.");
    private static final TextKey APT_REMOVING =
            TextKey.of("jsc.install.package_manager_voices.apt_removing", "Removing %s (%s) ...");
    private static final TextKey DNF_EXPIRATION = TextKey.of("jsc.install.package_manager_voices.dnf_expiration",
            "Last metadata expiration check: %s ago.");
    private static final TextKey DNF_RESOLVED =
            TextKey.of("jsc.install.package_manager_voices.dnf_resolved", "Dependencies resolved.");
    private static final TextKey DNF_INSTALLING =
            TextKey.of("jsc.install.package_manager_voices.dnf_installing", "Installing:  %s");
    private static final TextKey DNF_DOWNLOADING =
            TextKey.of("jsc.install.package_manager_voices.dnf_downloading", "Downloading Packages:");
    private static final TextKey DNF_REMOVING =
            TextKey.of("jsc.install.package_manager_voices.dnf_removing", "Removing:  %s");
    private static final TextKey DNF_TRANSACTION =
            TextKey.of("jsc.install.package_manager_voices.dnf_transaction", "Running transaction");
    private static final TextKey PACMAN_RESOLVING =
            TextKey.of("jsc.install.package_manager_voices.pacman_resolving", "resolving dependencies...");
    private static final TextKey PACMAN_CONFLICTS = TextKey.of("jsc.install.package_manager_voices.pacman_conflicts",
            "looking for conflicting packages...");
    private static final TextKey PACMAN_PACKAGES =
            TextKey.of("jsc.install.package_manager_voices.pacman_packages", "Packages (%s) %s");
    private static final TextKey PACMAN_DOWNLOAD =
            TextKey.of("jsc.install.package_manager_voices.pacman_download", "Total Download Size: %s");
    private static final TextKey PACMAN_RETRIEVING =
            TextKey.of("jsc.install.package_manager_voices.pacman_retrieving", ":: Retrieving packages...");
    private static final TextKey PACMAN_CHECKING =
            TextKey.of("jsc.install.package_manager_voices.pacman_checking", "checking dependencies...");
    private static final TextKey PACMAN_REMOVING =
            TextKey.of("jsc.install.package_manager_voices.pacman_removing", ":: Removing %s ...");
    private static final TextKey PKG_AFFECTED = TextKey.of("jsc.install.package_manager_voices.pkg_affected",
            "The following %s package(s) will be affected (of %s checked):");
    private static final TextKey PKG_NEW =
            TextKey.of("jsc.install.package_manager_voices.pkg_new", "New packages to be INSTALLED:");
    private static final TextKey PKG_COUNT =
            TextKey.of("jsc.install.package_manager_voices.pkg_count", "Number of packages to be installed: %s");
    private static final TextKey PKG_DOWNLOADED =
            TextKey.of("jsc.install.package_manager_voices.pkg_downloaded", "%s to be downloaded.");
    private static final TextKey PKG_FETCHING =
            TextKey.of("jsc.install.package_manager_voices.pkg_fetching", "[%s/%s] Fetching %s from %s");
    private static final TextKey PKG_INTEGRITY = TextKey.of("jsc.install.package_manager_voices.pkg_integrity",
            "Checking integrity... done (%s conflicting)");
    private static final TextKey PKG_DEINSTALL = TextKey.of("jsc.install.package_manager_voices.pkg_deinstall",
            "Deinstallation has been requested for the following %s packages:");
    private static final TextKey PKG_REMOVED =
            TextKey.of("jsc.install.package_manager_voices.pkg_removed", "Installed packages to be REMOVED:");
    private static final TextKey PKG_FREE =
            TextKey.of("jsc.install.package_manager_voices.pkg_free", "The operation will free %s.");
    private static final TextKey PKG_DEINSTALLING =
            TextKey.of("jsc.install.package_manager_voices.pkg_deinstalling", "[%s/%s] Deinstalling %s...");
    private static final TextKey FETCHING =
            TextKey.of("jsc.install.package_manager_voices.fetching", "Fetching %s %s from %s [%s]");
    private static final TextKey REMOVING =
            TextKey.of("jsc.install.package_manager_voices.removing", "Removing %s ...");

    private PackageManagerVoices() {
    }

    /**
     * Before the download starts.
     *
     * @param mb         how big the package is
     * @param mirrorHost the name the Mirror's machine goes by
     */
    public static Text fetch(final PackageManagerKind manager, final String pkg, final String version,
                             final int mb, final String mirrorHost) {
        final String mirror = "mirror://" + mirrorHost;
        return switch (manager) {
            case APT -> ICliPackages.lines(List.of(
                    APT_READING.text(),
                    APT_TREE.text(),
                    APT_NEW.text(),
                    Text.literal("  " + pkg),
                    APT_NEED.with(mb + " MB"),
                    Text.literal("Get:1 " + mirror + " stable/main " + pkg + " " + version + " [" + mb + " MB]")));
            case DNF -> ICliPackages.lines(List.of(
                    DNF_EXPIRATION.with("0:00:01"),
                    DNF_RESOLVED.text(),
                    DNF_INSTALLING.with(Text.literal(pkg + "  x86_64  " + version + "  mirror  " + mb + " MB")),
                    DNF_DOWNLOADING.text()));
            case PACMAN -> ICliPackages.lines(List.of(
                    PACMAN_RESOLVING.text(),
                    PACMAN_CONFLICTS.text(),
                    PACMAN_PACKAGES.with(1, pkg + "-" + version),
                    PACMAN_DOWNLOAD.with(Text.literal(mb + ".00 MiB")),
                    PACMAN_RETRIEVING.text()));
            case PKG -> ICliPackages.lines(List.of(
                    PKG_AFFECTED.with(1, 0),
                    Text.EMPTY,
                    PKG_NEW.text(),
                    Text.literal("        " + pkg + ": " + version),
                    Text.EMPTY,
                    PKG_COUNT.with(1),
                    Text.EMPTY,
                    PKG_DOWNLOADED.with(Text.literal(mb + " MiB")),
                    PKG_FETCHING.with(1, 1, pkg + "-" + version + ".pkg", mirror)));
            default -> FETCHING.with(pkg, version, mirror, mb + " MB");
        };
    }

    /**
     * Before the package is taken off.
     *
     * @param mb how much room taking it off frees
     */
    public static Text remove(final PackageManagerKind manager, final String pkg, final String version,
                              final int mb) {
        return switch (manager) {
            case APT -> ICliPackages.lines(List.of(
                    APT_READING.text(),
                    APT_TREE.text(),
                    APT_REMOVED.text(),
                    Text.literal("  " + pkg),
                    APT_FREED.with(mb + " MB"),
                    APT_REMOVING.with(pkg, version)));
            case DNF -> ICliPackages.lines(List.of(
                    DNF_RESOLVED.text(),
                    DNF_REMOVING.with(Text.literal(pkg + "  x86_64  " + version)),
                    DNF_TRANSACTION.text()));
            case PACMAN -> ICliPackages.lines(List.of(
                    PACMAN_CHECKING.text(),
                    PACMAN_PACKAGES.with(1, pkg + "-" + version),
                    PACMAN_REMOVING.with(pkg)));
            case PKG -> ICliPackages.lines(List.of(
                    PKG_INTEGRITY.with(0),
                    PKG_DEINSTALL.with(1),
                    Text.EMPTY,
                    PKG_REMOVED.text(),
                    Text.literal("        " + pkg + ": " + version),
                    Text.EMPTY,
                    PKG_FREE.with(Text.literal(mb + " MiB")),
                    PKG_DEINSTALLING.with(1, 1, pkg + "-" + version)));
            default -> REMOVING.with(pkg);
        };
    }
}
