/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.term;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * What Tab does at a terminal: finishes the command being typed, or the device a command is being pointed at.
 *
 * <p>A round of Tabs goes through everything that starts with what was typed by hand, one candidate a press,
 * and comes round again. The round is measured from what the player typed and not from what the last press put
 * on the line, since the line after one press is a whole command that nothing else starts with. Typing
 * anything ends the round.
 */
public final class TermCompletion {

    private final List<String> commands = new ArrayList<>();
    private final List<String> devices = new ArrayList<>();

    /** What was on the line when the round began, which every press of the round completes over again. */
    private String stem = "";

    /** How many presses the round has had, or none when the next press begins one. */
    private int presses;

    /** The commands whose first argument is a device, for which Tab offers the drives the machine has. */
    private static final Set<String> DEVICE_VERBS =
            Set.of("fdisk", "mkfs", "mkfs.ext4", "mkfs.fat", "mkfs.vfat", "mount", "grub-install");

    /** Takes what the machine said it has: the commands its shell knows and the drives in it. */
    public void know(final List<String> commandNames, final List<String> deviceNames) {
        commands.clear();
        commands.addAll(commandNames);
        devices.clear();
        devices.addAll(deviceNames);
        presses = 0;
    }

    /** The line was changed by the player and not by a press of Tab, which ends the round. */
    public void typedByHand() {
        presses = 0;
    }

    /**
     * One press of Tab.
     *
     * @param onTheLine what the command line holds now
     * @return what it should hold instead, or nothing when nothing completes it
     */
    public Optional<String> next(final String onTheLine) {
        if (presses == 0) {
            stem = onTheLine;
        }
        final int space = stem.indexOf(' ');
        final List<String> candidates = space < 0 ? commandsFrom(stem) : devicesFrom(stem, space);
        if (candidates.isEmpty()) {
            return Optional.empty();
        }
        final String pick = candidates.get(presses % candidates.size());
        presses++;
        // The only candidate there is gets the space that says it is finished; one of several may not be.
        return Optional.of(candidates.size() == 1 ? pick + " " : pick);
    }

    private List<String> commandsFrom(final String typed) {
        final List<String> out = new ArrayList<>();
        if (typed.isEmpty()) {
            return out;
        }
        final String prefix = typed.toLowerCase(Locale.ROOT);
        for (final String name : commands) {
            if (name.startsWith(prefix)) {
                out.add(name);
            }
        }
        return out;
    }

    /** The whole line with each drive in turn as its first argument, for a command that is pointed at one. */
    private List<String> devicesFrom(final String typed, final int space) {
        final List<String> out = new ArrayList<>();
        final String verb = typed.substring(0, space).toLowerCase(Locale.ROOT);
        final String argument = typed.substring(space + 1).toLowerCase(Locale.ROOT);
        if (!DEVICE_VERBS.contains(verb) || argument.contains(" ")) {
            return out;
        }
        for (final String device : devices) {
            final String path = "/dev/" + device;
            if (path.startsWith(argument) || device.startsWith(argument)) {
                out.add(typed.substring(0, space + 1) + path);
            }
        }
        return out;
    }
}
