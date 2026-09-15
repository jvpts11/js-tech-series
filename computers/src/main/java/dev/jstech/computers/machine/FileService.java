/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.program.cli.ICliComputer;
import java.util.ArrayList;
import java.util.List;

/**
 * A machine's drives, as what runs on the machine reaches them.
 *
 * <p>It goes through the same door the shell does, so a path means the same thing to a program as it does at the
 * prompt, and a file written by one is the file the other opens.
 */
public final class FileService {

    private final ICliComputer door;

    FileService(final ICliComputer door) {
        this.door = door;
    }

    /** Whether there is a file to read at that path. */
    public boolean exists(final String path) {
        return this.door.readFile(path).ok();
    }

    /** What the file at that path holds, or why it could not be read. */
    public ICliComputer.FsResult read(final String path) {
        return this.door.readFile(path);
    }

    /** Puts that text in the file at that path in place of what it held; false when it could not be written. */
    public boolean write(final String path, final String text) {
        return this.door.writeFile(path, text).ok();
    }

    /** Adds that text at the end of the file at that path, which starts out empty when there is none. */
    public boolean append(final String path, final String text) {
        final ICliComputer.FsResult had = this.door.readFile(path);
        return this.door.writeFile(path, (had.ok() ? had.message() : "") + text).ok();
    }

    /** Deletes the file at that path; false when it could not be deleted. */
    public boolean delete(final String path) {
        return this.door.deleteFile(path).ok();
    }

    /** Makes a folder at that path; false when it could not be made. */
    public boolean makeDir(final String path) {
        return this.door.makeDir(path).ok();
    }

    /**
     * The names in the folder at that path, or none when it cannot be read.
     *
     * <p>A name is the whole last segment of its path, extension included, so a name handed back is one that can be
     * opened as it is. A folder's ends in a slash, because whatever walks a tree has to tell which is which, and trying
     * to open each one to find out would be a poor answer.
     */
    public List<String> list(final String path) {
        final ICliComputer.FsResult listing = this.door.listDisk(path);
        final List<String> names = new ArrayList<>();
        if (listing.ok()) {
            for (final ICliComputer.FsEntry entry : listing.entries()) {
                names.add(entry.isDir() ? entry.name() + "/" : entry.name());
            }
        }
        return names;
    }
}
