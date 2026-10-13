/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os.boot;

import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.SystemLayout;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.world.item.ItemStack;

/**
 * Whether what is on the disk is still enough to start the system that is installed on it.
 *
 * <p>Nothing on these machines is protected: a player who deletes the system folder has deleted it, which is
 * the point. What that costs them is decided here, and it is decided by what is missing rather than by the
 * fact that something is. A machine that is running does not care at all, because the system is already in
 * memory, exactly as a real one would not; the reckoning comes at the next start.
 *
 * <p>Three answers, because there are three cases worth telling apart: everything is there; the system's own
 * folder is gone, so there is no system to find at all; or the folder is there but what starts it is not, and
 * the machine can say which file it wanted. Each family says that last one in its own words, because each of
 * them really did.
 *
 * <p>The words are the family's, read in the player's language where that family's machines were sold in one;
 * a file's name is data, and so is a kernel's panic, which no kernel ever printed in anything but its own words.
 */
@TextHolder
public final class SystemIntegrity {

    private static final TextKey FILE_MISSING = TextKey.of("jsc.boot.system_integrity.file_missing", "%s is missing");
    private static final TextKey NO_INTERPRETER =
            TextKey.of("jsc.boot.system_integrity.no_interpreter", "Bad or missing command interpreter");
    private static final TextKey REPAIR_FROM_SETUP = TextKey.of("jsc.boot.system_integrity.repair_from_setup",
            "Put in an installation medium and press F10 to install over it.");
    private static final TextKey REPAIR_BY_BOOTING = TextKey.of("jsc.boot.system_integrity.repair_by_booting",
            "Boot an installation medium and install over it to repair.");

    private SystemIntegrity() {
    }

    /** What a check found. */
    public enum State {
        /** Enough to start. */
        WHOLE,
        /** No system folder on the disk: nothing to find, so the firmware looks elsewhere. */
        NO_SYSTEM,
        /** The folder is there and what starts it is not: found, and it will not run. */
        NO_LOADER
    }

    /** What a check found, and what the machine says about it. */
    public record Result(State state, Text complaint, String missing) {

        public boolean whole() {
            return this.state == State.WHOLE;
        }
    }

    private static final Result FINE = new Result(State.WHOLE, Text.EMPTY, "");

    /**
     * Looks at the disk under the installed system.
     *
     * <p>A machine with no system installed at all is not damaged, it is empty, and that is somebody else's
     * question; the same goes for one whose disk keeps no files, since there was never anything to delete.
     */
    public static Result check(final IOsHost machine) {
        final OsDef os = machine.installedOs();
        final ItemStack disk = machine.systemDisk();
        if (os == null || disk == null || disk.isEmpty()) {
            return FINE;
        }
        final String folder = folderOf(os);
        final String loader = loaderOf(os);
        if (folder.isEmpty() && loader.isEmpty()) {
            return FINE;
        }
        /*
         * A system on a flat disk has no folder to lose, only the file that starts it, so it has one way of
         * being broken where the others have two. Asking after a folder that cannot exist would say a whole
         * machine was gone the moment it was installed.
         */
        final FilesystemKind kind = FilesystemKind.HIERARCHICAL;
        if (!folder.isEmpty()
                && DiskFilesystem.listDirs(disk, "", kind).stream().noneMatch(folder::equalsIgnoreCase)
                && !DiskFilesystem.exists(disk, loader)) {
            return new Result(State.NO_SYSTEM, Text.EMPTY, folder);
        }
        if (!loader.isEmpty() && !DiskFilesystem.exists(disk, loader)) {
            return new Result(State.NO_LOADER, complaintOf(os), loader);
        }
        return FINE;
    }

    /**
     * The folder the system lives in, whose absence means there is no system at all.
     *
     * <p>MC-NET keeps no files and has nothing to lose; the rest each have the one folder their system is.
     */
    public static String folderOf(final OsDef os) {
        return switch (os.platform()) {
            case FRAMES -> SystemLayout.SYSTEM_DIR;
            case MC_DOS -> "DOS";
            case LINUX, UNIX, FREEBSD -> "boot";
            case MC_NET -> "";
        };
    }

    /**
     * The one file that starts the system, whose absence stops it where it stands.
     *
     * <p>MC-NET's sits at the root because its disk is flat: there is no folder to put it in.
     */
    public static String loaderOf(final OsDef os) {
        return switch (os.platform()) {
            case FRAMES -> SystemLayout.SYSTEM_DIR + "/kickmgr.sys";
            case MC_DOS -> "COMMAND.COM";
            case LINUX -> "boot/vmlinuz";
            case UNIX, FREEBSD -> "boot/kernel";
            case MC_NET -> "netstart.sys";
        };
    }

    /** What the machine says when the loader is gone, in the words that family used. */
    public static Text complaintOf(final OsDef os) {
        return switch (os.platform()) {
            case FRAMES -> FILE_MISSING.with("kickmgr");
            case MC_DOS -> NO_INTERPRETER.text();
            case LINUX, UNIX, FREEBSD -> Text.literal("kernel panic - not syncing: no init found");
            case MC_NET -> FILE_MISSING.with("netstart.sys");
        };
    }

    /** What to tell a player standing in front of a machine that will not start. */
    public static Text repairLine(final Platform platform) {
        return (platform == Platform.MC_DOS || platform == Platform.FRAMES
                ? REPAIR_FROM_SETUP : REPAIR_BY_BOOTING).text();
    }
}
