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
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;

/**
 * What the last steps of an installation say: the bootloader going on, and its list being written.
 *
 * <p>Neither says much, and how little each says is what they sound like. The bootloader's installer prints
 * one line, works for a moment, and prints another. Its configuration generator names every image it finds,
 * which is the only way anybody learns that their new system is missing from the menu because the kernel is
 * not where the generator looks.
 */
@TextHolder
public final class BootVoices {

    private static final TextKey INSTALLING_FOR =
            TextKey.of("jsc.install.boot_voices.installing_for", "Installing for %s platform.");
    private static final TextKey INSTALLATION_FINISHED = TextKey.of("jsc.install.boot_voices.installation_finished",
            "Installation finished. No error reported.");
    private static final TextKey GENERATING_CONFIG =
            TextKey.of("jsc.install.boot_voices.generating_config", "Generating grub configuration file ...");
    private static final TextKey FOUND_LINUX =
            TextKey.of("jsc.install.boot_voices.found_linux", "Found linux image: %s");
    private static final TextKey FOUND_INITRD =
            TextKey.of("jsc.install.boot_voices.found_initrd", "Found initrd image: %s");
    /* The generator's warning, a line to a key as it is broken over the terminal. */
    private static final TextKey OS_PROBER_1 = TextKey.of("jsc.install.boot_voices.os_prober_1",
            "Warning: os-prober will not be executed to detect other bootable");
    private static final TextKey OS_PROBER_2 = TextKey.of("jsc.install.boot_voices.os_prober_2",
            "partitions. Systems on them will not be added to the GRUB boot");
    private static final TextKey OS_PROBER_3 = TextKey.of("jsc.install.boot_voices.os_prober_3",
            "configuration. Check GRUB_DISABLE_OS_PROBER documentation entry.");
    private static final TextKey FIRMWARE_ENTRY = TextKey.of("jsc.install.boot_voices.firmware_entry",
            "Adding boot menu entry for UEFI Firmware Settings ...");
    private static final TextKey DONE = TextKey.of("jsc.install.boot_voices.done", "done");

    private BootVoices() {
    }

    /**
     * Installing the bootloader.
     *
     * @param platform the firmware it is installed for, as the tool names it
     */
    public static TtyScript grubInstall(final String platform, final int ticks, final Runnable installed) {
        return TtyScript.script()
                .say(INSTALLING_FOR.with(platform))
                .pause(ticks)
                .effect(installed)
                .say(INSTALLATION_FINISHED)
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
                .say(GENERATING_CONFIG)
                .pause(Math.max(1, ticks * 35 / 100))
                .say(FOUND_LINUX.with(kernel))
                .pause(Math.max(1, ticks * 15 / 100))
                .say(FOUND_INITRD.with(initrd))
                .pause(Math.max(1, ticks * 20 / 100))
                .sayAll(List.of(CliLine.plain(OS_PROBER_1.text()), CliLine.plain(OS_PROBER_2.text()),
                        CliLine.plain(OS_PROBER_3.text())))
                .pause(Math.max(1, ticks * 30 / 100));
        if (uefi) {
            script.say(FIRMWARE_ENTRY);
        }
        return script.effect(written).say(DONE).done();
    }
}
