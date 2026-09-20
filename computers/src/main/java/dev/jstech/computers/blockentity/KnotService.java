/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.program.KnotRepository;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * KnotHub as a machine holds it: the source a network is keeping while it is still being argued over.
 *
 * <p>One repository per network, which is the simplest thing that is still useful: a base has one body of
 * code on it, and several would only ask a player to choose between them before they had a reason to.
 *
 * <p>Like the Messenger it costs what it really keeps, so a history of fifty revisions is fifty revisions
 * of disk space and the player can see it on the machine.
 */
final class KnotService {

    /** The name the one repository goes by until anybody has a reason to want another. */
    private static final String DEFAULT_NAME = "main";

    private final MainframeBlockEntity mainframe;
    private KnotRepository repository = new KnotRepository(DEFAULT_NAME);

    private boolean installed;

    KnotService(final MainframeBlockEntity mainframe) {
        this.mainframe = mainframe;
    }

    KnotRepository repository() {
        return repository;
    }

    boolean installed() {
        return installed;
    }

    /** Usable only when installed and the machine holding it is actually powered. */
    boolean active() {
        return installed && mainframe.isRunning();
    }

    boolean install() {
        if (installed) {
            return false;
        }
        installed = true;
        mainframe.setChanged();
        return true;
    }

    /** Takes it off, and the history with it: source nobody can reach is not kept. */
    boolean uninstall() {
        if (!installed) {
            return false;
        }
        installed = false;
        repository = new KnotRepository(DEFAULT_NAME);
        mainframe.setChanged();
        return true;
    }

    /** Saves a file as it stands; answers the revision made, or null when nothing changed. */
    KnotRepository.Revision commit(final String file, final String author, final String message,
                                   final String content, final long at) {
        if (!active()) {
            return null;
        }
        final KnotRepository.Revision revision = repository.commit(file, author, message, content, at);
        if (revision != null) {
            mainframe.setChanged();
        }
        return revision;
    }

    void save(final CompoundTag tag) {
        tag.putBoolean("KnotInstalled", installed);
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
        tag.put("KnotFiles", list);
    }

    void load(final CompoundTag tag) {
        installed = tag.getBoolean("KnotInstalled");
        repository = new KnotRepository(DEFAULT_NAME);
        final ListTag list = tag.getList("KnotFiles", Tag.TAG_COMPOUND);
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
                    continue; // a revision edited into nonsense is dropped rather than crashing the load
                }
                kept.add(new KnotRepository.Revision(number, author, one.getString("Msg"),
                        one.getLong("At"), one.getString("Body")));
            }
            repository.restore(file, kept);
        }
    }
}
