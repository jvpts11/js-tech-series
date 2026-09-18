/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program.install.voice;

import dev.jstech.computers.program.cli.CliLine;
import dev.jstech.computers.program.tty.TtyScript;
import java.util.List;

/**
 * What the last steps of an installation say: the bootloader going on, its list being written, and the
 * password being set.
 *
 * <p>None of them says much, and how little each says is what they sound like. The bootloader's installer
 * prints one line, works for a moment, and prints another. Its configuration generator names every image it
 * finds, which is the only way anybody learns that their new system is missing from the menu because the
 * kernel is not where the generator looks. The password tool asks twice and shows nothing of either answer.
 */
public final class BootVoices {

    private BootVoices() {
    }

    /**
     * Installing the bootloader.
     *
     * @param platform the firmware it is installed for, as the tool names it
     */
    public static TtyScript grubInstall(final String platform, final int ticks, final Runnable installed) {
        return TtyScript.script()
                .say("Installing for " + platform + " platform.")
                .pause(ticks)
                .effect(installed)
                .say("Installation finished. No error reported.")
                .done();
    }

    /**
     * Writing the bootloader's list of what it can start.
     *
     * @param kernel the kernel image it finds
     * @param initrd the boot image it finds beside it
     * @param uefi   whether the firmware has a setup of its own worth an entry on the menu
     */
    public static TtyScript grubMkconfig(final String kernel, final String initrd, final boolean uefi,
                                        final int ticks, final Runnable written) {
        final TtyScript.Builder script = TtyScript.script()
                .say("Generating grub configuration file ...")
                .pause(Math.max(1, ticks * 35 / 100))
                .say("Found linux image: " + kernel)
                .pause(Math.max(1, ticks * 15 / 100))
                .say("Found initrd image: " + initrd)
                .pause(Math.max(1, ticks * 20 / 100))
                .sayAll(List.of(
                        CliLine.plain("Warning: os-prober will not be executed to detect other bootable"),
                        CliLine.plain("partitions. Systems on them will not be added to the GRUB boot"),
                        CliLine.plain("configuration. Check GRUB_DISABLE_OS_PROBER documentation entry.")))
                .pause(Math.max(1, ticks * 30 / 100));
        if (uefi) {
            script.say("Adding boot menu entry for UEFI Firmware Settings ...");
        }
        return script.effect(written).say("done").done();
    }

    /**
     * Setting a password: asked for twice, and neither answer shown.
     *
     * <p>Two that do not match are refused in the tool's own words and nothing is set.
     */
    public static TtyScript passwd(final Runnable set) {
        final String[] first = {""};
        return TtyScript.script()
                .askUnseen(CliLine.plain("New password: "), answer -> {
                    first[0] = answer;
                    return null;
                })
                .askUnseen(CliLine.plain("Retype new password: "), answer -> answer.equals(first[0])
                        ? TtyScript.script().effect(set).say("passwd: password updated successfully").done()
                        : TtyScript.script().say("Sorry, passwords do not match.")
                                .say("passwd: Authentication token manipulation error")
                                .say("passwd: password unchanged").done())
                .done();
    }
}
