/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.program.MessengerLog;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * The Messenger Service as a machine holds it: whether it is installed, and the conversations it keeps.
 *
 * <p>Unlike every other service here it does not cost a fixed amount. It grows on the disk as it keeps
 * what people said, and in memory as more of them have the messenger open, so a busy chat on a small
 * machine really is expensive. That is the mechanic, and it is the shape the next cycle's engines want.
 *
 * <p>The rules and the arithmetic live in the pure {@link MessengerLog}; this class is what writes them to
 * a disk and back.
 */
final class MessengerService {

    private final MainframeBlockEntity mainframe;
    private final MessengerLog log = new MessengerLog();

    private boolean installed;
    private boolean running = true;

    MessengerService(final MainframeBlockEntity mainframe) {
        this.mainframe = mainframe;
    }

    MessengerLog log() {
        return log;
    }

    boolean installed() {
        return installed;
    }

    boolean running() {
        return running;
    }

    /** Usable only when installed, not stopped, and the machine holding it is actually powered. */
    boolean active() {
        return installed && running && mainframe.isRunning();
    }

    /** Installs the service; false when it was already on. */
    boolean install() {
        if (installed) {
            return false;
        }
        installed = true;
        running = true;
        mainframe.setChanged();
        return true;
    }

    /** Takes it off. What it kept goes with it, because a conversation nobody can reach is not kept. */
    boolean uninstall() {
        if (!installed) {
            return false;
        }
        installed = false;
        log.clear();
        mainframe.setChanged();
        return true;
    }

    boolean setRunning(final boolean value) {
        if (!installed || running == value) {
            return false;
        }
        running = value;
        if (!value) {
            /*
             * A stopped service holds nobody: the people who had it open are not connected to something
             * that is not running, and its memory cost has to fall to its floor the moment it stops.
             */
            for (final String who : log.connected()) {
                log.disconnect(who);
            }
        }
        mainframe.setChanged();
        return true;
    }

    /** Keeps what somebody said and marks the machine, because the history is part of what it holds. */
    boolean say(final String room, final String from, final String text,
                final long at, final boolean nudge) {
        if (!active() || log.say(room, from, text, at, nudge) == null) {
            return false;
        }
        mainframe.setChanged();
        return true;
    }

    void save(final CompoundTag tag) {
        tag.putBoolean("MessengerInstalled", installed);
        tag.putBoolean("MessengerRunning", running);
        final List<MessengerLog.Message> kept = log.all();
        if (kept.isEmpty()) {
            return;
        }
        final ListTag list = new ListTag();
        for (final MessengerLog.Message message : kept) {
            final CompoundTag entry = new CompoundTag();
            entry.putString("Room", message.room());
            entry.putString("From", message.from());
            entry.putString("Text", message.text());
            entry.putLong("At", message.at());
            if (message.nudge()) {
                entry.putBoolean("Nudge", true);
            }
            list.add(entry);
        }
        tag.put("MessengerLog", list);
    }

    void load(final CompoundTag tag) {
        installed = tag.getBoolean("MessengerInstalled");
        running = !tag.contains("MessengerRunning") || tag.getBoolean("MessengerRunning");
        final ListTag list = tag.getList("MessengerLog", Tag.TAG_COMPOUND);
        final List<MessengerLog.Message> kept = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            final CompoundTag entry = list.getCompound(i);
            final String from = entry.getString("From");
            final String room = entry.getString("Room");
            if (from.isBlank() || room.isBlank()) {
                continue; // a line somebody edited into nonsense is dropped rather than crashing the load
            }
            kept.add(new MessengerLog.Message(room, from, entry.getString("Text"),
                    entry.getLong("At"), entry.getBoolean("Nudge")));
        }
        log.restore(kept);
    }
}
