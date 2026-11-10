/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install.voice;

import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.PackageManagerKind;
import dev.jstech.computers.os.ProgramShelf;
import dev.jstech.computers.program.cli.ICliPackages;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.resources.ResourceLocation;

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
    /*
     * OpenBSD's pkg_add opens by checking the signed quirks package of the release, the one it has shipped with since
     * 7.5 came out; a desktop brings xenodm's start-up script, which says how it is switched on.
     */
    private static final TextKey PKG_ADD_QUIRKS =
            TextKey.of("jsc.install.package_manager_voices.pkg_add_quirks", "%s signed on %s");
    private static final TextKey PKG_ADD_RCSCRIPTS = TextKey.of("jsc.install.package_manager_voices.pkg_add_rcscripts",
            "The following new rcscripts were installed: %s");
    private static final TextKey PKG_ADD_SEE_RCCTL =
            TextKey.of("jsc.install.package_manager_voices.pkg_add_see_rcctl", "See rcctl(8) for details.");
    private static final TextKey PKG_ADD_SHARED =
            TextKey.of("jsc.install.package_manager_voices.pkg_add_shared", "Read shared items: ok");
    private static final String QUIRKS = "quirks-7.14";
    private static final String QUIRKS_SIGNED = "2026-11-02T09:12:40Z";
    private static final TextKey FETCHING =
            TextKey.of("jsc.install.package_manager_voices.fetching", "Fetching %s %s from %s [%s]");
    private static final TextKey REMOVING =
            TextKey.of("jsc.install.package_manager_voices.removing", "Removing %s ...");
    /* What each manager says over its list of the packages the Mirror has newer, and of each one in it. */
    private static final TextKey APT_LISTING =
            TextKey.of("jsc.install.package_manager_voices.apt_listing", "Listing... Done");
    private static final TextKey APT_UPGRADABLE = TextKey.of("jsc.install.package_manager_voices.apt_upgradable",
            "%s/mirror %s %s [upgradable from: %s]");
    private static final TextKey EMERGE_MERGED = TextKey.of("jsc.install.package_manager_voices.emerge_merged",
            "These are the packages that would be merged, in order:");
    private static final TextKey PKG_NEEDS_UPDATING = TextKey.of(
            "jsc.install.package_manager_voices.pkg_needs_updating", "%s<  needs updating (remote has %s)");
    /* How long ago dnf says it last read the Mirror's metadata, before its list. */
    private static final String DNF_CHECKED = "0:02:11";
    /* The release a package was built at, which the RPM and pacman families write after its version. */
    private static final String RELEASE = "-1";
    /* How wide dnf's first column and pkg's are, as they line their lists up. */
    private static final int DNF_NAME_W = 24;
    private static final int DNF_VERSION_W = 10;
    private static final int PKG_NAME_W = 24;
    /* The version written for a package installed before the machine kept any: older than every version. */
    private static final String NO_VERSION = "0";

    private PackageManagerVoices() {
    }

    /**
     * Before the download starts.
     *
     * @param mb         how big the package is
     * @param mirrorHost the name the Mirror's machine goes by
     * @param desktop    whether it is a desktop, which on OpenBSD brings xenodm's start-up script with it
     */
    public static Text fetch(final PackageManagerKind manager, final String pkg, final String version,
                             final int mb, final String mirrorHost, final boolean desktop) {
        final String mirror = FetchVoice.mirrorAddress(mirrorHost);
        return switch (manager) {
            case PKG_ADD -> ICliPackages.lines(pkgAdd(pkg, version, desktop));
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
            case PKG_ADD -> ICliPackages.lines(List.of(Text.literal(pkg + "-" + version + ": ok"),
                    PKG_ADD_SHARED.text()));
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

    /**
     * What each manager prints when asked which of its packages the Mirror has newer, as its real command for it does
     * (computers/docs/design/programs.md): apt's {@code list --upgradable}, dnf's {@code check-update}, pacman's
     * {@code -Qu}, emerge's {@code -pvuD @world}, pkg's {@code version -vRL=}, pkg_add's {@code -un} and keg's
     * {@code outdated}. A manager with nothing newer prints what it prints before its list, or nothing at all, as the
     * real one does.
     *
     * @param arch the architecture apt and dnf name a package's build for, such as {@code i686}
     */
    public static List<Text> upgradable(final PackageManagerKind manager,
                                        final List<ICliPackages.PackageUpdate> updates, final String arch) {
        final List<Text> lines = new ArrayList<>();
        switch (manager) {
            case APT -> lines.add(APT_LISTING.text());
            case DNF -> lines.add(DNF_EXPIRATION.with(DNF_CHECKED));
            case EMERGE -> lines.add(EMERGE_MERGED.text());
            case PKG_ADD -> lines.add(PKG_ADD_QUIRKS.with(QUIRKS, QUIRKS_SIGNED));
            default -> {
                // pacman, pkg and keg print their list alone.
            }
        }
        for (final ICliPackages.PackageUpdate update : updates) {
            final String name = update.name();
            final String was = update.installed().isEmpty() ? NO_VERSION : update.installed();
            final String now = update.available();
            switch (manager) {
                case APT -> lines.add(APT_UPGRADABLE.with(name, now, arch, was));
                case DNF -> lines.add(Text.literal(String.format(Locale.ROOT, "%-" + DNF_NAME_W + "s %-"
                        + DNF_VERSION_W + "s %s", name + "." + arch, now + RELEASE, "mirror")));
                case PACMAN -> lines.add(Text.literal(name + " " + was + RELEASE + " -> " + now + RELEASE));
                case EMERGE -> lines.add(Text.literal("[ebuild     U  ] " + portageOf(update.id()) + "/" + name + "-"
                        + now + "::mirror [" + was + "::mirror]"));
                case PKG -> lines.add(PKG_NEEDS_UPDATING.with(
                        String.format(Locale.ROOT, "%-" + PKG_NAME_W + "s", name + "-" + was), now));
                case PKG_ADD -> lines.add(Text.literal(name + "-" + was + "->" + now + ": ok"));
                case KEG -> lines.add(Text.literal(name + " (" + was + ") < " + now));
                default -> {
                    // A system with no manager lists its updates in its Settings alone.
                }
            }
        }
        return lines;
    }

    /* The category Gentoo files a package under, the shelf every system files that program on. */
    private static String portageOf(final String id) {
        final ResourceLocation program = ResourceLocation.tryParse(id);
        return ProgramShelf.of(program == null ? null : OsRegistry.getProgram(program)).portage();
    }

    /* What pkg_add says: the quirks it checked, the package, and for a desktop the start-up script it brought. */
    private static List<Text> pkgAdd(final String pkg, final String version, final boolean desktop) {
        final List<Text> lines = new ArrayList<>();
        lines.add(PKG_ADD_QUIRKS.with(QUIRKS, QUIRKS_SIGNED));
        lines.add(Text.literal(pkg + "-" + version + ": ok"));
        if (desktop) {
            lines.add(PKG_ADD_RCSCRIPTS.with("/etc/rc.d/xenodm"));
            lines.add(PKG_ADD_SEE_RCCTL.text());
        }
        return lines;
    }
}
