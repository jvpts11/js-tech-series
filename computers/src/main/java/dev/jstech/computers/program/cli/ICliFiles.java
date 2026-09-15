/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import java.util.List;

/**
 * What a command reaches of a computer's drives: where the terminal session stands, the files and folders on the drives
 * the computer sees, and the drives themselves.
 *
 * <p>Every member answers as a computer with no system disk would, so a computer made for a test writes only the files
 * its commands read.
 */
public interface ICliFiles {

    /**
     * This terminal session's current drive and directory, used by the DOS command line to resolve relative paths
     * and to draw the {@code C:\DIR>} prompt. Defaults to the boot drive's root; the server persists it per terminal.
     */
    default DosPath.Location currentLocation() {
        return DosPath.Location.root('C');
    }

    /** Sets the session's current drive and directory (used by {@code cd} and drive changes). */
    default void setCurrentLocation(final DosPath.Location location) {
    }

    /** The prompt to show for the current location, in the installed shell's style ({@code C:\>} by default). */
    default String prompt() {
        return currentLocation().dosPath() + ">";
    }

    /**
     * Changes the shell's current directory. {@code input} is a DOS path relative to the current
     * location (or absolute); implementations resolve it, verify the target directory exists, and
     * persist the new location. A blank input or {@code \} means the drive root.
     *
     * @param input the DOS path to change to
     * @return a confirmation, or a failure when the path does not exist
     */
    default ICliComputer.FsResult changeDir(final String input) {
        return ICliComputer.FsResult.noOs();
    }

    /**
     * Switches the shell's current drive to {@code drive}, restoring that drive's remembered current
     * directory. Fails when the drive letter is not mapped to any installed disk or linked medium.
     *
     * @param drive the drive letter (case-insensitive)
     * @return a confirmation, or a failure when the drive does not exist or is not ready
     */
    default ICliComputer.FsResult changeDrive(final char drive) {
        return ICliComputer.FsResult.noOs();
    }

    /**
     * Lists the files visible in the current directory of the host computer's system disk.
     *
     * <p>Returns a result whose {@link ICliComputer.FsResult#entries()} carries the listing, or a failure
     * message when no system disk or OS is present. The directory parameter follows the filesystem
     * kind of the installed OS kernel: {@code ""} for the root in FLAT and HIERARCHICAL.
     *
     * @param dir the directory to list ({@code ""} for the root)
     */
    default ICliComputer.FsResult listDisk(final String dir) {
        return ICliComputer.FsResult.noOs();
    }

    /**
     * Reads the content of a file on the host computer's system disk.
     *
     * <p>Returns a result carrying the file text, or a failure message when the file is
     * absent, is a read-only {@code .dat} projection, or no system disk is present.
     *
     * @param path the file path relative to the root of the system disk
     */
    default ICliComputer.FsResult readFile(final String path) {
        return ICliComputer.FsResult.noOs();
    }

    /**
     * Writes (creates or overwrites) a user file on the host computer's system disk.
     *
     * <p>Returns a result with a confirmation message on success, or a failure message when the
     * path is invalid for the filesystem, the file type is not user-editable, the disk is full, or no
     * system disk is present.
     *
     * @param path    the file path to write
     * @param content the UTF-8 text content
     */
    default ICliComputer.FsResult writeFile(final String path, final String content) {
        return ICliComputer.FsResult.noOs();
    }

    /**
     * Adds text at the end of a user file on the host computer's system disk, making the file when there is none,
     * without reading what it already holds.
     *
     * <p>Fails as {@link #writeFile} does, except that only what the file grows by has to fit on the disk.
     *
     * @param path    the file path to add to
     * @param content the UTF-8 text to add
     */
    default ICliComputer.FsResult appendFile(final String path, final String content) {
        return ICliComputer.FsResult.noOs();
    }

    /**
     * Deletes a file from the host computer's system disk.
     *
     * <p>Returns a result with a confirmation message on success, or a failure message when the
     * file does not exist, is a read-only {@code .dat} projection, or no system disk is present.
     *
     * @param path the file path to delete
     */
    default ICliComputer.FsResult deleteFile(final String path) {
        return ICliComputer.FsResult.noOs();
    }

    /**
     * Runs the content of an {@code .iql} file from the host computer's system disk as an IQL
     * statement, routing it through the same dispatch path as the {@code operation} command.
     *
     * <p>Returns a failure when the file is absent, is not an {@code .iql} file, or no system disk
     * is present. On success returns the same result the engine would have returned.
     *
     * @param path the file path of the IQL script to execute
     */
    default ICliComputer.FsResult runScript(final String path) {
        return ICliComputer.FsResult.noOs();
    }

    /**
     * Creates a directory. {@code path} is a DOS path relative to the current location (or absolute).
     *
     * @param path the directory to create
     * @return a confirmation, or a failure message
     */
    default ICliComputer.FsResult makeDir(final String path) {
        return ICliComputer.FsResult.noOs();
    }

    /**
     * Removes a directory and everything under it. {@code path} is a DOS path relative to the current
     * location (or absolute).
     *
     * @param path the directory to remove
     * @return a confirmation, or a failure message
     */
    default ICliComputer.FsResult removeDir(final String path) {
        return ICliComputer.FsResult.noOs();
    }

    /**
     * Copies a file (or directory subtree) from {@code src} to {@code dest}. Both are DOS paths
     * relative to the current location (or absolute).
     *
     * @param src  the source path
     * @param dest the destination path
     * @return a confirmation, or a failure message
     */
    default ICliComputer.FsResult copyPath(final String src, final String dest) {
        return ICliComputer.FsResult.noOs();
    }

    /**
     * Moves a file (or directory subtree) into the directory {@code destDir}. Both are DOS paths
     * relative to the current location (or absolute).
     *
     * @param src     the source path
     * @param destDir the destination directory
     * @return a confirmation, or a failure message
     */
    default ICliComputer.FsResult movePath(final String src, final String destDir) {
        return ICliComputer.FsResult.noOs();
    }

    /**
     * Renames a file or directory {@code src} to the new leaf name {@code newName} (kept in the same
     * parent directory). {@code src} is a DOS path relative to the current location (or absolute).
     *
     * @param src     the source path
     * @param newName the new leaf name
     * @return a confirmation, or a failure message
     */
    default ICliComputer.FsResult renamePath(final String src, final String newName) {
        return ICliComputer.FsResult.noOs();
    }

    /** Every drive the shell can see (the system disk first), for {@code df}; empty when there is no OS. */
    default List<ICliComputer.MountInfo> mounts() {
        return List.of();
    }

    /**
     * Formats the drive with the given letter: erases the installed system, every file, and the item
     * storage on it. Refuses the drive the running system lives on.
     */
    default ICliComputer.OpResult formatDrive(final char letter) {
        return ICliComputer.OpResult.fail("format: drive not found");
    }
}
