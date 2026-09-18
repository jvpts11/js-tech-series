/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.cli;

import dev.jstech.computers.program.install.LiveInstallState;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The shell of a live medium against the sequence behind it.
 *
 * <p>The shell turns an unlisted word away before the sequence ever sees it, so a verb the sequence answers
 * and the shell does not register is a verb nobody can type. That is what happened to {@code echo} and
 * {@code pacman}: both were answered, neither could be reached, so the line that sets the build options and
 * the one that installs a package into the new system were both unreachable. These hold the two together
 * from each side.
 */
class LiveInstallCommandsTest {

    /** How far the clock moves between one step of a prelude and the next, past any fetch they wait on. */
    private static final long APART = 4_000L;

    private long clock;

    private LiveInstallState.Env env() {
        this.clock += APART;
        return new LiveInstallState.Env(
                List.of(new LiveInstallState.Device("sda", 20_480)), true, this.clock, 4, 2000, 4, false);
    }

    private static Set<String> registered() {
        final Set<String> names = new HashSet<>();
        for (final ICliCommand command : LiveInstallCommands.all()) {
            names.add(command.name());
        }
        return names;
    }

    /** The steps that carry a session from the live medium into the new system, for that distribution. */
    private static List<String> intoTheChroot(final LiveInstallState.Distro distro) {
        if (distro == LiveInstallState.Distro.ARCH) {
            return List.of("mkfs.ext4 /dev/sda", "mount /dev/sda /mnt", "pacstrap /mnt base linux",
                    "genfstab -U /mnt >> /mnt/etc/fstab", "arch-chroot /mnt");
        }
        return List.of("mkfs.ext4 /dev/sda", "mount /dev/sda /mnt",
                "tar xpf stage3-amd64.tar.xz -C /mnt", "chroot /mnt");
    }

    /**
     * Whether any distribution answers this word from anywhere the player can stand.
     *
     * <p>Both sides of that matter. "Command not found" is a real answer here, not only the answer to a
     * stranger: a verb of the wrong distribution gets it, and so does a tool that lives inside the system
     * being built, since {@code grub-mkconfig} is not on the medium. So a word counts as answered if either
     * distribution takes it either on the medium or inside the chroot, and a word nothing takes anywhere is
     * a word in the list that no shell can ever run.
     */
    private boolean answered(final String verb) {
        for (final LiveInstallState.Distro distro : LiveInstallState.Distro.values()) {
            final LiveInstallState onMedium = new LiveInstallState(distro);
            if (!notFound(onMedium.run(verb, env()))) {
                return true;
            }
            final LiveInstallState inSystem = new LiveInstallState(distro);
            for (final String step : intoTheChroot(distro)) {
                inSystem.run(step, env());
            }
            if (!notFound(inSystem.run(verb, env()))) {
                return true;
            }
        }
        return false;
    }

    private static boolean notFound(final LiveInstallState.Result result) {
        return !result.lines().isEmpty() && result.lines().get(0).contains("command not found");
    }

    @Test
    void all_registersEveryVerbTheSequenceAnswers() {
        final Set<String> names = registered();
        for (final String verb : LiveInstallState.VERBS) {
            assertTrue(names.contains(verb),
                    verb + " is answered by the install sequence but no command carries it to the shell");
        }
    }

    @Test
    void all_registersNothingTheSequenceTurnsAway() {
        for (final String verb : LiveInstallState.VERBS) {
            assertTrue(answered(verb), verb + " is offered by the shell and answered by neither distribution");
        }
    }

    @Test
    void all_reachesEchoAndPacman() {
        final Set<String> names = registered();
        assertTrue(names.contains("echo"), "the line that sets the build options has to be typeable");
        assertTrue(names.contains("pacman"), "a package installed after the base system has to be typeable");
    }

    /** Only clear rides along beyond the sequence's own verbs, so the shell offers nothing invented here. */
    @Test
    void all_addsOnlyClearBeyondTheSequence() {
        final Set<String> extra = new HashSet<>(registered());
        LiveInstallState.VERBS.forEach(extra::remove);
        assertEquals(Set.of("clear"), extra);
    }
}
