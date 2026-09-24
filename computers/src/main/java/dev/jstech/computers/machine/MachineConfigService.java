/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.gui.CdeStyle;
import dev.jstech.computers.item.DiskItem;
import dev.jstech.computers.program.ComputerConsoleState;
import dev.jstech.computers.program.ComputerSettings;
import dev.jstech.computers.program.ThemePreset;
import dev.jstech.computers.program.cli.CliTexts;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * What a machine keeps about itself: its name, how it looks, how much of its disk the network may read, and the
 * folders it opens to the other machines.
 *
 * <p>Most of it belongs to the machine's own settings store, which this asks; what is here are the few keys with
 * rules of their own, and the folder sharing, which has to check the folder is really there before opening it.
 */
@TextHolder
public final class MachineConfigService {

    private final IComputerTerminalHost terminal;
    private final ServerLevel level;
    /** The machine's drives, for the system disk and for checking a folder before it is shared. */
    private final FileService files;

    private static final TextKey UNNAMED = TextKey.of("jsc.service.config.unnamed", "(unnamed)");
    private static final TextKey SHARE_HINT = TextKey.of("jsc.service.config.share_hint",
            "'config share <folder> [read|write]' opens a folder to the network as \\\\%s\\<name>; "
                    + "'config unshare <name>' closes it");
    private static final TextKey NO_STORE =
            TextKey.of("jsc.service.config.no_store", "this computer has no settings store");
    private static final TextKey SET = TextKey.of("jsc.service.config.set", "%s set");
    private static final TextKey NETSHARE_NUMBER =
            TextKey.of("jsc.service.config.netshare_number", "netshare needs a number from 0 to 1000");
    private static final TextKey NO_SYSTEM_DISK =
            TextKey.of("jsc.service.config.no_system_disk", "no system disk to share");
    private static final TextKey NOTHING_SHARED =
            TextKey.of("jsc.service.config.nothing_shared", "nothing is shared as %s");
    private static final TextKey UNSHARED = TextKey.of("jsc.service.config.unshared", "no longer shared: %s");
    private static final TextKey UNKNOWN = TextKey.of("jsc.service.config.unknown", "unknown setting: %s");
    private static final TextKey SHARE_USAGE =
            TextKey.of("jsc.service.config.share_usage", "<folder> [read|write]");
    private static final TextKey TOO_MANY_SHARES =
            TextKey.of("jsc.service.config.too_many_shares", "this computer already shares %s folders");
    private static final TextKey SHARED_READ_ONLY =
            TextKey.of("jsc.service.config.shared_read_only", "shared %s as \\\\%s\\%s (read only)");
    private static final TextKey SHARED_READ_WRITE =
            TextKey.of("jsc.service.config.shared_read_write", "shared %s as \\\\%s\\%s (read and write)");

    public MachineConfigService(final IComputerTerminalHost terminal, final ServerLevel level,
                                final FileService files) {
        this.terminal = terminal;
        this.level = level;
        this.files = files;
    }

    /**
     * The settings as the {@code config} command prints them, and the two lines telling how to share a folder. They
     * travel on as words, so they are in English, the machine's language.
     */
    public List<String> summary() {
        final ComputerConsoleState console = this.terminal.console();
        if (console == null) {
            return List.of();
        }
        final List<String> lines = new ArrayList<>();
        final String name = console.computerName();
        lines.add(String.format(Locale.ROOT, "  %-12s%s", "name", name.isEmpty() ? UNNAMED.text().english() : name));
        lines.add(String.format(Locale.ROOT, "  %-12s%d permille", "netshare", this.diskPermille()));
        lines.addAll(console.settings().summaryLines());
        lines.add("  " + SHARE_HINT.with(this.terminal.hostname()).english());
        return lines;
    }

    /**
     * Changes one setting, clamping where the setting says so.
     *
     * <p>A handful of keys have rules of their own and are answered here; everything else is the settings store's
     * to take or to refuse, which is what keeps this from having to know every setting there is.
     */
    public ICliComputer.OpResult set(final String key, final String value) {
        final ComputerConsoleState console = this.terminal.console();
        if (console == null) {
            return ICliComputer.OpResult.fail(NO_STORE);
        }
        final BlockEntity machine = (BlockEntity) this.terminal;
        final String k = key == null ? "" : key.toLowerCase(Locale.ROOT).trim();
        switch (k) {
            case "name" -> {
                console.setComputerName(value == null ? "" : value.trim());
                machine.setChanged();
                return ICliComputer.OpResult.ok(SET.with(k));
            }
            case "wallpaper" -> {
                console.desktop().setWallpaper(value == null ? "" : value.trim());
                machine.setChanged();
                return ICliComputer.OpResult.ok(SET.with(k));
            }
            case "cdestyle" -> {
                // Read forgivingly and kept as read, so a style nobody could draw is never what is stored.
                console.desktop().setCdeStyle(CdeStyle.parse(value));
                machine.setChanged();
                return ICliComputer.OpResult.ok(SET.with(k));
            }
            case "theme" -> {
                // A theme preset bundles an accent and a wallpaper, so picking one restyles the desktop.
                final ThemePreset preset = ThemePreset.byId(value);
                preset.applyTo(console.settings());
                console.desktop().setWallpaper(preset.wallpaper());
                machine.setChanged();
                return ICliComputer.OpResult.ok(SET.with(k));
            }
            case "netshare" -> {
                final Integer permille = tryInt(value);
                if (permille == null) {
                    return ICliComputer.OpResult.fail(NETSHARE_NUMBER);
                }
                if (!this.setDiskPermille(permille)) {
                    return ICliComputer.OpResult.fail(NO_SYSTEM_DISK);
                }
                machine.setChanged();
                return ICliComputer.OpResult.ok(SET.with(k));
            }
            case "share" -> {
                return this.share(console, value);
            }
            case "unshare" -> {
                final String wanted = value == null ? "" : value.trim();
                if (!console.settings().unshare(wanted)) {
                    return ICliComputer.OpResult.fail(NOTHING_SHARED.with(wanted));
                }
                machine.setChanged();
                return ICliComputer.OpResult.ok(UNSHARED.with(wanted));
            }
            default -> {
                if (console.settings().applySetting(k, value)) {
                    machine.setChanged();
                    return ICliComputer.OpResult.ok(SET.with(k));
                }
                return ICliComputer.OpResult.fail(UNKNOWN.with(k));
            }
        }
    }

    /** The folders this machine shares, in the order they were shared. */
    public List<ICliComputer.ShareInfo> shares() {
        return sharesOf(this.terminal);
    }

    /**
     * The folders any machine shares, in the order they were shared.
     *
     * <p>What a machine shares is written in its own settings, so reading it needs the machine and nothing else.
     * Whoever walks a network asks each machine directly instead of standing a shell up in front of it.
     */
    public static List<ICliComputer.ShareInfo> sharesOf(final IComputerTerminalHost machine) {
        final ComputerConsoleState console = machine.console();
        if (console == null) {
            return List.of();
        }
        final List<ICliComputer.ShareInfo> out = new ArrayList<>();
        for (final ComputerSettings.Share share : console.settings().shares()) {
            out.add(new ICliComputer.ShareInfo(share.name(), share.path(), share.writable()));
        }
        return out;
    }

    /**
     * Opens a folder of this machine to the others on its network: {@code config share C:\pub} for reading,
     * {@code config share C:\pub write} for writing too. The folder has to be there.
     */
    private ICliComputer.OpResult share(final ComputerConsoleState console, final String value) {
        String path = value == null ? "" : value.trim();
        boolean writable = false;
        final int space = path.lastIndexOf(' ');
        if (space > 0) {
            final String mode = path.substring(space + 1).toLowerCase(Locale.ROOT);
            if (mode.equals("write") || mode.equals("read")) {
                writable = mode.equals("write");
                path = path.substring(0, space).trim();
            }
        }
        if (path.isEmpty()) {
            return ICliComputer.OpResult.fail(CliTexts.USAGE.with("config share", SHARE_USAGE));
        }
        final ICliComputer.OpResult folder = this.files.folderForShare(path);
        if (!folder.ok()) {
            return folder;
        }
        final String dos = folder.message().english();
        if (!console.settings().share(dos, writable)) {
            return ICliComputer.OpResult.fail(TOO_MANY_SHARES.with(ComputerSettings.MAX_SHARES));
        }
        ((BlockEntity) this.terminal).setChanged();
        final String name = ComputerSettings.shareNameOf(dos);
        return ICliComputer.OpResult.ok((writable ? SHARED_READ_WRITE : SHARED_READ_ONLY)
                .with(dos, this.terminal.hostname(), name));
    }

    /** The system disk's public-share permille, or 0 when there is no system disk. */
    private int diskPermille() {
        final DriveTable.Drive drive = DriveTable.of((BlockEntity) this.terminal, this.level).find('C');
        return drive == null || drive.disk().isEmpty() ? 0 : DiskItem.publicPermille(drive.disk());
    }

    /** Writes a clamped public-share permille onto the system disk; false when there is none. */
    private boolean setDiskPermille(final int permille) {
        final DriveTable.Drive drive = DriveTable.of((BlockEntity) this.terminal, this.level).find('C');
        if (drive == null || drive.disk().isEmpty()) {
            return false;
        }
        DiskItem.setPublicPermille(drive.disk(), permille);
        return true;
    }

    /** A whole number typed by a player, or null when what was typed is not one. */
    @Nullable
    private static Integer tryInt(final String value) {
        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (final NumberFormatException e) {
            return null;
        }
    }
}
