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
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.terminal.IComputerTerminalHost;
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
public final class MachineConfigService {

    private final IComputerTerminalHost terminal;
    private final ServerLevel level;
    /** The machine's drives, for the system disk and for checking a folder before it is shared. */
    private final FileService files;

    public MachineConfigService(final IComputerTerminalHost terminal, final ServerLevel level,
                                final FileService files) {
        this.terminal = terminal;
        this.level = level;
        this.files = files;
    }

    /** The settings as the {@code config} command prints them, and the two lines telling how to share a folder. */
    public List<String> summary() {
        final ComputerConsoleState console = this.terminal.console();
        if (console == null) {
            return List.of();
        }
        final List<String> lines = new ArrayList<>();
        final String name = console.computerName();
        lines.add(String.format(Locale.ROOT, "  %-12s%s", "name", name.isEmpty() ? "(unnamed)" : name));
        lines.add(String.format(Locale.ROOT, "  %-12s%d permille", "netshare", this.diskPermille()));
        lines.addAll(console.settings().summaryLines());
        lines.add("  'config share <folder> [read|write]' opens a folder to the network as \\\\"
                + this.terminal.hostname() + "\\<name>; 'config unshare <name>' closes it");
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
            return ICliComputer.OpResult.fail("this computer has no settings store");
        }
        final BlockEntity machine = (BlockEntity) this.terminal;
        final String k = key == null ? "" : key.toLowerCase(Locale.ROOT).trim();
        switch (k) {
            case "name" -> {
                console.setComputerName(value == null ? "" : value.trim());
                machine.setChanged();
                return ICliComputer.OpResult.ok("name set");
            }
            case "wallpaper" -> {
                console.setWallpaper(value == null ? "" : value.trim());
                machine.setChanged();
                return ICliComputer.OpResult.ok("wallpaper set");
            }
            case "cdestyle" -> {
                // Read forgivingly and kept as read, so a style nobody could draw is never what is stored.
                console.setCdeStyle(CdeStyle.parse(value));
                machine.setChanged();
                return ICliComputer.OpResult.ok("cdestyle set");
            }
            case "theme" -> {
                // A theme preset bundles an accent and a wallpaper, so picking one restyles the desktop.
                final String preset = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
                console.settings().setThemePreset(preset.equals("system") ? "" : preset);
                switch (preset) {
                    case "ocean" -> {
                        console.settings().setAccent(0xFF12A26F);
                        console.setWallpaper("winxp");
                    }
                    case "slate" -> {
                        console.settings().setAccent(0xFF7B52C9);
                        console.setWallpaper("win11");
                    }
                    default -> {
                        console.settings().setAccent(0);
                        console.setWallpaper("");
                    }
                }
                machine.setChanged();
                return ICliComputer.OpResult.ok("theme set");
            }
            case "netshare" -> {
                final Integer permille = tryInt(value);
                if (permille == null) {
                    return ICliComputer.OpResult.fail("netshare needs a number from 0 to 1000");
                }
                if (!this.setDiskPermille(permille)) {
                    return ICliComputer.OpResult.fail("no system disk to share");
                }
                machine.setChanged();
                return ICliComputer.OpResult.ok("netshare set");
            }
            case "share" -> {
                return this.share(console, value);
            }
            case "unshare" -> {
                final String wanted = value == null ? "" : value.trim();
                if (!console.settings().unshare(wanted)) {
                    return ICliComputer.OpResult.fail("nothing is shared as " + wanted);
                }
                machine.setChanged();
                return ICliComputer.OpResult.ok("no longer shared: " + wanted);
            }
            default -> {
                if (console.settings().applySetting(k, value)) {
                    machine.setChanged();
                    return ICliComputer.OpResult.ok(k + " set");
                }
                return ICliComputer.OpResult.fail("unknown setting: " + k);
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
            return ICliComputer.OpResult.fail("usage: config share <folder> [read|write]");
        }
        final ICliComputer.OpResult folder = this.files.folderForShare(path);
        if (!folder.ok()) {
            return folder;
        }
        final String dos = folder.message();
        if (!console.settings().share(dos, writable)) {
            return ICliComputer.OpResult.fail("this computer already shares "
                    + ComputerSettings.MAX_SHARES + " folders");
        }
        ((BlockEntity) this.terminal).setChanged();
        final String name = ComputerSettings.shareNameOf(dos);
        return ICliComputer.OpResult.ok("shared " + dos + " as \\\\" + this.terminal.hostname() + "\\" + name
                + (writable ? " (read and write)" : " (read only)"));
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
