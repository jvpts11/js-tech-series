/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;

/**
 * What a command reaches of a computer's settings and of the folders shared on its network: the settings and changing
 * one, the folders this computer shares, and the ones the other machines share.
 *
 * <p>Every member answers as a computer with no settings store and nothing shared would.
 */
public interface ICliConfig {

    /**
     * The current per-computer settings as {@code key   value} lines, for the {@code config} command
     * to print. An empty list means this computer has no settings store.
     *
     * @return the settings summary lines, oldest-first
     */
    default List<String> configSummary() {
        return List.of();
    }

    /**
     * Changes one setting (the computer name, the network share, or a value owned by the settings
     * store), clamping as needed. Returns a confirmation or a failure describing the problem.
     *
     * @param key   the setting key (case-insensitive; e.g. {@code name}, {@code netshare}, {@code clock})
     * @param value the raw value
     * @return the outcome of the change
     */
    default ICliComputer.OpResult setConfig(final String key, final String value) {
        return ICliComputer.OpResult.fail(ConfigWords.NO_SETTINGS);
    }

    /** The folders this computer shares, in the order they were shared. */
    default List<ICliComputer.ShareInfo> shares() {
        return List.of();
    }

    /**
     * What this computer has starred, by the id the machine stars things under.
     *
     * <p>Starring belongs to the computer and not to the player, so what was starred in the window is starred
     * at its prompt, and the other way about.
     */
    default List<String> favourites() {
        return List.of();
    }

    /**
     * Whether this world lets a computer list everything it can run in one word.
     *
     * <p>A server's choice, not a machine's, but it is asked of the machine like everything else: what a
     * command may do is decided by the computer in front of the player and by nothing the command reaches
     * round it.
     */
    default boolean listsEverything() {
        return false;
    }

    /** Every folder the other running machines on this network share. */
    default List<ICliComputer.NetworkShare> networkShares() {
        return List.of();
    }

    /** What a computer with no settings store answers. */
    @TextHolder
    final class ConfigWords {

        static final TextKey NO_SETTINGS =
                TextKey.of("jsc.cli.config.no_settings", "this computer has no settings store");

        private ConfigWords() {
        }
    }
}
