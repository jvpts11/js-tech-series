/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install;

import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.cli.CliSpan;
import dev.jstech.computers.program.cli.CliStyle;

/**
 * A live medium's prompt, in the words and the colours each medium's shell really gives it.
 *
 * <p>Gentoo's root prompt is the machine's name in red and the rest in blue, inside the new system as much as
 * outside it, where it is marked as a chroot. Arch's medium runs a shell that puts only the user in red, and
 * the shell of the system it is building has not been given any colours yet, so in there the prompt is plain
 * and in the brackets a bare shell uses.
 */
final class LivePrompt {

    private LivePrompt() {
    }

    /**
     * @param inside whether the session has gone into the system it is building
     * @param where  where the session is standing, the way a prompt writes it
     */
    static CliLine of(final LiveInstallState.Distro distro, final boolean inside, final String where) {
        if (distro == LiveInstallState.Distro.ARCH) {
            return inside
                    ? new CliLine("[root@archiso " + where + "]#", CliStyle.PROMPT)
                    : CliLine.of(new CliSpan("root", CliStyle.ERROR),
                            new CliSpan("@archiso " + where + " #", CliStyle.PROMPT));
        }
        final CliLine.Builder prompt = CliLine.build();
        if (inside) {
            prompt.add("(chroot) ", CliStyle.PROMPT);
        }
        return prompt.add("livecd", CliStyle.ERROR).add(" " + where + " #", CliStyle.BLUE).done();
    }
}
