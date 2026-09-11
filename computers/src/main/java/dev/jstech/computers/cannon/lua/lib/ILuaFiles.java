/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.cannon.lua.lib;

import java.util.List;

/**
 * The disks of the machine a Lua program runs on, as its {@code fs} library reaches them.
 *
 * <p>Paths are ComputerCraft's: relative to the machine's root, parts joined with {@code /}. What goes
 * wrong comes back as the words to raise, and null means it went right.
 */
public interface ILuaFiles {

    /** One thing in a folder, or the thing at a path. */
    record Entry(String name, boolean directory, long size, boolean readOnly, long modified) {
    }

    /** What is in a folder; null when the path is not a folder. */
    List<Entry> list(String path);

    /** What is at a path; null when nothing is. */
    Entry stat(String path);

    /** A file's text; null when there is no such file. */
    String read(String path);

    String write(String path, String text);

    String makeDir(String path);

    /** Takes away a file, or an empty folder. */
    String delete(String path);

    long free(String path);

    long capacity(String path);
}
