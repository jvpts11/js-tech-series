/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.program.KnotRepository;
import dev.jstech.computers.program.MessengerLog;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * Reading and writing what the two network services keep.
 *
 * <p>Its own class because the same two things are written in the same way wherever the service runs, and
 * because a machine that holds them should not have to know how they are spelled out.
 *
 * <p>Both are read forgivingly. A line somebody edited into nonsense is dropped rather than taken as a
 * reason to fail the whole load: a machine that will not come back because of one bad row is worse than
 * one that comes back with a row missing.
 */
final class ServiceStateNbt {

    private static final String MESSENGER = "MessengerLog";
    private static final String KNOT = "KnotFiles";

    private ServiceStateNbt() {
    }

    /** Writes what a messenger is keeping, or nothing at all when it is keeping nothing. */
    static void saveMessenger(final CompoundTag tag, final MessengerLog log) {
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
        tag.put(MESSENGER, list);
    }

    /** Reads it back, replacing whatever the log held. */
    static void loadMessenger(final CompoundTag tag, final MessengerLog log) {
        final ListTag list = tag.getList(MESSENGER, Tag.TAG_COMPOUND);
        final List<MessengerLog.Message> kept = new ArrayList<>(list.size());
        for (int i = 0; i < list.size(); i++) {
            final CompoundTag entry = list.getCompound(i);
            final String from = entry.getString("From");
            final String room = entry.getString("Room");
            if (from.isBlank() || room.isBlank()) {
                continue;
            }
            kept.add(new MessengerLog.Message(room, from, entry.getString("Text"),
                    entry.getLong("At"), entry.getBoolean("Nudge")));
        }
        log.restore(kept);
    }

    /** Writes what a repository is keeping, or nothing at all when it is keeping nothing. */
    static void saveKnot(final CompoundTag tag, final KnotRepository repository) {
        final List<String> files = repository.files();
        if (files.isEmpty()) {
            return;
        }
        final ListTag list = new ListTag();
        for (final String file : files) {
            final CompoundTag entry = new CompoundTag();
            entry.putString("File", file);
            final ListTag revisions = new ListTag();
            for (final KnotRepository.Revision revision : repository.revisionsOf(file)) {
                final CompoundTag one = new CompoundTag();
                one.putInt("N", revision.number());
                one.putString("By", revision.author());
                one.putString("Msg", revision.message());
                one.putLong("At", revision.at());
                one.putString("Body", revision.content());
                revisions.add(one);
            }
            entry.put("Revisions", revisions);
            list.add(entry);
        }
        tag.put(KNOT, list);
    }

    /** Reads it back into a fresh repository, which is what the caller then holds. */
    static void loadKnot(final CompoundTag tag, final KnotRepository repository) {
        final ListTag list = tag.getList(KNOT, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            final CompoundTag entry = list.getCompound(i);
            final String file = entry.getString("File");
            if (file.isBlank()) {
                continue;
            }
            final ListTag revisions = entry.getList("Revisions", Tag.TAG_COMPOUND);
            final List<KnotRepository.Revision> kept = new ArrayList<>(revisions.size());
            for (int r = 0; r < revisions.size(); r++) {
                final CompoundTag one = revisions.getCompound(r);
                final int number = one.getInt("N");
                final String author = one.getString("By");
                if (number < 1 || author.isBlank()) {
                    continue;
                }
                kept.add(new KnotRepository.Revision(number, author, one.getString("Msg"),
                        one.getLong("At"), one.getString("Body")));
            }
            repository.restore(file, kept);
        }
    }
}
