/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import dev.jstech.computers.advancement.JscEvents;
import dev.jstech.computers.os.ConsoleIdentity;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.ShellFamily;
import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliStyle;
import dev.jstech.computers.program.cli.DosPath;
import dev.jstech.computers.program.cli.PosixPath;
import dev.jstech.computers.program.install.LiveInstallState;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * A shell's files: where it stands, the prompt that says so, and the drives it reads and writes. The layer of
 * {@link ServerCliComputer} over the machine itself.
 */
abstract class ServerCliFiles extends ServerCliShell {

    protected ServerCliFiles(final IComputerTerminalHost host, final ServerLevel level,
                             @Nullable final ServerPlayer typist) {
        super(host, level, typist);
    }

    @Override
    public String prompt() {
        final LiveInstallState live = liveInstall();
        if (live != null) {
            return live.prompt();
        }
        /*
         * A flat disk has no path to put in a prompt, so MC-NET's names the machine instead of a place. It
         * is also why it carries no drive letter: there is one store and nowhere in it to stand.
         */
        if (shellFamily() == ShellFamily.NET) {
            return "SYSTEM:>";
        }
        if (shellFamily() != ShellFamily.POSIX) {
            return currentLocation().dosPath() + ">";
        }
        final OsDef os = hostBlock instanceof IOsHost c ? c.installedOs() : null;
        return ConsoleIdentity.promptOf(os == null ? null : os.platform(), os == null ? null : os.shell(),
                hostname(), PosixPath.renderForPrompt(tree(), currentLocation()));
    }

    /** A live medium's prompt is in its shell's own colours, and so is FreeBSD's; every other one is in one. */
    @Override
    public CliLine promptLine() {
        final LiveInstallState live = liveInstall();
        if (live != null) {
            return live.promptLine();
        }
        final OsDef os = hostBlock instanceof IOsHost c ? c.installedOs() : null;
        if (shellFamily() == ShellFamily.POSIX && os != null) {
            return ConsoleIdentity.promptLineOf(os.platform(), os.shell(), hostname(),
                    PosixPath.renderForPrompt(tree(), currentLocation()));
        }
        return new CliLine(prompt(), CliStyle.ACCENT);
    }

    @Override
    public DosPath.Location currentLocation() {
        final ComputerConsoleState console = host.console();
        if (console == null) {
            return DosPath.Location.root('C');
        }
        // A fresh POSIX session starts in the home directory, a DOS one at the drive root.
        return console.locations().where(session,
                shellFamily() == ShellFamily.POSIX ? PosixPath.home(tree()) : null);
    }

    @Override
    public void setCurrentLocation(final DosPath.Location location) {
        final ComputerConsoleState console = host.console();
        if (console != null) {
            console.locations().moveTo(session, location);
        }
    }

    @Override
    public FsResult changeDir(final String input) {
        return files().changeDir(input, this::setCurrentLocation);
    }

    @Override
    public FsResult changeDrive(final char drive) {
        return files().changeDrive(drive, letter -> {
            final ComputerConsoleState console = host.console();
            if (console != null) {
                console.locations().setDrive(letter);
            }
        });
    }

    @Override
    public List<MountInfo> mounts() {
        return files().mounts();
    }

    @Override
    public List<String> fileNames() {
        // Named the way a listing names the folder the prompt stands in: with nothing, which is "here".
        return files().list("");
    }

    @Override
    public FsResult listDisk(final String dir) {
        return files().listDisk(dir);
    }

    @Override
    public FsResult readFile(final String path) {
        return files().readFile(path);
    }

    @Override
    public FsResult deleteFile(final String path) {
        return files().deleteFile(path);
    }

    @Override
    public FsResult runScript(final String path) {
        return iql().runFile(path);
    }

    @Override
    public FsResult writeFile(final String path, final String content) {
        return files().writeFile(path, content);
    }

    @Override
    public FsResult appendFile(final String path, final String content) {
        return files().appendFile(path, content);
    }

    @Override
    public FsResult makeDir(final String path) {
        return files().makeDir(path);
    }

    @Override
    public FsResult removeDir(final String path) {
        return files().removeDir(path);
    }

    @Override
    public FsResult copyPath(final String src, final String dest) {
        return files().copyPath(src, dest);
    }

    @Override
    public FsResult movePath(final String src, final String destDir) {
        return files().movePath(src, destDir);
    }

    @Override
    public FsResult renamePath(final String src, final String newName) {
        return files().renamePath(src, newName);
    }

    @Override
    public OpResult formatDrive(final char letterRaw) {
        final boolean heldSystem = files().holdsSystem(letterRaw);
        final OpResult result = files().formatDrive(letterRaw);
        if (heldSystem && result.ok()) {
            report(JscEvents.SYSTEM_ERASED, "");
        }
        return result;
    }
}
