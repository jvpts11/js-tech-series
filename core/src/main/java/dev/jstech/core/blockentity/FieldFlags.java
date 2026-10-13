/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.blockentity;

/**
 * Where one field goes: into the save, to the players who see the block, to the menu. A field is declared with its
 * destinations once, as the block entity is built, and they never change after the fields are first used.
 */
final class FieldFlags {

    private final BlockEntityFields owner;
    private final String key;
    private boolean saved;
    private boolean client;
    private boolean menu;

    FieldFlags(final BlockEntityFields owner, final String key) {
        this.owner = owner;
        this.key = key;
    }

    String key() {
        return key;
    }

    boolean saved() {
        return saved;
    }

    boolean client() {
        return client;
    }

    boolean menu() {
        return menu;
    }

    void save() {
        owner.checkOpen(key);
        saved = true;
    }

    void toClient() {
        owner.checkOpen(key);
        client = true;
    }

    void toMenu() {
        owner.checkOpen(key);
        menu = true;
    }

    /** The field's value changed: the save is marked changed and the players who see the block are sent it. */
    void changed() {
        owner.changed(this);
    }
}
