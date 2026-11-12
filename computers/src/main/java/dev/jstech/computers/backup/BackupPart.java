/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.backup;

import dev.jstech.core.id.IStableId;
import dev.jstech.core.id.IStableName;
import java.util.EnumSet;
import java.util.Set;

/**
 * What a backup can keep: the network's items, the files in its computers' home folders, their settings, the network's
 * {@code .craft} patterns, the IQL and Sigma# scripts, the computers' system images and their Teracoin wallets
 * (computers/docs/design/programs.md). Each has the id it travels by, a bit of a mask, and the name it is saved by.
 */
public enum BackupPart implements IStableId, IStableName {

    /** What the network's storage holds, given back only as far as it stopped existing since. */
    ITEMS(0, "items"),
    /** The files in every computer's home folder. */
    FILES(1, "files"),
    /** Every computer's settings. */
    SETTINGS(2, "settings"),
    /** The bench recipes of the network's Crafting Computers and the machine recipes of their interfaces. */
    PATTERNS(3, "patterns"),
    /** The IQL and Sigma# scripts on every computer, and the Mainframe's saved views, procedures and jobs. */
    SCRIPTS(4, "scripts"),
    /** Every computer's system: what it runs, its system files and its console, which puts a corrupted one right. */
    IMAGES(5, "images"),
    /**
     * Every computer's Teracoin wallets, wherever on its system's disk they are kept: each wallet is a file that holds
     * the key of its address, and a lost one takes its coins with it (computers/docs/design/teracoin.md).
     */
    WALLETS(6, "wallets");

    private final int id;
    private final String serializedName;

    BackupPart(final int id, final String serializedName) {
        this.id = id;
        this.serializedName = serializedName;
    }

    @Override
    public int id() {
        return id;
    }

    @Override
    public String serializedName() {
        return serializedName;
    }

    /** The parts as one number, a bit each, as they travel to a console and back. */
    public static int mask(final Set<BackupPart> parts) {
        int mask = 0;
        for (final BackupPart part : parts) {
            mask |= 1 << part.id;
        }
        return mask;
    }

    /** The parts a number names, a bit each. */
    public static Set<BackupPart> of(final int mask) {
        final Set<BackupPart> parts = EnumSet.noneOf(BackupPart.class);
        for (final BackupPart part : values()) {
            if ((mask & 1 << part.id) != 0) {
                parts.add(part);
            }
        }
        return parts;
    }
}
