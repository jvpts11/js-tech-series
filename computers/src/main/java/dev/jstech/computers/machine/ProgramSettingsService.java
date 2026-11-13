/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.KernelDef;
import dev.jstech.computers.os.OsCapability;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.os.ShellFamily;
import dev.jstech.computers.os.UnixTree;
import dev.jstech.computers.os.fs.SystemLayout;
import dev.jstech.computers.program.cli.ICliComputer;
import dev.jstech.computers.vm.program.ProgramEntry;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * A program's own settings, kept in a file on the system disk the way its system keeps them
 * (computers/docs/design/sigma.md): Frames in its registry, MC-DOS in an INI file, a Unix program in ~/.config and a
 * service in /etc, Quince OS X in the Library's Preferences. They weigh like any file, and the system's own tools read
 * them: the registry with type, a ~/.config file with cat.
 */
public final class ProgramSettingsService {

    private final IOsHost machine;
    private final FileService files;

    /** What a setting is kept under when the program has no name to give. */
    private static final String UNNAMED = "Sigma";
    /** The letters of a name DOS keeps, before its ending. */
    private static final int DOS_NAME_MOST = 8;
    /* The letters and digits a cut name's own two marks are made of. */
    private static final int SHORT_MARKS = 36;
    private static final String SYSTEM_DISK = "C:/";
    /**
     * The registry's file under Frames' own folder: the SOFTWARE hive, which holds HKEY_LOCAL_MACHINE\SOFTWARE and has
     * no ending, as on the real system.
     */
    private static final String REGISTRY_HIVE = "/System32/config/SOFTWARE";
    /** The first character past plain ASCII, which a file name of every system takes. */
    private static final char ASCII_END = 128;

    /** Where a program's settings are kept on the machine, and the way they are written there. */
    public record Place(String path, SettingsFormat format) {
    }

    ProgramSettingsService(final IOsHost machine, final FileService files) {
        this.machine = machine;
        this.files = files;
    }

    /**
     * The name a program's settings are kept under: the class it was started from, without its namespace, in the
     * letters a file name takes.
     */
    public static String programName(final String caller) {
        final StringBuilder name = new StringBuilder();
        for (final char c : caller.substring(caller.lastIndexOf('.') + 1).toCharArray()) {
            if ((Character.isLetterOrDigit(c) && c < ASCII_END) || c == '.' || c == '_' || c == '-') {
                name.append(c);
            }
        }
        return name.isEmpty() ? UNNAMED : name.toString();
    }

    /**
     * Where the settings of {@code program} are kept on this machine's system, a program that stays up as a service
     * keeping them in /etc on a Unix; null on a machine with no system.
     */
    @Nullable
    public Place placeOf(final String program, final boolean service) {
        final OsDef os = this.machine.installedOs();
        if (os == null) {
            return null;
        }
        final KernelDef kernel = OsRegistry.getKernel(os.kernelId());
        final ShellFamily family = kernel == null ? ShellFamily.DOS : kernel.shellFamily();
        if (os.platform() == Platform.QUINCE) {
            final UnixTree tree = UnixTree.of(os.platform(), os.familyRank());
            return tree == UnixTree.QUINCE_X
                    ? new Place(SYSTEM_DISK + tree.homePath() + "/Library/Preferences/" + program + ".plist",
                    SettingsFormat.PLIST)
                    : new Place(SYSTEM_DISK + "System Folder/Preferences/" + program + " Preferences",
                    SettingsFormat.PLAIN);
        }
        return switch (family) {
            case POSIX -> service ? new Place(SYSTEM_DISK + "etc/" + program + ".conf", SettingsFormat.PLAIN)
                    : new Place(SYSTEM_DISK + UnixTree.of(os.platform(), os.familyRank()).homePath() + "/.config/"
                    + program + "/settings", SettingsFormat.PLAIN);
            case DOS -> os.capability() == OsCapability.FULL_DESKTOP
                    ? new Place(SYSTEM_DISK + SystemLayout.SYSTEM_DIR + REGISTRY_HIVE, SettingsFormat.REGISTRY)
                    : new Place(SYSTEM_DISK + dosName(program) + ".INI", SettingsFormat.INI);
            case NET -> new Place(SYSTEM_DISK + program.toLowerCase(Locale.ROOT) + ".cfg", SettingsFormat.PLAIN);
        };
    }

    /** The settings of {@code program} kept at {@code place}; none when the file is not there. */
    public Map<String, String> read(final Place place, final String program) {
        final ICliComputer.FsResult read = this.files.readFile(place.path());
        return place.format().read(read.ok() ? read.message().english() : "", program);
    }

    /**
     * Keeps {@code settings} as everything {@code program} has at {@code place}, making the folders on the way; false
     * when the disk refused the file.
     */
    public boolean write(final Place place, final String program, final Map<String, String> settings) {
        final ICliComputer.FsResult read = this.files.readFile(place.path());
        final String before = read.ok() ? read.message().english() : "";
        final int last = place.path().lastIndexOf('/');
        if (last > SYSTEM_DISK.length() && !this.makeFolders(place.path().substring(0, last))) {
            return false;
        }
        return this.files.write(place.path(), place.format().write(before, program, settings));
    }

    /** Whether the program the machine lists under {@code id} stays up, as a service does. */
    public boolean isService(final int id) {
        final MachinePrograms programs = this.machine.programs();
        final ProgramEntry<IMachineRuntime> entry = programs == null || id <= 0 ? null : programs.byId(id);
        return entry != null && entry.process().isService();
    }

    /** {@code place}'s path as the machine's system writes paths, for a program to show. */
    public String shown(final Place place) {
        return FileService.shown(this.machine, place.path());
    }

    /* Makes every folder of {@code path} that is not there yet; false when one could not be made. */
    private boolean makeFolders(final String path) {
        final List<String> parts = List.of(path.substring(SYSTEM_DISK.length()).split("/"));
        String folder = SYSTEM_DISK.substring(0, SYSTEM_DISK.length() - 1);
        for (final String part : parts) {
            folder = folder + "/" + part;
            if (!this.files.folderExists(folder) && !this.files.makeFolder(folder)) {
                return false;
            }
        }
        return true;
    }

    /*
     * The program's name as DOS keeps a file name: eight letters at most, in capitals. A longer one is cut as the
     * systems of the time cut names, its first letters, a tilde and two of its own, so two programs whose names begin
     * alike never share a file.
     */
    private static String dosName(final String program) {
        final String letters = program.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        final String name = letters.isEmpty() ? UNNAMED.toUpperCase(Locale.ROOT) : letters;
        if (name.length() <= DOS_NAME_MOST) {
            return name;
        }
        final String own = Integer.toString(Math.floorMod(program.hashCode(), SHORT_MARKS * SHORT_MARKS),
                SHORT_MARKS).toUpperCase(Locale.ROOT);
        return name.substring(0, DOS_NAME_MOST - 3) + "~" + (own.length() < 2 ? "0" + own : own);
    }
}
