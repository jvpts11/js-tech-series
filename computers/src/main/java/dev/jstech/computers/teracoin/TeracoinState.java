/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.teracoin;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * The world's Teracoin chain, saved as the world's state: one for the whole server, whatever dimension its miners and
 * wallets are in, so it lives with the overworld's data.
 */
public final class TeracoinState extends SavedData {

    private final TeracoinChain chain = new TeracoinChain();

    private static final String FILE = "jsc_teracoin";

    public static TeracoinState of(final MinecraftServer server) {
        final TeracoinState state = server.overworld().getDataStorage().computeIfAbsent(factory(), FILE);
        state.chain.useRules(TeracoinSettings.rules());
        return state;
    }

    public static SavedData.Factory<TeracoinState> factory() {
        return new SavedData.Factory<>(TeracoinState::new, TeracoinState::load);
    }

    /** The chain. Whoever changes it marks it changed ({@link #setDirty()}), so it is saved. */
    public TeracoinChain chain() {
        return chain;
    }

    @Override
    public CompoundTag save(final CompoundTag tag, final HolderLookup.Provider registries) {
        tag.put("Chain", TeracoinNbt.save(chain));
        return tag;
    }

    private static TeracoinState load(final CompoundTag tag, final HolderLookup.Provider registries) {
        final TeracoinState state = new TeracoinState();
        TeracoinNbt.load(state.chain, tag.getCompound("Chain"));
        return state;
    }
}
