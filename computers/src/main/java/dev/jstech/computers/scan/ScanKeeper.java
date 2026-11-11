/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.scan;

import dev.jstech.computers.os.fs.ScanSeal;
import java.security.SecureRandom;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * The world's key for sealing its scans, kept with the world and made the first time the world runs. While the world
 * runs it is handed to {@link ScanSeal}, where every disk checks a scan against it; it never leaves the server.
 */
public final class ScanKeeper extends SavedData {

    private final byte[] key;

    private static final String FILE = "jsc_scan_seal";
    private static final String KEY = "Key";
    private static final int KEY_BYTES = 32;

    private ScanKeeper(final byte[] key) {
        this.key = key.clone();
    }

    /** Hands the game's bus what this answers, once, from the mod's constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ServerStartedEvent.class, event -> start(event.getServer()));
        NeoForge.EVENT_BUS.addListener(ServerStoppedEvent.class, event -> ScanSeal.use(null));
    }

    /** Hands the world's key over, making it the first time; also what a test's server does by starting. */
    public static void start(final MinecraftServer server) {
        ScanSeal.use(server.overworld().getDataStorage().computeIfAbsent(factory(), FILE).key);
    }

    @Override
    public CompoundTag save(final CompoundTag tag, final HolderLookup.Provider registries) {
        tag.putByteArray(KEY, key);
        return tag;
    }

    private static SavedData.Factory<ScanKeeper> factory() {
        return new SavedData.Factory<>(ScanKeeper::made, ScanKeeper::load);
    }

    /* A world's first key, kept at once so the next start reads the same one. */
    private static ScanKeeper made() {
        final byte[] key = new byte[KEY_BYTES];
        new SecureRandom().nextBytes(key);
        final ScanKeeper keeper = new ScanKeeper(key);
        keeper.setDirty();
        return keeper;
    }

    private static ScanKeeper load(final CompoundTag tag, final HolderLookup.Provider registries) {
        final byte[] key = tag.getByteArray(KEY);
        return key.length == KEY_BYTES ? new ScanKeeper(key) : made();
    }
}
