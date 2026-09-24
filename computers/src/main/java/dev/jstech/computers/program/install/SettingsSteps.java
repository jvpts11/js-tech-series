/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/**
 * What both distributions settle the same way once somebody is inside the new system: what the machine is
 * called, and the hardware clock.
 */
@TextHolder
final class SettingsSteps {

    private final LiveInstallState.Distro distro;
    private final LiveFiles files;
    private final LiveProgress progress;

    /** What a machine may be called: a letter or a digit, then up to fourteen more of those or hyphens. */
    private static final String A_NAME = "[A-Za-z0-9][A-Za-z0-9-]{0,14}";

    private static final TextKey NOT_ON_THE_MEDIUM = TextKey.of("jsc.install.settings_steps.not_on_the_medium",
            "hostname: you cannot change the host name of the live medium");
    private static final TextKey INVALID_NAME = TextKey.of("jsc.install.settings_steps.invalid_name",
            "hostname: the specified hostname is invalid");

    SettingsSteps(final LiveInstallState.Distro distro, final LiveFiles files, final LiveProgress progress) {
        this.distro = distro;
        this.files = files;
        this.progress = progress;
    }

    /** The host name the live medium itself reports. */
    String mediumName() {
        return this.distro == LiveInstallState.Distro.ARCH ? "archiso" : "livecd";
    }

    /**
     * The name the player gave the machine while installing it, empty when they gave none.
     *
     * <p>Given with the tool for it or written into the file that holds it, which is the same thing done two
     * ways and both are how people do it.
     */
    String chosenName() {
        if (!this.progress.chosenName.isEmpty()) {
            return this.progress.chosenName;
        }
        final String written = this.files.read(this.files.inNewSystem("/etc/hostname"));
        return written != null && written.trim().matches(A_NAME) ? written.trim() : "";
    }

    /** Names the machine, written into the new system's own file so it is there to read back and carry over. */
    LiveTurn hostname(final String[] parts) {
        if (!this.files.inside()) {
            return LiveTurn.refused(NOT_ON_THE_MEDIUM.text());
        }
        if (parts.length > 2) {
            return LiveTurn.refused(INVALID_NAME.text());
        }
        final String name = parts.length > 1 ? parts[1].trim() : "";
        if (name.isEmpty()) {
            final String chosen = this.chosenName();
            return LiveTurn.said(chosen.isEmpty() ? this.mediumName() : chosen);
        }
        if (!name.matches(A_NAME)) {
            return LiveTurn.refused(INVALID_NAME.text());
        }
        this.files.write(this.files.inNewSystem("/etc/hostname"), name);
        this.progress.chosenName = name;
        return LiveTurn.silent();
    }

    /** Sets the hardware clock from the system's, without a word, as it does. */
    LiveTurn setClock() {
        this.progress.clockSet = true;
        return LiveTurn.silent();
    }
}
