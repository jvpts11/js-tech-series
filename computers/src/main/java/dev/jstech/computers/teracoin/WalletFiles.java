/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.teracoin;

import dev.jstech.computers.machine.FileService;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.KernelDef;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.ShellFamily;
import dev.jstech.computers.os.fs.SystemLayout;
import dev.jstech.computers.program.cli.ICliComputer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * Teracoin's wallets as files on a computer's disks (computers/docs/design/teracoin.md): a wallet is a small text file
 * the Crypto Wallet makes, keeping the address and the key that spends from it. The address belongs to the file, not
 * to the machine: taking the file to another computer takes the address, and whoever has a copy spends the same
 * coins. A computer's wallets are the files in its wallets folder, and those at the top of, or in a Wallets folder
 * on, any other drive it sees, such as a USB stick.
 */
public final class WalletFiles {

    /** The ending of a wallet's file. */
    public static final String EXTENSION = ".wallet";
    /** The most wallets one computer lists. */
    public static final int MOST = 16;
    /** The letters of a wallet's name at most, its ending left out. */
    public static final int NAME_MOST = 24;
    /* The first line of every wallet's file, so one is told from any other text. */
    private static final String HEADER = "Blockwright Crypto Wallet, Teracoin";
    private static final String ADDRESS = "address=";
    private static final String KEY = "key=";

    private WalletFiles() {
    }

    /** The folder {@code host}'s system keeps wallets in, on its system disk, as its paths are written. */
    public static String folder(final IOsHost host) {
        final OsDef os = host.installedOs();
        final String folder = os == null ? SystemLayout.WALLETS_DIR
                : SystemLayout.walletsDirFor(os, OsRegistry.getKernel(os.kernelId()));
        return "C:/" + folder;
    }

    /** {@code path} written as {@code host}'s system writes paths: C:\Users\... or /home/player/... */
    public static String shown(final IOsHost host, final String path) {
        final OsDef os = host.installedOs();
        final KernelDef kernel = os == null ? null : OsRegistry.getKernel(os.kernelId());
        final boolean unix = kernel != null && kernel.shellFamily() == ShellFamily.POSIX;
        final String forward = path.replace('\\', '/');
        if (unix) {
            return forward.regionMatches(true, 0, "C:/", 0, 3) ? forward.substring(2) : forward;
        }
        return forward.replace('/', '\\');
    }

    /** The text of a wallet's file holding {@code keys}. */
    public static String content(final TeracoinChain.Keys keys) {
        return HEADER + "\n" + ADDRESS + keys.address() + "\n" + KEY + keys.key() + "\n";
    }

    /** The wallet in the file at {@code path}, or none when it holds none. */
    public static Optional<Wallet> read(final FileService files, final String path) {
        final ICliComputer.FsResult read = files.readFile(path);
        if (!read.ok()) {
            return Optional.empty();
        }
        String address = "";
        String key = "";
        boolean header = false;
        for (final String line : read.message().english().split("\n")) {
            final String trimmed = line.trim();
            if (trimmed.equals(HEADER)) {
                header = true;
            } else if (trimmed.startsWith(ADDRESS)) {
                address = trimmed.substring(ADDRESS.length());
            } else if (trimmed.startsWith(KEY)) {
                key = trimmed.substring(KEY.length());
            }
        }
        if (!header || !TeracoinAddress.valid(address) || !TeracoinAddress.validKey(key)) {
            return Optional.empty();
        }
        return Optional.of(new Wallet(path, fileName(path), address, key));
    }

    /** Every wallet {@code host} sees, those in its wallets folder first, at most {@link #MOST}. */
    public static List<Wallet> all(final FileService files, final IOsHost host) {
        final Set<String> folders = new LinkedHashSet<>();
        folders.add(folder(host));
        for (final ICliComputer.MountInfo mount : files.mounts()) {
            if (mount.ready() && mount.drive() != 'C') {
                folders.add(mount.drive() + ":/");
                folders.add(mount.drive() + ":/Wallets");
            }
        }
        final List<Wallet> out = new ArrayList<>();
        for (final String folder : folders) {
            for (final String name : files.list(folder)) {
                if (out.size() >= MOST) {
                    return out;
                }
                if (!name.endsWith("/") && name.toLowerCase(Locale.ROOT).endsWith(EXTENSION)) {
                    read(files, join(folder, name)).ifPresent(out::add);
                }
            }
        }
        return out;
    }

    /** The letter of a drive a copy may go to beside the system disk, a USB stick or the like; 0 when there is none. */
    public static char copyDrive(final FileService files) {
        for (final ICliComputer.MountInfo mount : files.mounts()) {
            if (mount.ready() && mount.drive() != 'C' && files.folderExists(mount.drive() + ":/")) {
                return mount.drive();
            }
        }
        return 0;
    }

    /** A wallet's name as a player typed it, cut to what a file may be called; empty when nothing of it is left. */
    public static String cleanName(final String typed) {
        final StringBuilder out = new StringBuilder();
        for (final char c : typed.trim().toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == '-' || c == '_') {
                out.append(c);
            }
            if (out.length() >= NAME_MOST) {
                break;
            }
        }
        return out.toString();
    }

    /**
     * Writes a new wallet holding {@code keys} as {@code name} in {@code folder}, and a copy at the top of the drive
     * {@code copyTo} when it is not 0. The path written, or null when it could not be.
     */
    @Nullable
    public static String write(final FileService files, final String folder, final String name,
                               final TeracoinChain.Keys keys, final char copyTo) {
        final String path = join(folder, name + EXTENSION);
        if (files.exists(path)) {
            return null;
        }
        if (!files.folderExists(folder)) {
            files.makeFolder(folder);
        }
        if (!files.write(path, content(keys))) {
            return null;
        }
        if (copyTo != 0) {
            files.write(copyTo + ":/" + name + EXTENSION, content(keys));
        }
        return path;
    }

    /** The file name at the end of {@code path}. */
    public static String fileName(final String path) {
        final String forward = path.replace('\\', '/');
        return forward.substring(forward.lastIndexOf('/') + 1);
    }

    /** The folder {@code path} is in. */
    public static String folderOf(final String path) {
        final String forward = path.replace('\\', '/');
        final int slash = forward.lastIndexOf('/');
        return slash < 0 ? "" : forward.substring(0, slash);
    }

    private static String join(final String folder, final String name) {
        return folder.endsWith("/") ? folder + name : folder + "/" + name;
    }

    /** A wallet found on a computer: where its file is, its name, its address and its key. */
    public record Wallet(String path, String file, String address, String key) {
    }
}
